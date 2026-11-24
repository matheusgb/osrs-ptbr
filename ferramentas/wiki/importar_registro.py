"""Transforma o registro de textos sem tradução do plugin numa lista para traduzir.

Uso: python3 importar_registro.py <sem-traducao.jsonl>

O plugin grava o registro em %USERPROFILE%\\.runelite\\osrs-translate-ptbr\\sem-traducao.jsonl
(no WSL: /mnt/c/Users/<usuário>/.runelite/osrs-translate-ptbr/sem-traducao.jsonl).
O script tira o que já tem tradução e grava dados/faltando_jogo.json, no formato das
outras listas, com tipo "arquivo:<JSON de destino>".
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import cobertura  # noqa: E402
import comum  # noqa: E402

SAIDA = os.path.join(comum.DADOS, "faltando_jogo.json")


def main():
    dicionarios = {}
    faltando = {}
    with open(sys.argv[1], encoding="utf-8") as entrada:
        for linha in entrada:
            try:
                item = json.loads(linha)
                arquivo, texto = item["arquivo"], cobertura.limpar(item["texto"])
                if arquivo not in dicionarios:
                    dicionarios[arquivo] = cobertura.carregar(arquivo)
            except (json.JSONDecodeError, KeyError, TypeError, OSError):
                continue  # linha cortada, editada à mão ou com arquivo desconhecido
            if not dicionarios[arquivo].tem(texto):
                faltando.setdefault((arquivo, texto), None)

    itens = [{"texto": texto, "tipo": "arquivo:" + arquivo, "paginas": [arquivo]}
             for arquivo, texto in sorted(faltando)]
    os.makedirs(comum.DADOS, exist_ok=True)
    with open(SAIDA, "w", encoding="utf-8") as saida:
        json.dump(itens, saida, ensure_ascii=False, indent=1)
    print(f"{len(itens)} textos sem tradução em {SAIDA}")


if __name__ == "__main__":
    main()
