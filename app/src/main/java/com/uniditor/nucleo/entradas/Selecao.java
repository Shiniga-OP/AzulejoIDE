package com.uniditor.nucleo.entradas;

import com.uniditor.nucleo.Buffer;

public class Selecao {
    public int ancorLinha = 0;
    public int ancorColuna = 0;
    public int fimLinha = 0;
    public int fimColuna = 0;
    public boolean ativa = false;

    public void iniciar(int linha, int coluna) {
        ancorLinha = linha;
        ancorColuna = coluna;
        fimLinha = linha;
        fimColuna = coluna;
        ativa = true;
    }

    public void att(int linha, int coluna) {
        fimLinha = linha;
        fimColuna = coluna;
    }

    public void limpar() {
        ativa = false;
    }

    public int[] inicio() {
        if(antes(ancorLinha, ancorColuna, fimLinha, fimColuna))
            return new int[]{ancorLinha, ancorColuna};
        return new int[]{fimLinha, fimColuna};
    }

    public int[] fim() {
        if(antes(ancorLinha, ancorColuna, fimLinha, fimColuna))
            return new int[]{fimLinha, fimColuna};
        return new int[]{ancorLinha, ancorColuna};
    }

    public String extrair(Buffer buffer) {
        if(!ativa) return "";
        int[] ini = inicio();
        int[] fim = fim();

        int maxLinha = buffer.totalLinhas() - 1;
        ini[0] = Math.min(ini[0], maxLinha);
        fim[0] = Math.min(fim[0], maxLinha);
        ini[1] = Math.min(ini[1], buffer.linha(ini[0]).length());
        fim[1] = Math.min(fim[1], buffer.linha(fim[0]).length());

        if(ini[0] == fim[0]) return buffer.linha(ini[0]).substring(ini[1], fim[1]);

        StringBuilder sb = new StringBuilder();
        sb.append(buffer.linha(ini[0]).substring(ini[1]));
        for(int i = ini[0] + 1; i < fim[0]; i++) {
            sb.append('\n');
            sb.append(buffer.linha(i));
        }
        sb.append('\n');
        sb.append(buffer.linha(fim[0]).substring(0, fim[1]));
        return sb.toString();
    }

    public static boolean antes(int l1, int c1, int l2, int c2) {
        return l1 < l2 || (l1 == l2 && c1 <= c2);
    }
}
