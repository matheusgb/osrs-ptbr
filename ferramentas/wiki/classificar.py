"""Separa o que fica igual ao inglês do que precisa de tradução, sem IA.

Uso: python3 classificar.py

Lê as listas dados/faltando*.json. Texto que é inteiro um termo do glossário, ou só
número, símbolo e placeholder, vai para dados/automaticas.json com tradução igual ao
inglês. O resto vai para dados/para_traduzir.json, no formato das listas.
"""
import glob
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import comum  # noqa: E402
import regras  # noqa: E402

AUTOMATICAS = os.path.join(comum.DADOS, "automaticas.json")
PARA_TRADUZIR = os.path.join(comum.DADOS, "para_traduzir.json")


def main():
    glossario = regras.carregar_glossario()
    automaticas, para_traduzir, vistos = {}, [], set()
    for lista in sorted(glob.glob(os.path.join(comum.DADOS, "faltando*.json"))):
        with open(lista, encoding="utf-8") as entrada:
            for item in json.load(entrada):
                chave = (item["texto"], item["tipo"])
                if chave in vistos:
                    continue
                vistos.add(chave)
                if regras.sem_traducao(item["texto"], glossario):
                    automaticas[item["texto"]] = item["texto"]
                else:
                    para_traduzir.append(item)

    with open(AUTOMATICAS, "w", encoding="utf-8") as saida:
        json.dump(automaticas, saida, ensure_ascii=False, indent=1)
    with open(PARA_TRADUZIR, "w", encoding="utf-8") as saida:
        json.dump(para_traduzir, saida, ensure_ascii=False, indent=1)
    print(f"{len(automaticas)} ficam iguais ao inglês ({AUTOMATICAS})")
    print(f"{len(para_traduzir)} para traduzir ({PARA_TRADUZIR})")


if __name__ == "__main__":
    main()
