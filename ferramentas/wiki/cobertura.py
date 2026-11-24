"""Mede quantas falas da wiki já têm tradução nos JSONs pt-BR.

Lê dados/falas.json e traducoes/pt-BR, grava dados/faltando.json e dados/cobertura.md.
A busca repete a do plugin (TranslationLookupHelper): texto limpo, busca exata e,
se não achar, os padrões com placeholder.
"""
import collections
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import comum  # noqa: E402

PLACEHOLDER = re.compile(r"\[[^\]]+\]")
# Arquivo que o plugin usa para cada tipo de fala.
ARQUIVO_DO_TIPO = {
    "fala": "translations.json",
    "opcao": "translations.json",
    "caixa": "translations.json",
    "overhead": "translations_overhead.json",
}


def limpar(texto):
    """cleanText + normalizeLookupText do plugin."""
    texto = re.sub(r"(?i)<br\s*/?>", " ", texto)
    texto = re.sub(r"<[^>]+>", " ", texto)
    texto = re.sub(r"\s+", " ", texto).strip()
    texto = re.sub(r"\s+([.,!?])", r"\1", texto)
    texto = re.sub(r"([Hh])ardcore Ironmen ", r"\1ardcore Ironman ", texto)
    return texto.replace("standard Ironmen ", "standard Ironman ")


def primeira_palavra(texto):
    return texto.split(" ", 1)[0] if " " in texto else None


def ultima_palavra(texto):
    return texto.rsplit(" ", 1)[1] if " " in texto else None


class Dicionario:
    def __init__(self, traducoes):
        self.exatas = set(traducoes)
        self.por_primeira = collections.defaultdict(list)
        self.por_ultima = collections.defaultdict(list)
        self.sem_palavra = []
        for chave, valor in traducoes.items():
            if not PLACEHOLDER.search(chave):
                continue
            if PLACEHOLDER.sub("\0", chave) == PLACEHOLDER.sub("\0", valor):
                continue  # tradução idêntica ao inglês
            partes = PLACEHOLDER.split(chave)
            if not re.search(r"[A-Za-z]", "".join(partes)):
                # Chave sem letra fora do placeholder, como "[name].": casaria com
                # qualquer frase terminada em ponto. O plugin aceita essas chaves
                # (bug), mas elas não traduzem nada.
                continue
            padrao = re.compile("(.+?)".join(re.escape(p) for p in partes))
            entrada = (partes[0], partes[-1], padrao)
            if primeira_palavra(partes[0]):
                self.por_primeira[primeira_palavra(partes[0])].append(entrada)
            elif ultima_palavra(partes[-1]):
                self.por_ultima[ultima_palavra(partes[-1])].append(entrada)
            else:
                self.sem_palavra.append(entrada)

    def tem(self, texto):
        if texto in self.exatas:
            return True
        candidatos = self.sem_palavra + self.por_primeira.get(primeira_palavra(texto), []) \
            + self.por_ultima.get(ultima_palavra(texto), [])
        return any(texto.startswith(inicio) and texto.endswith(fim) and padrao.fullmatch(texto)
                   for inicio, fim, padrao in candidatos)


def carregar(arquivo):
    with open(os.path.join(comum.PT_BR, arquivo), encoding="utf-8-sig") as entrada:
        return Dicionario(json.load(entrada))


def main():
    with open(comum.FALAS, encoding="utf-8") as entrada:
        falas = json.load(entrada)
    dicionarios = {arquivo: carregar(arquivo) for arquivo in set(ARQUIVO_DO_TIPO.values())}

    por_tipo = collections.Counter()
    cobertas = collections.Counter()
    faltando = []
    por_pagina = collections.Counter()
    for fala in falas:
        texto = limpar(fala["texto"])
        por_tipo[fala["tipo"]] += 1
        if dicionarios[ARQUIVO_DO_TIPO[fala["tipo"]]].tem(texto):
            cobertas[fala["tipo"]] += 1
        else:
            faltando.append({**fala, "texto": texto})
            for pagina in fala["paginas"]:
                por_pagina[pagina] += 1

    with open(comum.FALTANDO, "w", encoding="utf-8") as saida:
        json.dump(faltando, saida, ensure_ascii=False, indent=1)

    total, total_cobertas = sum(por_tipo.values()), sum(cobertas.values())
    if not total:
        print("Nenhuma fala em dados/falas.json. Rode baixar_transcricoes.py e extrair_falas.py.")
        return
    linhas = ["# Cobertura das transcrições da OSRS Wiki", "",
              f"Falas únicas: {total}. Traduzidas: {total_cobertas} "
              f"({total_cobertas / total:.1%}). Faltam: {total - total_cobertas}.", "",
              "| Tipo | Falas | Traduzidas | % |", "|---|---|---|---|"]
    for tipo in sorted(por_tipo):
        linhas.append(f"| {tipo} | {por_tipo[tipo]} | {cobertas[tipo]} | {cobertas[tipo] / por_tipo[tipo]:.1%} |")
    linhas += ["", "## Páginas com mais falas sem tradução", "", "| Página | Faltam |", "|---|---|"]
    linhas += [f"| {p} | {n} |" for p, n in por_pagina.most_common(40)]
    with open(comum.RESUMO, "w", encoding="utf-8") as saida:
        saida.write("\n".join(linhas) + "\n")
    print("\n".join(linhas[:4 + 2 + len(por_tipo) + 1]))


if __name__ == "__main__":
    main()
