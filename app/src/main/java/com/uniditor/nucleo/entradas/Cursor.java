package com.uniditor.nucleo.entradas;

import com.uniditor.nucleo.Buffer;

public interface Cursor {
    int linha();
    int coluna();
    void def(int linha, int coluna);
    void mover(int deltaLinha, int deltaColuna, Buffer buffer);
    void clampar(Buffer buffer); // garante que não sai dos limites
}
