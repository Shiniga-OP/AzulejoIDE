package com.uniditor.nucleo.util;

import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import com.uniditor.nucleo.Util;

public class Arquivos {
	public static File copiarAssets(String caminho) {
		final String cam = caminho.replaceAll("[/\\\\]", File.separator);
		File arquivo = new File(Util.assets.obterCache(), cam);

		try {
			InputStream is = Util.assets.obter(cam);
			File pai = arquivo.getParentFile();
			if(pai != null) pai.mkdirs();

			Files.copy(is, arquivo.toPath(), StandardCopyOption.REPLACE_EXISTING);
			return arquivo;
		} catch(IOException e) {
			throw new RuntimeException("Falha ao copiar asset: " + caminho, e);
		}
	}
	
	public static File copiar(String caminho, String destino) {
		final File origem = new File(caminho);
		final File alvo = new File(destino);
		try {
			final File pai = alvo.getParentFile();
			if(pai != null) pai.mkdirs();

			Files.copy(origem.toPath(), alvo.toPath(), StandardCopyOption.REPLACE_EXISTING);
			return alvo;
		} catch(IOException e) {
			throw new RuntimeException("Falha ao copiar arquivo: " + origem + " -> " + destino, e);
		}
	}
}
