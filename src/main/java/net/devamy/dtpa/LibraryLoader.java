package net.devamy.dtpa;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

public final class LibraryLoader {

    private static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2";

    private static final List<Library> LIBRARIES = List.of(
        new Library("H2",             "com/h2database/h2/2.2.224/h2-2.2.224.jar",                               "org.h2.Driver"),
        new Library("MySQL Connector","com/mysql/mysql-connector-j/8.2.0/mysql-connector-j-8.2.0.jar",          "com.mysql.cj.jdbc.Driver"),
        new Library("SQLite JDBC",    "org/xerial/sqlite-jdbc/3.45.1.0/sqlite-jdbc-3.45.1.0.jar",              "org.sqlite.JDBC")
    );

    private final File libDir;
    private final Logger logger;

    public LibraryLoader(File dataFolder, Logger logger) {
        this.libDir = new File(dataFolder, "libs");
        this.logger = logger;
    }

    public void load() throws Exception {
        libDir.mkdirs();

        for (Library lib : LIBRARIES) {
            File jarFile = new File(libDir, lib.fileName());
            if (!jarFile.exists()) {
                download(lib, jarFile);
            }
        }

        URL[] urls = LIBRARIES.stream()
            .map(lib -> new File(libDir, lib.fileName()))
            .filter(File::exists)
            .map(f -> {
                try { return f.toURI().toURL(); }
                catch (Exception e) { return null; }
            })
            .filter(u -> u != null)
            .toArray(URL[]::new);

        if (urls.length == 0) return;

        URLClassLoader driverLoader = new URLClassLoader(urls, getClass().getClassLoader());
        for (Library lib : LIBRARIES) {
            loadDriver(lib, driverLoader);
        }
    }

    private void download(Library lib, File target) throws Exception {
        URL url = new URL(MAVEN_CENTRAL + "/" + lib.mavenPath());
        logger.info("Downloading " + lib.name() + " from Maven Central...");
        try (InputStream in = url.openStream()) {
            Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        logger.info("Downloaded " + lib.name() + " → " + target.getName());
    }

    private void loadDriver(Library lib, URLClassLoader loader) {
        String dc = lib.driverClass();
        if (dc == null) return;
        try {
            Class<?> clazz = Class.forName(dc, true, loader);
            if (clazz != null && Driver.class.isAssignableFrom(clazz)) {
                Driver driver = (Driver) clazz.getDeclaredConstructor().newInstance();
                DriverManager.registerDriver(new DriverShim(driver));
                logger.info("Registered JDBC driver: " + dc);
            }
        } catch (Exception e) {
            logger.warning("Failed to load JDBC driver " + dc + ": " + e.getMessage());
        }
    }

    private record Library(String name, String mavenPath, String driverClass) {
        String fileName() {
            return mavenPath.substring(mavenPath.lastIndexOf('/') + 1);
        }
    }

    private static class DriverShim implements Driver {
        private final Driver delegate;

        DriverShim(Driver delegate) {
            this.delegate = delegate;
        }

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            return delegate.connect(url, info);
        }

        @Override
        public boolean acceptsURL(String url) throws SQLException {
            return delegate.acceptsURL(url);
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
            return delegate.getPropertyInfo(url, info);
        }

        @Override
        public int getMajorVersion() {
            return delegate.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {
            return delegate.getMinorVersion();
        }

        @Override
        public boolean jdbcCompliant() {
            return delegate.jdbcCompliant();
        }

        @Override
        public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return delegate.getParentLogger();
        }
    }
}
