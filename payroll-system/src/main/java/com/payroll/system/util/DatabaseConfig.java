package com.payroll.system.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class DatabaseConfig {

    private static final String DB_FILE_NAME = "hrms.db";

    private DatabaseConfig() {
    }

    public static String getJdbcUrl() {
        return "jdbc:sqlite:" + resolveDatabasePath();
    }

    private static Path resolveDatabasePath() {
        String override = System.getProperty("aether.db.path");
        if (override != null && !override.isBlank()) {
            return Path.of(override).toAbsolutePath().normalize();
        }

        Path cwd = Path.of("").toAbsolutePath().normalize();
        Path parent = cwd.getParent();
        Path grandParent = parent == null ? null : parent.getParent();

        List<Path> candidates = List.of(
                cwd.resolve(DB_FILE_NAME),
                cwd.resolve("payroll-system").resolve(DB_FILE_NAME),
                parent == null ? cwd.resolve(DB_FILE_NAME) : parent.resolve(DB_FILE_NAME),
                parent == null ? cwd.resolve(DB_FILE_NAME) : parent.resolve("payroll-system").resolve(DB_FILE_NAME),
                grandParent == null ? cwd.resolve(DB_FILE_NAME) : grandParent.resolve("payroll-system-local").resolve(DB_FILE_NAME),
                grandParent == null ? cwd.resolve(DB_FILE_NAME) : grandParent.resolve("payroll-system").resolve("payroll-system").resolve(DB_FILE_NAME));

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }

        return cwd.resolve(DB_FILE_NAME).toAbsolutePath().normalize();
    }
}
