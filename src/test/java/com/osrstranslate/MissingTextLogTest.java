package com.osrstranslate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MissingTextLogTest {
    @Rule
    public TemporaryFolder pasta = new TemporaryFolder();

    @Test
    public void ignoresNumbersAndSingleWords() {
        assertFalse(MissingTextLog.worthRecording("1,234"));
        assertFalse(MissingTextLog.worthRecording("Zezinho"));
        assertFalse(MissingTextLog.worthRecording("+0.4%"));
        assertTrue(MissingTextLog.worthRecording("Only dairy cows are suitable for milking."));
    }

    @Test
    public void writesEachTextOnceAcrossRestarts() throws Exception {
        File arquivo = new File(pasta.getRoot(), "sub/sem-traducao.jsonl");
        MissingTextLog primeiro = new MissingTextLog(arquivo);
        primeiro.record("translations_game_message.json", "Only dairy cows are suitable for milking.");
        primeiro.record("translations_game_message.json", "Only dairy cows are suitable for milking.");
        primeiro.record("translations.json", "Only dairy cows are suitable for milking.");
        primeiro.close();
        assertTrue(primeiro.awaitClosed(5000));

        MissingTextLog segundo = new MissingTextLog(arquivo);
        segundo.record("translations_game_message.json", "Only dairy cows are suitable for milking.");
        segundo.record("translations_game_message.json", "You catch a shrimp.");
        segundo.close();
        assertTrue(segundo.awaitClosed(5000));

        List<String> linhas = Files.readAllLines(arquivo.toPath(), StandardCharsets.UTF_8);
        assertEquals(3, linhas.size());
        assertTrue(linhas.get(0).contains("\"arquivo\":\"translations_game_message.json\""));
        assertTrue(linhas.get(2).contains("You catch a shrimp."));
    }
}
