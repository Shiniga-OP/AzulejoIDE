package com.uniditor.nucleo;

import java.util.ArrayList;

public class BufferSimples implements Buffer {
    public final ArrayList<String> linhas = new ArrayList<>();

    public BufferSimples() {
        linhas.add("");
    }

    public BufferSimples(String texto) {
        defTexto(texto);
    }

    @Override
    public void add(int linha, int coluna, String texto) {
        String l = linhas.get(linha);
        String nova = l.substring(0, coluna) + texto + l.substring(coluna);

        // se o texto inserido tem quebras de linha, divide
        if(nova.contains("\n")) {
            String[] partes = nova.split("\n", -1);
            linhas.set(linha, partes[0]);
            for(int i = 1; i < partes.length; i++) {
                linhas.add(linha + i, partes[i]);
            }
        } else {
            linhas.set(linha, nova);
        }
    }

    @Override
    public void rm(int linha, int coluna, int quantidade) {
        String l = linhas.get(linha);

        // deleção simples dentro da linha
        if(coluna + quantidade <= l.length()) {
            linhas.set(linha, l.substring(0, coluna) + l.substring(coluna + quantidade));
            return;
        }
        // deleção que cruza quebra de linha
        // constroi texto continuo a partir desta linha e deleta
        StringBuilder sb = new StringBuilder();
        sb.append(l);
        int linhaAtual = linha + 1;
        int restante = quantidade - (l.length() - coluna);

        while(restante > 0 && linhaAtual < linhas.size()) {
            sb.append("\n");
            String prox = linhas.get(linhaAtual);
            if(restante <= prox.length()) {
                sb.append(prox.substring(restante));
                restante = 0;
            } else {
                restante -= prox.length() + 1; // +1 pela quebra
            }
            linhas.remove(linhaAtual);
        }

        String resultado = sb.toString().substring(0, coluna) +
			sb.toString().substring(coluna + Math.min(quantidade, sb.length() - coluna));

        String[] partes = resultado.split("\n", -1);
        linhas.set(linha, partes[0]);
        for(int i = 1; i < partes.length; i++) {
            linhas.add(linha + i, partes[i]);
        }
    }

    @Override
    public String linha(int numero) {
        return linhas.get(numero);
    }

    @Override
    public int totalLinhas() {
        return linhas.size();
    }

    @Override
    public String texto() {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < linhas.size(); i++) {
            if(i > 0) sb.append("\n");
            sb.append(linhas.get(i));
        }
        return sb.toString();
    }

	@Override
	public void defTexto(String texto) {
		linhas.clear();
		String[] partes = texto.split("\n", -1);
		for(String p : partes) linhas.add(p);
		if(linhas.isEmpty()) linhas.add("");
	}
}

