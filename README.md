# OSRS Translate PT-BR

Plugin RuneLite que traduz o Old School RuneScape para Português brasileiro em
tempo real.

Autor: Walter Rezende

## 🤝 Quer contribuir?

Contribua com traduções, correções de bugs ou novos idiomas. Use o Discord ou relate um problema:

- [Discord](https://discord.gg/4eAbaj29Gt)
- [Reportar um problema](https://github.com/walterrezende12-tech/Traducao-OSRS-BR/issues)

## Funcionalidades

- Tradução de diálogos, opções de resposta e falas acima da cabeça.
- Tradução de menus, mensagens do jogo, Skill Guide, Quest Journal, diários
  de conquistas, pergaminhos de pistas, itens, livros e notas, tela de
  boas-vindas e configurações.
- Compatibilidade com os plugins:
  - Quest Helper
  - Menu Entry Swapper
  - Item Charges
  - Friend Notes
  - Clue Scroll
  - Examine (preços e valores de alquimia)
- Atualizações automáticas dos dicionários sem reinstalar o plugin.
- Modo desenvolvedor para testar dicionários locais com recarga automática.

## Traduções remotas

Os dicionários são publicados no repositório
[`osrs-translate-translations`](https://github.com/walterrezende12-tech/osrs-translate-translations).

O plugin verifica atualizações ao iniciar e, depois, uma vez por hora. Cada
arquivo é baixado por HTTPS e validado com SHA-256 antes da nova versão ser
ativada. Se a atualização falhar, o último cache válido continua em uso.

As requisições são feitas ao GitHub e enviam somente os dados técnicos normais
de uma conexão HTTPS, como endereço IP e `User-Agent`. Nenhum dado da conta,
personagem ou jogo é enviado.

O cache fica no diretório do RuneLite:

```text
~/.runelite/osrs-translate/translations/<idioma>/
```

## Traduções locais deste fork

A pasta `traducoes/` tem uma cópia corrigida dos dicionários pt-BR. O plugin
continua baixando os dicionários do repositório original. Para testar a cópia
local:

1. Ligue **Modo desenvolvedor** nas configurações do plugin.
2. Em **Fonte das traduções**, escolha **Local**.
3. Em **Caminho dos JSONs**, informe o caminho de `traducoes/pt-BR`.

Nomes de skill, gírias e termos do jogo ficam em inglês, como `Slayer`,
`Grand Exchange` e `Wilderness`. A pasta `traducoes/correcao/revisar` lista as
traduções que precisam de revisão manual.

## Instalação

Abra o RuneLite, acesse **Plugin Hub**, pesquise `OSRS Translate PT-BR` e
selecione **Install**.
