package com.osrstranslate;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Traduções de um domínio, com os textos já traduzidos e os padrões com placeholder. */
final class TranslationDictionary {
    static final TranslationDictionary EMPTY =
        new TranslationDictionary(Collections.emptyMap(), false);

    private static final Pattern ELLIPSIS = Pattern.compile("(\\.{3,}|…)\\s*");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    final Map<String, String> translations;
    /**
     * Mesmas traduções com a chave normalizada. A wiki transcreve com diferenças pequenas
     * em relação ao jogo: "Sermon" e "sermon", "Again...very" e "Again... very".
     */
    private final Map<String, String> normalized;
    private final Set<String> translatedValues;
    private final TranslationLookupHelper.PatternIndex patterns;

    TranslationDictionary(Map<String, String> translations, boolean usesPlaceholders) {
        this.translations = translations;
        this.translatedValues = new HashSet<>(translations.values());
        // putIfAbsent: na colisão, vale a primeira chave na ordem do mapa (no merge, a de MERGE_ORDER).
        this.normalized = new HashMap<>(translations.size() * 2);
        for (Map.Entry<String, String> entry : translations.entrySet()) {
            normalized.putIfAbsent(normalize(entry.getKey()), entry.getValue());
        }
        this.patterns = usesPlaceholders
            ? TranslationLookupHelper.compileRegexTranslations(translations)
            : TranslationLookupHelper.PatternIndex.EMPTY;
    }

    /** Retorna true se o texto já é uma tradução deste dicionário. */
    boolean isTranslated(String text) {
        return translatedValues.contains(text);
    }

    /** Busca exata, depois a normalizada, depois os padrões com placeholder. */
    String find(String lookup) {
        String translation = findText(lookup);
        return translation != null ? translation : patterns.find(lookup);
    }

    /** Busca exata e normalizada, sem padrões. */
    String findText(String lookup) {
        String translation = translations.get(lookup);
        return translation != null ? translation : normalized.get(normalize(lookup));
    }

    /** Minúscula, espaço único e reticências sempre seguidas de um espaço. */
    static String normalize(String text) {
        String spaced = ELLIPSIS.matcher(text).replaceAll("... ");
        return SPACES.matcher(spaced).replaceAll(" ").trim().toLowerCase(Locale.ROOT);
    }

    int size() {
        return translations.size();
    }

    int patternCount() {
        return patterns.size();
    }
}
