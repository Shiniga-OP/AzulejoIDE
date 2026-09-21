package com.uniditor.nucleo.sintaxe;

import com.uniditor.nucleo.graficos.Cor;

public interface Tokenizador {
    /*
     * tokeniza uma linha de texto e retorna um array de Token[]
     * pronto pra passar pro Renderizador.renderTextoCor()
     * o tokenizador é responsavel por decidir posição e cor de cada trecho
     */
    Token[] tokenizar(String linha);
    /*
     * reinicia estado interno entre linhas(comentario de bloco)
     */
    void reiniciar();
}

