package dev.mfgdiff;

import java.util.Locale;

/** xmldiff ratio_mode: "fast" is difflib quick_ratio, "accurate" is difflib ratio. */
public enum RatioMode {
    FAST, ACCURATE;

    public double ratio(String a, String b) {
        return this == FAST ? QuickRatio.ratio(a, b) : AccurateRatio.ratio(a, b);
    }

    public static RatioMode parse(String s) {
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "fast" -> FAST;
            case "accurate" -> ACCURATE;
            default -> throw new IllegalArgumentException("unknown ratio mode '" + s + "' (fast, accurate)");
        };
    }

    public String label() {
        return name().toLowerCase(Locale.ROOT);
    }
}
