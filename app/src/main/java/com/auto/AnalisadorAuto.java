package com.auto;

import com.auto.Tokenizador.Token;
import com.auto.Tokenizador.TipoToken;
import java.util.List;
/*
 * analisa os tokens de um arquivo .auto e monta um No raiz do tipo BLOCO.
 * gramatica:
 *   arquivo = par*
 *   par = PALAVRA IGUAL valor
 *   valor = TEXTO | NUMERO | bloco
 *   bloco = ABRE (par (VIRGULA par)* VIRGULA? | item (VIRGULA item)* VIRGULA?)? FECHA
 *   item = TEXTO | NUMERO | bloco
*/
public class AnalisadorAuto {
	public List<Token> tokens;
	public int pos = 0;

	public static No analisar(String fonte) {
		final List<Token> tokens = new Tokenizador(fonte).tokenizar();
		final AnalisadorAuto analisador = new AnalisadorAuto(tokens);
		return analisador.analisarArquivo();
	}

	public AnalisadorAuto(List<Token> tokens) {
		this.tokens = tokens;
	}

	public No analisarArquivo() {
		final No raiz = No.deBloco();

		while(!fimDosTokens()) {
			Token chave = esperar(TipoToken.PALAVRA);
			esperar(TipoToken.IGUAL);
			No valor = analisarValor();
			raiz.bloco.put(chave.valor, valor);
		}
		return raiz;
	}

	public No analisarValor() {
		final Token t = atual();

		if(t.tipo == TipoToken.TEXTO) {
			avancar();
			return No.deTexto(t.valor);
		}
		if(t.tipo == TipoToken.NUMERO) {
			avancar();
			return No.deNumero(t.valor);
		}
		if(t.tipo == TipoToken.ABRE) {
			return analisarBloco();
		}
		throw new AutoErro("Valor invalido: " + t.valor, t.linha);
	}

	public No analisarBloco() {
		esperar(TipoToken.ABRE);

		if(atual().tipo == TipoToken.FECHA) {
			avancar();
			return No.deBloco();
		}
		// olha o segundo token pra decidir se e mapa (PALAVRA IGUAL ...) ou lista (item, item, ...)
		final boolean ehMapa = atual().tipo == TipoToken.PALAVRA && olharAdiante(1).tipo == TipoToken.IGUAL;

		final No no = ehMapa ? No.deBloco() : No.deLista();

		while(true) {
			if(ehMapa) {
				final Token chave = esperar(TipoToken.PALAVRA);
				esperar(TipoToken.IGUAL);
				final No valor = analisarValor();
				no.bloco.put(chave.valor, valor);
			} else {
				no.lista.add(analisarValor());
			}
			if(atual().tipo == TipoToken.VIRGULA) {
				avancar();
				if(atual().tipo == TipoToken.FECHA) break; // virgula sobrando antes do fecha
				continue;
			}
			break;
		}
		esperar(TipoToken.FECHA);
		return no;
	}

	public Token esperar(TipoToken tipo) {
		final Token t = atual();
		if(t.tipo != tipo) {
			throw new AutoErro("Esperava " + tipo + " mas achou " + t.tipo + " (" + t.valor + ")", t.linha);
		}
		avancar();
		return t;
	}

	public Token atual() {
		if(fimDosTokens()) {
			final int ultimaLinha = tokens.isEmpty() ? 0 : tokens.get(tokens.size() - 1).linha;
			throw new AutoErro("Fim inesperado do arquivo", ultimaLinha);
		}
		return tokens.get(pos);
	}

	public Token olharAdiante(int distancia) {
		final int alvo = pos + distancia;
		if(alvo >= tokens.size()) {
			final int ultimaLinha = tokens.isEmpty() ? 0 : tokens.get(tokens.size() - 1).linha;
			throw new AutoErro("Fim inesperado do arquivo", ultimaLinha);
		}
		return tokens.get(alvo);
	}

	public void avancar() {
		pos++;
	}

	public boolean fimDosTokens() {
		return pos >= tokens.size();
	}
}
