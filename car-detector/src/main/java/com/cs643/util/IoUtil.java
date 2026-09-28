package com.cs643.util;
// Import Statements
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

// File I/O helper functions
public final class IoUtil {
    private IoUtil(){}

    // Check if Path Provided is Valid and Has Parent, if not create
    public static void ensureParent(Path p) {
        try {
            Path parent = p.getParent();
            if (parent != null) Files.createDirectories(parent);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create parent dir: " + p, e);
        }
    }

    // After Confirming Parent Exists (Or Create If Not), Write Content to provided Path
    public static void writeString(Path p, String content) {
        ensureParent(p);
        try {
            Files.writeString(p, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write " + p + ": " + e.getMessage(), e);
        }
    }
}
