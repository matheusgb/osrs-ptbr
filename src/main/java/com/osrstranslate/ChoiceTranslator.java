package com.osrstranslate;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Traduz o texto capturado por um placeholder de escolha, como "[sir/madam]".
 *
 * O jogo escreve uma das alternativas ("sir"). Sem tradução, ela apareceria em
 * inglês no meio da frase em português ("Bom dia, sir").
 */
final class ChoiceTranslator
{
    /** Palavras de tratamento e gênero que aparecem nas escolhas das transcrições. */
    private static final Map<String, String> WORDS = new HashMap<>();

    static
    {
        String[][] pairs = {
            {"sir", "senhor"}, {"madam", "senhora"}, {"ma'am", "senhora"}, {"mister", "senhor"},
            {"miss", "senhorita"}, {"lady", "senhora"}, {"lord", "senhor"}, {"my lord", "meu senhor"},
            {"my lady", "minha senhora"}, {"milord", "milorde"}, {"milady", "milady"},
            {"my liege", "meu soberano"}, {"lad", "rapaz"}, {"lass", "moça"}, {"laddie", "rapazinho"},
            {"lassie", "mocinha"}, {"boy", "garoto"}, {"girl", "garota"}, {"kid", "garoto"},
            {"man", "homem"}, {"woman", "mulher"}, {"gentleman", "cavalheiro"}, {"brother", "irmão"},
            {"sister", "irmã"}, {"son", "filho"}, {"daughter", "filha"}, {"king", "rei"},
            {"queen", "rainha"}, {"prince", "príncipe"}, {"princess", "princesa"}, {"he", "ele"},
            {"she", "ela"}, {"they", "eles"}, {"him", "ele"}, {"them", "eles"}, {"his", "dele"},
            {"hers", "dela"}, {"theirs", "deles"}, {"himself", "ele mesmo"}, {"herself", "ela mesma"},
            {"themselves", "eles mesmos"}, {"mate", "parceiro"}, {"friend", "amigo"},
            {"dear", "querido"}, {"darling", "querido"}, {"fella", "camarada"}, {"guy", "cara"},
            {"dude", "cara"}, {"bro", "mano"}, {"buddy", "parceiro"}, {"pal", "parceiro"},
        };
        for (String[] pair : pairs)
        {
            WORDS.put(pair[0], pair[1]);
        }
    }

    private ChoiceTranslator()
    {
    }

    /**
     * @param keyPlaceholder   placeholder da chave em inglês, com colchetes
     * @param valuePlaceholder placeholder da tradução na mesma posição, com colchetes
     * @param captured         texto que o jogo escreveu no lugar do placeholder
     * @return o texto a inserir na tradução
     */
    static String translate(String keyPlaceholder, String valuePlaceholder, String captured)
    {
        String[] options = alternatives(keyPlaceholder);
        if (options.length < 2)
        {
            return captured;
        }

        // A tradução tem a própria lista ("[senhor/senhora]"): usa a mesma posição.
        String[] translated = alternatives(valuePlaceholder);
        if (translated.length == options.length && !keyPlaceholder.equals(valuePlaceholder))
        {
            for (int i = 0; i < options.length; i++)
            {
                if (options[i].trim().equalsIgnoreCase(captured.trim()))
                {
                    return keepCase(captured, translated[i].trim());
                }
            }
        }

        String word = WORDS.get(captured.trim().toLowerCase(Locale.ROOT));
        return word == null ? captured : keepCase(captured, word);
    }

    private static String[] alternatives(String placeholder)
    {
        if (placeholder == null || placeholder.length() < 2 || placeholder.indexOf('/') < 0)
        {
            return new String[0];
        }
        return placeholder.substring(1, placeholder.length() - 1).split("/", -1);
    }

    private static String keepCase(String original, String translation)
    {
        String trimmed = original.trim();
        if (trimmed.isEmpty() || translation.isEmpty())
        {
            return translation;
        }
        if (trimmed.length() > 1 && trimmed.equals(trimmed.toUpperCase(Locale.ROOT)))
        {
            return translation.toUpperCase(Locale.ROOT);
        }
        if (Character.isUpperCase(trimmed.charAt(0)))
        {
            return Character.toUpperCase(translation.charAt(0)) + translation.substring(1);
        }
        return translation;
    }
}
