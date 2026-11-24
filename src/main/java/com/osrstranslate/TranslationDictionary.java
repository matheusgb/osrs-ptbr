package com.osrstranslate;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Traduções de um domínio, com os textos já traduzidos e os padrões com placeholder. */
final class TranslationDictionary {
    static final TranslationDictionary EMPTY =
        new TranslationDictionary(Collections.emptyMap(), false);

    final Map<String, String> translations;
    private final Set<String> translatedValues;
    private final TranslationLookupHelper.PatternIndex patterns;

    TranslationDictionary(Map<String, String> translations, boolean usesPlaceholders) {
        this.translations = translations;
        this.translatedValues = new HashSet<>(translations.values());
        this.patterns = usesPlaceholders
            ? TranslationLookupHelper.compileRegexTranslations(translations)
            : TranslationLookupHelper.PatternIndex.EMPTY;
    }

    /** Retorna true se o texto já é uma tradução deste dicionário. */
    boolean isTranslated(String text) {
        return translatedValues.contains(text);
    }

    /** Busca exata primeiro. Se não acha, tenta os padrões com placeholder. */
    String find(String lookup) {
        String translation = translations.get(lookup);
        return translation != null ? translation : patterns.find(lookup);
    }

    int size() {
        return translations.size();
    }

    int patternCount() {
        return patterns.size();
    }
}
