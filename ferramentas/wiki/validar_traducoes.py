"""Valida traduções antes de aplicar, com as regras de regras.py.

Uso: python3 validar_traducoes.py <lista.json> <traducoes.json>

A lista é uma das dados/faltando*.json ou dados/para_traduzir.json (itens com "texto"
ou "en" e "tipo"). As traduções são um objeto {"texto em inglês": "tradução"}. O
script lista cada erro com o código da regra e sai com código 1 se houver erro.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import regras  # noqa: E402
from destinos import arquivos_de_destino, imperativos  # noqa: E402


def main():
    with open(sys.argv[1], encoding="utf-8") as arquivo:
        itens = json.load(arquivo)
    with open(sys.argv[2], encoding="utf-8") as arquivo:
        traducoes = json.load(arquivo)
    glossario = regras.carregar_glossario()
    menu = imperativos()

    erros, textos = [], set()
    for item in itens:
        en = item.get("texto", item.get("en"))
        if not isinstance(en, str):
            erros.append(("extra", "(item sem texto na lista)", None))
            continue
        textos.add(en)
        for destino in arquivos_de_destino(item.get("tipo", "fala")):
            for codigo in regras.validar(en, traducoes.get(en), destino, glossario, menu):
                erros.append((codigo, en, traducoes.get(en)))
    for extra in sorted(set(traducoes) - textos)[:10]:
        erros.append(("extra", extra, None))

    print(f"{len(textos)} textos, {len(traducoes)} traduções, {len(erros)} erros")
    for codigo, en, pt in erros[:60]:
        print(f"  [{codigo}] {regras.MENSAGENS.get(codigo, 'chave que não está na lista')}")
        print(f"      en: {en[:100]}")
        if pt:
            print(f"      pt: {pt[:100]}")
    sys.exit(1 if erros else 0)


if __name__ == "__main__":
    main()
