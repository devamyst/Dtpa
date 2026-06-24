package net.devamy.dtpa.storage;

import net.devamy.dtpa.DTPA;
import net.devamy.dtpa.tpa.model.TpaRequest;
import net.devamy.dtpa.tpa.model.TpaType;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EmbeddedStorage implements Storage {

    private final DTPA plugin;
    private final String jdbcUrl;
    private final boolean isSQLite;
    private Connection writeConnection;
    private Connection readConnection;

    public EmbeddedStorage(DTPA plugin, String jdbcUrl) {
        this.plugin = plugin;
        this.jdbcUrl = jdbcUrl;
        this.isSQLite = jdbcUrl.startsWith("jdbc:sqlite");
    }

    @Override
    public void init() throws Exception {
        writeConnection = DriverManager.getConnection(jdbcUrl);
        readConnection = DriverManager.getConnection(jdbcUrl);
        try (Statement stmt = writeConnection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA synchronous=NORMAL");
        } catch (SQLException ignored) { } // H2 doesn't have PRAGMA
        try (Statement stmt = readConnection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA synchronous=NORMAL");
        } catch (SQLException ignored) { }
        createTables();
        cleanupExpired(System.currentTimeMillis());
    }

    private void createTables() throws SQLException {
        try (Statement stmt = writeConnection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS tpa_requests ("
                    + "sender_uuid VARCHAR(36) NOT NULL,"
                    + "receiver_uuid VARCHAR(36) NOT NULL,"
                    + "sender_name VARCHAR(16) NOT NULL,"
                    + "receiver_name VARCHAR(16) NOT NULL,"
                    + "type VARCHAR(10) NOT NULL,"
                    + "created_at BIGINT NOT NULL,"
                    + "expires_at BIGINT DEFAULT 0,"
                    + "PRIMARY KEY (sender_uuid, receiver_uuid))");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_tpa_sender ON tpa_requests(sender_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_tpa_receiver ON tpa_requests(receiver_uuid)");

            stmt.execute("CREATE TABLE IF NOT EXISTS player_toggles ("
                    + "player_uuid VARCHAR(36) PRIMARY KEY,"
                    + "tpa_disabled BOOLEAN DEFAULT FALSE,"
                    + "tpahere_disabled BOOLEAN DEFAULT FALSE,"
                    + "auto_accept BOOLEAN DEFAULT FALSE,"
                    + "confirm_disabled BOOLEAN DEFAULT FALSE)");

            stmt.execute("CREATE TABLE IF NOT EXISTS cooldowns ("
                    + "cooldown_key VARCHAR(100) PRIMARY KEY,"
                    + "end_time BIGINT NOT NULL)");

            stmt.execute("CREATE TABLE IF NOT EXISTS blocklist ("
                    + "blocker_uuid VARCHAR(36) NOT NULL,"
                    + "blocked_uuid VARCHAR(36) NOT NULL,"
                    + "PRIMARY KEY (blocker_uuid, blocked_uuid))");

            String autoInc = isSQLite ? "INTEGER PRIMARY KEY AUTOINCREMENT" : "INTEGER AUTO_INCREMENT PRIMARY KEY";
            stmt.execute("CREATE TABLE IF NOT EXISTS audit_log ("
                    + "id " + autoInc + ","
                    + "action VARCHAR(32) NOT NULL,"
                    + "sender_uuid VARCHAR(36),"
                    + "receiver_uuid VARCHAR(36),"
                    + "sender_name VARCHAR(16),"
                    + "receiver_name VARCHAR(16),"
                    + "details VARCHAR(256),"
                    + "timestamp BIGINT NOT NULL)");
        }
    }

    @Override
    public void close() {
        if (writeConnection != null) {
            try { writeConnection.close(); } catch (SQLException ignored) { }
        }
        if (readConnection != null) {
            try { readConnection.close(); } catch (SQLException ignored) { }
        }
    }

    private Connection wConn() { return writeConnection; }
    private Connection rConn() { return readConnection; }

    // ─── Requests ─────────────────────────────────────────────────────────────

    @Override
    public synchronized void saveRequest(TpaRequest request) {
        String sql = isSQLite
                ? "INSERT OR REPLACE INTO tpa_requests(sender_uuid, receiver_uuid, sender_name, receiver_name, type, created_at, expires_at) VALUES(?,?,?,?,?,?,?)"
                : "MERGE INTO tpa_requests(sender_uuid, receiver_uuid, sender_name, receiver_name, type, created_at, expires_at) KEY(sender_uuid, receiver_uuid) VALUES(?,?,?,?,?,?,?)";
        try (PreparedStatement ps = wConn().prepareStatement(sql)) {
            ps.setString(1, request.getSenderUuid().toString());
            ps.setString(2, request.getReceiverUuid().toString());
            ps.setString(3, request.getSenderName());
            ps.setString(4, request.getReceiverName());
            ps.setString(5, request.getType().name());
            ps.setLong(6, request.getCreatedAt());
            ps.setLong(7, request.getExpiresAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save request: " + e.getMessage());
        }
    }

    @Override
    public synchronized void removeRequest(UUID senderUuid, UUID receiverUuid) {
        try (PreparedStatement ps = wConn().prepareStatement(
                "DELETE FROM tpa_requests WHERE sender_uuid=? AND receiver_uuid=?")) {
            ps.setString(1, senderUuid.toString());
            ps.setString(2, receiverUuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to remove request: " + e.getMessage());
        }
    }

    @Override
    public synchronized TpaRequest removeAndGetRequest(UUID senderUuid, UUID receiverUuid) {
        String sql = "SELECT * FROM tpa_requests WHERE sender_uuid=? AND receiver_uuid=? AND (expires_at=0 OR expires_at>?)";
        try (PreparedStatement ps = wConn().prepareStatement(sql)) {
            ps.setString(1, senderUuid.toString());
            ps.setString(2, receiverUuid.toString());
            ps.setLong(3, System.currentTimeMillis());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    TpaRequest req = mapRequest(rs);
                    removeRequest(senderUuid, receiverUuid);
                    return req;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to remove and get request: " + e.getMessage());
        }
        return null;
    }

    @Override
    public TpaRequest getRequest(UUID senderUuid, UUID receiverUuid) {
        String sql = "SELECT * FROM tpa_requests WHERE sender_uuid=? AND receiver_uuid=? AND (expires_at=0 OR expires_at>?)";
        try (PreparedStatement ps = rConn().prepareStatement(sql)) {
            ps.setString(1, senderUuid.toString());
            ps.setString(2, receiverUuid.toString());
            ps.setLong(3, System.currentTimeMillis());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRequest(rs);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get request: " + e.getMessage());
        }
        return null;
    }

    @Override
    public Map<String, TpaRequest> getAllRequests() {
        Map<String, TpaRequest> map = new ConcurrentHashMap<>();
        long now = System.currentTimeMillis();
        try (Statement stmt = rConn().createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM tpa_requests WHERE expires_at=0 OR expires_at>" + now)) {
            while (rs.next()) {
                TpaRequest req = mapRequest(rs);
                map.put(req.getSenderUuid() + ":" + req.getReceiverUuid(), req);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load all requests: " + e.getMessage());
        }
        return map;
    }

    @Override
    public List<TpaRequest> getRequestsBySender(UUID sender) {
        List<TpaRequest> list = new ArrayList<>();
        long now = System.currentTimeMillis();
        try (PreparedStatement ps = rConn().prepareStatement(
                "SELECT * FROM tpa_requests WHERE sender_uuid=? AND (expires_at=0 OR expires_at>?)")) {
            ps.setString(1, sender.toString());
            ps.setLong(2, now);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRequest(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get requests by sender: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<TpaRequest> getRequestsByReceiver(UUID receiver) {
        List<TpaRequest> list = new ArrayList<>();
        long now = System.currentTimeMillis();
        try (PreparedStatement ps = rConn().prepareStatement(
                "SELECT * FROM tpa_requests WHERE receiver_uuid=? AND (expires_at=0 OR expires_at>?)")) {
            ps.setString(1, receiver.toString());
            ps.setLong(2, now);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRequest(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get requests by receiver: " + e.getMessage());
        }
        return list;
    }

    @Override
    public int getRequestCountBySender(UUID sender) {
        try (PreparedStatement ps = rConn().prepareStatement(
                "SELECT COUNT(*) FROM tpa_requests WHERE sender_uuid=? AND (expires_at=0 OR expires_at>?)")) {
            ps.setString(1, sender.toString());
            ps.setLong(2, System.currentTimeMillis());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to count requests: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public synchronized void clearAllRequests() {
        try (Statement stmt = wConn().createStatement()) {
            stmt.execute("DELETE FROM tpa_requests");
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to clear requests: " + e.getMessage());
        }
    }

    @Override
    public synchronized void cleanupExpired(long cutoffEpochMs) {
        try (PreparedStatement ps = wConn().prepareStatement(
                "DELETE FROM tpa_requests WHERE expires_at>0 AND expires_at<=?")) {
            ps.setLong(1, cutoffEpochMs);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to clean expired requests: " + e.getMessage());
        }
    }

    // ─── Toggles ──────────────────────────────────────────────────────────────

    @Override
    public boolean isTpaDisabled(UUID player) {
        return getToggle(player, "tpa_disabled");
    }

    @Override
    public synchronized void setTpaDisabled(UUID player, boolean disabled) {
        setToggle(player, "tpa_disabled", disabled);
    }

    @Override
    public boolean isTpaHereDisabled(UUID player) {
        return getToggle(player, "tpahere_disabled");
    }

    @Override
    public synchronized void setTpaHereDisabled(UUID player, boolean disabled) {
        setToggle(player, "tpahere_disabled", disabled);
    }

    @Override
    public boolean isAutoAccept(UUID player) {
        return getToggle(player, "auto_accept");
    }

    @Override
    public synchronized void setAutoAccept(UUID player, boolean enabled) {
        setToggle(player, "auto_accept", enabled);
    }

    @Override
    public boolean isConfirmDisabled(UUID player) {
        return getToggle(player, "confirm_disabled");
    }

    @Override
    public synchronized void setConfirmDisabled(UUID player, boolean disabled) {
        setToggle(player, "confirm_disabled", disabled);
    }

    private boolean getToggle(UUID player, String column) {
        try (PreparedStatement ps = rConn().prepareStatement(
                "SELECT " + column + " FROM player_toggles WHERE player_uuid=?")) {
            ps.setString(1, player.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBoolean(column);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get toggle " + column + ": " + e.getMessage());
        }
        return false;
    }

    private void setToggle(UUID player, String column, boolean value) {
        String sql = isSQLite
                ? "INSERT INTO player_toggles(player_uuid, " + column + ") VALUES(?,?) ON CONFLICT(player_uuid) DO UPDATE SET " + column + "=?"
                : "MERGE INTO player_toggles(player_uuid, " + column + ") KEY(player_uuid) VALUES(?,?,?)";
        try (PreparedStatement ps = wConn().prepareStatement(sql)) {
            ps.setString(1, player.toString());
            ps.setBoolean(2, value);
            ps.setBoolean(3, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to set toggle " + column + ": " + e.getMessage());
        }
    }

    // ─── Cooldowns ────────────────────────────────────────────────────────────

    @Override
    public Long getCooldown(String key) {
        try (PreparedStatement ps = rConn().prepareStatement(
                "SELECT end_time FROM cooldowns WHERE cooldown_key=?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("end_time");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get cooldown: " + e.getMessage());
        }
        return null;
    }

    @Override
    public synchronized void setCooldown(String key, long endTime) {
        String sql = isSQLite
                ? "INSERT INTO cooldowns(cooldown_key, end_time) VALUES(?,?) ON CONFLICT(cooldown_key) DO UPDATE SET end_time=?"
                : "MERGE INTO cooldowns(cooldown_key, end_time) KEY(cooldown_key) VALUES(?,?,?)";
        try (PreparedStatement ps = wConn().prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setLong(2, endTime);
            ps.setLong(3, endTime);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to set cooldown: " + e.getMessage());
        }
    }

    @Override
    public synchronized void removeCooldown(String key) {
        try (PreparedStatement ps = wConn().prepareStatement(
                "DELETE FROM cooldowns WHERE cooldown_key=?")) {
            ps.setString(1, key);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to remove cooldown: " + e.getMessage());
        }
    }

    // ─── Blocklist ────────────────────────────────────────────────────────────

    @Override
    public synchronized void blockPlayer(UUID blocker, UUID blocked) {
        String sql = isSQLite
                ? "INSERT OR IGNORE INTO blocklist(blocker_uuid, blocked_uuid) VALUES(?,?)"
                : "MERGE INTO blocklist(blocker_uuid, blocked_uuid) KEY(blocker_uuid, blocked_uuid) VALUES(?,?)";
        try (PreparedStatement ps = wConn().prepareStatement(sql)) {
            ps.setString(1, blocker.toString());
            ps.setString(2, blocked.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to block player: " + e.getMessage());
        }
    }

    @Override
    public synchronized void unblockPlayer(UUID blocker, UUID blocked) {
        try (PreparedStatement ps = wConn().prepareStatement(
                "DELETE FROM blocklist WHERE blocker_uuid=? AND blocked_uuid=?")) {
            ps.setString(1, blocker.toString());
            ps.setString(2, blocked.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to unblock player: " + e.getMessage());
        }
    }

    @Override
    public boolean isPlayerBlocked(UUID blocker, UUID blocked) {
        try (PreparedStatement ps = rConn().prepareStatement(
                "SELECT 1 FROM blocklist WHERE blocker_uuid=? AND blocked_uuid=?")) {
            ps.setString(1, blocker.toString());
            ps.setString(2, blocked.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to check blocklist: " + e.getMessage());
        }
        return false;
    }

    @Override
    public Set<UUID> getBlockedPlayers(UUID player) {
        Set<UUID> set = new HashSet<>();
        try (PreparedStatement ps = rConn().prepareStatement(
                "SELECT blocked_uuid FROM blocklist WHERE blocker_uuid=?")) {
            ps.setString(1, player.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    set.add(UUID.fromString(rs.getString("blocked_uuid")));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get blocked players: " + e.getMessage());
        }
        return set;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private TpaRequest mapRequest(ResultSet rs) throws SQLException {
        return new TpaRequest(
                UUID.fromString(rs.getString("sender_uuid")),
                UUID.fromString(rs.getString("receiver_uuid")),
                rs.getString("sender_name"),
                rs.getString("receiver_name"),
                TpaType.valueOf(rs.getString("type")),
                rs.getLong("created_at"),
                rs.getLong("expires_at"));
    }
}
