# CLAUDE.md

Guia para o Claude Code (e para quem trabalha neste repositório).

## O que é

Plugin RuneLite (Java 11, Gradle) que traduz o Old School RuneScape para pt-BR em tempo
real. Os dicionários são JSONs `{"inglês": "tradução"}` em `traducoes/pt-BR`, um por área
(ver `TranslationDomain.java`). O plugin roda por sideload, sem Plugin Hub: o
`instalar.bat` copia o jar e os JSONs para `%USERPROFILE%\.runelite`.

Documentação: `README.md` (usuário), `docs/TECNICO.md` (como funciona e fluxos),
`docs/CONTRIBUIR.md` (critérios de tradução), `docs/LICENCA.md`.

## Comandos

| Tarefa | Comando |
|---|---|
| Testes do plugin | `./gradlew test` |
| Gerar o jar | `./scripts/gerar-dist.sh` (atualiza `dist/`, que vai no commit) |
| Testes das ferramentas | `python3 ferramentas/wiki/test_regras.py` (e os outros `test_*.py`) |
| Auditar os dicionários | `cd ferramentas/wiki && python3 aplicar_traducoes.py --auditar` |
| Validar traduções novas | `python3 validar_traducoes.py <lista.json> <novas.json>` |
| Aplicar traduções novas | `python3 aplicar_traducoes.py <novas.json>` |
| Corrigir as existentes | `python3 corrigir_traducoes.py <lista.json> <correcoes.json> [--melhorar]` |
| Importar textos vistos no jogo | `python3 importar_registro.py <sem-traducao.jsonl>` |

Rode os scripts Python dentro de `ferramentas/wiki`. Eles usam só a biblioteca padrão.

## Critérios de tradução

1. PT-BR coloquial, com o tom de cada personagem.
2. Ficam em inglês, inteiros: skills, termos do jogo (`clue scroll`, `Slayer Master`, `XP`)
   e nomes de item, NPC, monstro, lugar, quest, magia, raid e minigame. Nada de nome
   traduzido com o original entre parênteses, nem nome misturado ("Chambers de Xeric").
3. Placeholders (`[player]`, `[number]`) iguais e na mesma ordem. Lista de escolha
   (`[hasn't/haven't]`) pode vir traduzida, com o mesmo número de opções e na mesma ordem.
4. Menu no infinitivo ("Comprar", "Falar com").
5. Pontuação final igual à do inglês.

`ferramentas/wiki/regras.py` confere isso. Toda tradução passa por ele antes de entrar.
Os termos que ficam em inglês estão em `traducoes/glossario.json` (gerado) e
`traducoes/glossario_manual.json` (editável).

## Regras que não se quebram

- **Privacidade do registro:** `MissingTextLog` nunca grava o chat de jogadores, o overlay
  de chat (grupo 263) nem as listas de amigos e clã. Ao mexer em `recordMissing`, mantenha
  essas exclusões.
- **OSRS Wiki:** uma requisição por vez, com 8 a 12 s de pausa e User-Agent identificado
  (`comum.py`). Não reduza a pausa e não rode downloads em paralelo.
- **Chisel (chisel.weirdgloop.org):** o `robots.txt` proíbe acesso automático. Não faça
  scraping.
- **Credencial:** `credentials.properties` guarda o token da conta Jagex em texto puro.
  Nunca leia, copie ou mande esse arquivo para lugar nenhum.
- **Dados da wiki** são CC BY-NC-SA 3.0. Mantenha o crédito em `ferramentas/wiki/ATRIBUICAO.md`.

## Tradução em lote com IA

O validador confere a forma, não o sentido. Agentes já passaram nele com atalhos que
pioram o texto: nome em inglês entre parênteses, placeholder movido por script, nome
misturado, nome enfiado numa frase que não fala dele e nome de item traduzido.

1. No pedido ao agente, proíba correção por script (regex, dicionário de troca) e esses
   atalhos.
2. Depois de cada lote, leia uma amostra por tipo de erro antes de aplicar.
3. Quando aparecer um atalho novo, transforme em regra em `regras.py`, com teste.

## Como o plugin acha uma tradução

1. Busca exata no dicionário da área.
2. Busca normalizada (minúscula, espaço após "...") no mesmo dicionário.
3. Padrões com placeholder, nos domínios que usam placeholder.
4. Busca exata e normalizada nos dicionários das outras áreas
   (`TranslationState.findInOtherDomains`), sem o menu e sem padrões. O overlay de chat
   (grupo 263) não usa este passo: as mensagens dele passam por `onChatMessage`.
5. Se nada achar, `recordMissing` anota o texto em `sem-traducao.jsonl`.

Interfaces sem domínio próprio ("Outras interfaces") usam `TranslationState.merged`, todos
os dicionários juntos na ordem de `MERGE_ORDER`.

## Convenções

- Commits e documentação em pt-BR.
- Eventos do RuneLite são registrados à mão no `startUp` (`subscribe(...)`), não com
  `@Subscribe`: em plugin sideloaded, o `@Subscribe` gera o aviso "Invalid caller".
- Depois de mudar o código, gere o jar e inclua `dist/` no commit: o instalador usa esse jar.
