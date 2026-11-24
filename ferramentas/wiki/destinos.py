"""Destino de cada tipo de texto e dados que as regras precisam dos JSONs atuais."""
import json
import os

import comum
from cobertura import ARQUIVO_DO_TIPO
import regras


def arquivos_de_destino(tipo):
    """"arquivo:<json>" vem do registro do plugin; os outros tipos estão em ARQUIVO_DO_TIPO."""
    if tipo.startswith("arquivo:"):
        return [tipo[len("arquivo:"):]]
    return [ARQUIVO_DO_TIPO[tipo]]


def imperativos():
    with open(os.path.join(comum.PT_BR, regras.ARQUIVO_MENU), encoding="utf-8-sig") as entrada:
        return frozenset(regras.imperativos_do_menu(json.load(entrada)))
