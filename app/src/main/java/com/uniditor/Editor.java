package com.uniditor;

import com.uniditor.graficos.Renderizador;
import com.uniditor.entradas.Cursor;
import com.uniditor.entradas.EntradaTexto;
import com.uniditor.entradas.Teclado;

public abstract class Editor {
    public Renderizador render;
    public Cursor cursor;
    public EntradaTexto entrada;
    public Buffer buffer;

	public float rolamentoY;
	public float rolamentoX;
	public float ESPACO_TOPO;

    public abstract void abrirTeclado();
    public abstract void att();
    public abstract void aoTocar(float x, float y);
	public abstract int[] posToque(float x, float y);
	public abstract void liberar();
	public abstract float larguraSarjeta();
	public abstract float larguraMaxLinha();
}
