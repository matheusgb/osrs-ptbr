"""Caminhos e funções usados pelos scripts da base de diálogos."""
import json
import os
import random
import time
import urllib.error
import urllib.parse
import urllib.request

PASTA = os.path.dirname(os.path.abspath(__file__))
DADOS = os.path.join(PASTA, "dados")
TRANSCRICOES = os.path.join(DADOS, "transcricoes.jsonl")
FALAS = os.path.join(DADOS, "falas.json")
FALTANDO = os.path.join(DADOS, "faltando.json")
RESUMO = os.path.join(DADOS, "cobertura.md")
PT_BR = os.path.normpath(os.path.join(PASTA, "..", "..", "traducoes", "pt-BR"))

API = "https://oldschool.runescape.wiki/api.php"
# Identifica o projeto, sem dado pessoal, como a wiki pede.
USER_AGENT = "osrs-ptbr-translation-research/0.1 (personal non-commercial translation project)"
PAUSA_MIN = 8.0
PAUSA_MAX = 12.0
ESPERA_ERRO_MIN = 60


class Wiki:
    """Cliente da API que faz uma requisição por vez e espera entre elas."""

    def __init__(self):
        self.ultima = 0.0
        self.requisicoes = 0

    def get(self, **params):
        params.update(format="json", formatversion="2", maxlag="5")
        url = API + "?" + urllib.parse.urlencode(params)
        while True:
            self._esperar()
            pedido = urllib.request.Request(url, headers={
                "User-Agent": USER_AGENT,
                "Accept-Encoding": "gzip",
            })
            try:
                with urllib.request.urlopen(pedido, timeout=60) as resposta:
                    corpo = resposta.read()
                    if resposta.headers.get("Content-Encoding") == "gzip":
                        import gzip
                        corpo = gzip.decompress(corpo)
                    dados = json.loads(corpo)
            except urllib.error.HTTPError as erro:
                if erro.code in (429, 503):
                    self._esperar_erro(erro.headers.get("Retry-After"), f"HTTP {erro.code}")
                    continue
                raise
            finally:
                self.ultima = time.monotonic()
                self.requisicoes += 1

            if dados.get("error", {}).get("code") == "maxlag":
                self._esperar_erro(None, "servidor ocupado (maxlag)")
                continue
            if "error" in dados:
                raise RuntimeError(dados["error"])
            return dados

    def _esperar(self):
        pausa = random.uniform(PAUSA_MIN, PAUSA_MAX)
        falta = self.ultima + pausa - time.monotonic()
        if self.ultima and falta > 0:
            time.sleep(falta)

    @staticmethod
    def _esperar_erro(retry_after, motivo):
        try:
            segundos = max(ESPERA_ERRO_MIN, int(retry_after or 0))
        except ValueError:
            segundos = ESPERA_ERRO_MIN
        print(f"  {motivo}: esperando {segundos} s", flush=True)
        time.sleep(segundos)


def ler_transcricoes():
    """Retorna {título: {"revid": n, "content": wikitext}} do arquivo local."""
    paginas = {}
    if os.path.exists(TRANSCRICOES):
        with open(TRANSCRICOES, encoding="utf-8") as arquivo:
            for numero, linha in enumerate(arquivo, 1):
                if not linha.strip():
                    continue
                try:
                    pagina = json.loads(linha)
                except json.JSONDecodeError:
                    # Linha cortada por um download interrompido: a página é baixada de novo.
                    print(f"  linha {numero} inválida em {TRANSCRICOES}, ignorada", flush=True)
                    continue
                # Uma página repetida no fim do arquivo é a revisão mais nova.
                paginas[pagina["title"]] = pagina
    return paginas
