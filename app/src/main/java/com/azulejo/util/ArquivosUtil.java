package com.azulejo.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import com.azulejo.InicioActivity;
import java.util.zip.ZipOutputStream;
import java.io.DataOutputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import com.azulejo.gerenciador.GerenciadorArquivos;

public class ArquivosUtil {
	public static boolean existe(String c) {
		return (new File(c)).exists();
	}

	// lê o arquivo inteiro como texto UTF-8, retorna null se falhar
	public static String ler(File arquivo) {
		InputStream entrada = null;

		try {
			entrada = new FileInputStream(arquivo);
			ByteArrayOutputStream saida = new ByteArrayOutputStream();
			byte[] bloco = new byte[8192];
			int lidos;

			while ((lidos = entrada.read(bloco)) != -1) {
				saida.write(bloco, 0, lidos);
			}
			return saida.toString("UTF-8");
		} catch(IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			if(entrada != null) {
				try {
					entrada.close();
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// grava o texto em UTF-8 por cima do arquivo, retorna false se falhar
	public static boolean salvar(File arquivo, String texto) {
		OutputStream saida = null;

		try {
			saida = new FileOutputStream(arquivo);
			saida.write(texto.getBytes("UTF-8"));
			saida.flush();
			return true;
		} catch(IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			if(saida != null) {
				try {
					saida.close();
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// cria um arquivo vazio, retorna false se ja existir ou falhar
	public static boolean criarArquivo(File arquivo) {
		try {
			return arquivo.createNewFile();
		} catch(IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	// cria a pasta(e as que faltarem no caminho), retorna false se ja existir ou falhar
	public static boolean criarPasta(File pasta) {
		return pasta.mkdirs();
	}

	// exclui o arquivo, ou a pasta com tudo que tem dentro
	public static boolean excluir(File alvo) {
		if(alvo.isDirectory()) {
			final File[] filhos = alvo.listFiles();

			if(filhos != null) {
				for(File filho : filhos) {
					if(!excluir(filho)) return false;
				}
			}
		}
		return alvo.delete();
	}

	public static void salvarPreferencias() {
		File destino = new File(InicioActivity.raiz+"/usuario/preferencias.bin");
		destino.getParentFile().mkdirs();
		try {
			DataOutputStream dos = new DataOutputStream(
				new FileOutputStream(destino)
			);
			BinConjunto bin = new BinConjunto();
			bin.defString("projeto:atual", GerenciadorArquivos.atual.getAbsolutePath());
			bin.defString("projeto:modulo", InicioActivity.moduloAtual.getAbsolutePath());
			bin.salvar(dos);
			dos.flush();
			dos.close();
		} catch(IOException e) {
			System.out.println("[ERRO]: ao salvar preferencias: "+e.getMessage());
		}
	}

	// nao faz nada se o arquivo ainda nao existir(primeira execucao)
	public static void carregarPreferencias() {
		File destino = new File(InicioActivity.raiz+"/usuario/preferencias.bin");
		if(!destino.isFile()) return;
		try {
			DataInputStream dis = new DataInputStream(
				new FileInputStream(destino)
			);
			BinConjunto bin = new BinConjunto();
			bin.carregar(dis);
			GerenciadorArquivos.atual = new File(bin.lerString("projeto:atual"));
			InicioActivity.moduloAtual = new File(bin.lerString("projeto:modulo"));
			dis.close();
		} catch(IOException e) {
			System.out.println("[ERRO]: ao carregar preferencias: "+e.getMessage());
		}
	}
}
