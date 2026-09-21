package com.apkc.projeto;

public class Projeto {
	public String java;
	public Chave chave;
	public String raiz;
	public String androidManifest;
	public String res;
	
	public Projeto(String raiz, String java, String res, String androidManifest, Chave chave) {
		this.raiz = raiz;
		this.java = java;
		this.res = res;
		this.androidManifest = androidManifest;
		this.chave = chave;
	}
}
