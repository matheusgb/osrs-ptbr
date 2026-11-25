package com.osrstranslate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * Anota num arquivo os textos que apareceram no jogo sem tradução.
 *
 * Cada linha é {"arquivo": "<JSON de destino>", "texto": "..."}. O script
 * ferramentas/wiki/importar_registro.py transforma o arquivo numa lista para
 * traduzir. A gravação roda numa thread própria para não travar o jogo.
 */
@Slf4j
final class MissingTextLog {
    static final int LIMIT = 50_000;
    static final int OUTPUT_LIMIT = 5_000;
    private static final Pattern WORD = Pattern.compile("[A-Za-z]{2,}");

    private final File file;
    private final Set<String> seen = ConcurrentHashMap.newKeySet();
    /** Traduções que o plugin pôs na tela: relidas depois, não são texto sem tradução. */
    private final Set<String> outputs = ConcurrentHashMap.newKeySet();
    private final AtomicInteger written = new AtomicInteger();
    private final ExecutorService writer = Executors.newSingleThreadExecutor();

    MissingTextLog(File file) {
        this.file = file;
        writer.execute(this::loadExisting);
    }

    /** Guarda uma tradução que o plugin mostrou, para não anotar o texto quando ele for relido. */
    void markOutput(String translation) {
        if (translation == null) {
            return;
        }
        if (outputs.size() >= OUTPUT_LIMIT) {
            outputs.clear();
        }
        outputs.add(normalize(translation));
    }

    private static String normalize(String text) {
        return TranslationLookupHelper.normalizeLookupText(TranslationLookupHelper.cleanText(text));
    }

    /** Anota o texto, se ele valer a pena e ainda não estiver no arquivo. */
    void record(String targetFile, String text) {
        String key = targetFile + '\u0000' + text;
        // Atalho na thread do jogo para o caso comum: texto já anotado.
        if (!worthRecording(text) || seen.contains(key) || outputs.contains(normalize(text))) {
            return;
        }
        // A thread de gravação roda em ordem, depois de ler o arquivo existente,
        // então a checagem final contra duplicata fica nela.
        writer.execute(() -> {
            // A duplicata é checada sempre; o limite só barra a gravação.
            if (seen.add(key) && written.incrementAndGet() <= LIMIT) {
                append(targetFile, text);
            }
        });
    }

    /**
     * Para de aceitar textos. Não espera: roda na thread do jogo, e as linhas
     * pendentes terminam de gravar em segundo plano.
     */
    void close() {
        writer.shutdown();
    }

    /** Espera as linhas pendentes; usado nos testes. */
    boolean awaitClosed(long millis) throws InterruptedException {
        return writer.awaitTermination(millis, TimeUnit.MILLISECONDS);
    }

    /** Texto com pelo menos duas palavras: descarta números, nomes soltos e símbolos. */
    static boolean worthRecording(String text) {
        if (text == null) {
            return false;
        }
        Matcher matcher = WORD.matcher(text);
        int words = 0;
        while (matcher.find() && words < 2) {
            words++;
        }
        return words >= 2;
    }

    private void loadExisting() {
        if (!file.isFile()) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    JsonObject entry = new JsonParser().parse(line).getAsJsonObject();
                    seen.add(entry.get("arquivo").getAsString() + '\u0000' + entry.get("texto").getAsString());
                    written.incrementAndGet();
                } catch (RuntimeException e) {
                    // Linha cortada ou editada à mão: ignora.
                }
            }
        } catch (IOException e) {
            log.warn("Nao foi possivel ler {}", file, e);
        }
    }

    private void append(String targetFile, String text) {
        JsonObject entry = new JsonObject();
        entry.addProperty("arquivo", targetFile);
        entry.addProperty("texto", text);
        try {
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            try (Writer out = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                out.write(entry.toString());
                out.write('\n');
            }
        } catch (IOException e) {
            log.warn("Nao foi possivel gravar {}", file, e);
        }
    }
}
