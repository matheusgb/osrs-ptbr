"""Gera traducoes/glossario.json: os termos que ficam em inglês.

Uso: python3 gerar_glossario.py

Junta os nomes de item, NPC, monstro, quest, lugar, objeto e loja da OSRS Wiki (API
Bucket, com o cliente gentil de comum.Wiki) com a lista manual de
traducoes/glossario_manual.json. A saída é ordenada e sem duplicata.
"""
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import comum  # noqa: E402

BUCKETS = ["infobox_item", "infobox_npc", "infobox_monster", "quest",
           "infobox_location", "infobox_scenery", "infobox_shop"]
POR_PAGINA = 5000
MANUAL = os.path.normpath(os.path.join(comum.PT_BR, "..", "glossario_manual.json"))
SAIDA = os.path.normpath(os.path.join(comum.PT_BR, "..", "glossario.json"))


def normalizar(nome):
    """'Cook (Lumbridge)' → 'Cook'; "Bob's Brilliant Axes." → "Bob's Brilliant Axes"."""
    nome = nome.strip()
    while re.search(r"\s*\([^)]*\)$", nome):
        nome = re.sub(r"\s*\([^)]*\)$", "", nome)
    nome = nome.rstrip(".").strip()
    return nome if re.search(r"[A-Za-z]{2}", nome) else ""


def nomes_do_bucket(wiki, bucket):
    nomes, offset = set(), 0
    while True:
        consulta = f"bucket('{bucket}').select('page_name').limit({POR_PAGINA}).offset({offset}).run()"
        linhas = wiki.get(action="bucket", query=consulta).get("bucket", [])
        nomes |= {normalizar(linha.get("page_name", "")) for linha in linhas}
        print(f"  {bucket}: {offset + len(linhas)} registros", flush=True)
        if len(linhas) < POR_PAGINA:
            return nomes - {""}
        offset += POR_PAGINA


def main():
    wiki = comum.Wiki()
    termos = set()
    for bucket in BUCKETS:
        termos |= nomes_do_bucket(wiki, bucket)
    with open(MANUAL, encoding="utf-8") as entrada:
        manual = json.load(entrada)
    for chave, valores in manual.items():
        if not chave.startswith("_"):
            termos |= set(valores)
    with open(SAIDA, "w", encoding="utf-8") as saida:
        json.dump(sorted(termos), saida, ensure_ascii=False, indent=0)
        saida.write("\n")
    print(f"{len(termos)} termos em {SAIDA}, {wiki.requisicoes} requisições")


if __name__ == "__main__":
    main()
