"""Regras determinísticas de tradução: classificação e validação.

O mesmo texto sempre recebe a mesma decisão. A tradução das frases fica com quem
traduz; estas regras só decidem o que não precisa de tradução e barram traduções
que quebram os critérios do README.
"""
import json
import os
import re

import comum

GLOSSARIO = os.path.normpath(os.path.join(comum.PT_BR, "..", "glossario.json"))
PALAVRAS_COMUNS = os.path.normpath(os.path.join(comum.PT_BR, "..", "palavras_comuns.json"))
_comuns = None


def palavras_comuns():
    """Termos de uma palavra que também são palavra comum ("King", "Tree")."""
    global _comuns
    if _comuns is None:
        try:
            with open(PALAVRAS_COMUNS, encoding="utf-8") as entrada:
                _comuns = frozenset(json.load(entrada))
        except FileNotFoundError:
            _comuns = frozenset()
    return _comuns


PLACEHOLDER = re.compile(r"\[[^\[\]]+\]")
TAG = re.compile(r"<[^>]+>")

# Regra 5: palavras do inglês que não existem em português ("for" fica de fora: "se for").
INGLES = re.compile(r"\b(the|and|you|your|with|this|that|have|of|to|is|are)\b", re.IGNORECASE)

# Regra 6: palavras escritas sem acento que não existem em português.
SEM_ACENTO = {
    "voce", "voces", "nao", "entao", "tambem", "ninguem", "alguem", "porem", "ja", "ate",
    "configuracoes", "configuracao", "opcoes", "opcao", "missao", "missoes", "nivel", "niveis",
    "informacoes", "informacao", "possivel", "impossivel", "disponivel", "estao", "sera", "tera",
    "podera", "facil", "dificil", "rapido", "rapida", "experiencia", "pagina", "numero",
    "inventario", "proximo", "proxima", "ultimo", "ultima", "agua", "arvore", "familia",
    "historia", "magica", "magico", "pocao", "pocoes", "tres", "ola", "parabens", "atraves",
    "alem", "apos", "saude", "servico", "espaco", "preco", "cabeca", "coracao", "acao", "acoes",
    "atencao", "equilibrio", "construcao", "dragao", "mao", "maos",
}
SO_ERRADO = re.compile(r"\b\w+só\b", re.IGNORECASE)

ARQUIVO_MENU = "translations_menu.json"
# Regra 7: substantivos com a mesma forma de um imperativo ("Passe rápido", "Corte de cabelo").
SUBSTANTIVOS_MENU = ("passe", "teleporte", "monte", "corte de")

MENSAGENS = {
    "vazia": "tradução vazia",
    "placeholder": "placeholders diferentes do inglês ou fora de ordem (lista de escolha pode vir traduzida, com o mesmo número de opções)",
    "glossario": "termo do glossário (nome próprio) mudou na tradução",
    "pontuacao": "pontuação final diferente do inglês",
    "ingles": "palavra do inglês sobrando na tradução",
    "acento": "palavra sem acento ou \"-só\" errado",
    "menu_imperativo": "verbo no imperativo no menu (o menu usa infinitivo)",
    "parenteses": "nome em inglês acrescentado entre parênteses",
    "hibrido": "nome do glossário com \"of\" trocado por \"de\" (\"Chambers de Xeric\")",
}


def carregar_glossario(caminho=GLOSSARIO):
    with open(caminho, encoding="utf-8") as entrada:
        return frozenset(json.load(entrada))


def sem_tags(texto):
    return re.sub(r"\s+", " ", TAG.sub(" ", texto)).strip()


# ---------------------------------------------------------------- classificação

def sem_traducao(texto, glossario):
    """True se o texto deve ficar igual ao inglês, sem precisar de tradução."""
    limpo = sem_tags(texto)
    if not re.search(r"[A-Za-z]", PLACEHOLDER.sub("", limpo)):
        return True  # só número, símbolo ou placeholder
    base = limpo.rstrip(".!?:").strip()
    return limpo in glossario or base in glossario


# ---------------------------------------------------------------- validação

PALAVRA_MAIUSCULA = r"[A-Z][\w'’\-]*"
LIGACAO = r"(?:of|the|de|and|&|du|la|le|von|van)"
SEQUENCIA_NOME = re.compile(rf"{PALAVRA_MAIUSCULA}(?:\s+(?:{LIGACAO}\s+)*{PALAVRA_MAIUSCULA})*")


def nomes_proprios(ingles, glossario):
    """Termos do glossário que aparecem no inglês com maiúscula, fora do início de frase.

    Em cada sequência de palavras com maiúscula, pega o maior trecho que está no
    glossário. Uma palavra sozinha no começo de uma frase fica de fora, porque ali a
    maiúscula é só gramática ("Cook the fish").
    """
    texto = PLACEHOLDER.sub(" ", sem_tags(ingles))
    palavras_texto = re.findall(r"[A-Za-z][\w'’\-]*", texto)
    # Em Texto Com Todas As Palavras Em Maiúscula, a maiúscula não indica nome:
    # só termos de mais de uma palavra contam.
    titulo = len(palavras_texto) > 1 and all(p[0].isupper() for p in palavras_texto)
    achados = []
    for sequencia in SEQUENCIA_NOME.finditer(texto):
        palavras = sequencia.group().split()
        inicio_frase = sequencia.start() == 0 or re.search(r"[.!?]\s*$", texto[:sequencia.start()])
        i = 0
        while i < len(palavras):
            for fim in range(len(palavras), i, -1):
                trecho = " ".join(palavras[i:fim])
                if trecho in glossario and re.match(r"[A-Z]", trecho):
                    palavra_solta = fim - i == 1
                    if palavra_solta and trecho in palavras_comuns():
                        i = fim - 1
                        break
                    if not (palavra_solta and (titulo or (i == 0 and inicio_frase))):
                        achados.append(trecho)
                    i = fim - 1
                    break
            i += 1
    return achados


def sequencias_de_nome(ingles, glossario):
    """Sequências com maiúscula do inglês, sem a palavra que só tem maiúscula por abrir a frase.

    "Complete the Theatre of Blood" vira "Theatre of Blood": "Complete" abre a frase e não
    está no glossário, então ela e o "the" depois dela não fazem parte do nome.
    """
    texto = PLACEHOLDER.sub(" ", sem_tags(ingles))
    resultado = set()
    for m in SEQUENCIA_NOME.finditer(texto):
        palavras = m.group().split()
        inicio_frase = m.start() == 0 or re.search(r"[.!?]\s*$", texto[:m.start()])
        # "The"/"A" abrindo a sequência costuma ser parte do nome ("The Shadow Reef").
        if inicio_frase and palavras[0] not in glossario and palavras[0] not in ("The", "A", "An") \
                and m.group() not in glossario:
            palavras = palavras[1:]
            while palavras and re.fullmatch(LIGACAO, palavras[0]):
                palavras = palavras[1:]
        if palavras:
            resultado.add(" ".join(palavras))
    return resultado


def termos_mantidos(ingles, traducao, glossario):
    """Termos do glossário que estão no inglês e ficaram iguais na tradução.

    Pega qualquer sequência de até 12 palavras do inglês, inclusive com palavra
    minúscula no meio ("Amulet of accuracy").
    """
    palavras = re.findall(r"[\w'’&\-]+", PLACEHOLDER.sub(" ", sem_tags(ingles)))
    minusculo = _glossario_minusculo(glossario)
    termos = set()
    for i in range(len(palavras)):
        for fim in range(i + 1, min(i + 12, len(palavras)) + 1):
            trecho = " ".join(palavras[i:fim])
            # Nome de item costuma vir em minúscula na frase: "a necklace of rupture".
            if (trecho in glossario or trecho.lower() in minusculo) and trecho in traducao:
                termos.add(trecho)
    return termos


_minusculo = (None, frozenset())


def _glossario_minusculo(glossario):
    """Termos compostos do glossário em minúscula; recalcula se o glossário mudar."""
    global _minusculo
    if _minusculo[0] is not glossario:
        _minusculo = (glossario, frozenset(t.lower() for t in glossario if " " in t))
    return _minusculo[1]


def hibridos(ingles, traducao, glossario):
    """Nomes "A of B" do glossário que aparecem na tradução como "A de B"."""
    achados = []
    palavras = re.findall(r"[\w'’&\-]+", PLACEHOLDER.sub(" ", sem_tags(ingles)))
    for i in range(len(palavras)):
        for fim in range(i + 3, min(i + 12, len(palavras)) + 1):
            trecho = " ".join(palavras[i:fim])
            if " of " not in trecho or trecho not in glossario or trecho in traducao:
                continue
            a, b = trecho.split(" of ", 1)
            if re.search(re.escape(a) + r" d(?:e|o|a|os|as) " + re.escape(b), traducao):
                achados.append(trecho)
    return achados


def forma_placeholders(texto):
    """Placeholders em ordem. Lista de escolha ("[hasn't/haven't]") vale pelo número de opções:
    o plugin escolhe a opção traduzida pela posição, então "[não terminou/não terminaram]" serve."""
    # Lista só de números ("[10/15/20]") é valor do jogo: precisa ficar igual.
    return [("/", p.count("/")) if "/" in p and re.search(r"[^\W\d_]", p) else p
            for p in PLACEHOLDER.findall(texto)]


def pontuacao_final(texto):
    """Classe da pontuação final: ":", "frase" (. ! ? ...) ou "" (nenhuma).

    Ponto, exclamação, interrogação e reticências são da mesma classe: "I wonder if."
    pode virar "Será que...?" em português. Dois-pontos precisam ficar.
    """
    fim = sem_tags(texto).rstrip(" \"'’”)")
    if fim.endswith(":"):
        return ":"
    return "frase" if fim and fim[-1] in ".!?…" else ""


def imperativos_do_menu(traducoes_menu):
    """Imperativos dos verbos que o menu usa no infinitivo: "comprar" → "compre"."""
    formas = set()
    for valor in traducoes_menu.values():
        palavra = valor.split(" ")[0].split("-")[0].lower()
        if re.fullmatch(r"[a-zçãéêíóôú]{3,}ar", palavra):
            raiz = palavra[:-2]
            raiz = re.sub(r"c$", "qu", re.sub(r"g$", "gu", re.sub(r"ç$", "c", raiz)))
            formas.add(raiz + "e")
        elif re.fullmatch(r"[a-zçãéêíóôú]{3,}[ei]r", palavra):
            raiz = palavra[:-2]
            raiz = re.sub(r"c$", "ç", re.sub(r"g$", "j", raiz))  # tecer → teça, proteger → proteja
            formas.add(raiz + "a")
    return formas


def validar(ingles, traducao, arquivo, glossario, imperativos=frozenset()):
    """Retorna a lista de códigos de erro (vazia se a tradução passa)."""
    if not isinstance(traducao, str) or not traducao.strip():
        return ["vazia"]
    erros = []
    if forma_placeholders(ingles) != forma_placeholders(traducao):
        erros.append("placeholder")

    nomes = nomes_proprios(ingles, glossario)
    if any(nome not in traducao for nome in nomes):
        erros.append("glossario")
    # O nome tem que estar no lugar dele na frase, não pendurado: "...desse jeito (Black Knight)?".
    if any(f"({nome})" in traducao and f"({nome})" not in ingles for nome in nomes):
        erros.append("parenteses")
    if hibridos(ingles, traducao, glossario):
        erros.append("hibrido")

    if pontuacao_final(ingles) and pontuacao_final(ingles) != pontuacao_final(traducao):
        erros.append("pontuacao")

    # Tradução igual ao inglês só passa se a classificação permite (nome, número).
    if not (traducao == ingles and sem_traducao(ingles, glossario)):
        resto = PLACEHOLDER.sub(" ", sem_tags(traducao))
        # Nome com maiúscula que ficou igual nas duas línguas pode ter palavra inglesa
        # ("Theatre of Blood"), mesmo fora do glossário.
        sequencias = sequencias_de_nome(ingles, glossario)
        mantidos = termos_mantidos(ingles, traducao, glossario)
        for nome in sorted(set(nomes) | sequencias | mantidos, key=len, reverse=True):
            resto = re.sub(rf"(?<!\w){re.escape(nome)}(?!\w)", " ", resto)
        if INGLES.search(resto):
            erros.append("ingles")

    palavras = re.findall(r"[a-zA-ZçãõáéíóúâêôÇÃÕÁÉÍÓÚÂÊÔ]+", traducao)
    if any(p.lower() in SEM_ACENTO for p in palavras) or SO_ERRADO.search(traducao):
        erros.append("acento")

    if arquivo == ARQUIVO_MENU and imperativos:
        valor = sem_tags(traducao).lower()
        primeira = valor.split(" ")[0].split("-")[0]
        if primeira in imperativos and not valor.startswith(SUBSTANTIVOS_MENU):
            erros.append("menu_imperativo")
    return erros
