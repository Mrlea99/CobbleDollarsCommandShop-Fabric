package com.mrlea99.cobbledollarscommandshop.runtime;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;

public class AuditLog {
    private final Path auditFile;

    public AuditLog(Path auditFile) {
        this.auditFile = auditFile;
    }

    public synchronized void write(String actor, String action, String details) {
        String line = "%s actor=%s action=%s details=%s%n".formatted(
                OffsetDateTime.now(), actor, action, details.replace('\n', ' ')
        );
        try {
            Files.createDirectories(auditFile.getParent());
            Files.writeString(
                    auditFile,
                    line,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException ignored) {
        }
    }
}
