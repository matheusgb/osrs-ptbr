"""Testes do extrator. Rode: python3 ferramentas/wiki/test_extrair_falas.py"""
import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from extrair_falas import falas_da_linha, limpar  # noqa: E402

HANS = """{{Transcript|npc}}
==Standard dialogue==
* '''Hans:''' Hello. What are you doing here?
* {{tselect|Select an option}}
* {{topt|I'm looking for whoever is in charge of this place.}}
** '''Player:''' I'm looking for whoever is in charge of this place.
** '''Hans:''' Who, the Duke? He's in his study, on the first floor.
** {{tact|end}}
** {{qact|Hans runs away from the player screaming}}
** '''Hans:''' {{overhead|Help! Help!}}
** '''Hans:''' You are in Lumbridge Castle, in the Kingdom of [[Misthalin]]. Across the [[River Lum|river]], the road leads north.
* {{tact|receives=a [new clue/casket]}}
* {{tbox|You've [found a new clue/obtained a casket]!}}
* '''Hans:''' You've spent [amount] days, [amount] hours, [amount] minutes in the world since you arrived [amount] days ago.
"""


def todas(texto):
    return [f for linha in texto.splitlines() for f in falas_da_linha(linha)]


class ExtrairFalasTest(unittest.TestCase):
    def test_hans(self):
        falas = todas(HANS)
        self.assertIn(("fala", "Hello. What are you doing here?"), falas)
        self.assertIn(("opcao", "I'm looking for whoever is in charge of this place."), falas)
        self.assertIn(("fala", "Who, the Duke? He's in his study, on the first floor."), falas)
        self.assertIn(("overhead", "Help! Help!"), falas)
        self.assertIn(("caixa", "You've [found a new clue/obtained a casket]!"), falas)
        self.assertIn(("fala", "You are in Lumbridge Castle, in the Kingdom of Misthalin. "
                               "Across the river, the road leads north."), falas)

    def test_ignora_cena_e_titulo(self):
        textos = [t for _, t in todas(HANS)]
        self.assertNotIn("Select an option", textos)
        self.assertNotIn("Hans runs away from the player screaming", textos)
        self.assertNotIn("end", textos)

    def test_nome_com_apostrofo(self):
        self.assertEqual(falas_da_linha("* '''Beefy' Burns:''' Hello there."), [("fala", "Hello there.")])

    def test_nome_em_nowiki(self):
        linha = "* '''<nowiki>'Black-eye':</nowiki>''' Ah, we be building this tower."
        self.assertEqual(falas_da_linha(linha), [("fala", "Ah, we be building this tower.")])

    def test_texto_com_igual_nao_e_parametro(self):
        self.assertEqual(falas_da_linha("* {{topt|Total xp = lots}}"), [("opcao", "Total xp = lots")])
        self.assertEqual(falas_da_linha("* {{tbox|pic=Coins.png|You hand over the coins.}}"),
                         [("caixa", "You hand over the coins.")])

    def test_modelo_sem_fechamento(self):
        self.assertEqual(limpar("Hello {{plink|Bob"), "Hello plink|Bob")

    def test_limpar(self):
        self.assertEqual(limpar("Take the {{plink|Bronze sword}} to [[Bob]]&nbsp;now.<br>Thanks"),
                         "Take the Bronze sword to Bob now. Thanks")


if __name__ == "__main__":
    unittest.main()
