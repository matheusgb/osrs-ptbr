package com.osrstranslate;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Conjunto imutável de dicionários carregados. */
final class TranslationState {
    private final Map<TranslationDomain, TranslationDictionary> dictionaries;
    final Map<String, String> reverseTranslationsMenu;

    TranslationState(
        Map<TranslationDomain, TranslationDictionary> dictionaries,
        Map<String, String> reverseTranslationsMenu
    ) {
        this.dictionaries = new EnumMap<>(dictionaries);
        this.reverseTranslationsMenu = reverseTranslationsMenu;
    }

    TranslationDictionary get(TranslationDomain domain) {
        return dictionaries.getOrDefault(domain, TranslationDictionary.EMPTY);
    }

    static TranslationState empty() {
        return new TranslationState(
            new EnumMap<>(TranslationDomain.class),
            Collections.emptyMap()
        );
    }
}
