package net.devamy.dtpa;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.List;
import java.util.logging.Logger;

public final class LibraryLoader {

    private static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2";

    private static final List<Library> LIBRARIES = List.of(
        new Library("HikariCP",       "com/zaxxer/HikariCP/5.1.0/HikariCP-5.1.0.jar",                           "com.zaxxer.hikari.HikariConfig"),
        new Library("H2",             "com/h2database/h2/2.2.224/h2-2.2.224.jar",                               "org.h2.Driver"),
        new Library("MySQL Connector","com/mysql/mysql-connector-j/8.2.0/mysql-connector-j-8.2.0.jar",          "com.mysql.cj.jdbc.Driver"),
        new Library("SQLite JDBC",    "org/xerial/sqlite-jdbc/3.45.1.0/sqlite-jdbc-3.45.1.0.jar",              "org.sqlite.JDBC")
    );

    private final File libDir;
    private final URLClassLoader classLoader;
    private final Logger logger;

    public LibraryLoader(File dataFolder, ClassLoader classLoader, Logger logger) {
        this.libDir = new File(dataFolder, "libs");
        this.classLoader = (URLClassLoader) classLoader;
        this.logger = logger;
    }

    public void load() throws Exception {
        libDir.mkdirs();

        for (Library lib : LIBRARIES) {
            File jarFile = new File(libDir, lib.fileName());
            if (!jarFile.exists()) {
                download(lib, jarFile);
            }
            addToClasspath(jarFile);
            loadDriver(lib);
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

    private void addToClasspath(File jar) throws Exception {
        Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        addURL.setAccessible(true);
        addURL.invoke(classLoader, jar.toURI().toURL());
    }

    private void loadDriver(Library lib) {
        String dc = lib.driverClass();
        if (dc == null) return;
        try {
            Class<?> clazz = Class.forName(dc, true, classLoader);
            if (clazz != null && Driver.class.isAssignableFrom(clazz)) {
                Driver driver = (Driver) clazz.getDeclaredConstructor().newInstance();
                DriverManager.registerDriver(driver);
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
}
