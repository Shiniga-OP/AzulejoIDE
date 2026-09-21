package com.apkc.compilacao;

import com.apkc.projeto.Projeto;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.File;
import java.io.PrintWriter;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.io.InputStream;
import java.util.Enumeration;
import java.io.FileInputStream;
import com.apkc.util.Assinador;
import com.dfoda.dexer.DFodaCtx;
import com.dfoda.dexer.Main;
import com.apkc.otimizadores.OCJ;

public class Processo {
	public Projeto prj;
	
	public Processo(Projeto prj) {
		this.prj = prj;
	}
	
	public boolean compilarAPK(final String androidJar,final Aapt2 aapt2) {
		final String projeto = prj.raiz;
		final String src = prj.java;
		final String pastaClasses = prj.raiz + "tmp/classes/";
		final String saidaDex = prj.raiz + "tmp/classes.dex";
		final String res = prj.res;
		final String manifest = prj.androidManifest;
		final String gordo = prj.raiz + "tmp/recursos.zip";
		final String base = prj.raiz + "tmp/base.apk";
		final String gen = prj.raiz + "gen";
		
		System.out.println("Iniciando...\n");

		final ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.execute(new Runnable() {
				public void run() {
					try {
						new File(pastaClasses).mkdirs();
						new File(gen).mkdirs();
						new File(gordo).delete();
						new File(base).delete();
						new File(saidaDex).delete();

						if(!aapt2.exec("version")) {
							System.out.println("ERRO: aapt2 não executou\n");
							return;
						}
						if(!aapt2.exec("compile", "--dir", res, "-o", gordo)) {
							System.out.println("ERRO no aapt2 compile\n");
							return;
						}
						System.out.println("aapt2 compile ok\n");

						if(!aapt2.exec("link", "-o", base, "-I", androidJar, "--manifest", manifest, "--java", gen, gordo)) {
							System.out.println("ERRO no aapt2 link\n");
							return;
						}
						System.out.println("aapt2 link ok\n");

						final String[] ecjArgs = {
							"-source", "1.7", "-target", "1.7",
							"-proc:none", "-d", pastaClasses,
							"-bootclasspath", androidJar, src
						};
						final boolean sucesso = new org.eclipse.jdt.internal.compiler.batch.Main(
							new PrintWriter(System.out),
							new PrintWriter(System.err),
							false, null, null
						).compile(ecjArgs);

						if(!sucesso) {
							System.out.println("ERRO na compilação\n");
							return;
						}
						System.out.println("Java compilado\n");
						
						final boolean sucesso2 = OCJ.rodar(pastaClasses);
						
						if(!sucesso2) {
							System.out.println("ERRO na otimização\n");
							return;
						}
						System.out.println("Classes otimizadas\n");
						final DFodaCtx ctx = new DFodaCtx();
						final Main.Args dexArgs = new Main.Args(ctx);
						dexArgs.analisar(new String[]{"--saida=" + saidaDex, pastaClasses});
						if(Main.run(dexArgs) != 0) {
							System.out.println("ERRO no dexer\n");
							return;
						}
						final File dex = new File(saidaDex);
						if(!dex.exists() || dex.length() == 0) {
							System.out.println("ERRO: DEX não gerado\n");
							return;
						}
						System.out.println("DEX gerado: " + dex.length() + " bytes\n");

						addZip(base, "classes.dex", dex);
						System.out.println("classes.dex adicionado ao APK\n");
						System.out.println("SUCESSO! " + base + "\n");
						Assinador.assinarV2(base, prj.raiz + "app.apk", prj.chave.caminho, prj.chave.senha);
						System.out.println("SUCESSO! " + projeto + "app.apk\n");
					} catch(Throwable e) {
						System.out.println("ERRO GRAVE:\n" + e + "\n");
					}
				}
			});
			executor.shutdown();
			return false;
	}
	
	public void addZip(String zipCam, String nome, File arq) throws IOException {
		final File orig = new File(zipCam);
		final File tmp = new File(zipCam + ".tmp");

		final ZipFile zin = new ZipFile(orig);
		final ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(tmp));
		final byte[] buf = new byte[8192];
		int n;

		final Enumeration<? extends ZipEntry> en = zin.entries();
		while(en.hasMoreElements()) {
			final ZipEntry e = en.nextElement();
			zout.putNextEntry(new ZipEntry(e.getName()));
			final InputStream in = zin.getInputStream(e);
			while((n = in.read(buf)) != -1) zout.write(buf, 0, n);
			in.close();
			zout.closeEntry();
		}
		zin.close();

		zout.putNextEntry(new ZipEntry(nome));
		final FileInputStream fin = new FileInputStream(arq);
		while((n = fin.read(buf)) != -1) zout.write(buf, 0, n);
		fin.close();
		zout.closeEntry();
		zout.close();

		orig.delete();
		tmp.renameTo(orig);
	}
}
