package com.osrstranslate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.Collections;
import org.junit.Test;

public class TranslationStateTest {
    @Test
    public void mergedPrefersInterfaceTextOverDialog() {
        Map<TranslationDomain, TranslationDictionary> dictionaries = new EnumMap<>(TranslationDomain.class);
        dictionaries.put(TranslationDomain.DIALOG, new TranslationDictionary(
            Map.of("Short", "Baixinho", "Deposit inventory", "Depositar inventário"), true));
        dictionaries.put(TranslationDomain.QUESTS, new TranslationDictionary(Map.of("Short", "Curta"), false));
        dictionaries.put(TranslationDomain.GAME_MESSAGE, new TranslationDictionary(
            Map.of("You need [number] more.", "Você precisa de mais [number]."), true));

        TranslationState state = new TranslationState(dictionaries, Collections.emptyMap());

        assertEquals("Curta", state.merged.find("Short"));
        assertEquals("Depositar inventário", state.merged.find("Deposit inventory"));
        assertNull("merged usa só busca exata", state.merged.find("You need 3 more."));
    }

    @Test
    public void otherDomainsSkipMenuAndPatterns() {
        Map<TranslationDomain, TranslationDictionary> dictionaries = new EnumMap<>(TranslationDomain.class);
        dictionaries.put(TranslationDomain.DIALOG, new TranslationDictionary(
            Map.of("Juliet gives you a message.", "Juliet te dá uma mensagem.",
                "You need [number] more.", "Você precisa de mais [number]."), true));
        dictionaries.put(TranslationDomain.MENU, new TranslationDictionary(Map.of("Attack", "Atacar"), false));

        TranslationState state = new TranslationState(dictionaries, Collections.emptyMap());

        assertEquals("Juliet te dá uma mensagem.",
            state.findInOtherDomains(TranslationDomain.GAME_MESSAGE, "juliet gives you a message."));
        assertNull("menu fica de fora", state.findInOtherDomains(TranslationDomain.DIALOG, "Attack"));
        assertNull("sem padrões de outra área",
            state.findInOtherDomains(TranslationDomain.GAME_MESSAGE, "You need 3 more."));
    }
}
