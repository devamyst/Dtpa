package net.trickycreations.trickytpa.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.trickycreations.trickytpa.TrickyTPA;
import net.trickycreations.trickytpa.tpa.model.TpaRequest;
import net.trickycreations.trickytpa.tpa.model.TpaType;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MySQLStorage implements Storage {

    private final TrickyTPA plugin;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final boolean useSSL;
    private final int poolSize;
    private final int timeout;
    private final String prefix;

    private DataSource dataSource;

    // Requests are cached in RAM for MySQL (fast reads)
    private final Map<String, TpaRequest> requestsCache = new ConcurrentHashMap<>();

    public MySQLStorage(TrickyTPA plugin, String host, int port, String database,
                        String username, String password, boolean useSSL,
                        int poolSize, int timeout, String prefix) {
        this.plugin = plugin;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.useSSL = useSSL;
        this.poolSize = poolSize;
        this.timeout = timeout;
        this.prefix = prefix;
    }

    @Override
    public void init() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=" + useSSL + "&serverTimezone=UTC");
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(poolSize);
        config.setConnectionTimeout(timeout);
        config.setMinimumIdle(Math.max(1, poolSize / 4));
        config.setPoolName("TrickyTPA-MySQL");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "50");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        dataSource = new HikariDataSource(config);
        createTables();
        loadCache();
        cleanupExpired(System.currentTimeMillis());
    }

    private Connection conn() throws SQLException {
        return dataSource.getConnection();
    }

    private String t(String table) { return prefix + table; }

    private void createTables() throws SQLException {
        try (Connection c = conn(); Statement stmt = c.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS " + t("tpa_requests") + " ("
                    + "sender_uuid VARCHAR(36) NOT NULL,"
                    + "receiver_uuid VARCHAR(36) NOT NULL,"
                    + "sender_name VARCHAR(16) NOT NULL,"
                    + "receiver_name VARCHAR(16) NOT NULL,"
                    + "type VARCHAR(10) NOT NULL,"
                    + "created_at BIGINT NOT NULL,"
                    + "expires_at BIGINT DEFAULT 0,"
                    + "PRIMARY KEY (sender_uuid, receiver_uuid),"
                    + "INDEX idx_sender (sender_uuid),"
                    + "INDEX idx_receiver (receiver_uuid)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            stmt.execute("CREATE TABLE IF NOT EXISTS " + t("player_toggles") + " ("
                    + "player_uuid VARCHAR(36) PRIMARY KEY,"
                    + "tpa_disabled BOOLEAN DEFAULT FALSE,"
                    + "tpahere_disabled BOOLEAN DEFAULT FALSE,"
                    + "auto_accept BOOLEAN DEFAULT FALSE,"
                    + "confirm_disabled BOOLEAN DEFAULT FALSE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            stmt.execute("CREATE TABLE IF NOT EXISTS " + t("cooldowns") + " ("
                    + "cooldown_key VARCHAR(100) PRIMARY KEY,"
                    + "end_time BIGINT NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            stmt.execute("CREATE TABLE IF NOT EXISTS " + t("blocklist") + " ("
                    + "blocker_uuid VARCHAR(36) NOT NULL,"
                    + "blocked_uuid VARCHAR(36) NOT NULL,"
                    + "PRIMARY KEY (blocker_uuid, blocked_uuid),"
                    + "INDEX idx_blocker (blocker_uuid)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            stmt.execute("CREATE TABLE IF NOT EXISTS " + t("audit_log") + " ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                    + "action VARCHAR(32) NOT NULL,"
                    + "sender_uuid VARCHAR(36),"
                    + "receiver_uuid VARCHAR(36),"
                    + "sender_name VARCHAR(16),"
                    + "receiver_name VARCHAR(16),"
                    + "details VARCHAR(256),"
                    + "timestamp BIGINT NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }
    }

    private void loadCache() {
        requestsCache.clear();
        long now = System.currentTimeMillis();
        String sql = "SELECT * FROM " + t("tpa_requests") + " WHERE expires_at=0 OR expires_at>?";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, now);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TpaRequest req = mapRequest(rs);
                    requestsCache.put(req.getSenderUuid() + ":" + req.getReceiverUuid(), req);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load request cache: " + e.getMessage());
        }
        plugin.getLogger().info("Loaded " + requestsCache.size() + " active requests from MySQL.");
    }

    @Override
    public void close() {
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
        }
        requestsCache.clear();
    }

    // ─── Requests (cache + DB) ────────────────────────────────────────────────

    @Override
    public void saveRequest(TpaRequest request) {
        String key = request.getSenderUuid() + ":" + request.getReceiverUuid();
        requestsCache.put(key, request);
        String sql = "INSERT INTO " + t("tpa_requests")
                + "(sender_uuid, receiver_uuid, sender_name, receiver_name, type, created_at, expires_at) "
                + "VALUES(?,?,?,?,?,?,?) "
                + "ON DUPLICATE KEY UPDATE sender_name=VALUES(sender_name), receiver_name=VALUES(receiver_name), "
                + "type=VALUES(type), created_at=VALUES(created_at), expires_at=VALUES(expires_at)";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, request.getSenderUuid().toString());
            ps.setString(2, request.getReceiverUuid().toString());
            ps.setString(3, request.getSenderName());
            ps.setString(4, request.getReceiverName());
            ps.setString(5, request.getType().name());
            ps.setLong(6, request.getCreatedAt());
            ps.setLong(7, request.getExpiresAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save request to MySQL: " + e.getMessage());
        }
    }

    @Override
    public void removeRequest(UUID senderUuid, UUID receiverUuid) {
        String key = senderUuid + ":" + receiverUuid;
        requestsCache.remove(key);
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(
                "DELETE FROM " + t("tpa_requests") + " WHERE sender_uuid=? AND receiver_uuid=?")) {
            ps.setString(1, senderUuid.toString());
            ps.setString(2, receiverUuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to remove request: " + e.getMessage());
        }
    }

    @Override
    public TpaRequest getRequest(UUID senderUuid, UUID receiverUuid) {
        String key = senderUuid + ":" + receiverUuid;
        TpaRequest cached = requestsCache.get(key);
        if (cached != null) return cached;
        // Fallback to DB
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM " + t("tpa_requests") + " WHERE sender_uuid=? AND receiver_uuid=? AND (expires_at=0 OR expires_at>?)")) {
            ps.setString(1, senderUuid.toString());
            ps.setString(2, receiverUuid.toString());
            ps.setLong(3, System.currentTimeMillis());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    TpaRequest req = mapRequest(rs);
                    requestsCache.put(key, req);
                    return req;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get request: " + e.getMessage());
        }
        return null;
    }

    @Override
    public Map<String, TpaRequest> getAllRequests() {
        return new ConcurrentHashMap<>(requestsCache);
    }

    @Override
    public List<TpaRequest> getRequestsBySender(UUID sender) {
        List<TpaRequest> list = new ArrayList<>();
        for (TpaRequest req : requestsCache.values()) {
            if (req.getSenderUuid().equals(sender)) list.add(req);
        }
        return list;
    }

    @Override
    public List<TpaRequest> getRequestsByReceiver(UUID receiver) {
        List<TpaRequest> list = new ArrayList<>();
        for (TpaRequest req : requestsCache.values()) {
            if (req.getReceiverUuid().equals(receiver)) list.add(req);
        }
        return list;
    }

    @Override
    public int getRequestCountBySender(UUID sender) {
        int count = 0;
        for (TpaRequest req : requestsCache.values()) {
            if (req.getSenderUuid().equals(sender)) count++;
        }
        return count;
    }

    @Override
    public void clearAllRequests() {
        requestsCache.clear();
        try (Connection c = conn(); Statement stmt = c.createStatement()) {
            stmt.execute("DELETE FROM " + t("tpa_requests"));
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to clear requests: " + e.getMessage());
        }
    }

    @Override
    public void cleanupExpired(long cutoffEpochMs) {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(
                "DELETE FROM " + t("tpa_requests") + " WHERE expires_at>0 AND expires_at<=?")) {
            ps.setLong(1, cutoffEpochMs);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to clean expired requests: " + e.getMessage());
        }
        requestsCache.entrySet().removeIf(e -> {
            long exp = e.getValue().getExpiresAt();
            return exp > 0 && exp <= cutoffEpochMs;
        });
    }

    // ─── Toggles ──────────────────────────────────────────────────────────────

    @Override
    public boolean isTpaDisabled(UUID player) { return getToggle(player, "tpa_disabled"); }

    @Override
    public void setTpaDisabled(UUID player, boolean disabled) { setToggle(player, "tpa_disabled", disabled); }

    @Override
    public boolean isTpaHereDisabled(UUID player) { return getToggle(player, "tpahere_disabled"); }

    @Override
    public void setTpaHereDisabled(UUID player, boolean disabled) { setToggle(player, "tpahere_disabled", disabled); }

    @Override
    public boolean isAutoAccept(UUID player) { return getToggle(player, "auto_accept"); }

    @Override
    public void setAutoAccept(UUID player, boolean enabled) { setToggle(player, "auto_accept", enabled); }

    @Override
    public boolean isConfirmDisabled(UUID player) { return getToggle(player, "confirm_disabled"); }

    @Override
    public void setConfirmDisabled(UUID player, boolean disabled) { setToggle(player, "confirm_disabled", disabled); }

    private boolean getToggle(UUID player, String column) {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(
                "SELECT " + column + " FROM " + t("player_toggles") + " WHERE player_uuid=?")) {
            ps.setString(1, player.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBoolean(column);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get toggle: " + e.getMessage());
        }
        return false;
    }

    private void setToggle(UUID player, String column, boolean value) {
        String sql = "INSERT INTO " + t("player_toggles") + "(player_uuid, " + column + ") VALUES(?,?) "
                + "ON DUPLICATE KEY UPDATE " + column + "=VALUES(" + column + ")";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, player.toString());
            ps.setBoolean(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to set toggle: " + e.getMessage());
        }
    }

    // ─── Cooldowns ────────────────────────────────────────────────────────────

    @Override
    public Long getCooldown(String key) {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(
                "SELECT end_time FROM " + t("cooldowns") + " WHERE cooldown_key=?")) {
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
    public void setCooldown(String key, long endTime) {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO " + t("cooldowns") + "(cooldown_key, end_time) VALUES(?,?) "
                        + "ON DUPLICATE KEY UPDATE end_time=VALUES(end_time)")) {
            ps.setString(1, key);
            ps.setLong(2, endTime);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to set cooldown: " + e.getMessage());
        }
    }

    @Override
    public void removeCooldown(String key) {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(
                "DELETE FROM " + t("cooldowns") + " WHERE cooldown_key=?")) {
            ps.setString(1, key);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to remove cooldown: " + e.getMessage());
        }
    }

    // ─── Blocklist ────────────────────────────────────────────────────────────

    @Override
    public void blockPlayer(UUID blocker, UUID blocked) {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(
                "INSERT IGNORE INTO " + t("blocklist") + "(blocker_uuid, blocked_uuid) VALUES(?,?)")) {
            ps.setString(1, blocker.toString());
            ps.setString(2, blocked.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to block player: " + e.getMessage());
        }
    }

    @Override
    public void unblockPlayer(UUID blocker, UUID blocked) {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(
                "DELETE FROM " + t("blocklist") + " WHERE blocker_uuid=? AND blocked_uuid=?")) {
            ps.setString(1, blocker.toString());
            ps.setString(2, blocked.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to unblock player: " + e.getMessage());
        }
    }

    @Override
    public boolean isPlayerBlocked(UUID blocker, UUID blocked) {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM " + t("blocklist") + " WHERE blocker_uuid=? AND blocked_uuid=?")) {
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
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(
                "SELECT blocked_uuid FROM " + t("blocklist") + " WHERE blocker_uuid=?")) {
            ps.setString(1, player.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) set.add(UUID.fromString(rs.getString("blocked_uuid")));
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
