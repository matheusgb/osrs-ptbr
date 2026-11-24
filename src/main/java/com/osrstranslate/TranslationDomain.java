package com.osrstranslate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Um dicionário de tradução e o arquivo JSON que o alimenta. */
enum TranslationDomain {
    DIALOG("translations.json", true),
    SKILLS("translations_skills.json", false),
    QUESTS("translations_quests.json", false),
    ITEMS("translations_items.json", false),
    MENU("translations_menu.json", false),
    OVERHEAD("translations_overhead.json", true),
    GAME_MESSAGE("translations_game_message.json", true),
    WELCOME("translations_welcome.json", true),
    SETTINGS("translations_settings.json", false);

    static final List<String> FILE_NAMES = fileNames();

    final String fileName;
    final boolean usesPlaceholders;

    TranslationDomain(String fileName, boolean usesPlaceholders) {
        this.fileName = fileName;
        this.usesPlaceholders = usesPlaceholders;
    }

    private static List<String> fileNames() {
        List<String> names = new ArrayList<>();
        for (TranslationDomain domain : values()) {
            names.add(domain.fileName);
        }
        return Collections.unmodifiableList(names);
    }
}
