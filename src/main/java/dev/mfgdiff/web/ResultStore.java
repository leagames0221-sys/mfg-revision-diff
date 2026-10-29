package dev.mfgdiff.web;

import dev.mfgdiff.DiffResult;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;

/** Keeps the last few comparisons in memory under a random id, so that the result page and the Excel download can be fetched by GET. */
final class ResultStore {
    /** Comparisons kept before the oldest is dropped (author's decision, docs/sources.md D19). */
    static final int CAPACITY = 16;

    record Stored(DiffResult result, String nameA, String nameB, double f, dev.mfgdiff.RatioMode mode) {
    }

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Stored> map = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Stored> eldest) {
            return size() > CAPACITY;
        }
    };

    synchronized String put(Stored s) {
        byte[] b = new byte[16];
        random.nextBytes(b);
        StringBuilder id = new StringBuilder();
        for (byte x : b) {
            id.append(String.format("%02x", x));
        }
        map.put(id.toString(), s);
        return id.toString();
    }

    synchronized Stored get(String id) {
        return map.get(id);
    }
}
