package com.apkc.otimizadores.passes;

import com.apkc.otimizadores.analise.AnalisadorClasse;

public interface Passe {
	String nome();
	long aplicar(byte[] dados, AnalisadorClasse classe);
}
