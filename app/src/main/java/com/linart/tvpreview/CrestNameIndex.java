package com.linart.tvpreview;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Name fallback across leagues is allowed only for an unambiguous club. */
final class CrestNameIndex<T> {
    private final Map<String, T> entries = new HashMap<>();
    private final Set<String> ambiguous = new HashSet<>();
    void add(String name, T value) {
        String key = normalize(name);
        if (key.isEmpty() || ambiguous.contains(key)) return;
        T old = entries.get(key);
        if (old != null && old != value) { entries.remove(key); ambiguous.add(key); }
        else entries.put(key, value);
    }
    T find(String name) { return entries.get(normalize(name)); }
    static String normalize(String name) {
        if (name == null) return "";
        return Normalizer.normalize(name.toLowerCase(Locale.ROOT).replace('ł', 'l'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9]+", " ").trim();
    }
}
