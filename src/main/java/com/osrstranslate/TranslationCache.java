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

    /**
     * Retorna a tradução do texto, ou null. Só chama compute na primeira vez.
     *
     * O texto traduzido também fica marcado como "sem tradução": o plugin traduz o
     * mesmo widget a cada quadro, e uma tradução por padrão pode casar de novo com
     * um padrão ("[x] XP" vira "[x] de XP", que vira "[x] de de XP"...).
     *
     * @param prefix parte da chave que não é texto, como o id da interface
     */
    String get(TranslationState currentState, String prefix, String text, Supplier<String> compute) {
        if (currentState != state) {
            entries.clear();
            state = currentState;
        }

        String key = prefix + text;
        String cached = entries.get(key);
        if (cached != null) {
            return cached.isEmpty() ? null : cached;
        }

        String translation = compute.get();
        if (entries.size() >= limit) {
            entries.clear();
        }
        if (translation == null || translation.isEmpty()) {
            entries.put(key, NO_TRANSLATION);
            return null;
        }
        entries.put(key, translation);
        entries.putIfAbsent(prefix + translation, NO_TRANSLATION);
        return translation;
    }

    void clear() {
        entries.clear();
        state = null;
    }
}
