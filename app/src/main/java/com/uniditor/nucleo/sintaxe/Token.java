package com.uniditor.nucleo.sintaxe;

import com.uniditor.nucleo.graficos.Cor;

public class Token {
    public final String descricao; // futuro: exibido no autocomplete
	public final boolean negrito, italico;
	public final int inicio, fim, cor;

    public Token(int inicio, int fim, int cor, String descricao, boolean negrito, boolean italico) {
        this.descricao = descricao;
		this.negrito = negrito;
		this.italico = italico;
		this.inicio = inicio;
		this.fim = fim;
		this.cor = cor;
    }
	
	public Token(int inicio, int fim, int cor, boolean negrito, boolean italico) {
        this(inicio, fim, cor, null, negrito, italico);
    }

    public Token(int inicio, int fim, int cor) {
        this(inicio, fim, cor, null, false, false);
    }
}

