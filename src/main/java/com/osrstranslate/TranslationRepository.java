package com.osrstranslate;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
final class TranslationRepository {
    private volatile File remoteCacheDirectory;
    private volatile File localTranslationDirectory;

    boolean configureRemoteCacheDirectory(File directory) {
        File previous = remoteCacheDirectory;
        remoteCacheDirectory = directory;
        return previous == null ? directory != null : !previous.equals(directory);
    }

    void configureLocalDirectory(File directory) {
        localTranslationDirectory = directory;
    }

    String localFingerprint() {
        File directory = localTranslationDirectory;
        StringBuilder fingerprint = new StringBuilder();
        for (String fileName : TranslationDomain.FILE_NAMES) {
            File file = directory == null ? null : new File(directory, fileName);
            if (file == null || !file.isFile()) {
                fingerprint.append(fileName).append(":missing;");
            } else {
                fingerprint.append(fileName)
                    .append(':').append(file.length())
                    .append(':').append(file.lastModified())
                    .append(';');
            }
        }
        return fingerprint.toString();
    }

    boolean areLocalFilesValid() {
        File directory = localTranslationDirectory;
        if (directory == null) {
            return true;
        }

        for (String fileName : TranslationDomain.FILE_NAMES) {
            File file = new File(directory, fileName);
            if (!file.isFile()) {
                continue;
            }

            try (InputStream input = new FileInputStream(file)) {
                TranslationLookupHelper.parseJsonMapStrict(input);
            } catch (Exception e) {
                log.warn("Aguardando a conclusao da edicao do arquivo local: {}", file);
                return false;
            }
        }
        return true;
    }

    TranslationState loadState(OsrsTranslateConfig.TranslationSource source) {
        Map<TranslationDomain, TranslationDictionary> dictionaries = new EnumMap<>(TranslationDomain.class);
        StringBuilder summary = new StringBuilder("PT-BR carregado:");
        for (TranslationDomain domain : TranslationDomain.values()) {
            TranslationDictionary dictionary = new TranslationDictionary(
                loadMap(domain.fileName, source),
                domain.usesPlaceholders
            );
            dictionaries.put(domain, dictionary);
            summary.append(' ').append(domain.name().toLowerCase(Locale.ROOT))
                .append('=').append(dictionary.size());
            if (domain.usesPlaceholders) {
                summary.append(" (padroes=").append(dictionary.patternCount()).append(')');
            }
        }

        TranslationState state = new TranslationState(
            dictionaries,
            buildReverseMenuMap(dictionaries.get(TranslationDomain.MENU).translations)
        );
        log.info(summary.toString());
        return state;
    }

    private Map<String, String> loadMap(
        String fileName,
        OsrsTranslateConfig.TranslationSource source
    ) {
        if (source == OsrsTranslateConfig.TranslationSource.LOCAL) {
            return loadLocalMap(fileName);
        }

        File cacheDirectory = remoteCacheDirectory;
        if (cacheDirectory == null) {
            return Collections.emptyMap();
        }

        File remoteFile = new File(cacheDirectory, fileName);
        if (!remoteFile.isFile()) {
            log.warn("Arquivo ausente no cache remoto: {}", fileName);
            return Collections.emptyMap();
        }

        try (InputStream input = new FileInputStream(remoteFile)) {
            return TranslationLookupHelper.parseJsonMap(input);
        } catch (Exception e) {
            log.warn("Cache remoto invalido para {}", fileName, e);
            return Collections.emptyMap();
        }
    }

    private Map<String, String> loadLocalMap(String fileName) {
        File directory = localTranslationDirectory;
        if (directory == null || !directory.isDirectory()) {
            return Collections.emptyMap();
        }

        File localFile = new File(directory, fileName);
        if (!localFile.isFile()) {
            log.warn("Arquivo local de traducoes ausente: {}", localFile);
            return Collections.emptyMap();
        }

        try (InputStream input = new FileInputStream(localFile)) {
            return TranslationLookupHelper.parseJsonMapStrict(input);
        } catch (Exception e) {
            log.warn("Arquivo local de traducoes invalido: {}", localFile, e);
            return Collections.emptyMap();
        }
    }

    private Map<String, String> buildReverseMenuMap(Map<String, String> source) {
        Map<String, String> reverse = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : source.entrySet()) {
            String key = stripMenuText(entry.getKey());
            String value = normalizeMenuLookup(entry.getValue());
            if (key.isEmpty() || value.isEmpty()) {
                continue;
            }
            reverse.putIfAbsent(value, key);
        }
        return reverse;
    }

    private String stripMenuText(String text) {
        return text.replaceAll("<[^>]+>", "").trim();
    }

    private String normalizeMenuLookup(String text) {
        return stripMenuText(text).toLowerCase(Locale.ROOT);
    }
}
