"""Acrescenta traduções novas aos JSONs de traducoes/pt-BR, só as que passam nas regras.

Uso:
    python3 aplicar_traducoes.py traducoes.json [outro.json ...]
    python3 aplicar_traducoes.py --auditar

Cada arquivo de entrada é um objeto {"texto em inglês": "tradução"}. O tipo de
cada texto vem das listas dados/faltando*.json e define o JSON de destino
(ver destinos.py). Cada tradução passa pelo validador de regras.py: o que falha vai
para dados/rejeitadas.json com o código do erro, e não entra. Chave que já existe no
destino não muda. O script mantém a formatação e o fim de linha do destino.

--auditar roda as mesmas regras sobre os JSONs atuais e grava dados/auditoria.json
(resumo) e dados/auditoria_lista.json (todos os casos), sem mudar nada.
"""
import glob
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import comum  # noqa: E402
import regras  # noqa: E402
from destinos import arquivos_de_destino, imperativos  # noqa: E402

REJEITADAS = os.path.join(comum.DADOS, "rejeitadas.json")
AUDITORIA = os.path.join(comum.DADOS, "auditoria.json")
AUDITORIA_LISTA = os.path.join(comum.DADOS, "auditoria_lista.json")


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


def auditar():
    glossario = regras.carregar_glossario()
    menu = imperativos()
    relatorio, lista = {}, []
    for caminho in sorted(glob.glob(os.path.join(comum.PT_BR, "*.json"))):
        arquivo = os.path.basename(caminho)
        with open(caminho, encoding="utf-8-sig") as entrada:
            traducoes = json.load(entrada)
        por_regra = {}
        for en, pt in traducoes.items():
            erros = regras.validar(en, pt, arquivo, glossario, menu)
            for codigo in erros:
                por_regra.setdefault(codigo, []).append({"en": en, "pt": pt})
            if erros:
                lista.append({"arquivo": arquivo, "en": en, "pt": pt, "erros": erros})
        relatorio[arquivo] = {codigo: {"total": len(itens), "exemplos": itens[:20]}
                              for codigo, itens in sorted(por_regra.items())}
        resumo = ", ".join(f"{c}={len(i)}" for c, i in sorted(por_regra.items())) or "sem erros"
        print(f"{arquivo} ({len(traducoes)}): {resumo}")
    with open(AUDITORIA, "w", encoding="utf-8") as saida:
        json.dump(relatorio, saida, ensure_ascii=False, indent=1)
    with open(AUDITORIA_LISTA, "w", encoding="utf-8") as saida:
        json.dump(lista, saida, ensure_ascii=False, indent=1)
    print(f"Relatório em {AUDITORIA}; todos os {len(lista)} casos em {AUDITORIA_LISTA}")


def main():
    if "--auditar" in sys.argv[1:]:
        if len(sys.argv) != 2:
            sys.exit("Uso: python3 aplicar_traducoes.py --auditar (sem outros argumentos)")
        auditar()
        return

    tipo_de = {}
    for lista in sorted(glob.glob(os.path.join(comum.DADOS, "faltando*.json"))):
        with open(lista, encoding="utf-8") as entrada:
            for fala in json.load(entrada):
                # A mesma frase pode estar em mais de uma lista (fala e interface).
                tipo_de.setdefault(fala["texto"], set()).add(fala["tipo"])

    glossario = regras.carregar_glossario()
    menu = imperativos()
    por_arquivo, rejeitadas = {}, []
    sem_tipo = 0
    for caminho in sys.argv[1:]:
        with open(caminho, encoding="utf-8") as entrada:
            for en, pt in json.load(entrada).items():
                tipos = tipo_de.get(en)
                if not tipos:
                    sem_tipo += 1
                    continue
                # Tipos diferentes podem ir para o mesmo arquivo: valida uma vez por arquivo.
                destinos = {arquivo for tipo in tipos for arquivo in arquivos_de_destino(tipo)}
                for arquivo in sorted(destinos):
                    erros = regras.validar(en, pt, arquivo, glossario, menu)
                    if erros:
                        rejeitadas.append({"en": en, "pt": pt, "arquivo": arquivo, "erros": erros})
                    else:
                        por_arquivo.setdefault(arquivo, {})[en] = pt

    for arquivo, novas in sorted(por_arquivo.items()):
        n = acrescentar(os.path.join(comum.PT_BR, arquivo), novas)
        print(f"{arquivo}: {n} traduções novas")
    with open(REJEITADAS, "w", encoding="utf-8") as saida:
        json.dump(rejeitadas, saida, ensure_ascii=False, indent=1)
    if rejeitadas:
        print(f"{len(rejeitadas)} traduções barradas pelas regras: veja {REJEITADAS}")
    if sem_tipo:
        print(f"{sem_tipo} textos ignorados: não estão em dados/faltando*.json")

if __name__ == "__main__":
    main()
