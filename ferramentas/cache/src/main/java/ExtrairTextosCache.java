import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import net.runelite.cache.ConfigType;
import net.runelite.cache.IndexType;
import net.runelite.cache.InterfaceManager;
import net.runelite.cache.StructManager;
import net.runelite.cache.definitions.EnumDefinition;
import net.runelite.cache.definitions.InterfaceDefinition;
import net.runelite.cache.definitions.ScriptDefinition;
import net.runelite.cache.definitions.StructDefinition;
import net.runelite.cache.definitions.loaders.EnumLoader;
import net.runelite.cache.definitions.loaders.ScriptLoader;
import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.FSFile;
import net.runelite.cache.fs.Index;
import net.runelite.cache.fs.Storage;
import net.runelite.cache.fs.Store;

/**
 * Lê o cache do OSRS e grava os textos em inglês de interfaces, enums, structs e
 * clientscripts.
 *
 * Uso: gradlew -p ferramentas/cache run --args="<pasta do cache> <saida.json>"
 * A pasta do cache tem os arquivos main_file_cache.*. Use uma cópia: a biblioteca
 * abre os arquivos para escrita.
 */
public final class ExtrairTextosCache
{
    private static final Pattern TAG = Pattern.compile("<[^>]+>");
    private static final Pattern PALAVRA = Pattern.compile("[A-Za-z]{3}");
    /** Identificador de código, como "com_shop_buy" ou "%1": não aparece para o jogador. */
    private static final Pattern IDENTIFICADOR = Pattern.compile("^[a-z0-9_:%.]+$");

    private final Map<String, String> textos = new TreeMap<>();

    public static void main(String[] args) throws IOException
    {
        ExtrairTextosCache extrator = new ExtrairTextosCache();
        try (Store store = new Store(new File(args[0])))
        {
            store.load();
            extrator.interfaces(store);
            extrator.enums(store);
            extrator.structs(store);
            extrator.scripts(store);
        }
        try (Writer saida = new FileWriter(args[1], StandardCharsets.UTF_8))
        {
            new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(extrator.textos, saida);
        }
        System.out.println(extrator.textos.size() + " textos em " + args[1]);
    }

    private void interfaces(Store store) throws IOException
    {
        InterfaceManager manager = new InterfaceManager(store);
        manager.load();
        for (InterfaceDefinition[] grupo : manager.getInterfaces())
        {
            if (grupo == null)
            {
                continue;
            }
            for (InterfaceDefinition widget : grupo)
            {
                if (widget == null)
                {
                    continue;
                }
                adicionar(widget.text, "interface");
                adicionar(widget.alternateText, "interface");
                adicionar(widget.name, "interface");
                adicionar(widget.tooltip, "interface");
                adicionar(widget.targetVerb, "interface");
                adicionar(widget.spellName, "interface");
                adicionarTodos(widget.actions, "interface");
                adicionarTodos(widget.configActions, "interface");
            }
        }
    }

    private void enums(Store store) throws IOException
    {
        Index configs = store.getIndex(IndexType.CONFIGS);
        Archive archive = configs.getArchive(ConfigType.ENUM.getId());
        Storage storage = store.getStorage();
        EnumLoader loader = new EnumLoader();
        for (FSFile file : archive.getFiles(storage.loadArchive(archive)).getFiles())
        {
            EnumDefinition definition = loader.load(file.getFileId(), file.getContents());
            if (definition != null)
            {
                adicionar(definition.getDefaultString(), "enum");
                adicionarTodos(definition.getStringVals(), "enum");
            }
        }
    }

    private void structs(Store store) throws IOException
    {
        StructManager manager = new StructManager(store);
        manager.load();
        for (StructDefinition struct : manager.getStructs().values())
        {
            if (struct.getParams() == null)
            {
                continue;
            }
            for (Object valor : struct.getParams().values())
            {
                if (valor instanceof String)
                {
                    adicionar((String) valor, "struct");
                }
            }
        }
    }

    private void scripts(Store store) throws IOException
    {
        Index index = store.getIndex(IndexType.CLIENTSCRIPT);
        Storage storage = store.getStorage();
        ScriptLoader loader = new ScriptLoader();
        for (Archive archive : index.getArchives())
        {
            byte[] dados = storage.loadArchive(archive);
            if (dados == null)
            {
                continue;
            }
            try
            {
                ScriptDefinition script = loader.load(archive.getArchiveId(), archive.decompress(dados));
                adicionarTodos(script.getStringOperands(), "script");
            }
            catch (RuntimeException e)
            {
                // Script num formato que a biblioteca não lê: os outros continuam.
            }
        }
    }

    private void adicionarTodos(String[] valores, String origem)
    {
        if (valores != null)
        {
            for (String valor : valores)
            {
                adicionar(valor, origem);
            }
        }
    }

    private void adicionar(String valor, String origem)
    {
        if (valor == null)
        {
            return;
        }
        String texto = TAG.matcher(valor.replaceAll("(?i)<br\\s*/?>", " ")).replaceAll(" ")
            .replaceAll("\\s+", " ").trim();
        if (!PALAVRA.matcher(texto).find() || IDENTIFICADOR.matcher(texto).matches())
        {
            return;
        }
        textos.putIfAbsent(texto, origem);
    }
}
