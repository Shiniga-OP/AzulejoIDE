package com.auto;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import com.apkc.projeto.Chave;
import com.apkc.projeto.Projeto;
import com.apkc.projeto.Biblis;
import com.apkc.compilacao.Aapt2;
import com.apkc.compilacao.Processo;
import com.auto.ambiente.Ambiente;

public class Auto {
	public static void main(String[] args) {
		final String[] arg = args[0].split(":");
		
		if(arg[0].equals("")) return;
		
		final File caminho = new File(arg[0]);
		
		// tarefas:
		switch(arg[1]) {
			case "exec":
				compilar(caminho);
			break;
			case "exec-android":
				Projeto projeto = compilar(caminho);
				gerarApk(projeto);
			break;
		}
	}
	
	public static Projeto compilar(File arq) {
		final No config = ArquivoAuto.carregarModulo(arq);
		final No android = config.pos("android");

		final String modulo = arq.getAbsolutePath() + File.separator;
		final List<String> fontes = new ArrayList<String>();
		for(String f : textos(config.pos("java"))) fontes.add(modulo + f);
		final String res = modulo + android.posTexto("res");
		final String manifest = modulo + android.posTexto("androidManifest");

		final No assinar = android.pos("assinar");

		// pega uma chave que ja existe ou cria uma se não existir
		final Chave chave = new Chave(
			modulo + assinar.posTexto("chave"),
			assinar.posTexto("nome"),
			assinar.posTexto("dono"),
			assinar.posTexto("senha")
		);
		final Projeto projeto = new Projeto(modulo, fontes, res, manifest, chave);
		projeto.versaoJava = config.posTexto("versaoJava");
		projeto.versaoAlvo = config.posTexto("versaoAlvo");
		final String assets = android.posTexto("assets");
		if(assets != null) projeto.assets = modulo + assets;
		projeto.sdkMinimo = android.posTexto("sdkMinimo");
		projeto.sdkAlvo = android.posTexto("sdkAlvo");
		projeto.codigoVersao = android.posTexto("codigoVersao");
		projeto.nomeVersao = android.posTexto("nomeVersao");

		No biblis = config.pos("biblis");
		if(biblis != null) {
			for(String padrao : textos(biblis.pos("jars"))) projeto.jars.addAll(Biblis.jars(modulo + padrao));
			final String jni = biblis.posTexto("jni");
			if(jni != null) projeto.nativas = Biblis.nativas(modulo + jni);
		}
		return projeto;
	}
	
	public static void gerarApk(Projeto projeto) {
		Aapt2 aapt2 = new Aapt2(Ambiente.obterAapt2());
		Processo processo = new Processo(projeto);
		processo.compilarAPK(
			Ambiente.obterAndroidJar(),
			Ambiente.obterLambdasJar(),
			aapt2
		);
	}
	
	// aceita um texto ou uma lista de textos
	public static List<String> textos(No no) {
		final List<String> r = new ArrayList<String>();
		if(no == null) return r;
		if(no.ehLista()) {
			for(No item : no.lista) r.add(item.texto);
		} else if(no.texto != null) {
			r.add(no.texto);
		}
		return r;
	}
}
