package com.auto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/*
 * um valor da linguagem Auto.
 * pode ser:
 * - TEXTO: uma string
 * - NUMERO: um numero(guardado como String, convertido sob demanda)
 * - BLOCO: um mapa chave = valor
 * - LISTA: uma lista de valores
*/
public class No {
	public static enum Tipo {
		TEXTO, NUMERO, BLOCO, LISTA
	}
	public Tipo tipo;
	public String texto;
	public Map<String, No> bloco;
	public List<No> lista;

	public No(Tipo tipo) {
		this.tipo = tipo;
	}

	public static No deTexto(String valor) {
		final No n = new No(Tipo.TEXTO);
		n.texto = valor;
		return n;
	}

	public static No deNumero(String valor) {
		final No n = new No(Tipo.NUMERO);
		n.texto = valor;
		return n;
	}

	public static No deBloco() {
		final No n = new No(Tipo.BLOCO);
		n.bloco = new LinkedHashMap<String, No>();
		return n;
	}

	public static No deLista() {
		final No n = new No(Tipo.LISTA);
		n.lista = new ArrayList<No>();
		return n;
	}

	public boolean ehTexto() {
		return tipo == Tipo.TEXTO;
	}

	public boolean ehBloco() {
		return tipo == Tipo.BLOCO;
	}

	public boolean ehLista() {
		return tipo == Tipo.LISTA;
	}

	// atalhos pra navegar num bloco sem ficar fazendo cast toda hora
	public No pos(String chave) {
		if(bloco == null) return null;
		return bloco.get(chave);
	}

	public String posTexto(String chave) {
		final No n = pos(chave);
		return n == null ? null : n.texto;
	}

	public String posTexto(String chave, String padrao) {
		final String valor = posTexto(chave);
		return valor == null ? padrao : valor;
	}

	public int posInteiro(String chave, int padrao) {
		final String valor = posTexto(chave);
		return valor == null ? padrao : Integer.parseInt(valor);
	}

	@Override
	public String toString() {
		if(tipo == Tipo.TEXTO) return "\"" + texto + "\"";
		if(tipo == Tipo.NUMERO) return texto;
		if(tipo == Tipo.BLOCO) return bloco.toString();
		if(tipo == Tipo.LISTA) return textoDaLista();
		return "?";
	}

	public String textoDaLista() {
		final StringBuilder sb = new StringBuilder();
		sb.append("[");
		for(int i = 0; i < lista.size(); i++) {
			if(i > 0) sb.append(", ");
			sb.append(lista.get(i).toString());
		}
		sb.append("]");
		return sb.toString();
	}
}
