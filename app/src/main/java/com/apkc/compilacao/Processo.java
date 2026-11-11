package com.apkc.compilacao;

import com.apkc.projeto.Projeto;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import com.apkc.util.Assinador;
import com.dfoda.dexer.DFodaCtx;
import com.dfoda.dexer.Main;
import com.apkc.otimizadores.OCJ;

public class Processo {
	public static final int ALINHA_SO = 16384;
	public static final int ALINHA_PADRAO = 4;

	public Projeto prj;

	public Processo(Projeto prj) {
		this.prj = prj;
	}

	public static class Contador extends FilterOutputStream {
		public long pos = 0;

		public Contador(OutputStream out) {
			super(out);
		}

		@Override
		public void write(int b) throws IOException {
			out.write(b);
			pos++;
		}

		@Override
		public void write(byte[] b, int de, int n) throws IOException {
			out.write(b, de, n);
			pos += n;
		}
	}

	public boolean compilarAPK(final String androidJar, final String lambdasJar, final Aapt2 aapt2) {
		final String projeto = prj.raiz;
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
						apagarRecursivo(new File(pastaClasses));
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

						if(!aapt2.exec(argsLink(androidJar, manifest, gen, base, gordo))) {
							System.out.println("ERRO no aapt2 link\n");
							return;
						}
						System.out.println("aapt2 link ok\n");

						// junta o android.jar e o lambdasJar em uma unica string separada por ':'
						final String bootClasspath = androidJar + (lambdasJar != null && !lambdasJar.isEmpty() ? ":" + lambdasJar : "");

						final StringBuilder cp = new StringBuilder();
						for(File j : prj.jars) {
							if(cp.length() > 0) cp.append(":");
							cp.append(j.getAbsolutePath());
						}
						final List<String> ecjArgs = new ArrayList<String>(Arrays.asList(
																			   "-source", prj.versaoJava,
																			   "-target", prj.versaoAlvo,
																			   "-proc:none",
																			   "-d", pastaClasses,
																			   "-bootclasspath", bootClasspath
																		   ));
						if(cp.length() > 0) {
							ecjArgs.add("-classpath");
							ecjArgs.add(cp.toString());
						}
						ecjArgs.addAll(prj.java);
						ecjArgs.add(gen);

						final boolean sucesso = new org.eclipse.jdt.internal.compiler.batch.Main(
							new PrintWriter(System.out),
							new PrintWriter(System.err),
							false, null, null
						).compile(ecjArgs.toArray(new String[0]));

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

						achaClassesVazias(prj.jars);

						final DFodaCtx ctx = new DFodaCtx();
						final Main.Args dexArgs = new Main.Args(ctx);
						final List<String> entradaDex = new ArrayList<String>();
						entradaDex.add("--saida=" + saidaDex);
						entradaDex.add(pastaClasses);
						for(File j : prj.jars) entradaDex.add(j.getAbsolutePath());
						dexArgs.analisar(entradaDex.toArray(new String[0]));
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

						addZip(base, dex, prj.nativas);
						System.out.println("classes.dex e " + prj.nativas.size() + " .so adicionados ao APK\n");
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

	public void apagarRecursivo(File f) {
		final File[] filhos = f.listFiles();
		if(filhos != null) {
			for(File filho : filhos) apagarRecursivo(filho);
		}
		f.delete();
	}

	public void achaClassesVazias(List<File> jars) throws IOException {
		for(File j : jars) {
			final ZipFile z = new ZipFile(j);
			try {
				final Enumeration<? extends ZipEntry> en = z.entries();
				while(en.hasMoreElements()) {
					final ZipEntry e = en.nextElement();
					if(e.getName().endsWith(".class") && e.getSize() < 10) {
						System.out.println("Classe pequena: " + j.getName() + " -> " + e.getName() + " (" + e.getSize() + ")");
					}
				}
			} finally {
				z.close();
			}
		}
	}

	public String[] argsLink(String androidJar, String manifest, String gen, String base, String gordo) {
		final List<String> a = new ArrayList<String>(Arrays.asList(
														 "link",
														 "-o", base,
														 "-I", androidJar,
														 "--manifest", manifest,
														 "--java", gen
													 ));
		if(prj.assets != null && new File(prj.assets).isDirectory()) {
			a.add("-A");
			a.add(prj.assets);
		}
		if(prj.sdkMinimo != null) {
			a.add("--min-sdk-version");
			a.add(prj.sdkMinimo);
		}
		if(prj.sdkAlvo != null) {
			a.add("--target-sdk-version");
			a.add(prj.sdkAlvo);
		}
		if(prj.codigoVersao != null) {
			a.add("--version-code");
			a.add(prj.codigoVersao);
		}
		if(prj.nomeVersao != null) {
			a.add("--version-name");
			a.add(prj.nomeVersao);
		}
		a.add(gordo);
		return a.toArray(new String[0]);
	}

	// reescreve o zip uma vez so: classes.dex + .so (STORED, alinhados em 16 KB)
	// resources.arsc e entradas ja STORED ficam STORED alinhadas em 4
	public void addZip(String zipCam, File dex, List<File> nativas) throws IOException {
		final File orig = new File(zipCam);
		final File tmp = new File(zipCam + ".tmp");
		final ZipFile zin = new ZipFile(orig);
		final Contador cont = new Contador(new FileOutputStream(tmp));
		final ZipOutputStream zout = new ZipOutputStream(cont);

		final Enumeration<? extends ZipEntry> en = zin.entries();
		while(en.hasMoreElements()) {
			final ZipEntry e = en.nextElement();
			final byte[] dados = lerTudo(zin.getInputStream(e));
			if(e.getName().equals("resources.arsc") || e.getMethod() == ZipEntry.STORED) {
				guardar(zout, cont, e.getName(), dados, ALINHA_PADRAO);
			} else {
				zout.putNextEntry(new ZipEntry(e.getName()));
				zout.write(dados);
				zout.closeEntry();
			}
		}
		zin.close();

		zout.putNextEntry(new ZipEntry("classes.dex"));
		zout.write(lerTudo(new FileInputStream(dex)));
		zout.closeEntry();

		for(File so : nativas) {
			final String nome = "lib/" + so.getParentFile().getName() + "/" + so.getName();
			guardar(zout, cont, nome, lerTudo(new FileInputStream(so)), ALINHA_SO);
		}
		zout.close();

		orig.delete();
		tmp.renameTo(orig);
	}

	public void guardar(ZipOutputStream zout, Contador cont, String nome, byte[] dados, int alinha) throws IOException {
		final CRC32 crc = new CRC32();
		crc.update(dados, 0, dados.length);
		final ZipEntry e = new ZipEntry(nome);
		e.setMethod(ZipEntry.STORED);
		e.setSize(dados.length);
		e.setCompressedSize(dados.length);
		e.setCrc(crc.getValue());
		e.setExtra(preenchimento(cont.pos, nome, alinha));
		zout.putNextEntry(e);
		zout.write(dados);
		zout.closeEntry();
	}

	// cabecalho local = 30 bytes + nome + extra; os dados precisam cair num multiplo de "alinha"
	public byte[] preenchimento(long pos, String nome, int alinha) throws IOException {
		final long inicio = pos + 30 + nome.getBytes("UTF-8").length;
		int falta = (int)((alinha - inicio % alinha) % alinha);
		if(falta == 0) return null;
		if(falta < 4) falta += alinha; // o extra field ocupa no minimo 4 bytes
		final byte[] extra = new byte[falta];
		extra[0] = 0x35; // id 0xD935 (little-endian), o mesmo do zipalign
		extra[1] = (byte)0xD9;
		extra[2] = (byte)((falta - 4) & 0xFF);
		extra[3] = (byte)(((falta - 4) >> 8) & 0xFF);
		return extra;
	}

	public byte[] lerTudo(InputStream in) throws IOException {
		final ByteArrayOutputStream saida = new ByteArrayOutputStream();
		final byte[] buf = new byte[8192];
		int n;
		while((n = in.read(buf)) != -1) saida.write(buf, 0, n);
		in.close();
		return saida.toByteArray();
	}
}
