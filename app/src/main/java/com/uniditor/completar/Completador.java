package com.uniditor.completar;

import com.uniditor.Buffer;
import java.util.List;

public interface Completador {
    /*
     * devolve até "max" sugestões para o prefixo digitado na posição(linha, coluna)
     * as sugestões já vêm ordenadas da melhor para a pior
     */
    List<Sugestao> sugerir(Buffer buffer, int linha, int coluna, String prefixo, int max);
}
