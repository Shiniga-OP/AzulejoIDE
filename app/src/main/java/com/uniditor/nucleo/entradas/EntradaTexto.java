package com.uniditor.nucleo.entradas;

import com.uniditor.nucleo.Buffer;

public interface EntradaTexto {
    Teclado teclado;

    void add(String texto);
    void rmAntes();
    void rmDepois();
	
	void aoDigitar(String texto);
    // seleção
    Selecao selecao();
    void iniciarSelecao(int linha, int coluna);
    void attSelecao(int linha, int coluna);
    void limparSelecao();
    void selecionarTudo(Buffer buffer);
    String copiar();
    void rmSelecao(); // recortar/deletar seleção
}

