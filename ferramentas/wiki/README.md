# Base de diálogos da OSRS Wiki

Estes scripts montam uma base local com as falas das páginas `Transcript:` da
[OSRS Wiki](https://oldschool.runescape.wiki) e medem quanto delas já tem tradução em
`traducoes/pt-BR`. Os scripts usam só a biblioteca padrão do Python 3.

## Como usar

Aviso: o download faz uma requisição por vez, com 8 a 12 s de pausa entre elas. A
primeira execução leva cerca de 25 minutos. Não reduza a pausa e não rode duas
cópias ao mesmo tempo, para não sobrecarregar a wiki.

1. Baixe as transcrições: `python3 baixar_transcricoes.py`. Para buscar também as
   páginas editadas desde o último download, use `--atualizar`.
2. Extraia as falas: `python3 extrair_falas.py`.
3. Meça a cobertura: `python3 cobertura.py`.
4. Leia o resumo em `dados/cobertura.md` e a lista do que falta em
   `dados/faltando.json`.
5. Traduza o que falta num JSON `{"texto em inglês": "tradução"}`, valide com
   `python3 validar_traducoes.py dados/faltando.json novas.json` e aplique com
   `python3 aplicar_traducoes.py novas.json`.

## Arquivos gerados

| Arquivo | Conteúdo |
|---|---|
| `dados/transcricoes.jsonl` | Wikitext de cada página, com o número da revisão |
| `dados/falas.json` | Cada fala única, o tipo e as páginas onde aparece |
| `dados/faltando.json` | Falas sem tradução nos JSONs pt-BR |
| `dados/cobertura.md` | Resumo por tipo e as páginas com mais falas sem tradução |

O tipo de cada fala é `fala` (NPC ou jogador), `opcao` (opção de diálogo), `caixa`
(caixa de mensagem) ou `overhead` (texto acima da cabeça).

## Testes

Rode `python3 test_extrair_falas.py`.

## Licença

O conteúdo baixado é da OSRS Wiki e segue a licença dela. Veja `ATRIBUICAO.md`.
