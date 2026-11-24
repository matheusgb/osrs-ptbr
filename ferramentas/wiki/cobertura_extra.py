"""Lista examines e textos de interface sem tradução.

Uso: python3 cobertura_extra.py

Lê dados/examines.json (de baixar_examines.py) e ../cache/dados/textos_cache.json
(de ferramentas/cache) e grava dados/faltando_examines.json e
dados/faltando_interfaces.json, no mesmo formato de dados/faltando.json.
"""
import glob
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import cobertura  # noqa: E402
import comum  # noqa: E402

TEXTOS_CACHE = os.path.normpath(os.path.join(comum.PASTA, "..", "cache", "dados", "textos_cache.json"))
PALAVRAS_LIGACAO = {"of", "the", "and", "a", "an", "in", "on", "to", "de"}


def texto_de_interface_util(texto):
    """Descarta lixo técnico e nomes próprios, que ficam em inglês."""
    if re.fullmatch(r"[a-zA-Z]{1,2}|[iszlYIJ]+", texto):
        return False  # assinatura de script, como "iiiY"
    if re.search(r"[{}\\|_]|https?:|\.png", texto):
        return False
    palavras = re.findall(r"[A-Za-z][A-Za-z'\-]*", texto)
    if len(palavras) < 2:
        return False
    minusculas = [p for p in palavras if p[0].islower() and p not in PALAVRAS_LIGACAO]
    # Todas as palavras com maiúscula e sem pontuação de frase: nome de item, NPC ou lugar.
    return bool(minusculas) or bool(re.search(r"[.!?:]$", texto))


def gravar(caminho, itens):
    with open(caminho, "w", encoding="utf-8") as saida:
        json.dump(itens, saida, ensure_ascii=False, indent=1)
    print(f"{len(itens)} textos sem tradução em {caminho}")


def main():
    examines_gm = cobertura.carregar("translations_game_message.json")
    with open(os.path.join(comum.DADOS, "examines.json"), encoding="utf-8") as entrada:
        examines = json.load(entrada)
    gravar(os.path.join(comum.DADOS, "faltando_examines.json"), [
        {"texto": cobertura.limpar(t), "tipo": "examine", "paginas": [origem]}
        for t, origem in sorted(examines.items())
        if not examines_gm.tem(cobertura.limpar(t))
    ])

    todas = set()
    for caminho in glob.glob(os.path.join(comum.PT_BR, "*.json")):
        with open(caminho, encoding="utf-8-sig") as entrada:
            todas |= set(json.load(entrada))
    with open(TEXTOS_CACHE, encoding="utf-8") as entrada:
        textos = json.load(entrada)
    faltando = {}
    for texto, origem in textos.items():
        limpo = cobertura.limpar(texto)
        if limpo not in todas and texto_de_interface_util(limpo):
            faltando.setdefault(limpo, origem)
    gravar(os.path.join(comum.DADOS, "faltando_interfaces.json"), [
        {"texto": t, "tipo": "interface", "paginas": [origem]} for t, origem in sorted(faltando.items())
    ])


if __name__ == "__main__":
    main()
