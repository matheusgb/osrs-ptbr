# Como contribuir

[Voltar ao README](../README.md)

Escolha o modo que combina com você:

- [Modo simples](#modo-simples): você joga e quer avisar de um texto em inglês ou de uma
  tradução estranha. Não precisa saber programar.
- [Modo técnico](#modo-técnico): você usa Git e Python e quer traduzir em quantidade ou
  mexer no código.

Os dois modos seguem os mesmos [critérios de tradução](#critérios-de-tradução).

Aviso: este repositório é privado. Para abrir issue ou propor mudança, você precisa de
acesso. Peça ao dono do repositório para te adicionar como colaborador.

## Modo simples

### Avisar de um texto

1. Abra a aba **Issues** deste repositório no GitHub.
2. Clique em **New issue** e escolha **Texto em inglês ou tradução ruim**.
3. Preencha o formulário: o texto que apareceu, a tradução que você sugere (se tiver) e
   onde ele apareceu no jogo.

Dica: tire um print da tela e arraste para o formulário.

### Mandar a lista de textos sem tradução

O plugin anota sozinho os textos que aparecem em inglês enquanto você joga. A lista não
inclui o chat de jogadores nem as listas de amigos e clã.

1. Jogue normalmente.
2. Abra a pasta `%USERPROFILE%\.runelite\osrs-translate-ptbr` (cole esse caminho na barra
   do Explorador de Arquivos).
3. Anexe o arquivo `sem-traducao.jsonl` numa issue.

### Corrigir uma tradução pelo site

1. Abra a pasta `traducoes/pt-BR` no GitHub.
2. Procure a frase: aperte `t` para buscar arquivos, ou use a busca do repositório.
3. Abra o arquivo e clique no ícone de lápis (**Edit this file**).
4. Mude só o texto depois dos dois-pontos, entre aspas. Não mude o texto em inglês.
5. Clique em **Commit changes** e depois em **Propose changes**. Eu reviso antes de
   aplicar.

Aviso: os arquivos são grandes. Se o site travar, use o formulário de issue.

## Modo técnico

1. Faça um fork e clone o repositório.
2. Leia a [documentação técnica](TECNICO.md), principalmente
   [Atualizar as traduções](TECNICO.md#atualizar-as-traduções) e
   [Regras automáticas](TECNICO.md#regras-automáticas).
3. Traduza num JSON `{"texto em inglês": "tradução"}`.
4. Valide até dar 0 erros, dentro de `ferramentas/wiki`:
   `python3 validar_traducoes.py <lista.json> <novas.json>`.
5. Aplique com `python3 aplicar_traducoes.py <automaticas.json> <novas.json>`, ou corrija
   traduções existentes com `python3 corrigir_traducoes.py`.
6. Rode a auditoria e confira que o número de casos não subiu:
   `python3 aplicar_traducoes.py --auditar`.
7. Rode os testes:
   - Python: cada `test_*.py` de `ferramentas/wiki`;
   - plugin: `./gradlew test`.
8. Abra um pull request. Diga o que mudou e quantas frases entraram.

Se você mudou o código do plugin, gere o jar com `./scripts/gerar-dist.sh` e teste no
jogo antes do pull request.

Aviso para quem usa IA na tradução em lote: confira uma amostra à mão. O validador
confere a forma, não o sentido. Já apareceram atalhos que passam nele e pioram o texto:
nome em inglês entre parênteses, placeholder movido por script, nome híbrido ("Chambers
de Xeric") e nome enfiado numa frase que não fala dele.

## Critérios de tradução

1. **PT-BR coloquial, como fala de jogo.** O tom de cada personagem fica: pirata, goblin,
   bêbado e sotaque viram um equivalente em português.
2. **Termos do jogo ficam em inglês.** Isso vale para nomes de skill (`Slayer`, `Fishing`,
   `Hitpoints`), gírias de jogador e termos do universo do jogo (`Grand Exchange`,
   `Wilderness`, `Collection Log`, `clue scroll`, `Slayer Master`, `Quest Points`).
3. **Nomes próprios ficam em inglês, inteiros.** Isso vale para nomes de item, NPC,
   monstro, lugar, quest, magia, raid e minigame (`Bronze sword`, `Lumbridge Castle`,
   `Chambers of Xeric`, `Ice Barrage`). Não escreva o nome traduzido com o original entre
   parênteses.
4. **Placeholders ficam iguais e na mesma ordem.** O plugin preenche `[player]`,
   `[number]` e outros pela posição. Se a frase em português pedir outra ordem, a frase
   muda, e o placeholder não.
5. **Listas de escolha podem ser traduzidas.** `[hasn't/haven't]` pode virar
   `[não terminou/não terminaram]`, com o mesmo número de opções e na mesma ordem. Lista
   de nomes do jogo ou de números fica igual.
6. **Menus usam o infinitivo**: "Comprar", "Falar com", "Pegar".
7. **A pontuação final do inglês fica.** Uma tradução sem o ponto final quebra a busca do
   plugin.
