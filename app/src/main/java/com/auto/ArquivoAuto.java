package com.auto;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
/*
 * ponto de entrada: le o projeto.auto na raiz e, a partir dele,
 * o modulo.auto de cada modulo declarado.
 *
 * modulos fica indexado por caminho relativo, sem
 * classe propria pra isso: quem usa pega o No e navega com pos()/posTexto().
 */
public class ArquivoAuto {
	public File raiz;
	public No projeto;
	public Map<String, No> modulos;

	public static ArquivoAuto carregar(File pastaRaiz) {
		final ArquivoAuto a = new ArquivoAuto();
		a.raiz = pastaRaiz;

		final File arquivoProjeto = new File(pastaRaiz, "projeto.auto");
		a.projeto = AnalisadorAuto.analisar(lerArquivo(arquivoProjeto));

		a.modulos = new LinkedHashMap<String, No>();
		final No listaModulos = a.projeto.pos("modulos");

		if(listaModulos != null && listaModulos.ehLista()) {
			for(No item : listaModulos.lista) {
				final String caminhoRelativo = item.texto;
				final File arquivoModulo = new File(new File(pastaRaiz, caminhoRelativo), "modulo.auto");
				a.modulos.put(caminhoRelativo, AnalisadorAuto.analisar(lerArquivo(arquivoModulo)));
			}
		}
		return a;
	}

	// le so o modulo.auto de uma pasta, sem passar por projeto.auto (uso da IDE: abrir um modulo direto)
	public static No carregarModulo(File pastaModulo) {
		final File arquivoModulo = new File(pastaModulo, "modulo.auto");
		return AnalisadorAuto.analisar(lerArquivo(arquivoModulo));
	}

	// existe modulo.auto nessa pasta?
	public static boolean temModulo(File pasta) {
		return new File(pasta, "modulo.auto").isFile();
	}

	private static String lerArquivo(File arquivo) {
		try {
			final InputStream in = new FileInputStream(arquivo);
			try {
				final byte[] bytes = new byte[(int)arquivo.length()];
				int lidos = 0;
				while(lidos < bytes.length) {
					final int n = in.read(bytes, lidos, bytes.length - lidos);
					if(n < 0) break;
					lidos += n;
				}
				return new String(bytes, "UTF-8");
			} finally {
				in.close();
			}
		} catch(IOException e) {
			throw new AutoErro("Não consegui ler " + arquivo.getAbsolutePath() + ": " + e.getMessage(), 0);
		}
	}
}
