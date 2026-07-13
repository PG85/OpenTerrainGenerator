package com.pg85.otg.shared.i18n;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime translation map for dynamically registered OTG translation keys.
 * Populated during world preset registration, consumed by {@code LanguageMixin}.
 * Presets are user content, so their display names cannot ship in a static
 * lang file.
 */
public final class OTGTranslations {

    private static final ConcurrentHashMap<String, String> translations = new ConcurrentHashMap<>();

    private OTGTranslations() {}

    public static void put(String key, String value) {
        translations.put(key, value);
    }

    public static String get(String key) {
        return translations.get(key);
    }

    public static boolean has(String key) {
        return translations.containsKey(key);
    }
}
