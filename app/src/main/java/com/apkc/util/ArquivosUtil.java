package com.apkc.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
}
