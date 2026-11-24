# Revisão manual de placeholders

Estes arquivos listam traduções que um script não consegue corrigir. Cada
arquivo é um objeto JSON no mesmo formato de `pt-BR/`: chave em inglês e
tradução atual.

## O que corrigir

- `placeholder_*.json`: o inglês não tem placeholder, mas a tradução tem,
  por exemplo `[player]` ou `[querido/senhor]`. O jogo mostra o colchete
  escrito. Troque o placeholder pela palavra do inglês.
- `placeholder_a_mais_*.json`: a tradução tem mais placeholders que o
  inglês. Um nome do inglês virou placeholder, como `Bronze Dragons` virou
  `[monster]`. O plugin preenche os placeholders por posição, então o valor
  vai para o lugar errado. Troque o placeholder extra pelo nome do inglês.

## Como aplicar

1. Corrija a tradução no arquivo de `pt-BR/`, não aqui.
2. Remova a entrada corrigida deste arquivo.
