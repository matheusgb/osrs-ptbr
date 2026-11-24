package com.osrstranslate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

public class TranslationCacheTest {
    @Test
    public void computesEachTextOnce() {
        TranslationCache cache = new TranslationCache(10);
        TranslationState state = TranslationState.empty();
        AtomicInteger calls = new AtomicInteger();

        for (int i = 0; i < 5; i++) {
            assertEquals("Olá", cache.get(state, "Hello", () -> { calls.incrementAndGet(); return "Olá"; }));
            assertNull(cache.get(state, "Bob", () -> { calls.incrementAndGet(); return null; }));
        }
        assertEquals("uma busca por texto, com ou sem tradução", 2, calls.get());
    }

    @Test
    public void clearsWhenDictionariesChange() {
        TranslationCache cache = new TranslationCache(10);
        cache.get(TranslationState.empty(), "Hello", () -> "Olá");
        assertEquals("Oi", cache.get(TranslationState.empty(), "Hello", () -> "Oi"));
    }

    @Test
    public void clearsWhenFull() {
        TranslationCache cache = new TranslationCache(2);
        TranslationState state = TranslationState.empty();
        AtomicInteger calls = new AtomicInteger();
        for (String text : new String[] {"a", "b", "c", "a"}) {
            cache.get(state, text, () -> { calls.incrementAndGet(); return text; });
        }
        assertEquals(4, calls.get());
    }
}
