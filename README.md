# OSRS Translate PT-BR (fork)

Este repositório é um fork do plugin RuneLite
[OSRS Translate PT-BR](https://github.com/walterrezende12-tech/Traducao-OSRS-BR),
criado por Walter Rezende. O plugin traduz o Old School RuneScape para português
brasileiro em tempo real.

O fork remove o espanhol, corrige os dicionários pt-BR, completa a tradução dos
diálogos e muda o código para traduzir mais rápido e sem piscar em inglês.

**Uso pessoal.** Não tenho intenção de publicar este plugin no Plugin Hub. Ele existe
para eu jogar em português no meu PC, e o `instalar.bat` deixa isso plug and play.

## Instalação (Windows)

Aviso: se a versão original estiver instalada pelo Plugin Hub, desinstale antes. As
duas entram em conflito.

1. Instale o RuneLite e abra o jogo uma vez pelo Jagex Launcher.
2. Dê dois cliques em `instalar.bat`. Se a sua conta for Jagex, o instalador oferece
   criar o arquivo de credencial (veja o aviso abaixo).
3. Abra o jogo pelo atalho **RuneLite PT-BR** na área de trabalho.

O plugin já vem configurado: traduções locais e "Traduzir outras interfaces" ligado.
Para atualizar o plugin ou as traduções, rode o `instalar.bat` de novo.

O instalador:

- copia `dist/osrs-translate-ptbr.jar` para `%USERPROFILE%\.runelite\sideloaded-plugins`;
- copia `traducoes/pt-BR` para `%USERPROFILE%\.runelite\osrs-translate-ptbr\pt-BR`;
- cria `%USERPROFILE%\.runelite\RuneLite-PTBR.bat` e o atalho na área de trabalho.

O atalho abre o RuneLite direto pelo Java, em modo de desenvolvedor. Esse é o único
modo que carrega plugins de fora do Plugin Hub, e o lançador oficial desliga esse modo.

Aviso sobre conta Jagex: fora do Jagex Launcher, o login precisa do arquivo
`%USERPROFILE%\.runelite\credentials.properties`. Esse arquivo guarda o token da conta
em texto puro. Quem copiar esse arquivo entra na sua conta. Para cancelar o acesso,
apague o arquivo.

Depois de mudar o código, gere o jar de novo com `./scripts/gerar-dist.sh` (Linux ou
WSL, precisa de JDK 11 ou mais novo) e rode o `instalar.bat`.

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

## Licença

- **Código e traduções deste fork:** BSD 2-Clause (arquivo `LICENSE`). O uso é livre,
  inclusive para modificar e redistribuir. A única condição é manter o aviso de
  copyright: o de Walter Rezende pelo projeto original e o de matheusgb pelas
  alterações e traduções novas deste fork.
- **Dados da OSRS Wiki** (`ferramentas/wiki/dados` e as traduções feitas a partir das
  transcrições): CC BY-NC-SA 3.0, uso não comercial com crédito à wiki.
- **Dicionários do projeto original:** a maior parte (95%) estava no repositório do
  plugin, sob a BSD 2-Clause. O resto, cerca de 8.500 frases (diário de quest e
  configurações), veio do repositório `osrs-translate-translations`, que não declara
  licença.

## O que muda neste fork

| Área | Mudança |
|---|---|
| Idioma | Só pt-BR. O espanhol e a opção de idioma saíram. |
| Atraso | As interfaces abertas são traduzidas antes de cada quadro, então o texto não aparece em inglês por um instante. As opções de diálogo continuam no caminho antigo por causa do Quest Helper. |
| Desempenho | A busca por frases com placeholder usa um índice e um cache. A busca de diálogo ficou cerca de 140 vezes mais rápida. |
| Outras interfaces | Opção, ligada por padrão, que traduz interfaces sem tradução própria, como banco, lojas e GE. Ela pode atrapalhar plugins que leem o texto em inglês. |
| Fonte das traduções | Por padrão, o plugin lê os JSONs locais que o instalador copia e não baixa nada do repositório original. |
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
| `dist/` | Jar pronto que o instalador copia |
| `scripts/` | `instalar.ps1` (chamado pelo `instalar.bat`) e `gerar-dist.sh` |
| `traducoes/pt-BR/` | Dicionários pt-BR corrigidos |
| `traducoes/correcao/revisar/` | Traduções que precisam de revisão manual |
| `ferramentas/wiki/` | Scripts que baixam as transcrições da wiki e medem a cobertura |

## Funcionalidades

- Tradução de diálogos, opções de resposta e falas acima da cabeça.
- Tradução de menus, mensagens do jogo, Skill Guide, Quest Journal, diários de
  conquistas, clue scrolls, livros e notas, tela de boas-vindas e configurações.
- Compatibilidade com Quest Helper, Menu Entry Swapper, Item Charges, Friend Notes,
  Clue Scroll e Examine.
- Recarga automática dos JSONs locais: uma tradução editada vale no jogo em 2 segundos.

## Traduções remotas (opcional)

Na configuração do plugin, a fonte **Remota** baixa os dicionários do repositório
original
[`osrs-translate-translations`](https://github.com/walterrezende12-tech/osrs-translate-translations)
em vez de usar os locais. Nesse modo, o plugin verifica atualizações ao iniciar e
depois uma vez por hora, e valida cada arquivo com SHA-256. As requisições vão para o
GitHub e enviam só os dados normais de uma conexão HTTPS, como endereço IP e
`User-Agent`.
