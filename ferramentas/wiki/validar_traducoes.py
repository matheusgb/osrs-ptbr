"""Valida traduções antes de aplicar.

Uso: python3 validar_traducoes.py dados/faltando.json traducoes_novas.json

O primeiro arquivo é a lista de falas (dados/faltando.json). O segundo é um objeto
{"texto em inglês": "tradução"}. O script confere se toda fala tem tradução não vazia
e se os placeholders, como [player], ficaram iguais e na mesma ordem do inglês.
"""
import json
import re
import sys

PLACEHOLDER = re.compile(r"\[[^\]]+\]")


def main():
    with open(sys.argv[1], encoding="utf-8") as arquivo:
        falas = [f.get("texto", f.get("en")) for f in json.load(arquivo)]
    with open(sys.argv[2], encoding="utf-8") as arquivo:
        traducoes = json.load(arquivo)

    erros = []
    for en in falas:
        pt = traducoes.get(en)
        if not isinstance(pt, str) or not pt.strip():
            erros.append(f"FALTA: {en[:80]}")
        elif PLACEHOLDER.findall(en) != PLACEHOLDER.findall(pt):
            erros.append(f"PLACEHOLDER: {en[:60]} => {pt[:60]}")
    for extra in sorted(set(traducoes) - set(falas))[:10]:
        erros.append(f"CHAVE EXTRA: {extra[:80]}")

    print(f"{len(falas)} falas, {len(traducoes)} traduções, {len(erros)} erros")
    for erro in erros[:40]:
        print("  " + erro)
    sys.exit(1 if erros else 0)


if __name__ == "__main__":
    main()
