package com.auto;

import java.util.ArrayList;
import java.util.List;
/*
 * quebra um texto .auto em tokens.
 * tipos de token:
 * - PALAVRA: identificador de chave
 * - TEXTO: string entre aspas
 * - NUMERO: numero
 * - IGUAL: =
 * - ABRE: {
 * - FECHA: }
 * - VIRGULA: ,
*/
public class Tokenizador {
	public static enum TipoToken {
		PALAVRA, TEXTO, NUMERO,
		IGUAL, ABRE, FECHA, VIRGULA
	}

	public static class Token {
		public TipoToken tipo;
		public String valor;
		public int linha;

		public Token(TipoToken tipo, String valor, int linha) {
			this.tipo = tipo;
			this.valor = valor;
			this.linha = linha;
		}

		@Override
		public String toString() {
			return tipo + "(" + valor + ")";
		}
	}
	public String fonte;
	public int pos = 0;
	public int linha = 1;

	public Tokenizador(String fonte) {
		this.fonte = fonte;
	}

	public List<Token> tokenizar() {
		final List<Token> tokens = new ArrayList<Token>();

		while(true) {
			pularEspacosEComentarios();
			if(fimDoArquivo()) break;

			final char c = atual();

			if(c == '=') {
				tokens.add(new Token(TipoToken.IGUAL, "=", linha));
				avancar();
			} else if(c == '{') {
				tokens.add(new Token(TipoToken.ABRE, "{", linha));
				avancar();
			} else if(c == '}') {
				tokens.add(new Token(TipoToken.FECHA, "}", linha));
				avancar();
			} else if(c == ',') {
				tokens.add(new Token(TipoToken.VIRGULA, ",", linha));
				avancar();
			} else if(c == '"') {
				tokens.add(lerTexto());
			} else if(Character.isDigit(c)) {
				tokens.add(lerNumero());
			} else if(ehInicioDePalavra(c)) {
				tokens.add(lerPalavra());
			} else {
				throw new AutoErro("Caractere inesperado '" + c + "'", linha);
			}
		}
		return tokens;
	}

	public Token lerTexto() {
		final int linhaInicio = linha;
		avancar(); // pula a aspa de abertura

		final StringBuilder sb = new StringBuilder();
		while(!fimDoArquivo() && atual() != '"') {
			sb.append(atual());
			avancar();
		}
		if(fimDoArquivo()) {
			throw new AutoErro("Texto sem aspas de fechamento", linhaInicio);
		}
		avancar(); // pula a aspa de fechamento
		return new Token(TipoToken.TEXTO, sb.toString(), linhaInicio);
	}

	public Token lerNumero() {
		final int linhaInicio = linha;
		final StringBuilder sb = new StringBuilder();

		while(!fimDoArquivo() && (Character.isDigit(atual()) || atual() == '.')) {
			sb.append(atual());
			avancar();
		}
		return new Token(TipoToken.NUMERO, sb.toString(), linhaInicio);
	}

	public Token lerPalavra() {
		final int linhaInicio = linha;
		final StringBuilder sb = new StringBuilder();

		while(!fimDoArquivo() && ehContinuacaoDePalavra(atual())) {
			sb.append(atual());
			avancar();
		}
		return new Token(TipoToken.PALAVRA, sb.toString(), linhaInicio);
	}

	public boolean ehInicioDePalavra(char c) {
		return Character.isLetter(c) || c == '_';
	}

	public boolean ehContinuacaoDePalavra(char c) {
		return Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == '/';
	}

	public void pularEspacosEComentarios() {
		while(!fimDoArquivo()) {
			final char c = atual();

			if(c == '\n') {
				linha++;
				avancar();
			} else if(Character.isWhitespace(c)) {
				avancar();
			} else if(c == '#' || (c == '/' && proximo() == '/')) {
				while(!fimDoArquivo() && atual() != '\n') avancar();
			} else {
				break;
			}
		}
	}

	public char atual() {
		return fonte.charAt(pos);
	}

	public char proximo() {
		return pos + 1 < fonte.length() ? fonte.charAt(pos + 1) : '\0';
	}

	public void avancar() {
		pos++;
	}

	public boolean fimDoArquivo() {
		return pos >= fonte.length();
	}
}
