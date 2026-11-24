# OSRS Translate PT-BR (fork)

Este repositório é um fork do plugin RuneLite
[OSRS Translate PT-BR](https://github.com/walterrezende12-tech/Traducao-OSRS-BR),
criado por Walter Rezende. O plugin traduz o Old School RuneScape para português
brasileiro em tempo real.

O fork remove o espanhol, corrige os dicionários pt-BR, completa a tradução dos
diálogos e muda o código para traduzir mais rápido e sem piscar em inglês.

## Créditos

- **Walter Rezende**: autor do plugin original e dos dicionários de tradução,
  publicados em
  [`osrs-translate-translations`](https://github.com/walterrezende12-tech/osrs-translate-translations).
  Este fork parte desse trabalho.
- **Comunidade do plugin original**: as pessoas que traduziram e corrigiram os
  dicionários pelo [Discord do projeto](https://discord.gg/4eAbaj29Gt).
- **[Old School RuneScape Wiki](https://oldschool.runescape.wiki)**, mantida pela
  Weird Gloop e pelos editores da wiki: fonte das transcrições de diálogo usadas para
  medir e completar a tradução. O conteúdo da wiki segue a licença
  [CC BY-NC-SA 3.0](https://creativecommons.org/licenses/by-nc-sa/3.0/). Veja
  `ferramentas/wiki/ATRIBUICAO.md`.
- **[RuneLite](https://runelite.net)**: cliente e API usados pelo plugin.
- **Jagex**: dona do Old School RuneScape e dos textos originais do jogo.

O código segue a licença BSD 2-Clause do projeto original (arquivo `LICENSE`), com o
copyright de Walter Rezende.

## O que muda neste fork

| Área | Mudança |
|---|---|
| Idioma | Só pt-BR. O espanhol e a opção de idioma saíram. |
| Atraso | As interfaces abertas são traduzidas antes de cada quadro, então o texto não aparece em inglês por um instante. As opções de diálogo continuam no caminho antigo por causa do Quest Helper. |
| Desempenho | A busca por frases com placeholder usa um índice e um cache. A busca de diálogo ficou cerca de 140 vezes mais rápida. |
| Outras interfaces | Opção experimental, desligada por padrão, que traduz interfaces sem tradução própria, como banco, lojas e GE. |
| Correção | Chaves só com pontuação fora do placeholder, como `[name].`, não casam mais com qualquer frase. |
| Dicionários | Correções de acento, de colchete, de termo do jogo e de menu, e 8.723 falas novas. As 126.571 falas das transcrições da wiki têm tradução. |

## Critérios de tradução

1. **PT-BR coloquial, como fala de jogo.** O tom de cada personagem fica: pirata,
   goblin, bêbado e sotaque viram um equivalente em português.
2. **Termos do jogo ficam em inglês.** Isso vale para nomes de skill (`Slayer`,
   `Fishing`, `Hitpoints`), gírias de jogador e termos do universo do jogo
   (`Grand Exchange`, `Wilderness`, `Collection Log`, `clue scroll`,
   `Slayer Master`, `Quest Points`).
3. **Nomes próprios ficam em inglês.** Isso vale para nomes de item, NPC, monstro,
   lugar e quest (`Bronze sword`, `Lumbridge`, `Fishing Guild`).
4. **Placeholders ficam iguais e na mesma ordem.** O plugin preenche `[player]`,
   `[number]` e outros pela posição. Se a frase em português pedir outra ordem, a
   frase muda, e o placeholder não.
5. **Menus usam o infinitivo**: "Comprar", "Falar com", "Pegar".
6. **A pontuação do inglês fica.** Uma tradução sem o ponto final quebra a busca do
   plugin.

## Estrutura

| Pasta | Conteúdo |
|---|---|
| `src/` | Código do plugin |
| `traducoes/pt-BR/` | Dicionários pt-BR corrigidos |
| `traducoes/correcao/revisar/` | Traduções que precisam de revisão manual |
| `ferramentas/wiki/` | Scripts que baixam as transcrições da wiki e medem a cobertura |

## Funcionalidades

- Tradução de diálogos, opções de resposta e falas acima da cabeça.
- Tradução de menus, mensagens do jogo, Skill Guide, Quest Journal, diários de
  conquistas, clue scrolls, livros e notas, tela de boas-vindas e configurações.
- Compatibilidade com Quest Helper, Menu Entry Swapper, Item Charges, Friend Notes,
  Clue Scroll e Examine.
- Modo desenvolvedor para testar dicionários locais com recarga automática.

## Traduções remotas

Por padrão, o plugin baixa os dicionários do repositório original
[`osrs-translate-translations`](https://github.com/walterrezende12-tech/osrs-translate-translations).
Ele verifica atualizações ao iniciar e depois uma vez por hora, e valida cada arquivo
com SHA-256. Se a atualização falha, o último cache válido continua em uso. O cache
fica em `~/.runelite/osrs-translate/translations/<idioma>/`.

As requisições vão para o GitHub e enviam só os dados normais de uma conexão HTTPS,
como endereço IP e `User-Agent`. Nenhum dado da conta, do personagem ou do jogo é
enviado.

## Como testar o fork

O Plugin Hub instala só a versão original. Para rodar o fork, o RuneLite precisa
abrir em modo de desenvolvedor, e o lançador oficial desliga esse modo.

Aviso: desinstale a versão original do Plugin Hub antes. As duas usam a mesma classe
e entram em conflito.

1. Gere o jar: `./gradlew jar`.
2. Copie `build/libs/*.jar` para `%USERPROFILE%\.runelite\sideloaded-plugins\`.
3. Abra o cliente sem o lançador, com o Java que vem no RuneLite:

   ```bat
   "%LOCALAPPDATA%\RuneLite\jre\bin\java.exe" -ea -Xmx768m -Xss2m -cp "%USERPROFILE%\.runelite\repository2\*" net.runelite.client.RuneLite --developer-mode
   ```

4. Na configuração do plugin, ligue **Modo desenvolvedor**, escolha a fonte
   **Local** e informe o caminho de `traducoes/pt-BR`.

Com conta Jagex, o cliente aberto assim não faz login sozinho. Aviso: o passo a
seguir grava o token da conta em texto puro em
`%USERPROFILE%\.runelite\credentials.properties`. Quem copiar esse arquivo entra na
conta. Apague o arquivo quando terminar os testes.

1. Abra **RuneLite (configure)**, escreva `--insecure-write-credentials` em
   **Client arguments** e salve.
2. Abra o jogo uma vez pelo Jagex Launcher e feche.
3. Apague o argumento no **RuneLite (configure)**.
4. Abra o cliente pelo comando do passo 3 acima.

## Contribuir com o projeto original

Correções que servem a todos os jogadores devem ir para o projeto original, pelo
[Discord](https://discord.gg/4eAbaj29Gt) ou pelas
[issues](https://github.com/walterrezende12-tech/Traducao-OSRS-BR/issues).
