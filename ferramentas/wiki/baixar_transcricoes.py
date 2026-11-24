"""Baixa as páginas Transcript: da OSRS Wiki, com carga mínima no servidor.

Uso:
    python3 baixar_transcricoes.py              baixa só as páginas que faltam
    python3 baixar_transcricoes.py --atualizar  também baixa de novo as páginas editadas

Uma requisição por vez, com 8 a 12 s de pausa. A segunda execução não baixa de
novo o que já está em dados/transcricoes.jsonl.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import comum  # noqa: E402

NAMESPACE_TRANSCRIPT = 120
LOTE = 50


def listar_titulos(wiki):
    titulos = []
    continuar = {}
    while True:
        dados = wiki.get(action="query", list="allpages", apnamespace=NAMESPACE_TRANSCRIPT,
                         apfilterredir="nonredirects", aplimit="max", **continuar)
        titulos += [p["title"] for p in dados["query"]["allpages"]]
        if "continue" not in dados:
            return titulos
        continuar = dados["continue"]


def revisoes_atuais(wiki, titulos):
    """Retorna {título: revid} sem baixar o conteúdo."""
    atuais = {}
    for i in range(0, len(titulos), LOTE):
        dados = wiki.get(action="query", prop="revisions", rvprop="ids",
                         titles="|".join(titulos[i:i + LOTE]))
        for pagina in dados["query"]["pages"]:
            if pagina.get("revisions"):
                atuais[pagina["title"]] = pagina["revisions"][0]["revid"]
    return atuais


def baixar(wiki, titulos, saida):
    for i in range(0, len(titulos), LOTE):
        lote = titulos[i:i + LOTE]
        dados = wiki.get(action="query", prop="revisions", rvprop="ids|content",
                         rvslots="main", titles="|".join(lote))
        for pagina in dados["query"]["pages"]:
            revisoes = pagina.get("revisions")
            if not revisoes:
                continue
            saida.write(json.dumps({
                "title": pagina["title"],
                "revid": revisoes[0]["revid"],
                "content": revisoes[0]["slots"]["main"]["content"],
            }, ensure_ascii=False) + "\n")
        saida.flush()
        print(f"  {min(i + LOTE, len(titulos))}/{len(titulos)} páginas", flush=True)


def main():
    atualizar = "--atualizar" in sys.argv
    os.makedirs(comum.DADOS, exist_ok=True)
    wiki = comum.Wiki()

    print("Listando as páginas Transcript:", flush=True)
    titulos = listar_titulos(wiki)
    locais = comum.ler_transcricoes()
    faltando = [t for t in titulos if t not in locais]

    if atualizar:
        print("Conferindo quais páginas foram editadas", flush=True)
        atuais = revisoes_atuais(wiki, [t for t in titulos if t in locais])
        faltando += [t for t, revid in atuais.items() if locais[t]["revid"] != revid]

    print(f"{len(titulos)} páginas na wiki, {len(locais)} locais, {len(faltando)} para baixar", flush=True)
    with open(comum.TRANSCRICOES, "a", encoding="utf-8") as saida:
        baixar(wiki, faltando, saida)

    # Uma página baixada de novo fica no fim do arquivo; reescreve sem duplicata.
    paginas = comum.ler_transcricoes()
    with open(comum.TRANSCRICOES, "w", encoding="utf-8") as saida:
        for titulo in sorted(paginas):
            saida.write(json.dumps(paginas[titulo], ensure_ascii=False) + "\n")
    print(f"Pronto: {len(paginas)} páginas, {wiki.requisicoes} requisições", flush=True)


if __name__ == "__main__":
    main()
