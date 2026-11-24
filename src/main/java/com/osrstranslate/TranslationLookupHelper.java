package com.osrstranslate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TranslationLookupHelper
{
    private static final Pattern BR_TAG = Pattern.compile("(?i)<br\\s*/?>");
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\[[^\\]]+\\]");

    private TranslationLookupHelper()
    {
    }

    static Map<String, String> parseJsonMap(InputStream is)
    {
        try
        {
            return parseJsonMapStrict(is);
        }
        catch (Exception e)
        {
            return Collections.emptyMap();
        }
    }

    static Map<String, String> parseJsonMapStrict(InputStream is) throws IOException
    {
        try
        {
            JsonElement element = new JsonParser().parse(
                new InputStreamReader(is, StandardCharsets.UTF_8)
            );
            if (element == null || !element.isJsonObject())
            {
                throw new IOException("A raiz do JSON deve ser um objeto");
            }

            JsonObject object = element.getAsJsonObject();
            Map<String, String> result = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : object.entrySet())
            {
                JsonElement value = entry.getValue();
                if (value == null || !value.isJsonPrimitive()
                    || !value.getAsJsonPrimitive().isString())
                {
                    throw new IOException("Traducao invalida para a chave: " + entry.getKey());
                }
                result.putIfAbsent(entry.getKey(), value.getAsString());
            }
            return result;
        }
        catch (IOException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new IOException("JSON de traducoes invalido", e);
        }
    }

    static PatternIndex compileRegexTranslations(Map<String, String> source)
    {
        List<PatternEntry> entries = new ArrayList<>();
        for (Map.Entry<String, String> entry : source.entrySet())
        {
            String key = entry.getKey();
            String translation = entry.getValue();
            if (key == null || translation == null || !PLACEHOLDER.matcher(key).find())
            {
                continue;
            }

            if (isIdentityTemplate(key, translation))
            {
                continue;
            }

            int specificity = staticTextLength(key);
            if (!hasLetterOutsidePlaceholders(key))
            {
                // Uma chave sem letra fora dos placeholders, como "[name]" ou
                // "[name].", casaria com qualquer texto terminado assim,
                // inclusive nomes de NPC como "Banker", e devolveria o próprio
                // inglês sem a pontuação final.
                continue;
            }

            String[] parts = PLACEHOLDER.split(key, -1);
            StringBuilder pattern = new StringBuilder();
            for (int i = 0; i < parts.length; i++)
            {
                pattern.append(Pattern.quote(parts[i]));
                if (i < parts.length - 1)
                {
                    pattern.append("(.+?)");
                }
            }
            entries.add(new PatternEntry(
                Pattern.compile(pattern.toString()),
                translation,
                specificity,
                parts[0],
                parts[parts.length - 1]
            ));
        }
        return new PatternIndex(entries);
    }

    private static boolean isIdentityTemplate(String key, String translation)
    {
        return templateSignature(key).equals(templateSignature(translation));
    }

    private static String templateSignature(String text)
    {
        return PLACEHOLDER.matcher(text).replaceAll(Matcher.quoteReplacement("\u0000"));
    }

    private static boolean hasLetterOutsidePlaceholders(String text)
    {
        String staticText = PLACEHOLDER.matcher(text).replaceAll("");
        for (int i = 0; i < staticText.length(); i++)
        {
            if (Character.isLetter(staticText.charAt(i)))
            {
                return true;
            }
        }
        return false;
    }

    private static int staticTextLength(String text)
    {
        Matcher matcher = PLACEHOLDER.matcher(text);
        int length = 0;
        int end = 0;
        while (matcher.find())
        {
            length += matcher.start() - end;
            end = matcher.end();
        }
        return length + text.length() - end;
    }

    /** Primeiro padrão da lista que traduz o texto, ou null. A lista vem em ordem de prioridade. */
    private static Match firstMatch(List<PatternEntry> patterns, String lookup)
    {
        if (patterns == null)
        {
            return null;
        }
        for (PatternEntry entry : patterns)
        {
            String result = entry.apply(lookup);
            if (result != null)
            {
                return new Match(entry.order, result);
            }
        }
        return null;
    }

    static String cleanText(String text)
    {
        if (text == null)
        {
            return "";
        }

        String clean = BR_TAG.matcher(text).replaceAll(" ");
        clean = HTML_TAG.matcher(clean).replaceAll(" ");
        return clean.replaceAll("\\s+", " ").trim();
    }

    static String normalizeLookupText(String text)
    {
        return text
            .replaceAll("\\s+([.,!?])", "$1")
            .replaceAll("([Hh])ardcore Ironmen ", "$1ardcore Ironman ")
            .replaceAll("standard Ironmen ", "standard Ironman ");
    }

    /** Texto até o primeiro espaço, ou null se não tem espaço. */
    private static String firstWord(String text)
    {
        int space = text.indexOf(' ');
        return space < 0 ? null : text.substring(0, space);
    }

    /** Texto depois do último espaço, ou null se não tem espaço. */
    private static String lastWord(String text)
    {
        int space = text.lastIndexOf(' ');
        return space < 0 ? null : text.substring(space + 1);
    }

    /**
     * Padrões com placeholder, do mais específico para o menos específico.
     *
     * Cada padrão entra num grupo pela primeira palavra do texto fixo inicial.
     * Se esse texto não tem espaço (por exemplo "[player], you're..."), o
     * padrão entra num grupo pela última palavra do texto fixo final. Se nenhum
     * dos dois tem espaço, o padrão fica sem grupo. A busca testa só os grupos
     * da primeira e da última palavra do texto, mais os padrões sem grupo.
     */
    static final class PatternIndex
    {
        static final PatternIndex EMPTY = new PatternIndex(new ArrayList<>());

        private final Map<String, List<PatternEntry>> byFirstWord = new HashMap<>();
        private final Map<String, List<PatternEntry>> byLastWord = new HashMap<>();
        private final List<PatternEntry> withoutWord = new ArrayList<>();
        private final int size;

        private PatternIndex(List<PatternEntry> entries)
        {
            entries.sort(ORDER);
            for (int i = 0; i < entries.size(); i++)
            {
                PatternEntry entry = entries.get(i);
                entry.order = i;
                String first = firstWord(entry.prefix);
                String last = first == null ? lastWord(entry.suffix) : null;
                if (first != null)
                {
                    byFirstWord.computeIfAbsent(first, k -> new ArrayList<>()).add(entry);
                }
                else if (last != null)
                {
                    byLastWord.computeIfAbsent(last, k -> new ArrayList<>()).add(entry);
                }
                else
                {
                    withoutWord.add(entry);
                }
            }
            size = entries.size();
        }

        String find(String lookup)
        {
            if (size == 0)
            {
                return null;
            }

            String first = firstWord(lookup);
            String last = lastWord(lookup);
            Match best = firstMatch(withoutWord, lookup);
            best = better(best, first == null ? null : firstMatch(byFirstWord.get(first), lookup));
            best = better(best, last == null ? null : firstMatch(byLastWord.get(last), lookup));
            return best == null ? null : best.text;
        }

        int size()
        {
            return size;
        }

        private static Match better(Match left, Match right)
        {
            if (left == null)
            {
                return right;
            }
            return right != null && right.order < left.order ? right : left;
        }
    }

    private static final Comparator<PatternEntry> ORDER =
        (left, right) -> Integer.compare(right.specificity, left.specificity);

    private static final class Match
    {
        private final int order;
        private final String text;

        private Match(int order, String text)
        {
            this.order = order;
            this.text = text;
        }
    }

    private static final class PatternEntry
    {
        private final Pattern pattern;
        private final String translation;
        private final int specificity;
        private final String prefix;
        private final String suffix;
        private int order;

        private PatternEntry(Pattern pattern, String translation, int specificity, String prefix, String suffix)
        {
            this.pattern = pattern;
            this.translation = translation;
            this.specificity = specificity;
            this.prefix = prefix;
            this.suffix = suffix;
        }

        /** Tradução preenchida, ou null se o padrão não serve para o texto. */
        private String apply(String lookup)
        {
            // Teste barato antes do regex: o texto fixo do início e do fim precisa bater.
            if (lookup.length() <= specificity || !lookup.startsWith(prefix) || !lookup.endsWith(suffix))
            {
                return null;
            }

            Matcher matcher = pattern.matcher(lookup);
            if (!matcher.matches())
            {
                return null;
            }

            String result = translation;
            boolean replacedPlaceholder = false;
            for (int i = 1; i <= matcher.groupCount(); i++)
            {
                Matcher placeholderMatcher = PLACEHOLDER.matcher(result);
                if (!placeholderMatcher.find())
                {
                    break;
                }

                result = result.substring(0, placeholderMatcher.start())
                    + matcher.group(i)
                    + result.substring(placeholderMatcher.end());
                replacedPlaceholder = true;
            }
            return replacedPlaceholder && !lookup.equals(result) ? result : null;
        }
    }
}
