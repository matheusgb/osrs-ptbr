package com.osrstranslate;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/** Conjunto imutável de dicionários carregados. */
final class TranslationState {
    /**
     * Ordem de prioridade quando a mesma frase aparece em mais de um JSON.
     * Os textos curtos de interface vêm antes dos diálogos.
     */
    private static final TranslationDomain[] MERGE_ORDER = {
        TranslationDomain.SETTINGS,
        TranslationDomain.MENU,
        TranslationDomain.SKILLS,
        TranslationDomain.QUESTS,
        TranslationDomain.ITEMS,
        TranslationDomain.WELCOME,
        TranslationDomain.GAME_MESSAGE,
        TranslationDomain.OVERHEAD,
        TranslationDomain.DIALOG,
    };

    private final Map<TranslationDomain, TranslationDictionary> dictionaries;
    final Map<String, String> reverseTranslationsMenu;
    /** Todos os dicionários juntos, só com busca exata. Serve para interfaces sem domínio próprio. */
    final TranslationDictionary merged;

    TranslationState(
        Map<TranslationDomain, TranslationDictionary> dictionaries,
        Map<String, String> reverseTranslationsMenu
    ) {
        this.dictionaries = new EnumMap<>(dictionaries);
        this.reverseTranslationsMenu = reverseTranslationsMenu;
        this.merged = merge(this.dictionaries);
    }

    private static TranslationDictionary merge(Map<TranslationDomain, TranslationDictionary> dictionaries) {
        Map<String, String> all = new HashMap<>();
        for (TranslationDomain domain : MERGE_ORDER) {
            TranslationDictionary dictionary = dictionaries.get(domain);
            if (dictionary == null) {
                continue;
            }
            for (Map.Entry<String, String> entry : dictionary.translations.entrySet()) {
                all.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
        return new TranslationDictionary(all, false);
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
