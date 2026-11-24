package com.osrstranslate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public class TranslationDictionaryTest {
    private static Map<String, String> sample() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("Switch world", "Mudar de mundo");
        map.put("You still need to kill [number] [monster].", "Você ainda precisa matar [number] [monster].");
        map.put("You need [number] more.", "Você precisa de mais [number].");
        map.put("[player], you're under arrest!", "[player], você está preso!");
        map.put("Hello[name]!", "Olá[name]!");
        map.put("[a] [b]", "[b] [a]");
        return map;
    }

    @Test
    public void exactMatchWins() {
        TranslationDictionary dictionary = new TranslationDictionary(sample(), true);
        assertEquals("Mudar de mundo", dictionary.find("Switch world"));
    }

    @Test
    public void placeholdersAreFilled() {
        TranslationDictionary dictionary = new TranslationDictionary(sample(), true);
        assertEquals("Você ainda precisa matar 5 goblins.", dictionary.find("You still need to kill 5 goblins."));
        assertEquals("Você precisa de mais 3.", dictionary.find("You need 3 more."));
        assertEquals("Zezinho, você está preso!", dictionary.find("Zezinho, you're under arrest!"));
        assertEquals("OláBob!", dictionary.find("HelloBob!"));
    }

    @Test
    public void keyMadeOnlyOfPlaceholdersIsIgnored() {
        TranslationDictionary dictionary = new TranslationDictionary(sample(), true);
        assertNull(dictionary.find("Banker Bob"));
    }

    @Test
    public void keyWithOnlyPunctuationOutsidePlaceholderIsIgnored() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("[Vampyre name].", "[Vampyre name]");
        map.put("[player]?", "[jogador]?");
        map.put("Hi [player].", "Oi, [player].");
        TranslationDictionary dictionary = new TranslationDictionary(map, true);
        assertNull("frase sem tradução não pode perder o ponto", dictionary.find("Some untranslated sentence."));
        assertNull(dictionary.find("Who are you?"));
        assertEquals("Oi, Bob.", dictionary.find("Hi Bob."));
    }

    @Test
    public void domainWithoutPlaceholdersUsesExactMatchOnly() {
        TranslationDictionary dictionary = new TranslationDictionary(sample(), false);
        assertNull(dictionary.find("You need 3 more."));
        assertEquals("Mudar de mundo", dictionary.find("Switch world"));
    }

    @Test
    public void recognisesTranslatedText() {
        TranslationDictionary dictionary = new TranslationDictionary(sample(), true);
        assertTrue(dictionary.isTranslated("Mudar de mundo"));
        assertFalse(dictionary.isTranslated("Switch world"));
    }

    @Test
    public void moreSpecificPatternWinsAcrossIndexGroups() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("[amount] more.", "mais [amount].");
        map.put("You need [number] more.", "Você precisa de mais [number].");
        TranslationDictionary dictionary = new TranslationDictionary(map, true);
        assertEquals("Você precisa de mais 3.", dictionary.find("You need 3 more."));
        assertEquals("mais tudo isso.", dictionary.find("tudo isso more."));
    }
}
