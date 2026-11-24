package com.osrstranslate;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MessageNode;
import net.runelite.api.Menu;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@PluginDescriptor(
    name = "OSRS Translate PT-BR",
    description = "Traduz dialogos do OSRS para Portugues Brasileiro",
    tags = {"translate", "portuguese", "ptbr"}
)
public class OsrsTranslatePlugin extends Plugin {
    private static final int CHATBOX_UNIVERSE = 162;
    private static final int DIALOG_MESSAGE = 229;
    private static final int CHAT_OVERLAY = 263;
    private static final int PLAYER_DESIGN = 679;
    private static final int TUTORIAL_PLAYER_EXPERIENCE = 929;
    private static final int TUTORIAL_DISPLAY_NAME = 558;
    private static final int ITEM_PREVIEW = 203;
    private static final int SKILL_GUIDE = 214;
    private static final int SKILL_GUIDE_ALTERNATE = 860;
    private static final int BOOKS_NOTES = 680;
    private static final int QUEST_SCROLL = 153;
    private static final int QUEST_JOURNAL_MINIMAP = 782;
    private static final int QUEST_JOURNAL = 119;
    private static final int ACHIEVEMENT_DIARY = 259;
    private static final int ACHIEVEMENT_DIARY_SCROLL = 741;
    private static final int CLUE_SCROLL_TEXT = net.runelite.api.gameval.InterfaceID.TRAIL_CLUETEXT;
    private static final int WELCOME_SCREEN = 378;
    private static final int SETTINGS = 134;
    private static final int BANK_PIN = 213;
    private static final int MACRO_MIME_EMOTES = 188;
    private static final int CRAFTING_GOLD = 446;
    private static final int TOPLEVEL_OSRS_STRETCH = 161;
    private static final int LOCAL_TRANSLATION_POLL_SECONDS = 2;
    private static final int TRANSLATION_CACHE_LIMIT = 20_000;

    private static final int[] DIALOG_INTERFACES = {
        InterfaceID.DIALOG_NPC,
        InterfaceID.DIALOG_PLAYER,
        InterfaceID.DIALOG_OPTION,
        InterfaceID.DIALOG_SPRITE,
    };

    private static final int[] MAIN_TEXT_INTERFACES = {
        InterfaceID.LEVEL_UP,
        DIALOG_MESSAGE,
    };

    private static final int[] QUEST_INTERFACES = {
        QUEST_SCROLL,
        QUEST_JOURNAL_MINIMAP,
        QUEST_JOURNAL,
    };

    private static final int[] ACHIEVEMENT_DIARY_INTERFACES = {
        ACHIEVEMENT_DIARY,
        ACHIEVEMENT_DIARY_SCROLL,
    };

    private static final int[] ITEM_INTERFACES = {
        ITEM_PREVIEW,
        BOOKS_NOTES,
    };

    private static final int[] SKILL_GUIDE_INTERFACES = {
        SKILL_GUIDE,
        SKILL_GUIDE_ALTERNATE,
    };

    private static final int[] SETTINGS_INTERFACES = {
        SETTINGS,
        BANK_PIN,
        TOPLEVEL_OSRS_STRETCH,
    };

    /** Interfaces com tradução própria ou que não devem ser tocadas, como o chat. */
    private static final Set<Integer> HANDLED_INTERFACES = handledInterfaces();

    private static Set<Integer> handledInterfaces() {
        Set<Integer> ids = new HashSet<>();
        for (int[] group : new int[][] {
            DIALOG_INTERFACES, MAIN_TEXT_INTERFACES, QUEST_INTERFACES, ACHIEVEMENT_DIARY_INTERFACES,
            ITEM_INTERFACES, SKILL_GUIDE_INTERFACES, SETTINGS_INTERFACES,
        }) {
            for (int id : group) {
                ids.add(id);
            }
        }
        ids.addAll(Set.of(
            CHATBOX_UNIVERSE, CHAT_OVERLAY, PLAYER_DESIGN, TUTORIAL_PLAYER_EXPERIENCE,
            TUTORIAL_DISPLAY_NAME, CLUE_SCROLL_TEXT, WELCOME_SCREEN, MACRO_MIME_EMOTES, CRAFTING_GOLD
        ));
        return Collections.unmodifiableSet(ids);
    }

    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern BR_TAG = Pattern.compile("(?i)<br\\s*/?>");
    private static final Pattern START_TAGS = Pattern.compile("^(<str>|<col=[^>]+>)+");
    private static final Set<String> TRIVIAL_MENU_OPTIONS = Set.of(
        "Walk here",
        "Cancel",
        "Continue"
    );
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private OsrsTranslateConfig config;
    @Inject private EventBus eventBus;
    private final TranslationRepository translationRepository = new TranslationRepository();
    private final WelcomeTranslationService welcomeTranslationService = new WelcomeTranslationService();
    private volatile TranslationState translationState = TranslationState.empty();

    private ScheduledExecutorService localTranslationHotReloadScheduler;
    private volatile String localTranslationFingerprint = "";
    private EventBus.Subscriber menuOpenedTranslationSubscriber;
    /** Registro de textos sem tradução; null com a opção desligada. */
    private volatile MissingTextLog missingTextLog;
    /** Interfaces abertas na tela de login; usam o dicionário de boas-vindas. */
    private final Set<Integer> loginInterfaceGroups = new HashSet<>();
    private final TranslationCache otherInterfacesCache = new TranslationCache(TRANSLATION_CACHE_LIMIT);
    /** Interfaces abertas que são traduzidas antes de cada quadro. */
    private final Set<Integer> openInterfaces = new HashSet<>();
    /** Interfaces abertas sem tradução própria (opção "Outras interfaces"). */
    private final Set<Integer> openOtherInterfaces = new HashSet<>();
    /** Tradução já calculada por interface e texto cru. */
    private final TranslationCache widgetTextCache = new TranslationCache(TRANSLATION_CACHE_LIMIT);
    /**
     * Opção original em inglês de cada entrada de menu traduzida. Várias opções
     * têm a mesma tradução ("Subir" para Climb-up e Climb-over), então o texto
     * traduzido sozinho não diz qual era o original.
     */
    private final Map<String, String> originalMenuOptions = new HashMap<>();

    @Override
    protected void startUp() {
        reloadTranslations();
        menuOpenedTranslationSubscriber = eventBus.register(
            MenuOpened.class,
            event -> {
                if (config.enableMenuEntries()) {
                    // Run after Menu Entry Swapper has created its entries and
                    // submenus, but before the client renders the menu box.
                    translateMenu(client.getMenu());
                }
            },
            -1000f
        );
        startLocalTranslationHotReload();
        startMissingTextLog();
        // Interfaces abertas antes de o plugin ligar não geram WidgetLoaded.
        clientThread.invokeLater(this::registerOpenInterfaces);
    }

    @Override
    protected void shutDown() {
        stopLocalTranslationHotReload();
        stopMissingTextLog();
        eventBus.unregister(menuOpenedTranslationSubscriber);
        menuOpenedTranslationSubscriber = null;
        translationState = TranslationState.empty();
        loginInterfaceGroups.clear();
        openInterfaces.clear();
        openOtherInterfaces.clear();
        widgetTextCache.clear();
        otherInterfacesCache.clear();
        originalMenuOptions.clear();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (!OsrsTranslateConfig.GROUP.equals(event.getGroup())) {
            return;
        }

        // Uma opção pode ter sido desligada: refaz a lista de interfaces traduzidas.
        openInterfaces.clear();
        openOtherInterfaces.clear();
        clientThread.invokeLater(this::registerOpenInterfaces);

        if ("recordMissingTexts".equals(event.getKey())) {
            stopMissingTextLog();
            startMissingTextLog();
        }

        if ("translationHotReload".equals(event.getKey()) || "localTranslationPath".equals(event.getKey())) {
            reloadTranslations();
            startLocalTranslationHotReload();
        }
    }

    private void startMissingTextLog() {
        if (config.recordMissingTexts()) {
            missingTextLog = new MissingTextLog(new File(RuneLite.RUNELITE_DIR,
                "osrs-translate-ptbr" + File.separator + "sem-traducao.jsonl"));
        }
    }

    private void stopMissingTextLog() {
        MissingTextLog current = missingTextLog;
        missingTextLog = null;
        if (current != null) {
            current.close();
        }
    }

    /** Anota um texto que apareceu sem tradução, se o registro estiver ligado. */
    private void recordMissing(TranslationDomain domain, String text) {
        MissingTextLog current = missingTextLog;
        if (current != null) {
            current.record(domain.fileName, text);
        }
    }

    private File localTranslationDirectory() {
        String path = config.localTranslationPath();
        if (path == null || path.trim().isEmpty()) {
            // Pasta onde o instalador (instalar.bat) copia as traduções.
            return new File(RuneLite.RUNELITE_DIR, "osrs-translate-ptbr" + File.separator + "pt-BR");
        }
        return new File(path.trim());
    }

    private void reloadTranslations() {
        File directory = localTranslationDirectory();
        if (!directory.isDirectory()) {
            log.warn("Pasta de traducoes nao encontrada: {}. Rode o instalar.bat.", directory);
        }
        translationRepository.configureLocalDirectory(directory);
        translationState = translationRepository.loadState();
    }

    private void startLocalTranslationHotReload() {
        stopLocalTranslationHotReload();
        if (!config.translationHotReload()) {
            return;
        }

        localTranslationFingerprint = translationRepository.localFingerprint();
        localTranslationHotReloadScheduler = Executors.newSingleThreadScheduledExecutor();
        localTranslationHotReloadScheduler.scheduleWithFixedDelay(
            this::checkLocalTranslationHotReload,
            LOCAL_TRANSLATION_POLL_SECONDS,
            LOCAL_TRANSLATION_POLL_SECONDS,
            TimeUnit.SECONDS
        );
    }

    private void stopLocalTranslationHotReload() {
        if (localTranslationHotReloadScheduler != null) {
            localTranslationHotReloadScheduler.shutdownNow();
            localTranslationHotReloadScheduler = null;
        }
        localTranslationFingerprint = "";
    }

    private void checkLocalTranslationHotReload() {
        try {
            String fingerprint = translationRepository.localFingerprint();
            if (fingerprint.equals(localTranslationFingerprint)
                || !translationRepository.areLocalFilesValid()) {
                return;
            }

            TranslationState updatedState = translationRepository.loadState();
            if (!fingerprint.equals(translationRepository.localFingerprint())) {
                return;
            }

            // O cache de tradução troca junto, porque ele é ligado ao estado.
            translationState = updatedState;
            localTranslationFingerprint = fingerprint;
            log.info("Traducoes locais recarregadas automaticamente");
        } catch (Exception e) {
            log.warn("Nao foi possivel recarregar as traducoes locais", e);
        }
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event) {
        registerInterface(event.getGroupId());
    }

    /** Decide como uma interface que abriu é traduzida. */
    private void registerInterface(int groupId) {
        if (groupId == CLUE_SCROLL_TEXT) {
            // O plugin Clue Scroll lê o texto original logo depois de abrir.
            if (config.enableClueScrolls()) {
                clientThread.invokeLater(() ->
                    clientThread.invokeLater(() -> translateInterface(CLUE_SCROLL_TEXT)));
            }
            return;
        }

        if (isLoginScreenState()) {
            if (config.enableWelcome()) {
                loginInterfaceGroups.add(groupId);
                scheduleTranslation(groupId);
            }
            return;
        }

        if (isKnownInterfaceEnabled(groupId)) {
            scheduleTranslation(groupId);
        } else if (config.enableOtherInterfaces() && !HANDLED_INTERFACES.contains(groupId)) {
            openOtherInterfaces.add(groupId);
        }
    }

    /** Interface com dicionário próprio, com a opção dela ligada. */
    private boolean isKnownInterfaceEnabled(int groupId) {
        if (contains(DIALOG_INTERFACES, groupId)) {
            return config.enableDialogues();
        }
        if (contains(MAIN_TEXT_INTERFACES, groupId)) {
            return true;
        }
        if (groupId == CHAT_OVERLAY) {
            return config.enableGameMessages();
        }
        if (groupId == PLAYER_DESIGN || groupId == TUTORIAL_PLAYER_EXPERIENCE || groupId == TUTORIAL_DISPLAY_NAME
            || groupId == MACRO_MIME_EMOTES || groupId == CRAFTING_GOLD) {
            return config.enableMenuEntries();
        }
        if (contains(QUEST_INTERFACES, groupId)) {
            return config.enableQuestJournal();
        }
        if (contains(ACHIEVEMENT_DIARY_INTERFACES, groupId)) {
            return config.enableAchievementDiary();
        }
        if (contains(ITEM_INTERFACES, groupId)) {
            return config.enableItems();
        }
        if (groupId == WELCOME_SCREEN) {
            return config.enableWelcome();
        }
        if (contains(SKILL_GUIDE_INTERFACES, groupId)) {
            return config.enableSkillGuide();
        }
        if (contains(SETTINGS_INTERFACES, groupId)) {
            return config.enableSettings();
        }
        return false;
    }

    /**
     * Registra as interfaces que já estão abertas: ao ligar o plugin, ao mudar uma
     * opção e ao entrar na tela de login.
     */
    private void registerOpenInterfaces() {
        // Antes da tela de login o cliente ainda não montou as interfaces, e
        // getWidgetRoots() lança NullPointerException dentro do próprio cliente.
        if (client.getGameState().getState() < GameState.LOGIN_SCREEN.getState()) {
            return;
        }
        Set<Integer> groups = new HashSet<>();
        Widget[] roots = client.getWidgetRoots();
        if (roots != null) {
            for (Widget root : roots) {
                collectGroups(root, groups);
            }
        }
        for (int groupId : groups) {
            registerInterface(groupId);
        }
    }

    private void collectGroups(Widget widget, Set<Integer> groups) {
        if (widget == null) {
            return;
        }
        groups.add(WidgetUtil.componentToInterface(widget.getId()));
        // Interfaces abertas dentro de outras ficam nos filhos aninhados.
        Widget[] nested = widget.getNestedChildren();
        if (nested != null) {
            for (Widget child : nested) {
                collectGroups(child, groups);
            }
        }
        for (Widget[] children : new Widget[][] {widget.getStaticChildren(), widget.getDynamicChildren()}) {
            if (children != null) {
                for (Widget child : children) {
                    collectGroups(child, groups);
                }
            }
        }
    }

    @Subscribe
    public void onWidgetClosed(WidgetClosed event) {
        openInterfaces.remove(event.getGroupId());
        openOtherInterfaces.remove(event.getGroupId());
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (!isLoginScreenState()) {
            return;
        }
        // A tela de login troca as interfaces: começa a lista de novo.
        loginInterfaceGroups.clear();
        openInterfaces.clear();
        openOtherInterfaces.clear();
        registerOpenInterfaces();
    }

    private boolean contains(int[] values, int target) {
        for (int value : values) {
            if (value == target) {
                return true;
            }
        }
        return false;
    }

    private void scheduleTranslation(int interfaceId) {
        // As opções de diálogo ficam fora por padrão: o Quest Helper lê o texto em
        // inglês logo depois de abrir, e a tradução antecipada pode quebrar o destaque.
        if (interfaceId != InterfaceID.DIALOG_OPTION || config.translateDialogOptionsEarly()) {
            openInterfaces.add(interfaceId);
            return;
        }
        clientThread.invokeLater(() -> clientThread.invokeLater(() -> translateInterface(interfaceId)));
    }

    /**
     * Traduz as interfaces abertas antes de o quadro ser desenhado, para o
     * texto não aparecer em inglês. Com o cache, cada widget custa uma
     * consulta de mapa quando o texto não mudou.
     */
    private void translateOpenInterfaces() {
        // Sem checar o componente 0: em algumas interfaces ele é um fundo oculto
        // e o texto fica em outro componente. Interface fechada não tem widget.
        for (int groupId : openInterfaces) {
            translateInterface(groupId);
        }

        if (!config.enableOtherInterfaces() || openOtherInterfaces.isEmpty()) {
            return;
        }
        TranslationDictionary merged = translationState.merged;
        for (int groupId : openOtherInterfaces) {
            for (int i = 0; i < 100; i++) {
                translateOtherWidget(client.getWidget(groupId, i), merged);
            }
        }
    }

    private boolean isLoginScreenState() {
        GameState state = client.getGameState();
        return state == GameState.LOGIN_SCREEN
            || state == GameState.LOGIN_SCREEN_AUTHENTICATOR
            || state == GameState.LOGGING_IN
            || state == GameState.HOPPING;
    }

    /**
     * Retorna a tradução do texto cru, ou null. Guarda o resultado em cache,
     * porque o mesmo texto aparece em todo quadro.
     */
    private String findOtherTranslation(String text, TranslationDictionary merged) {
        return otherInterfacesCache.get(translationState, "", text, () -> {
            String startTags = preserveStartTags(text);
            // Tag no meio do texto (cor de um valor, por exemplo) se perderia na troca.
            if (text.indexOf('<', startTags.length()) >= 0) {
                return null;
            }
            String lookup = TranslationLookupHelper.normalizeLookupText(TranslationLookupHelper.cleanText(text));
            if (lookup.isEmpty() || merged.isTranslated(lookup)) {
                return null;
            }
            String found = merged.find(lookup);
            // Sem registro aqui: lista de amigos e chat do clã mostram nomes de jogadores.
            return found == null ? null : startTags + found;
        });
    }

    private void translateOtherWidget(Widget widget, TranslationDictionary merged) {
        if (widget == null || widget.isSelfHidden()) {
            return;
        }

        int groupId = WidgetUtil.componentToInterface(widget.getId());
        String text = widget.getText();
        if (text != null && !text.isEmpty()
            && !HANDLED_INTERFACES.contains(groupId)
            && !loginInterfaceGroups.contains(groupId)) {
            String translation = findOtherTranslation(text, merged);
            if (translation != null) {
                widget.setText(translation);
            }
        }

        translateOtherChildren(widget.getChildren(), merged);
        translateOtherChildren(widget.getDynamicChildren(), merged);
        translateOtherChildren(widget.getNestedChildren(), merged);
    }

    private void translateOtherChildren(Widget[] children, TranslationDictionary merged) {
        if (children == null) {
            return;
        }
        for (Widget child : children) {
            translateOtherWidget(child, merged);
        }
    }

    @Subscribe(priority = 1000f)
    public void onMenuOptionClicked(MenuOptionClicked event) {
        if (!config.enableMenuEntries()) {
            return;
        }
        // Outros plugins e o RuneLite recebem a opção original em inglês.
        String englishOption = englishMenuOption(event.getMenuEntry());
        if (englishOption != null) {
            event.getMenuEntry().setOption(englishOption);
        }
    }

    @Subscribe
    public void onBeforeRender(BeforeRender event) {
        translateOpenInterfaces();
        suppressMouseHighlightTooltipForTranslatedMenuOptions();
    }

    private void suppressMouseHighlightTooltipForTranslatedMenuOptions() {
        if (!config.enableMenuEntries() || client.isMenuOpen()) {
            return;
        }

        MenuEntry[] menuEntries = client.getMenuEntries();
        if (menuEntries == null || menuEntries.length == 0) {
            return;
        }

        MenuEntry lastEntry = menuEntries[menuEntries.length - 1];
        if (lastEntry == null) {
            return;
        }

        String englishOption = englishMenuOption(lastEntry);
        if (englishOption == null || !TRIVIAL_MENU_OPTIONS.contains(englishOption)) {
            return;
        }

        int currentCycle = client.getGameCycle();
        int tooltipTimeout = client.getVarcIntValue(VarClientID.TOOLTIP_TIME);
        if (tooltipTimeout <= currentCycle) {
            client.setVarcIntValue(VarClientID.TOOLTIP_TIME, currentCycle + 1);
        }
    }

    private void translateInterface(int interfaceId) {
        for (int i = 0; i < 100; i++) {
            Widget widget = client.getWidget(interfaceId, i);
            if (widget != null) {
                translateWidget(widget, interfaceId);
            }
        }
    }

    private void translateWidget(Widget widget, int interfaceId) {
        if (widget == null) {
            return;
        }

        String text = widget.getText();
        if (text != null && !text.isEmpty()) {
            int lineHeightBefore = widget.getLineHeight();
            applyLineHeightFix(widget, interfaceId, lineHeightBefore);

            String translated = translateWidgetText(text, interfaceId);
            if (translated != null) {
                widget.setText(translated);
                applyLineHeightFix(widget, interfaceId, Math.max(lineHeightBefore, widget.getLineHeight()));
                return;
            }
        }

        Widget[] children = widget.getChildren();
        if (children != null) {
            for (Widget child : children) {
                translateWidget(child, interfaceId);
            }
        }

        Widget[] dynamicChildren = widget.getDynamicChildren();
        if (dynamicChildren != null) {
            for (Widget child : dynamicChildren) {
                translateWidget(child, interfaceId);
            }
        }

        Widget[] nestedChildren = widget.getNestedChildren();
        if (nestedChildren != null) {
            for (Widget child : nestedChildren) {
                translateWidget(child, interfaceId);
            }
        }
    }

    private void applyLineHeightFix(Widget widget, int interfaceId, int referenceLineHeight) {
        if (!config.enableTextWrapFix() || widget == null || referenceLineHeight <= 18) {
            return;
        }

        // Chat/game chat widgets use their own spacing and get visually squashed if we force line height.
        if (interfaceId == CHATBOX_UNIVERSE || interfaceId == CHAT_OVERLAY) {
            return;
        }

        if (widget.getLineHeight() == 18) {
            return;
        }

        widget.setLineHeight(18);
        log.debug("[LineHeightFix] id={} {} -> 18", widget.getId(), referenceLineHeight);
    }

    private String translateWidgetText(String originalText, int interfaceId) {
        TranslationState state = translationState;
        return widgetTextCache.get(
            state,
            // O dicionário muda se a interface é de login, então isso entra na chave.
            (loginInterfaceGroups.contains(interfaceId) ? "L" : "J") + interfaceId + "\u0000",
            originalText,
            () -> findWidgetTranslation(originalText, interfaceId, state)
        );
    }

    private String findWidgetTranslation(String originalText, int interfaceId, TranslationState state) {
        String cleanText = TranslationLookupHelper.normalizeLookupText(TranslationLookupHelper.cleanText(originalText));
        if (cleanText.isEmpty()) {
            return null;
        }

        String numberingPrefix = TranslationLookupHelper.extractNumberingPrefix(cleanText);
        String lookup = numberingPrefix.isEmpty() ? cleanText : TranslationLookupHelper.stripNumberingPrefix(cleanText);

        String specialTranslation = welcomeTranslationService.translateSpecialText(
            lookup,
            interfaceId,
            loginInterfaceGroups,
            state.get(TranslationDomain.WELCOME).translations
        );
        if (specialTranslation != null) {
            return preserveStartTags(originalText) + numberingPrefix + specialTranslation;
        }

        TranslationDictionary dictionary = dictionaryForInterface(interfaceId, state);
        if (dictionary.isTranslated(lookup)) {
            return null;
        }

        String translation = dictionary.find(lookup);
        // O overlay do chat mostra também mensagens de jogadores: não entra no registro.
        if (translation == null && interfaceId != CHAT_OVERLAY) {
            recordMissing(domainForInterface(interfaceId), lookup);
        }

        if (translation != null && interfaceId == CHAT_OVERLAY) {
            translation = normalizeChatOverlayTags(translation);
        }

        return translation == null ? null : preserveStartTags(originalText) + numberingPrefix + translation;
    }

    private String normalizeChatOverlayTags(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        return BR_TAG.matcher(text)
            .replaceAll("\n")
            .replace("</col>", "<col=000000>");
    }

    private TranslationDictionary dictionaryForInterface(int interfaceId, TranslationState state) {
        return state.get(domainForInterface(interfaceId));
    }

    private TranslationDomain domainForInterface(int interfaceId) {
        if (contains(SKILL_GUIDE_INTERFACES, interfaceId)) {
            return TranslationDomain.SKILLS;
        }
        if (interfaceId == QUEST_JOURNAL_MINIMAP
            || interfaceId == QUEST_JOURNAL
            || interfaceId == ACHIEVEMENT_DIARY
            || interfaceId == ACHIEVEMENT_DIARY_SCROLL) {
            return TranslationDomain.QUESTS;
        }
        if (interfaceId == ITEM_PREVIEW || interfaceId == BOOKS_NOTES || interfaceId == CLUE_SCROLL_TEXT) {
            return TranslationDomain.ITEMS;
        }
        if (interfaceId == PLAYER_DESIGN
            || interfaceId == TUTORIAL_PLAYER_EXPERIENCE
            || interfaceId == TUTORIAL_DISPLAY_NAME
            || interfaceId == MACRO_MIME_EMOTES
            || interfaceId == CRAFTING_GOLD) {
            return TranslationDomain.MENU;
        }
        if (interfaceId == CHAT_OVERLAY) {
            return TranslationDomain.GAME_MESSAGE;
        }
        if (interfaceId == WELCOME_SCREEN || loginInterfaceGroups.contains(interfaceId)) {
            return TranslationDomain.WELCOME;
        }
        if (interfaceId == SETTINGS || interfaceId == BANK_PIN) {
            return TranslationDomain.SETTINGS;
        }
        return TranslationDomain.DIALOG;
    }

    @Subscribe
    public void onOverheadTextChanged(OverheadTextChanged event) {
        if (!config.enableOverhead()) {
            return;
        }

        Actor actor = event.getActor();
        if (!(actor instanceof NPC)) {
            return;
        }

        String clean = TranslationLookupHelper.normalizeLookupText(TranslationLookupHelper.cleanText(event.getOverheadText()));
        if (clean.length() <= 3) {
            return;
        }

        TranslationDictionary overheads = translationState.get(TranslationDomain.OVERHEAD);
        if (overheads.isTranslated(clean)) {
            return;
        }
        String translation = overheads.find(clean);
        if (translation == null) {
            recordMissing(TranslationDomain.OVERHEAD, clean);
        } else {
            actor.setOverheadText(translation);
            log.debug("[OverheadTranslate] '{}' -> '{}'", clean, translation);
        }
    }

    // Menu Entry Swapper also handles PostMenuSort and matches the original
    // English option/target text. Translate only after it has completed all
    // built-in, custom, item, NPC, object, UI, and submenu swaps.
    @Subscribe(priority = -1000f)
    public void onPostMenuSort(PostMenuSort event) {
        // O menu é refeito a cada quadro com ele fechado; guarda só o atual.
        originalMenuOptions.clear();
        if (!config.enableMenuEntries()) {
            return;
        }

        translateMenu(client.getMenu());
    }

    @Subscribe(priority = 1000f)
    public void onMenuOpened(MenuOpened event) {
        if (!config.enableMenuEntries()) {
            return;
        }

        // Menu Entry Swapper identifies original entries such as Examine when
        // it creates its Swap left-click/shift-click submenus. Restore those
        // labels before its MenuOpened handler runs.
        restoreEnglishMenu(client.getMenu());
    }

    private void translateMenu(Menu menu) {
        if (menu == null) {
            return;
        }

        for (MenuEntry entry : menu.getMenuEntries()) {
            translateMenuEntry(entry);

            Menu subMenu = entry.getSubMenu();
            if (subMenu != null) {
                translateMenu(subMenu);
            }
        }
    }

    private void translateMenuEntry(MenuEntry entry) {
        if (entry == null) {
            return;
        }

        String option = entry.getOption();
        if (option == null || option.isEmpty()) {
            return;
        }

        String cleanOption = HTML_TAG.matcher(option).replaceAll("").trim();
        String translation = translationState.get(TranslationDomain.MENU).translations.get(cleanOption);
        if (translation != null && !translation.equals(cleanOption)) {
            String translated = option.replaceAll(Pattern.quote(cleanOption), Matcher.quoteReplacement(translation));
            entry.setOption(translated);
            originalMenuOptions.put(menuEntryKey(entry, translated), option);
        }
    }

    /** Chave de uma entrada: texto da opção, alvo e o que o jogo usa para executar a ação. */
    private static String menuEntryKey(MenuEntry entry, String option) {
        return option + '\u0000' + entry.getTarget() + '\u0000' + entry.getType()
            + '\u0000' + entry.getIdentifier() + '\u0000' + entry.getParam0() + '\u0000' + entry.getParam1();
    }

    /** Opção original em inglês da entrada, ou null se ela não foi traduzida. */
    String englishMenuOption(MenuEntry entry) {
        if (entry == null) {
            return null;
        }
        String option = entry.getOption();
        String original = option == null ? null : originalMenuOptions.get(menuEntryKey(entry, option));
        return original != null ? original : reverseTranslateMenuOption(option);
    }

    private void restoreEnglishMenu(Menu menu) {
        if (menu == null) {
            return;
        }

        for (MenuEntry entry : menu.getMenuEntries()) {
            restoreEnglishMenuEntry(entry);

            Menu subMenu = entry.getSubMenu();
            if (subMenu != null) {
                restoreEnglishMenu(subMenu);
            }
        }
    }

    private void restoreEnglishMenuEntry(MenuEntry entry) {
        if (entry == null) {
            return;
        }

        String englishOption = englishMenuOption(entry);
        if (englishOption != null) {
            entry.setOption(englishOption);
        }
    }

    boolean isMenuTranslationEnabled() {
        return config.enableMenuEntries();
    }

    String reverseTranslateMenuOption(String option) {
        if (option == null || option.isEmpty()) {
            return null;
        }

        String normalized = normalizeMenuLookup(option);
        if (normalized.isEmpty()) {
            return null;
        }

        String english = translationState.reverseTranslationsMenu.get(normalized);
        if (english == null || english.equals(normalized)) {
            return null;
        }

        return english;
    }

    private String stripMenuText(String text) {
        return HTML_TAG.matcher(text).replaceAll("").trim();
    }

    private String normalizeMenuLookup(String text) {
        return stripMenuText(text).toLowerCase(Locale.ROOT);
    }

    // Plugins such as Item Charges parse the original English game message
    // synchronously. Translate only after their default-priority listeners
    // have consumed the event, matching the menu integration below.
    @Subscribe(priority = -1000f)
    public void onChatMessage(ChatMessage event) {
        if (!config.enableGameMessages()) {
            return;
        }

        if (!shouldTranslateChatMessage(event.getType())) {
            return;
        }

        String clean = TranslationLookupHelper.cleanText(event.getMessage());
        TranslationDictionary gameMessages = translationState.get(TranslationDomain.GAME_MESSAGE);
        if (clean.isEmpty() || gameMessages.isTranslated(clean)) {
            return;
        }

        String translation = gameMessages.find(clean);
        if (translation == null) {
            recordMissing(TranslationDomain.GAME_MESSAGE, clean);
            return;
        }

        if (shouldDeferRichChatOverlayTranslation(translation)) {
            translateInterface(CHAT_OVERLAY);
            return;
        }

        translation = normalizeChatOverlayTags(translation);

        log.debug("[ChatTranslate] type={} translatedLen={}", event.getType(), translation.length());

        MessageNode messageNode = event.getMessageNode();
        if (messageNode != null) {
            messageNode.setValue(translation);
            messageNode.setRuneLiteFormatMessage(translation);
        }
        event.setMessage(translation);
        client.refreshChat();
    }

    private boolean shouldDeferRichChatOverlayTranslation(String translation) {
        if (translation == null) {
            return false;
        }

        if (!containsRichOverlayFormatting(translation)) {
            return false;
        }

        // Com o overlay do chat na tela, a tradução vai para o widget dele.
        Widget overlay = client.getWidget(CHAT_OVERLAY, 0);
        return overlay != null && !overlay.isHidden();
    }

    private boolean containsRichOverlayFormatting(String text) {
        return text.contains("<br") || text.contains("</col>") || text.contains("\n");
    }

    private boolean shouldTranslateChatMessage(ChatMessageType type) {
        if (type == null) {
            return false;
        }

        switch (type) {
            case GAMEMESSAGE:
            case LEVELUPMESSAGE:
            case ENGINE:
            case SPAM:
            case MESBOX:
            case CONSOLE:
            case DIDYOUKNOW:
            case LOGINLOGOUTNOTIFICATION:
            case ITEM_EXAMINE:
            case NPC_EXAMINE:
            case OBJECT_EXAMINE:
                return true;
            case WELCOME:
                return config.enableWelcome();
            default:
                return false;
        }
    }

    private String preserveStartTags(String text) {
        Matcher matcher = START_TAGS.matcher(text);
        return matcher.find() ? matcher.group() : "";
    }

    @Provides
    OsrsTranslateConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(OsrsTranslateConfig.class);
    }

}
