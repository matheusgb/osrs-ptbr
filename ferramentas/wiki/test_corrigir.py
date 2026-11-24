"""Testes da troca de traduções. Rode: python3 ferramentas/wiki/test_corrigir.py"""
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from corrigir_traducoes import trocar  # noqa: E402


class TrocarTest(unittest.TestCase):
    def test_troca_so_valor_esperado_e_mantem_crlf(self):
        with tempfile.TemporaryDirectory() as pasta:
            caminho = os.path.join(pasta, "t.json")
            with open(caminho, "wb") as f:
                f.write('{\r\n  "A": "um",\r\n  "B":"dois"\r\n}\r\n'.encode("utf-8"))
            n = trocar(caminho, {"A": ("um", "UM"), "B": ("outro valor", "DOIS")})
            self.assertEqual(n, 1)
            with open(caminho, "rb") as f:
                self.assertEqual(f.read().decode("utf-8"), '{\r\n  "A": "UM",\r\n  "B":"dois"\r\n}\r\n')


if __name__ == "__main__":
    unittest.main()
