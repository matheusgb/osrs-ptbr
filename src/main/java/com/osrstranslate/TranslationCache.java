package com.osrstranslate;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Guarda a tradução já calculada de cada texto. O cache se limpa quando os
 * dicionários mudam ou quando passa do limite de entradas.
 */
final class TranslationCache {
    private static final String NO_TRANSLATION = "";

    private final int limit;
    private final Map<String, String> entries = new HashMap<>();
    private TranslationState state;

    TranslationCache(int limit) {
        this.limit = limit;
    }

    /** Retorna a tradução da chave, ou null. Só chama compute na primeira vez. */
    String get(TranslationState currentState, String key, Supplier<String> compute) {
        if (currentState != state) {
            entries.clear();
            state = currentState;
        }

        String cached = entries.get(key);
        if (cached != null) {
            return cached.isEmpty() ? null : cached;
        }

        String translation = compute.get();
        if (entries.size() >= limit) {
            entries.clear();
        }
        entries.put(key, translation == null || translation.isEmpty() ? NO_TRANSLATION : translation);
        return translation == null || translation.isEmpty() ? null : translation;
    }

    void clear() {
        entries.clear();
        state = null;
    }
}
