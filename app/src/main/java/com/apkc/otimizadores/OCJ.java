package com.apkc.otimizadores;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.attribute.BasicFileAttributes;
import java.io.IOException;
import com.apkc.otimizadores.passes.Passe;
import com.apkc.otimizadores.passes.PasseDupCarga;
import com.apkc.otimizadores.analise.AnalisadorClasse;
import com.apkc.otimizadores.passes.PasseDupArmazena;

public class OCJ {
	private static final Passe[] PASSES = {
		new PasseDupCarga(),
		new PasseDupArmazena()
	};

	public static long otimizacoes = 0;

	public static void main(String[] args) {
		if(args.length == 0) {
			System.out.println("Uso: java OCJ <arquivo_ou_diretorio>");
			return;
		}
		rodar(args[0]);
	}

	public static boolean rodar(String caminho) {
		final Path alvo = Paths.get(caminho);
		final boolean[] falhou = {false};
		try {
			if(Files.isDirectory(alvo)) {
				System.out.println("[OCJ] Iniciando varredura no diretório: " + alvo);

				Files.walkFileTree(alvo, new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult visitFile(Path arquivo, BasicFileAttributes attrs) throws IOException {
							if(arquivo.toString().endsWith(".class")) {
								try {
									otimizacoes = 0;
									final byte[] dados = otimizar(arquivo);

									if(otimizacoes > 0) {
										Files.write(arquivo, dados);
										System.out.println("[+] Otimizado: " + arquivo.getFileName() + " (" + otimizacoes + " melhorias)");
									}
								} catch(Exception e) {
									falhou[0] = true;
									System.err.println("[!] Erro ao processar " + arquivo + ": " + e.getMessage());
								}
							}
							return FileVisitResult.CONTINUE;
						}
					});
				System.out.println("[OCJ] Varredura concluída.");
			} else {
				otimizacoes = 0;
				final byte[] dados = otimizar(alvo);

				if(otimizacoes > 0) {
					Files.write(alvo, dados);
					System.out.println("[OCJ] Sucesso! " + otimizacoes + " redundâncias eliminadas.");
				}
			}
		} catch(Exception e) {
			System.out.println("[ERRO]: ao otimizar: " + e.getMessage());
			return false;
		}
		return !falhou[0];
	}

	public static byte[] otimizar(Path caminho) throws Exception {
		byte[] dados = Files.readAllBytes(caminho);

		for(Passe p : PASSES) {
			final AnalisadorClasse classe = new AnalisadorClasse(dados);
			final long qtd = p.aplicar(dados, classe);
			if(qtd > 0) {
				System.out.println("    [" + p.nome() + "] " + qtd + " ocorrência(s)");
			}
			otimizacoes += qtd;
		}
		return dados;
	}
}
