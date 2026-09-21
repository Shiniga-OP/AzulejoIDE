package com.azulejo.gerenciador;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class No {
	public final File arquivo;
	public final int nivel;
	public boolean expandido = false;

	public No(File arquivo, int nivel) {
		this.arquivo = arquivo;
		this.nivel = nivel;
	}

	public boolean pasta() {
		return arquivo.isDirectory();
	}

	public String nome() {
		return arquivo.getName();
	}

	// pastas primeiro, depois arquivos, ambos em ordem alfabética
	public List<No> filhos() {
		List<No> lista = new ArrayList<No>();
		File[] itens = arquivo.listFiles();

		if(itens == null) return lista;
		
		Arrays.sort(itens, new Comparator<File>() {
				@Override
				public int compare(File a, File b) {
					if (a.isDirectory() != b.isDirectory()) {
						return a.isDirectory() ? -1 : 1;
					}
					return a.getName().compareToIgnoreCase(b.getName());
				}
			});
		for(File f : itens) {
			lista.add(new No(f, nivel + 1));
		}
		return lista;
	}
}
