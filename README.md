# Old School RuneScape em português

Este plugin do RuneLite traduz o Old School RuneScape para português do Brasil enquanto
você joga: diálogos, missões, menus, mensagens e interfaces.

Nomes de item, NPC, lugar, missão e skill ficam em inglês, do jeito que a comunidade usa.

> **Uso pessoal.** Este plugin não está no Plugin Hub e não vai ser publicado. Ele roda
> só no seu PC, com o instalador abaixo.

## Como usar (Windows)

Antes de começar:

- Instale o [RuneLite](https://runelite.net) e abra o jogo pelo menos uma vez.
- Se você usa o plugin original "OSRS Translate" do Plugin Hub, desinstale esse plugin
  no RuneLite. Os dois juntos entram em conflito.

1. Baixe este projeto: clique no botão verde **Code** e depois em **Download ZIP**.
2. Extraia o ZIP numa pasta qualquer (clique com o botão direito, depois **Extrair tudo**).
3. Dentro da pasta, dê dois cliques em **`instalar.bat`** e siga o que aparece na tela.
4. Abra o jogo pelo atalho **RuneLite PT-BR** que apareceu na área de trabalho.

Pronto. O jogo abre em português.

## Textos que ainda aparecem em inglês

Alguns textos do jogo não têm tradução ainda. Enquanto você joga, o plugin anota sozinho
cada texto que aparece em inglês, no arquivo:

`%USERPROFILE%\.runelite\osrs-translate-ptbr\sem-traducao.jsonl`

- O arquivo fica só no seu PC. Nada é enviado pela internet.
- O chat de outros jogadores e as listas de amigos e clã nunca entram.
- Para ajudar a traduzir esses textos, mande o arquivo como explica
  [Como contribuir](docs/CONTRIBUIR.md#mandar-a-lista-de-textos-sem-tradução).
- Para desligar, abra a configuração do plugin no RuneLite (ícone de chave inglesa) e
  desmarque **Registrar textos sem tradução**.

## Atualizar

Feche o jogo. Baixe o ZIP de novo, extraia e dê dois cliques em `instalar.bat`.

## Desinstalar

Feche o jogo e dê dois cliques em `desinstalar.bat`. O atalho e o plugin somem, e o seu
RuneLite normal continua igual.

## Problemas comuns

- **O plugin aparece duas vezes ou o jogo fica em inglês:** você tem o plugin original
  "OSRS Translate" do Plugin Hub. Desinstale esse plugin no RuneLite.
- **Conta Jagex não entra pelo atalho:** rode o `instalar.bat` e responda `s` quando ele
  perguntar da conta Jagex. O instalador explica cada passo. Para cancelar esse acesso
  depois, dê dois cliques em `limpar-credencial.bat`.
- **O Windows mostra "O Windows protegeu o computador":** clique em **Mais informações**
  e depois em **Executar assim mesmo**. O aviso aparece porque o arquivo veio da internet.
- **Achou um texto em inglês ou uma tradução estranha?** Veja como avisar em
  [Como contribuir](docs/CONTRIBUIR.md).

## Saiba mais

| Documento | Para quem |
|---|---|
| [Como contribuir](docs/CONTRIBUIR.md) | Quem quer avisar de um erro ou ajudar a traduzir. Tem um modo simples e um modo técnico. |
| [Documentação técnica](docs/TECNICO.md) | Quem quer entender o plugin, mexer no código ou atualizar as traduções. |
| [Créditos e licença](docs/LICENCA.md) | Quem quer saber de onde vem cada parte e o que pode fazer com ela. |

Este projeto é um fork do plugin
[OSRS Translate PT-BR](https://github.com/walterrezende12-tech/Traducao-OSRS-BR), de
Walter Rezende. As traduções usam a [OSRS Wiki](https://oldschool.runescape.wiki) como
fonte. Old School RuneScape é da Jagex.
