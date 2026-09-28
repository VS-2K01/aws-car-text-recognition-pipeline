package com.cs643.util;
// Import Statement
import java.util.Comparator;

// Sorts keys "1.jpg","2.jpg","10.jpg" numerically
public final class KeySort {
    private KeySort(){}
    // Function To Compare Two Numbers
    public static Comparator<String> numericJpgs() {
        return (a, b) -> Integer.compare(num(a), num(b));
    }

    // Get Number In File Name
    private static int num(String key) {
        String[] parts = key.split("/");
        String last = parts[parts.length - 1];
        String n = last.replace(".jpg", "").replaceAll("[^0-9]", "");
        try { return Integer.parseInt(n); } catch (Exception e) { return Integer.MAX_VALUE; }
    }
}
