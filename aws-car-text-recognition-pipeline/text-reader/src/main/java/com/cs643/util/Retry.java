package com.cs643.util;
// Import Statement
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

// Exponential backoff wrapper to deal with / account for transient AWS errors.
public final class Retry {
    private Retry(){}
    // Retry AWS Function Call, each time it fails, catch exception, wait 200 ms and then retry up to maxAttempts parameter
    public static <T> T withBackoff(String name, int maxAttempts, Supplier<T> op) {
        RuntimeException last = null;
        long backoff = 200L; // ms
        for (int i = 1; i <= maxAttempts; i++) {
            try { return op.get(); }
            catch (RuntimeException ex) {
                last = ex;
                if (i == maxAttempts) break;            // give up after maxAttempts
                sleep(jitter(backoff));                 // wait a bit with jitter
                backoff = Math.min(backoff * 2, 5000L); // cap at 5s
            }
        }
        throw new RuntimeException("Operation failed [" + name + "]: " + last.getMessage(), last);
    }
    // Add ~20% random delay to the base retry delay to prevent synchronized retry spikes.
    private static long jitter(long baseMs) {
        long delta = (long)(baseMs * 0.2);
        return baseMs + ThreadLocalRandom.current().nextLong(-delta, delta + 1);
    }
    // Sleep Thread for X ms
    private static void sleep(long ms){
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
