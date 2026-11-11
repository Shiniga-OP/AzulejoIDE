package com.apkc.projeto;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Biblis {
	// aceita "pasta/*.jar", "pasta/lib*.jar" ou um arquivo exato
	public static List<File> jars(String padrao) {
		final List<File> r = new ArrayList<File>();
		final int i = padrao.lastIndexOf('/');
		final File pasta = new File(i < 0 ? "." : padrao.substring(0, i));
		final String nome = padrao.substring(i + 1);
		final int estrela = nome.indexOf('*');

		if(estrela < 0) {
			final File f = new File(pasta, nome);
			if(f.isFile()) r.add(f);
			return r;
		}
		final String ini = nome.substring(0, estrela);
		final String fim = nome.substring(estrela + 1);
		final File[] arqs = pasta.listFiles();
		if(arqs == null) return r;
		Arrays.sort(arqs);
		for(File f : arqs) {
			final String n = f.getName();
			if(f.isFile() && n.length() >= ini.length() + fim.length() && n.startsWith(ini) && n.endsWith(fim)) r.add(f);
		}
		return r;
	}

	// pasta/<abi>/*.so
	public static List<File> nativas(String caminho) {
		final List<File> r = new ArrayList<File>();
		final File[] abis = new File(caminho).listFiles();
		if(abis == null) return r;
		Arrays.sort(abis);
		for(File abi : abis) {
			if(!abi.isDirectory()) continue;
			final File[] sos = abi.listFiles();
			if(sos == null) continue;
			Arrays.sort(sos);
			for(File so : sos) {
				if(so.isFile() && so.getName().endsWith(".so")) r.add(so);
			}
		}
		return r;
	}
}
