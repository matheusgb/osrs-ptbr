# Documentação técnica

[Voltar ao README](../README.md)

1. [O que muda neste fork](#o-que-muda-neste-fork)
2. [Como o plugin funciona](#como-o-plugin-funciona)
3. [Estrutura do repositório](#estrutura-do-repositório)
4. [Instalação por dentro](#instalação-por-dentro)
5. [Gerar o plugin](#gerar-o-plugin)
6. [Atualizar as traduções](#atualizar-as-traduções)
7. [Regras automáticas](#regras-automáticas)
8. [Examines e textos de interface](#examines-e-textos-de-interface)
9. [Textos que faltam enquanto você joga](#textos-que-faltam-enquanto-você-joga)

Os critérios de tradução ficam em [Como contribuir](CONTRIBUIR.md#critérios-de-tradução).

## O que muda neste fork

| Área | Mudança |
|---|---|
| Idioma | Só pt-BR. O espanhol e a opção de idioma saíram. |
| Atraso | As interfaces abertas são traduzidas antes de cada quadro, então o texto não aparece em inglês por um instante. As opções de diálogo ficam no caminho antigo por causa do Quest Helper; a opção "Traduzir opções de diálogo sem atraso" (desligada) muda isso para quem quiser testar. |
| Desempenho | A busca por frases com placeholder usa um índice e um cache. A busca de diálogo ficou cerca de 140 vezes mais rápida. |
| Outras interfaces | Opção, ligada por padrão, que traduz interfaces sem tradução própria, como banco, lojas e GE. Ela pode atrapalhar plugins que leem o texto em inglês. |
| Fonte das traduções | O plugin lê só os JSONs locais que o instalador copia. Ele não baixa nada da internet. |
| Correções | Chaves só com pontuação fora do placeholder, como `[name].`, não casam mais com qualquer frase. O menu guarda a opção original de cada entrada, então opções com a mesma tradução ("Subir") voltam ao inglês certo para o Menu Entry Swapper. |
| Escolhas | Em falas como "Good day, [sir/madam]", a opção escolhida pelo jogo também é traduzida ("Bom dia, senhora"). |
| Dicionários | Correções de acento, de colchete, de placeholder, de termo do jogo e de menu, e as falas novas. As 126.571 falas das transcrições da wiki têm tradução, além de examines e textos de interface do cache. |

## Como o plugin funciona

- Os dicionários são JSONs `{"texto em inglês": "tradução"}` em `traducoes/pt-BR`, um por
  área: diálogos, mensagens, menu, falas acima da cabeça, quests, configurações, skills,
  itens e tela de boas-vindas.
- Uma chave pode ter placeholders, como `[player]` ou `[number]`. O plugin preenche cada
  um pela posição, com o texto que o jogo escreveu naquele lugar.
- Uma lista de escolha, como `[sir/madam]` ou `[hasn't/haven't]`, pode vir traduzida na
  mesma ordem (`[senhor/senhora]`). O plugin acha a opção que o jogo escreveu e usa a
  opção traduzida na mesma posição. Se a lista vier em inglês, o plugin traduz só
  palavras de tratamento e gênero (`ChoiceTranslator.java`).
- Com o jogo aberto, o plugin recarrega os JSONs da pasta instalada a cada 2 segundos.
- Funciona junto com Quest Helper, Menu Entry Swapper, Item Charges, Friend Notes, Clue
  Scroll e Examine.

## Estrutura do repositório

| Pasta | Conteúdo |
|---|---|
| `src/` | Código do plugin (Java 11) e testes |
| `dist/` | Jar pronto que o instalador copia |
| `scripts/` | `instalar.ps1`, `desinstalar.ps1` e `gerar-dist.sh` |
| `traducoes/pt-BR/` | Dicionários pt-BR |
| `traducoes/` | Glossário (`glossario.json`, `glossario_manual.json`) e `palavras_comuns.json` |
| `ferramentas/wiki/` | Scripts que baixam dados da wiki, medem a cobertura, validam e aplicam traduções |
| `ferramentas/cache/` | Ferramenta Java que extrai os textos de interface do cache do jogo |

## Instalação por dentro

O `instalar.bat` chama `scripts/instalar.ps1`, que:

1. copia `dist/osrs-translate-ptbr.jar` para `%USERPROFILE%\.runelite\sideloaded-plugins`;
2. copia `traducoes/pt-BR` para `%USERPROFILE%\.runelite\osrs-translate-ptbr\pt-BR`;
3. cria `%USERPROFILE%\.runelite\RuneLite-PTBR.bat`, o atalho na área de trabalho e o
   `RuneLite-PTBR-Steam.vbs`;
4. oferece criar a credencial da conta Jagex.

O instalador também cria `RuneLite-PTBR-Steam.vbs`, para abrir pela Steam (veja o
README). Ele abre o jogo sem janela de terminal e só termina quando o jogo fecha, então a
Steam conta as horas.

O atalho abre o RuneLite direto pelo Java, em modo de desenvolvedor. Esse é o único modo
que carrega plugins de fora do Plugin Hub, e o lançador oficial desliga esse modo.

Aviso sobre conta Jagex: fora do Jagex Launcher, o login precisa do arquivo
`%USERPROFILE%\.runelite\credentials.properties`. Esse arquivo guarda o token da conta em
texto puro. Quem copiar esse arquivo entra na sua conta. Para cancelar o acesso, apague o
arquivo com `limpar-credencial.bat`.

O `desinstalar.bat` remove o jar, as traduções, o atalho, o `RuneLite-PTBR.bat` e o
`RuneLite-PTBR-Steam.vbs`. Ele não
apaga a credencial nem o registro `sem-traducao.jsonl`.

## Gerar o plugin

Depois de mudar o código:

1. Rode os testes: `./gradlew test`.
2. Gere o jar: `./scripts/gerar-dist.sh` (Linux ou WSL, precisa de JDK 11 ou mais novo).
3. Rode o `instalar.bat` no Windows.

## Atualizar as traduções

Quando o jogo ganhar diálogos novos, atualize a base pela OSRS Wiki. O jogo atualiza às
quartas-feiras, e a wiki costuma transcrever os diálogos novos em poucos dias.

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
   [critérios de tradução](CONTRIBUIR.md#critérios-de-tradução). O Claude Code faz isso
   se você pedir: "traduza o para_traduzir.json seguindo os critérios do CONTRIBUIR.md".
6. Valide e aplique:
   1. `python3 validar_traducoes.py dados/para_traduzir.json novas.json` (corrija até dar
      0 erros);
   2. `python3 aplicar_traducoes.py dados/automaticas.json novas.json`. Tradução que
      falha em alguma regra não entra e vai para `dados/rejeitadas.json`.
7. Faça commit e push, e rode o `instalar.bat` no Windows.

Para corrigir traduções que já existem em lote, use
`python3 corrigir_traducoes.py <lista.json> <correcoes.json>`. Sem opção, ele só troca o
que ainda falha nas regras. Com `--melhorar`, troca também traduções que já passam.

## Regras automáticas

As regras ficam em `ferramentas/wiki/regras.py` e têm testes (`test_regras.py`). A mesma
entrada sempre recebe a mesma decisão.

- **Glossário:** `traducoes/glossario.json` tem os termos que ficam em inglês: nomes de
  item, NPC, monstro, quest, lugar, objeto, loja, magia e atividade da wiki, mais a lista
  editável `traducoes/glossario_manual.json` (skills e termos do jogo). Gere de novo com
  `python3 gerar_glossario.py` (15 requisições à wiki).
- **Palavras comuns:** `traducoes/palavras_comuns.json` lista nomes que também são
  palavras comuns ("King", "Tree"). Sozinhos, eles não são cobrados como nome.
- **Classificação:** texto que é inteiro um termo do glossário, ou só número, símbolo e
  placeholder, fica igual ao inglês.
- **Validação:** cada tradução precisa:
  - ter os placeholders iguais e na mesma ordem (lista de escolha com palavras pode vir
    traduzida, com o mesmo número de opções);
  - manter os nomes próprios do glossário;
  - manter em inglês os termos do jogo de `glossario_manual.json` ("clue scroll", "Slayer
    Master", "XP"), mesmo quando o inglês usa minúscula;
  - manter a pontuação final (ponto, interrogação e exclamação contam como a mesma;
    dois-pontos precisam ficar);
  - não ter palavra comum do inglês sobrando nem palavra sem acento da lista;
  - não pendurar o nome em inglês entre parênteses nem misturar línguas num nome
    ("Chambers de Xeric");
  - no menu, usar o infinitivo.
- **Auditoria:** `python3 aplicar_traducoes.py --auditar` roda a validação nos JSONs
  atuais e grava `dados/auditoria.json` e `dados/auditoria_lista.json`, sem mudar nada.

## Examines e textos de interface

As transcrições cobrem diálogos, opções, caixas de mensagem e falas acima da cabeça.
Examines e textos de interface vêm de outras fontes:

1. Examines de item, NPC e monstro: `python3 baixar_examines.py` (7 requisições à API
   Bucket da wiki).
2. Textos de interface do cache do jogo (configurações, tooltips, conquistas, relíquias).
   Copie a pasta `%USERPROFILE%\.runelite\jagexcache\oldschool\LIVE` para uma pasta
   temporária, porque a ferramenta abre os arquivos para escrita, e rode na raiz do
   repositório:
   `./gradlew -p ferramentas/cache run --args="<cópia do cache> $PWD/ferramentas/cache/dados/textos_cache.json"`.
3. Liste o que falta: `python3 cobertura.py`. Além de `dados/faltando.json`, saem
   `dados/faltando_examines.json` e `dados/faltando_interfaces.json`.
4. Traduza, valide e aplique como nos passos 5 e 6 de
   [Atualizar as traduções](#atualizar-as-traduções). O `aplicar_traducoes.py` põe
   examines em `translations_game_message.json` e textos de interface em
   `translations_settings.json`.

Aviso: aplique as traduções antes de rodar `cobertura.py` de novo. O
`aplicar_traducoes.py` usa essas listas para saber o arquivo de destino.

## Textos que faltam enquanto você joga

Mensagens do chat e alguns textos de missão vêm do servidor do jogo e não estão em
nenhuma fonte pública. Para esses, o plugin anota sozinho cada texto que aparece em
inglês, em `%USERPROFILE%\.runelite\osrs-translate-ptbr\sem-traducao.jsonl` (opção
"Registrar textos sem tradução", ligada por padrão). O chat de jogadores, o overlay de
chat e as listas de amigos e clã nunca entram.

1. Jogue normalmente.
2. Importe o registro dentro de `ferramentas/wiki`:
   `python3 importar_registro.py /mnt/c/Users/<usuário>/.runelite/osrs-translate-ptbr/sem-traducao.jsonl`.
   Sai `dados/faltando_jogo.json`, já sem o que tem tradução.
3. Traduza, valide e aplique como nos passos 5 e 6 de
   [Atualizar as traduções](#atualizar-as-traduções). Cada texto vai para o JSON em que
   o plugin procurou por ele.
4. Rode o `instalar.bat`.
