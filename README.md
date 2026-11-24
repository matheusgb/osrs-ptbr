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
apague o arquivo: dê dois cliques em `limpar-credencial.bat`.

Depois de mudar o código, gere o jar de novo com `./scripts/gerar-dist.sh` (Linux ou
WSL, precisa de JDK 11 ou mais novo) e rode o `instalar.bat`.

## Atualizar as traduções

Quando o jogo ganhar diálogos novos, atualize a base pela OSRS Wiki. O jogo atualiza
às quartas-feiras, e a wiki costuma transcrever os diálogos novos em poucos dias.

Aviso: o download faz uma requisição por vez, com 8 a 12 s de pausa, para não
sobrecarregar a wiki. Não reduza a pausa e não rode duas cópias ao mesmo tempo. A
conferência leva cerca de 25 minutos.

Rode os comandos dentro de `ferramentas/wiki`, no Linux ou no WSL:

1. Baixe as transcrições novas e as editadas: `python3 baixar_transcricoes.py --atualizar`.
2. Extraia as falas: `python3 extrair_falas.py`.
3. Meça o que falta: `python3 cobertura.py`. O resumo sai na tela, e as falas sem
   tradução ficam em `dados/faltando.json`.
4. Classifique: `python3 classificar.py`. Sem IA, o script separa o que fica igual ao
   inglês (`dados/automaticas.json`) do que precisa de tradução
   (`dados/para_traduzir.json`).
5. Traduza as frases de `dados/para_traduzir.json` num arquivo JSON
   `{"texto em inglês": "tradução"}`, seguindo os
   [critérios de tradução](#critérios-de-tradução). O Claude Code faz isso se você
   pedir: "traduza o para_traduzir.json seguindo os critérios do README".
6. Valide e aplique:
   1. `python3 validar_traducoes.py dados/para_traduzir.json novas.json` (corrija até dar 0 erros);
   2. `python3 aplicar_traducoes.py dados/automaticas.json novas.json`. Tradução que
      falha em alguma regra não entra e vai para `dados/rejeitadas.json`.
7. Faça commit e push, e rode o `instalar.bat` no Windows para copiar as traduções novas.

### Regras automáticas

As regras ficam em `ferramentas/wiki/regras.py` e têm testes (`test_regras.py`). A mesma
entrada sempre recebe a mesma decisão.

- **Glossário:** `traducoes/glossario.json` tem os termos que ficam em inglês: nomes de
  item, NPC, monstro, quest, lugar, objeto e loja da wiki, mais a lista editável
  `traducoes/glossario_manual.json` (skills e termos do jogo). Gere de novo com
  `python3 gerar_glossario.py` (13 requisições à wiki).
- **Classificação:** texto que é inteiro um termo do glossário, ou só número, símbolo e
  placeholder, fica igual ao inglês.
- **Validação:** cada tradução precisa ter placeholders iguais e na mesma ordem, manter
  os nomes próprios do glossário, manter a pontuação final (ponto, interrogação e
  exclamação contam como a mesma; dois-pontos precisam ficar), não ter palavra comum do
  inglês sobrando, não ter palavra sem acento da lista e, no menu, usar o infinitivo.
- **Auditoria:** `python3 aplicar_traducoes.py --auditar` roda a validação nos JSONs
  atuais e grava `dados/auditoria.json`, sem mudar nada.

Para corrigir uma tradução que já existe, edite o JSON em `traducoes/pt-BR` e rode o
`instalar.bat`. Com o jogo aberto, o plugin recarrega os JSONs da pasta instalada em
2 segundos.

As transcrições cobrem diálogos, opções, caixas de mensagem e falas acima da cabeça.
Examines e textos de interface vêm de outras fontes:

1. Examines de item, NPC e monstro: `python3 baixar_examines.py` (7 requisições à
   API Bucket da wiki).
2. Textos de interface do cache do jogo (configurações, tooltips, conquistas,
   relíquias). Copie a pasta `%USERPROFILE%\.runelite\jagexcache\oldschool\LIVE`
   para uma pasta temporária, porque a ferramenta abre os arquivos para escrita, e rode
   na raiz do repositório:
   `./gradlew -p ferramentas/cache run --args="<cópia do cache> $PWD/ferramentas/cache/dados/textos_cache.json"`.
3. Liste o que falta: `python3 cobertura.py`. Além de `dados/faltando.json`, saem `dados/faltando_examines.json`
   e `dados/faltando_interfaces.json`.
4. Traduza, valide e aplique como nos passos 4 e 5 acima. O `aplicar_traducoes.py`
   põe examines em `translations_game_message.json` e textos de interface em
   `translations_settings.json`.

Aviso: aplique as traduções antes de rodar `cobertura.py` de
novo. O `aplicar_traducoes.py` usa essas listas para saber o arquivo de destino.

### Textos que faltam enquanto você joga

Mensagens do chat e alguns textos de missão vêm do servidor do jogo e não estão em
nenhuma fonte pública. Para esses, o plugin anota sozinho cada texto que aparece em
inglês, em `%USERPROFILE%\.runelite\osrs-translate-ptbr\sem-traducao.jsonl` (opção
"Registrar textos sem tradução", ligada por padrão). O chat de jogadores nunca entra.

1. Jogue normalmente.
2. Importe o registro dentro de `ferramentas/wiki`:
   `python3 importar_registro.py /mnt/c/Users/<usuário>/.runelite/osrs-translate-ptbr/sem-traducao.jsonl`.
   Sai `dados/faltando_jogo.json`, já sem o que tem tradução.
3. Traduza, valide e aplique como nos passos 4 e 5 acima. Cada texto vai para o JSON
   em que o plugin procurou por ele.
4. Rode o `instalar.bat`.

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
| Atraso | As interfaces abertas são traduzidas antes de cada quadro, então o texto não aparece em inglês por um instante. As opções de diálogo ficam no caminho antigo por causa do Quest Helper; a opção "Traduzir opções de diálogo sem atraso" (desligada) muda isso para quem quiser testar. |
| Desempenho | A busca por frases com placeholder usa um índice e um cache. A busca de diálogo ficou cerca de 140 vezes mais rápida. |
| Outras interfaces | Opção, ligada por padrão, que traduz interfaces sem tradução própria, como banco, lojas e GE. Ela pode atrapalhar plugins que leem o texto em inglês. |
| Fonte das traduções | O plugin lê só os JSONs locais que o instalador copia. Ele não baixa nada da internet. |
| Correções | Chaves só com pontuação fora do placeholder, como `[name].`, não casam mais com qualquer frase. O menu guarda a opção original de cada entrada, então opções com a mesma tradução ("Subir") voltam ao inglês certo para o Menu Entry Swapper. |
| Escolhas | Em falas como "Good day, [sir/madam]", a palavra escolhida pelo jogo também é traduzida ("Bom dia, senhora"). |
| Dicionários | Correções de acento, de colchete, de placeholder, de termo do jogo e de menu, e 8.723 falas novas. As 126.571 falas das transcrições da wiki têm tradução, além de examines e textos de interface do cache. |

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
| `ferramentas/wiki/` | Scripts que baixam transcrições e examines da wiki, medem a cobertura e aplicam traduções |
| `ferramentas/cache/` | Ferramenta Java que extrai os textos de interface do cache do jogo |

## Funcionalidades

- Tradução de diálogos, opções de resposta e falas acima da cabeça.
- Tradução de menus, mensagens do jogo, Skill Guide, Quest Journal, diários de
  conquistas, clue scrolls, livros e notas, tela de boas-vindas e configurações.
- Compatibilidade com Quest Helper, Menu Entry Swapper, Item Charges, Friend Notes,
  Clue Scroll e Examine.
- Recarga automática dos JSONs locais: uma tradução editada vale no jogo em 2 segundos.
