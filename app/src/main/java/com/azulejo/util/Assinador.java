package com.azulejo.util;

import java.security.MessageDigest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.security.Signature;
import java.security.KeyStore;
import java.io.FileInputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class Assinador {
	public static void assinarV2(String entrada, String saida, String ks, String senha) throws Exception {
		final KeyStore fabrica = KeyStore.getInstance("PKCS12");
		final FileInputStream fin = new FileInputStream(ks);
		fabrica.load(fin, senha.toCharArray());
		fin.close();
		final String alias = fabrica.aliases().nextElement();
		final PrivateKey chave = (PrivateKey)fabrica.getKey(alias, senha.toCharArray());
		X509Certificate cert = (X509Certificate) fabrica.getCertificate(alias);

		final byte[] apk = lerTudo(entrada);
		
		int eocd = -1;
		for(int i = apk.length - 22; i >= 0; i--) {
			if(apk[i] == 0x50 && apk[i + 1] == 0x4b && apk[i + 2] == 0x05 && apk[i + 3] == 0x06) {
				eocd = i;
				break;
			}
		}
		if(eocd < 0) throw new IOException("EOCD não encontrado");

		final int cdPos = le32(apk, eocd + 16);
		final byte[] secao1 = copiar(apk, 0, cdPos);
		final byte[] secao3 = copiar(apk, cdPos, eocd);
		final byte[] secao4 = copiar(apk, eocd, apk.length);
		poe32(secao4, 16, cdPos);

		final byte[] digerir = digerirApk(new byte[][]{secao1, secao3, secao4});

		final byte[] dadosAssinados = seq(
			seqPares(0x0103, digerir),
			seq(cert.getEncoded()),
			new byte[0]
		);
		final Signature sig = Signature.getInstance("SHA256withRSA");
		sig.initSign(chave);
		sig.update(dadosAssinados);
		final byte[] assinatura = sig.sign();

		final byte[] assinador = seq(
			dadosAssinados,
			seqPares(0x0103, assinatura),
			cert.getPublicKey().getEncoded()
		);
		final byte[] bloco = seq(seq(assinador));
		final byte[] contentor = contentorAssinatura(bloco);

		poe32(secao4, 16, cdPos + contentor.length);

		FileOutputStream out = new FileOutputStream(saida);
		out.write(secao1);
		out.write(contentor);
		out.write(secao3);
		out.write(secao4);
		out.close();
	}

	public static byte[] digerirApk(byte[][] secoes) throws Exception {
		final List<byte[]> digerirs = new ArrayList<>();
		for(byte[] s : secoes) {
			for(int pos = 0; pos < s.length; pos += 1048576) {
				final int tam = Math.min(1048576, s.length - pos);
				final MessageDigest md = MessageDigest.getInstance("SHA-256");
				md.update((byte) 0xa5);
				md.update(u32(tam));
				md.update(s, pos, tam);
				digerirs.add(md.digest());
			}
		}
		final MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update((byte) 0x5a);
		md.update(u32(digerirs.size()));
		for(byte[] d : digerirs) md.update(d);
		return md.digest();
	}

	public static byte[] seq(byte[]... elementos) throws IOException {
		final ByteArrayOutputStream o = new ByteArrayOutputStream();
		for(byte[] e : elementos) {
			o.write(u32(e.length));
			o.write(e);
		}
		return o.toByteArray();
	}

	public static byte[] seqPares(int id, byte[] dados) throws IOException {
		final ByteArrayOutputStream o = new ByteArrayOutputStream();
		o.write(u32(8 + dados.length));
		o.write(u32(id));
		o.write(u32(dados.length));
		o.write(dados);
		return o.toByteArray();
	}

	public static byte[] contentorAssinatura(byte[] bloco) throws IOException {
		final ByteArrayOutputStream o = new ByteArrayOutputStream();
		final long total = 8 + 8 + 4 + bloco.length + 8 + 16;
		o.write(u64(total - 8));
		o.write(u64(4 + bloco.length));
		o.write(u32(0x7109871a));
		o.write(bloco);
		o.write(u64(total - 8));
		o.write("APK Sig Block 42".getBytes("US-ASCII"));
		return o.toByteArray();
	}

	public static byte[] u32(int v) {
		return new byte[]{(byte) v, (byte) (v >> 8), (byte) (v >> 16), (byte) (v >> 24)};
	}

	public static byte[] u64(long v) {
		byte[] b = new byte[8];
		for(int i = 0; i < 8; i++) b[i] = (byte) (v >> (8 * i));
		return b;
	}

	public static int le32(byte[] b, int o) {
		return (b[o] & 0xff) | ((b[o + 1] & 0xff) << 8) | ((b[o + 2] & 0xff) << 16) | ((b[o + 3] & 0xff) << 24);
	}

	public static void poe32(byte[] b, int o, int v) {
		b[o] = (byte) v;
		b[o + 1] = (byte) (v >> 8);
		b[o + 2] = (byte) (v >> 16);
		b[o + 3] = (byte) (v >> 24);
	}

	public static byte[] copiar(byte[] b, int de, int ate) {
		final byte[] r = new byte[ate - de];
		System.arraycopy(b, de, r, 0, r.length);
		return r;
	}

	public static byte[] lerTudo(String cam) throws IOException {
		final FileInputStream in = new FileInputStream(cam);
		final ByteArrayOutputStream o = new ByteArrayOutputStream();
		final byte[] buf = new byte[8192];
		int n;
		while((n = in.read(buf)) != -1) o.write(buf, 0, n);
		in.close();
		return o.toByteArray();
	}
}
