"""Baixa os examines de item, NPC e monstro pela API Bucket da OSRS Wiki.

Uso: python3 baixar_examines.py

Grava dados/examines.json: {"texto do examine": "tipo"}. Usa o mesmo cliente
gentil das transcrições: uma requisição por vez, com 8 a 12 s de pausa.
"""
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import comum  # noqa: E402
from extrair_falas import limpar  # noqa: E402

BUCKETS = {"infobox_item": "item", "infobox_npc": "npc", "infobox_monster": "monstro"}
POR_PAGINA = 5000
EXAMINES = os.path.join(comum.DADOS, "examines.json")


def textos(valor):
    """O campo pode vir como texto ou lista, e com marcação da wiki."""
    valores = valor if isinstance(valor, list) else [valor]
    for item in valores:
        if not isinstance(item, str):
            continue
        for parte in re.split(r"<br\s*/?>|\n", item):
            texto = limpar(parte)
            if re.search(r"[A-Za-z]{2}", texto):
                yield texto


def baixar_bucket(wiki, nome):
    resultado, offset = [], 0
    while True:
        consulta = (f"bucket('{nome}').select('examine')"
                    f".limit({POR_PAGINA}).offset({offset}).run()")
        dados = wiki.get(action="bucket", query=consulta)
        linhas = dados.get("bucket", [])
        resultado += linhas
        print(f"  {nome}: {len(resultado)} registros", flush=True)
        if len(linhas) < POR_PAGINA:
            return resultado
        offset += POR_PAGINA


def main():
    os.makedirs(comum.DADOS, exist_ok=True)
    wiki = comum.Wiki()
    examines = {}
    for nome, tipo in BUCKETS.items():
        for linha in baixar_bucket(wiki, nome):
            for texto in textos(linha.get("examine")):
                examines.setdefault(texto, tipo)
    with open(EXAMINES, "w", encoding="utf-8") as saida:
        json.dump(examines, saida, ensure_ascii=False, indent=1, sort_keys=True)
    print(f"{len(examines)} examines únicos em {EXAMINES}, {wiki.requisicoes} requisições")


if __name__ == "__main__":
    main()
