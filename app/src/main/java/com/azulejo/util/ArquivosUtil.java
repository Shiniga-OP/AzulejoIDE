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

	// salva só o que estiver definido (atual e moduloAtual podem ser null)
	// grava num .tmp e renomeia, assim uma falha no meio não apaga as preferências antigas
	public static void salvarPreferencias() {
		if(InicioActivity.raiz == null) return;

		File destino = new File(InicioActivity.raiz + "/usuario/preferencias.bin");
		File temporario = new File(InicioActivity.raiz + "/usuario/preferencias.bin.tmp");
		File pai = destino.getParentFile();
		if(pai != null) pai.mkdirs();

		BinConjunto bin = new BinConjunto();
		if(GerenciadorArquivos.atual != null)
			bin.defString("projeto:atual", GerenciadorArquivos.atual.getAbsolutePath());
		if(InicioActivity.moduloAtual != null)
			bin.defString("projeto:modulo", InicioActivity.moduloAtual.getAbsolutePath());

		DataOutputStream dos = null;
		boolean ok = false;
		try {
			dos = new DataOutputStream(new FileOutputStream(temporario));
			bin.salvar(dos);
			dos.flush();
			ok = true;
		} catch(IOException e) {
			System.out.println("[ERRO]: ao salvar preferencias: " + e.getMessage());
		} finally {
			if(dos != null) {
				try {
					dos.close();
				} catch(IOException e) {
					ok = false;
				}
			}
		}

		if(ok) {
			if(destino.exists()) destino.delete();
			if(!temporario.renameTo(destino)) {
				System.out.println("[ERRO]: ao salvar preferencias: nao foi possivel renomear o arquivo temporario");
			}
		} else {
			temporario.delete();
		}
	}

	// nao faz nada se o arquivo ainda nao existir(primeira execucao)
	public static void carregarPreferencias() {
		if(InicioActivity.raiz == null) return;

		File destino = new File(InicioActivity.raiz + "/usuario/preferencias.bin");
		if(!destino.isFile()) return;

		DataInputStream dis = null;
		try {
			dis = new DataInputStream(new FileInputStream(destino));
			BinConjunto bin = new BinConjunto();
			bin.carregar(dis);

			// cada chave pode não ter sido salva
			String atual = bin.lerString("projeto:atual");
			String modulo = bin.lerString("projeto:modulo");
			if(atual != null) GerenciadorArquivos.atual = new File(atual);
			if(modulo != null) InicioActivity.moduloAtual = new File(modulo);
		} catch(IOException e) {
			System.out.println("[ERRO]: ao carregar preferencias: " + e.getMessage());
		} finally {
			if(dis != null) {
				try {
					dis.close();
				} catch(IOException e) {
					// ignora
				}
			}
		}
	}
}
