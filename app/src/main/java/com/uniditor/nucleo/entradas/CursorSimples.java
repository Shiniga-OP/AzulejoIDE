package com.uniditor.nucleo.entradas;

import com.uniditor.nucleo.Buffer;

public class CursorSimples implements Cursor {
    public int linha = 0;
    public int coluna = 0;

    @Override
    public int linha() {
        return linha;
    }

    @Override
    public int coluna() {
        return coluna;
    }

    @Override
    public void def(int linha, int coluna) {
        this.linha = linha;
        this.coluna = coluna;
    }

    @Override
    public void mover(int deltaLinha, int deltaColuna, Buffer buffer) {
        linha = Math.max(0, Math.min(linha + deltaLinha, buffer.totalLinhas() - 1));
        coluna = Math.max(0, Math.min(coluna + deltaColuna, buffer.linha(linha).length()));
    }

    @Override
    public void clampar(Buffer buffer) {
        linha = Math.max(0, Math.min(linha, buffer.totalLinhas() - 1));
        coluna = Math.max(0, Math.min(coluna, buffer.linha(linha).length()));
    }
}

