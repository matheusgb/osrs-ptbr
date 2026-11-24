"""Corrige traduções que já existem, só se a correção passar nas regras.

Uso: python3 corrigir_traducoes.py <lista.json> <correcoes.json>

A lista tem itens {"arquivo", "en", "pt"} (como dados/auditoria_lista.json): "pt" é a
tradução atual. As correções são {"texto em inglês": "tradução nova"}. Uma chave só
muda se o valor atual no arquivo ainda for o "pt" da lista e se a tradução nova passar
em regras.validar. O que falhar vai para dados/rejeitadas_correcoes.json. A formatação e o fim de
linha do arquivo ficam iguais.
"""
import collections
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import comum  # noqa: E402
import regras  # noqa: E402
from destinos import imperativos  # noqa: E402

LINHA = re.compile(r'^(\s*)("(?:[^"\\]|\\.)*")(\s*:\s*)("(?:[^"\\]|\\.)*")(\s*,?\s*)$')
REJEITADAS = os.path.join(comum.DADOS, "rejeitadas_correcoes.json")


def trocar(caminho, mudancas):
    """Troca os valores; mudancas = {chave: (valor_atual, valor_novo)}. Devolve quantas."""
    with open(caminho, "rb") as arquivo:
        linhas = arquivo.read().decode("utf-8").splitlines(keepends=True)
    trocadas = set()
    for i, linha in enumerate(linhas):
        corpo = linha.rstrip("\r\n")
        m = LINHA.match(corpo)
        if not m:
            continue
        chave, valor = json.loads(m.group(2)), json.loads(m.group(4))
        if chave in mudancas and valor == mudancas[chave][0]:
            novo = json.dumps(mudancas[chave][1], ensure_ascii=False)
            linhas[i] = m.group(1) + m.group(2) + m.group(3) + novo + m.group(5) + linha[len(corpo):]
            trocadas.add(chave)
    with open(caminho, "wb") as arquivo:
        arquivo.write("".join(linhas).encode("utf-8"))
    with open(caminho, encoding="utf-8-sig") as arquivo:
        resultado = json.load(arquivo)
    if any(resultado.get(k) != mudancas[k][1] for k in trocadas):
        raise ValueError(f"{caminho} ficou diferente do esperado depois de gravar")
    return len(trocadas)


def main():
    with open(sys.argv[1], encoding="utf-8") as entrada:
        lista = json.load(entrada)
    with open(sys.argv[2], encoding="utf-8") as entrada:
        correcoes = json.load(entrada)
    glossario = regras.carregar_glossario()
    menu = imperativos()

    por_arquivo, rejeitadas = collections.defaultdict(dict), []
    for item in lista:
        novo = correcoes.get(item["en"])
        if novo is None or novo == item["pt"]:
            continue
        # Só corrige o que ainda falha nas regras de agora: uma regra ajustada depois
        # da lista desfaz a cobrança, e a tradução atual fica.
        if not regras.validar(item["en"], item["pt"], item["arquivo"], glossario, menu):
            continue
        erros = regras.validar(item["en"], novo, item["arquivo"], glossario, menu)
        if erros:
            rejeitadas.append({**item, "correcao": novo, "erros": erros})
        else:
            por_arquivo[item["arquivo"]][item["en"]] = (item["pt"], novo)

    for arquivo, mudancas in sorted(por_arquivo.items()):
        n = trocar(os.path.join(comum.PT_BR, arquivo), mudancas)
        print(f"{arquivo}: {n} de {len(mudancas)} corrigidas")
        if n < len(mudancas):
            print(f"  {len(mudancas) - n} não trocadas: chave ausente, linha fora do formato"
                  " ou valor atual diferente do da lista")
    with open(REJEITADAS, "w", encoding="utf-8") as saida:
        json.dump(rejeitadas, saida, ensure_ascii=False, indent=1)
    if rejeitadas:
        print(f"{len(rejeitadas)} correções barradas pelas regras: veja {REJEITADAS}")


if __name__ == "__main__":
    main()
