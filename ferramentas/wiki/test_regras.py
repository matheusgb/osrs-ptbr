"""Testes das regras. Rode: python3 ferramentas/wiki/test_regras.py"""
import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import regras  # noqa: E402

GLOSSARIO = frozenset({"Romeo", "Varrock Square", "Romeo & Juliet", "Sheep Shearer", "Cook",
                       "Grand Exchange", "Slayer", "Bronze sword", "Amulet of accuracy"})
MENU = frozenset(regras.imperativos_do_menu({"Buy": "Comprar", "Talk-to": "Falar com", "Take": "Pegar", "Pass": "Passar"}))
QUESTS = "translations_quests.json"


# Os testes não dependem do arquivo real de palavras comuns.
regras._comuns = frozenset()


def erros(en, pt, arquivo=QUESTS):
    return regras.validar(en, pt, arquivo, GLOSSARIO, MENU)


class ClassificacaoTest(unittest.TestCase):
    def test_nome_inteiro_fica_em_ingles(self):
        self.assertTrue(regras.sem_traducao("Romeo & Juliet", GLOSSARIO))
        self.assertTrue(regras.sem_traducao("Sheep Shearer.", GLOSSARIO))

    def test_numero_e_placeholder_ficam(self):
        self.assertTrue(regras.sem_traducao("1,234", GLOSSARIO))
        self.assertTrue(regras.sem_traducao("[number] / [number]", GLOSSARIO))

    def test_frase_vai_para_traducao(self):
        self.assertFalse(regras.sem_traducao("Talk to Romeo.", GLOSSARIO))


class ValidacaoTest(unittest.TestCase):
    EN = "I can start this quest by talking to Romeo in Varrock Square."

    def test_traducao_certa_passa(self):
        self.assertEqual(erros(self.EN, "Posso começar esta quest falando com Romeo na Varrock Square."), [])

    def test_vazia(self):
        self.assertEqual(erros(self.EN, " "), ["vazia"])

    def test_placeholder(self):
        self.assertIn("placeholder", erros("You need [number] more.", "Você precisa de mais [número]."))
        self.assertIn("placeholder", erros("[a] and [b].", "[b] e [a]."))

    def test_glossario(self):
        self.assertIn("glossario", erros(self.EN, "Posso começar esta quest falando com Romeu na Varrock Square."))

    def test_nome_pendurado_entre_parenteses(self):
        self.assertIn("parenteses", erros("Talk to Romeo.", "Fale com o Romeu (Romeo)."))
        self.assertEqual(erros("Talk to Romeo (again).", "Fale com Romeo (de novo)."), [])

    def test_titulo_com_maiusculas_so_conta_termo_composto(self):
        self.assertNotIn("glossario", erros("View Cook Options", "Ver opções do cozinheiro"))
        self.assertIn("glossario", erros("Open Grand Exchange Menu", "Abrir menu do Mercado"))

    def test_for_e_portugues_e_nome_com_ingles_passa(self):
        self.assertEqual(erros("If it is important.", "Se for importante."), [])
        self.assertEqual(erros("She runs the Theatre of Blood.", "Ela comanda o Theatre of Blood."), [])
        self.assertEqual(erros("He gives you an Amulet of accuracy.", "Ele te dá um Amulet of accuracy."), [])
        self.assertIn("ingles", erros("Complete the Theatre of Blood.", "Complete the Theatre of Blood em 14 minutos."))
        self.assertEqual(erros("Complete the Theatre of Blood.", "Complete o Theatre of Blood."), [])
        self.assertEqual(erros("The Shadow Reef is dangerous.", "The Shadow Reef é perigoso."), [])
        self.assertEqual(erros("You craft an amulet of accuracy.", "Você cria um amulet of accuracy."), [])

    def test_palavra_no_inicio_da_frase_nao_e_nome(self):
        self.assertNotIn("glossario", erros("Cook the fish.", "Cozinhe o peixe."))

    def test_pontuacao(self):
        self.assertIn("pontuacao", erros("Hello there!", "Olá"))
        self.assertNotIn("pontuacao", erros("Wait...", "Espere…"))
        self.assertNotIn("pontuacao", erros("I wonder if she forgave me.", "Será que ela me perdoou?"))
        self.assertIn("pontuacao", erros("Total:", "Total"))
        self.assertNotIn("pontuacao", erros("Tell me about..", "Fale sobre..."))

    def test_ingles_sobrando(self):
        self.assertIn("ingles", erros("Give it to the guard.", "Dê para the guarda."))
        self.assertIn("ingles", erros("Give it to the guard.", "Give it to the guard."))
        self.assertEqual(erros("Romeo & Juliet", "Romeo & Juliet"), [])

    def test_ingles_dentro_de_nome_nao_conta(self):
        self.assertEqual(erros("Ask about Romeo & Juliet.", "Pergunte sobre Romeo & Juliet."), [])

    def test_acento(self):
        self.assertIn("acento", erros("You can go.", "Voce pode ir."))
        self.assertIn("acento", erros("That is it.", "É issó."))

    def test_menu_imperativo(self):
        self.assertIn("menu_imperativo", erros("Buy-1", "Compre-1", "translations_menu.json"))
        self.assertEqual(erros("Buy-1", "Comprar-1", "translations_menu.json"), [])
        self.assertNotIn("menu_imperativo", erros("Buy-1", "Compre-1", QUESTS))
        self.assertEqual(erros("Quick-pass", "Passe rápido", "translations_menu.json"), [])


if __name__ == "__main__":
    unittest.main()
