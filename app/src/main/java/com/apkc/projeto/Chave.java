package com.apkc.projeto;

import com.apkc.util.GeradorChave;
import com.apkc.util.ArquivosUtil;

public class Chave {
	public String senha;
	public String nome;
	public String dono;
	public String caminho;
	
	public Chave(String caminho, String nomeDono, String nomeChave, String senha) {
		this.nome = nomeChave;
		this.dono = nomeDono;
		this.caminho = caminho;
		this.senha = senha;
		try {
			if(!ArquivosUtil.existe(caminho)) GeradorChave.gerar(caminho, nomeChave, senha, nomeDono);
		} catch(Exception e){
			System.out.println("[ERRO]: ao criar chave: "+e.getMessage());
		}
	}
}
