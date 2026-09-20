package com.azulejo.util;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;

public class GeradorChave {
    public static void gerar(String saida, String alias, String senha, String nome) throws Exception {
        final KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        final KeyPair par = kpg.generateKeyPair();

        final byte[] algoritmo = seq(oid(new int[]{1, 2, 840, 113549, 1, 1, 11}), nulo());
        final byte[] dn = seq(def(seq(oid(new int[]{2, 5, 4, 3}), utf8(nome))));

        final byte[] tbs = seq(
            explicito(0, inteiro(BigInteger.valueOf(2))),
            inteiro(BigInteger.valueOf(System.currentTimeMillis())),
            algoritmo,
            dn,
            seq(utc("200101000000Z"), utc("490101000000Z")),
            dn,
            par.getPublic().getEncoded()
        );
        final Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(par.getPrivate());
        sig.update(tbs);
        final byte[] assinatura = sig.sign();

        final byte[] certDer = seq(tbs, algoritmo, bits(assinatura));

        final CertificateFactory cf = CertificateFactory.getInstance("X.509");
        final X509Certificate cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certDer));

        final KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry(alias, par.getPrivate(), senha.toCharArray(), new Certificate[]{cert});

        final FileOutputStream fos = new FileOutputStream(saida);
        ks.store(fos, senha.toCharArray());
        fos.close();
    }

    public static byte[] tlv(int tag, byte[] conteudo) throws IOException {
        final ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write(tag);
        int n = conteudo.length;
        if(n < 128) {
            o.write(n);
        } else if(n < 256) {
            o.write(0x81);
            o.write(n);
        } else if(n < 65536) {
            o.write(0x82);
            o.write(n >> 8);
            o.write(n);
        } else {
            o.write(0x83);
            o.write(n >> 16);
            o.write(n >> 8);
            o.write(n);
        }
        o.write(conteudo);
        return o.toByteArray();
    }

    public static byte[] seq(byte[]... partes) throws IOException {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        for(byte[] p : partes) o.write(p);
        return tlv(0x30, o.toByteArray());
    }

    public static byte[] def(byte[] conteudo) throws IOException {
        return tlv(0x31, conteudo);
    }

    public static byte[] explicito(int n, byte[] conteudo) throws IOException {
        return tlv(0xa0 | n, conteudo);
    }

    public static byte[] inteiro(BigInteger v) throws IOException {
        return tlv(0x02, v.toByteArray());
    }

    public static byte[] nulo() throws IOException {
        return tlv(0x05, new byte[0]);
    }

    public static byte[] utf8(String s) throws IOException {
        return tlv(0x0c, s.getBytes("UTF-8"));
    }

    public static byte[] utc(String s) throws IOException {
        return tlv(0x17, s.getBytes("US-ASCII"));
    }

    public static byte[] bits(byte[] dados) throws IOException {
        final byte[] c = new byte[dados.length + 1];
        System.arraycopy(dados, 0, c, 1, dados.length);
        return tlv(0x03, c);
    }

    public static byte[] oid(int[] arcos) throws IOException {
        final ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write(arcos[0] * 40 + arcos[1]);
        for(int i = 2; i < arcos.length; i++) {
            int v = arcos[i];
            final byte[] tmp = new byte[5];
            int k = 5;
            tmp[--k] = (byte) (v & 0x7f);
            v >>= 7;
            while(v > 0) {
                tmp[--k] = (byte) ((v & 0x7f) | 0x80);
                v >>= 7;
            }
            o.write(tmp, k, 5 - k);
        }
        return tlv(0x06, o.toByteArray());
    }
}
