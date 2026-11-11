package com.apkc.projeto;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Projeto {
	public List<String> java;
	public Chave chave;
	public String raiz;
	public String androidManifest;
	public String res;
	public String versaoJava = "1.7";
	public String versaoAlvo = "1.7";
	public String assets;
	public String sdkMinimo;
	public String sdkAlvo;
	public String codigoVersao;
	public String nomeVersao;
	public List<File> jars = new ArrayList<File>();
	public List<File> nativas = new ArrayList<File>();

	public Projeto(String raiz, List<String> java, String res, String androidManifest, Chave chave) {
		this.raiz = raiz;
		this.java = java;
		this.res = res;
		this.androidManifest = androidManifest;
		this.chave = chave;
	}
}
