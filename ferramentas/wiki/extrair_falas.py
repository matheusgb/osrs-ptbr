"""Extrai as falas em inglês do wikitext das transcrições.

Lê dados/transcricoes.jsonl e grava dados/falas.json, uma entrada por texto:
    {"texto": "...", "tipo": "fala|opcao|caixa|overhead", "paginas": [...]}
"""
import html
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import comum  # noqa: E402

# Modelos que só descrevem a cena e não aparecem no jogo.
IGNORAR = {"tact", "qact", "tcond", "tbranch", "tselect", "transcript", "tcheck", "tmusic"}
# "'''Hans:''' texto" ou "'''Hans''': texto". O nome pode ter apóstrofo ('Beefy' Burns).
FALA = re.compile(r"^'''(?P<nome>.+?)(?::'''|''':)\s*(?P<texto>.*)$")


def modelos(texto):
    """Divide o texto em pedaços: ("texto", s) ou ("modelo", nome, [parâmetros])."""
    pedacos, i, inicio = [], 0, 0
    while i < len(texto):
        if texto.startswith("{{", i):
            if i > inicio:
                pedacos.append(("texto", texto[inicio:i]))
            fim, profundidade, j = None, 0, i
            while j < len(texto):
                if texto.startswith("{{", j):
                    profundidade += 1
                    j += 2
                elif texto.startswith("}}", j):
                    profundidade -= 1
                    j += 2
                    if profundidade == 0:
                        fim = j
                        break
                else:
                    j += 1
            if fim is None:
                # "{{" sem fechamento: descarta a marcação e fica o texto.
                pedacos.append(("texto", texto[i + 2:]))
                return pedacos
            corpo = texto[i + 2:fim - 2]
            partes = dividir_parametros(corpo)
            pedacos.append(("modelo", partes[0].strip().lower(), partes[1:]))
            i = inicio = fim
        else:
            i += 1
    if inicio < len(texto):
        pedacos.append(("texto", texto[inicio:]))
    return pedacos


def dividir_parametros(corpo):
    """Divide por "|" fora de modelos e links aninhados."""
    partes, atual, nivel = [], [], 0
    i = 0
    while i < len(corpo):
        dois = corpo[i:i + 2]
        if dois in ("{{", "[["):
            nivel += 1
            atual.append(dois)
            i += 2
        elif dois in ("}}", "]]"):
            nivel -= 1
            atual.append(dois)
            i += 2
        elif corpo[i] == "|" and nivel == 0:
            partes.append("".join(atual))
            atual = []
            i += 1
        else:
            atual.append(corpo[i])
            i += 1
    partes.append("".join(atual))
    return partes


def posicionais(parametros):
    """Parâmetros sem nome. Nome de parâmetro é uma palavra só, como pic= ou receives=."""
    return [p for p in parametros if not re.match(r"^\s*[A-Za-z]\w{0,19}\s*=(?!=)", p)]


def limpar(texto):
    """Tira a marcação da wiki e deixa o texto como o jogo mostra."""
    saida = []
    for pedaco in modelos(texto):
        if pedaco[0] == "texto":
            saida.append(pedaco[1])
        elif pedaco[1] in ("overhead", "sic", "*") or pedaco[1] in IGNORAR:
            continue
        else:
            # Modelo de formatação (cor, nome de item): fica o último parâmetro posicional.
            valores = posicionais(pedaco[2])
            if valores:
                saida.append(limpar(valores[-1]))
    texto = "".join(saida)
    texto = re.sub(r"\[\[(?:[^\]|]*\|)?([^\]]*)\]\]", r"\1", texto)
    texto = re.sub(r"\[https?://\S+\s+([^\]]*)\]", r"\1", texto)
    texto = re.sub(r"'''?", "", texto)
    texto = re.sub(r"<br\s*/?>", " ", texto, flags=re.I)
    texto = re.sub(r"<[^>]+>", "", texto)
    texto = html.unescape(texto)
    return re.sub(r"\s+", " ", texto).strip()


def falas_da_linha(linha):
    """Retorna [(tipo, texto)] de uma linha da transcrição."""
    # A wiki põe em <nowiki> o nome que tem apóstrofo, como 'Black-eye'.
    linha = re.sub(r"</?nowiki>", "", linha)
    linha = re.sub(r"^[*#:\s]+", "", linha).strip()
    if not linha:
        return []

    resultado = []
    for pedaco in modelos(linha):
        if pedaco[0] != "modelo":
            continue
        nome, valores = pedaco[1], posicionais(pedaco[2])
        if nome == "topt" and valores:
            resultado.append(("opcao", limpar(valores[0])))
        elif nome == "tbox" and valores:
            resultado.append(("caixa", limpar(valores[-1])))
        elif nome == "overhead" and valores:
            resultado.append(("overhead", limpar(valores[0])))

    fala = FALA.match(linha)
    if fala:
        resultado.append(("fala", limpar(fala.group("texto"))))
    return [(tipo, texto) for tipo, texto in resultado if re.search(r"[A-Za-z]", texto)]


def main():
    paginas = comum.ler_transcricoes()
    falas = {}
    for titulo, pagina in sorted(paginas.items()):
        for linha in pagina["content"].splitlines():
            for tipo, texto in falas_da_linha(linha):
                entrada = falas.setdefault(texto, {"texto": texto, "tipo": tipo, "paginas": []})
                if titulo not in entrada["paginas"]:
                    entrada["paginas"].append(titulo)
    with open(comum.FALAS, "w", encoding="utf-8") as saida:
        json.dump(sorted(falas.values(), key=lambda f: f["texto"]), saida, ensure_ascii=False, indent=1)
    print(f"{len(falas)} falas de {len(paginas)} páginas em {comum.FALAS}")


if __name__ == "__main__":
    main()
