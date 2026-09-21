package com.uniditor.nucleo;

public interface Buffer {
    void add(int linha, int coluna, String texto);
	void defTexto(String texto);
    void rm(int linha, int coluna, int quantidade);
    String linha(int numero);
    int totalLinhas();
    String texto(); // documento inteiro, pra salvar
}
