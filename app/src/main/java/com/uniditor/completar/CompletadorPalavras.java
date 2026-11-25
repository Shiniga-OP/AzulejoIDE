package com.uniditor.completar;

import com.uniditor.Buffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompletadorPalavras implements Completador {
    // palavras que sempre entram nas sugestões (palavras-chave, tipos...)
    public final List<String> fixas = new ArrayList<String>();

    public CompletadorPalavras(String[]... grupos) {
        for(String[] grupo : grupos) {
            for(String palavra : grupo) fixas.add(palavra);
        }
    }

    @Override
    public List<Sugestao> sugerir(Buffer buffer, int linha, int coluna, final String prefixo, int max) {
        final Map<String, int[]> contagem = new HashMap<String, int[]>();
        for(String fixa : fixas) contagem.put(fixa, new int[]{0});

        final int total = buffer.totalLinhas();
        for(int i = 0; i < total; i++) contar(buffer.linha(i), contagem);

        final List<String> achadas = new ArrayList<String>();
        final int tam = prefixo.length();
        for(String palavra : contagem.keySet()) {
            if(palavra.length() > tam && palavra.regionMatches(true, 0, prefixo, 0, tam)) achadas.add(palavra);
        }
        Collections.sort(achadas, new Comparator<String>() {
				@Override
				public int compare(String a, String b) {
					// mesma caixa do que foi digitado vem primeiro
					final boolean caixaA = a.startsWith(prefixo);
					final boolean caixaB = b.startsWith(prefixo);
					if(caixaA != caixaB) return caixaA ? -1 : 1;
					// depois as mais usadas no arquivo
					final int usoA = contagem.get(a)[0];
					final int usoB = contagem.get(b)[0];
					if(usoA != usoB) return usoB - usoA;
					if(a.length() != b.length()) return a.length() - b.length();
					return a.compareTo(b);
				}
			});
        final int n = Math.min(achadas.size(), max);
        final List<Sugestao> r = new ArrayList<Sugestao>(n);
        for(int i = 0; i < n; i++) r.add(new Sugestao(achadas.get(i), Sugestao.PALAVRA));
        return r;
    }

    // conta os identificadores da linha, ignorando números e comentário de linha
    public void contar(String linha, Map<String, int[]> contagem) {
        final int n = linha.length();
        int i = 0;
        while(i < n) {
            final char c = linha.charAt(i);
            if(c == '/' && i + 1 < n && linha.charAt(i + 1) == '/') return;
            if(eIdentificador(c)) {
                final int inicio = i;
                while(i < n && eIdentificador(linha.charAt(i))) i++;
                if(!Character.isDigit(c) && i - inicio > 2) {
                    final String palavra = linha.substring(inicio, i);
                    final int[] uso = contagem.get(palavra);
                    if(uso == null) contagem.put(palavra, new int[]{1});
                    else uso[0]++;
                }
            } else {
                i++;
            }
        }
    }

    public static boolean eIdentificador(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
