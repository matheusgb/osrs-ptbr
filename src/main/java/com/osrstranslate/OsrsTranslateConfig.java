package com.osrstranslate;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(OsrsTranslateConfig.GROUP)
public interface OsrsTranslateConfig extends Config {
    /** Grupo próprio do fork, separado das configurações salvas do plugin original. */
    String GROUP = "osrstranslateptbr";

    @ConfigItem(
        keyName = "translationHotReload",
        name = "Hot reload das traduções",
        description = "Recarrega automaticamente os JSONs locais quando forem alterados",
        section = translationCorrection,
        position = 3
    )
    default boolean translationHotReload() {
        return true;
    }

    @ConfigItem(
        keyName = "localTranslationPath",
        name = "Caminho dos JSONs",
        description = "Pasta com os JSONs de tradução. Vazio usa ~/.runelite/osrs-translate-ptbr/pt-BR, onde o instalador copia as traduções",
        section = translationCorrection,
        position = 2
    )
    default String localTranslationPath() {
        return "";
    }

    @ConfigSection(
        name = "Correção tradução",
        description = "Opções para testar e corrigir traduções locais",
        position = 3
    )
    String translationCorrection = "Correção tradução";

    @ConfigSection(
        name = "Traduções Estáticas",
        description = "Configurações de traduções para interfaces do jogo",
        position = 1
    )
    String staticTranslations = "Traduções Estáticas";

    @ConfigItem(
        keyName = "enableDialogues",
        name = "Traduzir diálogos",
        description = "Traduz diálogos de NPCs, opções, sprites e ações",
        section = staticTranslations,
        position = 0
    )
    default boolean enableDialogues() {
        return true;
    }

    @ConfigItem(
        keyName = "enableSkillGuide",
        name = "Traduzir Skill Guide",
        description = "Traduz textos do Skill Guide",
        section = staticTranslations,
        position = 1
    )
    default boolean enableSkillGuide() {
        return true;
    }

    @ConfigItem(
        keyName = "enableQuestJournal",
        name = "Traduzir Quest Journal",
        description = "Traduz textos do Quest Journal",
        section = staticTranslations,
        position = 2
    )
    default boolean enableQuestJournal() {
        return true;
    }

    @ConfigItem(
        keyName = "enableAchievementDiary",
        name = "Traduzir Achievement Diary",
        description = "Traduz textos dos diários de conquistas",
        section = staticTranslations,
        position = 3
    )
    default boolean enableAchievementDiary() {
        return true;
    }

    @ConfigItem(
        keyName = "enableItems",
        name = "Traduzir livros",
        description = "Traduz textos de livros e notas no jogo",
        section = staticTranslations,
        position = 4
    )
    default boolean enableItems() {
        return true;
    }

    @ConfigItem(
        keyName = "enableMenuEntries",
        name = "Traduzir opções de menu",
        description = "Traduz opções de menu dos npcs, objetos e clique direito",
        section = staticTranslations,
        position = 5
    )
    default boolean enableMenuEntries() {
        return true;
    }

    @ConfigItem(
        keyName = "enableOverhead",
        name = "Traduzir falas acima da cabeça",
        description = "Traduz textos que aparecem acima da cabeça dos NPCs",
        section = staticTranslations,
        position = 6
    )
    default boolean enableOverhead() {
        return true;
    }

    @ConfigItem(
        keyName = "enableGameMessages",
        name = "Traduzir mensagens do jogo",
        description = "Traduz mensagens do jogo como examinar, ações que aparecem no chat do jogo",
        section = staticTranslations,
        position = 7
    )
    default boolean enableGameMessages() {
        return true;
    }

    @ConfigItem(
        keyName = "enableWelcome",
        name = "Traduzir boas-vindas",
        description = "Traduz a tela e mensagens de boas-vindas/login",
        section = staticTranslations,
        position = 8
    )
    default boolean enableWelcome() {
        return true;
    }

    @ConfigItem(
        keyName = "enableSettings",
        name = "Traduzir Settings",
        description = "Traduz textos da interface de configuracoes",
        section = staticTranslations,
        position = 9
    )
    default boolean enableSettings() {
        return true;
    }

    @ConfigItem(
        keyName = "enableClueScrolls",
        name = "Traduzir clue scrolls",
        description = "Traduz o texto dos pergaminhos de pistas após o RuneLite identificar o original",
        section = staticTranslations,
        position = 10
    )
    default boolean enableClueScrolls() {
        return true;
    }

    @ConfigItem(
        keyName = "enableOtherInterfaces",
        name = "Traduzir outras interfaces (experimental)",
        description = "Traduz textos de qualquer interface aberta, como banco, lojas e GE. "
            + "Usa só tradução exata e pode atrapalhar plugins que leem o texto em inglês",
        section = staticTranslations,
        position = 11
    )
    default boolean enableOtherInterfaces() {
        return true;
    }

    @ConfigItem(
        keyName = "translateDialogOptionsEarly",
        name = "Traduzir opções de diálogo sem atraso",
        description = "Traduz as opções de resposta antes de aparecerem na tela. "
            + "Desligado por padrão porque o Quest Helper pode deixar de destacar a opção certa",
        section = staticTranslations,
        position = 12
    )
    default boolean translateDialogOptionsEarly() {
        return false;
    }

    @ConfigItem(
        keyName = "recordMissingTexts",
        name = "Registrar textos sem tradução",
        description = "Anota em ~/.runelite/osrs-translate-ptbr/sem-traducao.jsonl os textos que "
            + "aparecem em inglês, para traduzir depois. Só mensagens do jogo, nunca o chat de jogadores",
        section = staticTranslations,
        position = 13
    )
    default boolean recordMissingTexts() {
        return true;
    }

    @ConfigSection(
        name = "Correções Visuais",
        description = "Ajustes de layout e apresentação",
        position = 2
    )
    String visualFixes = "Correções Visuais";

    @ConfigItem(
        keyName = "enableTextWrapFix",
        name = "Espaçamento entre linhas",
        description = "Ajusta automaticamente a altura das linhas quando o texto traduzido é maior",
        section = visualFixes,
        position = 0
    )
    default boolean enableTextWrapFix() {
        return true;
    }
}
