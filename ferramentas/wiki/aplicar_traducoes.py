"""Acrescenta traduções novas aos JSONs de traducoes/pt-BR.

Uso: python3 aplicar_traducoes.py traducoes.json [outro.json ...]

Cada arquivo de entrada é um objeto {"texto em inglês": "tradução"}. O tipo de
cada texto vem das listas dados/faltando*.json e define o JSON de destino
(ver ARQUIVO_DO_TIPO em cobertura.py). Chave que já existe no destino não muda. O
script mantém a formatação e o fim de linha do arquivo de destino.
"""
import glob
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import comum  # noqa: E402
from cobertura import ARQUIVO_DO_TIPO  # noqa: E402


def acrescentar(caminho, novas):
    """Insere as entradas antes do "}" final e devolve quantas entraram."""
    with open(caminho, "rb") as arquivo:
        texto = arquivo.read().decode("utf-8")
    bom = texto.startswith("\ufeff")
    existentes = json.loads(texto.lstrip("\ufeff"))
    novas = {k: v for k, v in novas.items() if k not in existentes}
    if not novas:
        return 0

    fim_de_linha = "\r\n" if "\r\n" in texto else "\n"
    corpo = texto.rstrip()
    if not corpo.endswith("}"):
        raise ValueError(f"{caminho} não termina com }}")
    corpo = corpo[:-1].rstrip()
    separador = "," if existentes else ""
    linhas = [f'  {json.dumps(k, ensure_ascii=False)}: {json.dumps(v, ensure_ascii=False)}'
              for k, v in novas.items()]
    texto = corpo + separador + fim_de_linha + ("," + fim_de_linha).join(linhas) + fim_de_linha + "}" + fim_de_linha
    with open(caminho, "wb") as arquivo:
        arquivo.write(texto.encode("utf-8"))

    with open(caminho, encoding="utf-8-sig") as arquivo:
        resultado = json.load(arquivo)
    if any(resultado.get(k) != v for k, v in novas.items()) or bom != texto.startswith("\ufeff"):
        raise ValueError(f"{caminho} ficou diferente do esperado depois de gravar")
    return len(novas)


def main():
    tipo_de = {}
    for lista in sorted(glob.glob(os.path.join(comum.DADOS, "faltando*.json"))):
        with open(lista, encoding="utf-8") as entrada:
            for fala in json.load(entrada):
                # A mesma frase pode estar em mais de uma lista (fala e interface).
                tipo_de.setdefault(fala["texto"], set()).add(fala["tipo"])

    por_arquivo = {}
    sem_tipo = 0
    for caminho in sys.argv[1:]:
        with open(caminho, encoding="utf-8") as entrada:
            for en, pt in json.load(entrada).items():
                tipos = tipo_de.get(en)
                if not tipos:
                    sem_tipo += 1
                    continue
                for arquivo in {ARQUIVO_DO_TIPO[t] for t in tipos}:
                    por_arquivo.setdefault(arquivo, {})[en] = pt

    for arquivo, novas in sorted(por_arquivo.items()):
        n = acrescentar(os.path.join(comum.PT_BR, arquivo), novas)
        print(f"{arquivo}: {n} traduções novas")
    if sem_tipo:
        print(f"{sem_tipo} textos ignorados: não estão em dados/faltando*.json")


if __name__ == "__main__":
    main()
