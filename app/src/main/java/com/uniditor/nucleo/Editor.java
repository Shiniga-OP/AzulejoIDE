package com.uniditor.nucleo;

import com.uniditor.nucleo.graficos.Renderizador;
import com.uniditor.nucleo.entradas.Cursor;
import com.uniditor.nucleo.entradas.EntradaTexto;
import com.uniditor.nucleo.entradas.Teclado;

public abstract class Editor {
    public Renderizador render;
    public Cursor cursor;
    public EntradaTexto entrada;
    public Buffer buffer;
	
	public float rolamentoY;
	public float ESPACO_TOPO;

    public abstract void abrirTeclado();
    public abstract void att();
    public abstract void aoTocar(float x, float y);
	public abstract int[] posToque(float x, float y);
	public abstract void liberar();
}
