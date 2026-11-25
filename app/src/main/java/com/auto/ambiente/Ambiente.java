package com.auto.ambiente;

import java.io.File;
import java.util.Arrays;

public class Ambiente {
    public static String obterRaizSdk() {
        // 1. tenta pegar via System.setProperty (ex: definido no código do Android)
        String raiz = System.getProperty("android.home");
        // 2. tenta pegar via Variável de Ambiente do SO (ex: ANDROID_HOME no PC)
        if(raiz == null) raiz = System.getenv("ANDROID_HOME");
        // 3. tenta outra variável comum (ANDROID_SDK_ROOT no PC)
        if(raiz == null) raiz = System.getenv("ANDROID_SDK_ROOT");

        if(raiz == null) {
            throw new IllegalStateException("ANDROID_HOME / android.home não definido");
        }
        return raiz;
    }

    public static File obterUltimoSubdiretorio(File pai) {
        final File[] filhos = pai.listFiles();
        if(filhos == null || filhos.length == 0) {
            throw new IllegalStateException("Nada encontrado em: " + pai.getAbsolutePath());
        }
        Arrays.sort(filhos);
        return filhos[filhos.length - 1];
    }

    public static String obterAndroidJar() {
        // no Android, o android.jar pode vir direto da raiz informada ou via System.getProperty
        String jarCustomizado = System.getProperty("android.jar");
        if(jarCustomizado != null) return jarCustomizado;

        // no PC, costuma estar dentro de platforms/android-XX/android.jar
        try {
            final File plataformas = new File(obterRaizSdk(), "platforms");
            if(plataformas.exists()) {
                return new File(obterUltimoSubdiretorio(plataformas), "android.jar").getPath();
            }
        } catch(Exception e) {
            // ignora se falhar e tenta retornar baseado direto na raiz (caso do Android app)
        }
        return new File(obterRaizSdk(), "android.jar").getPath();
    }

    public static String obterAapt2() {
        // 1. verifica se foi definido explicitamente via System.setProperty("aapt2", ...)(usado no Android)
        String caminho = System.getProperty("aapt2");
        if(caminho == null) {
            // 2. ou via variavel de Ambiente do SO(usado no PC se quiser sobrescrever)
            caminho = System.getenv("AAPT2_PATH");
        }
        if(caminho != null) {
            File arquivo = new File(caminho);
            if(arquivo.exists()) {
                return arquivo.getPath();
            }
        }
        // 3. padrão de PC(procura no build-tools do SDK do Android)
        try {
            final File ferramentas = new File(obterRaizSdk(), "build-tools");
            if(ferramentas.exists()) {
                final String nome = System.getProperty("os.name").toLowerCase().contains("win") ? "aapt2.exe" : "aapt2";
                return new File(obterUltimoSubdiretorio(ferramentas), nome).getPath();
            }
        } catch(Exception e) {
            // se falhar e não achar no build-tools do PC, lança exceção explicativa
        }
        throw new IllegalStateException("Aapt2 não encontrado! Defina a propriedade 'aapt2' ou configure o SDK.");
    }

    public static String obterLambdasJar() {
        String caminho = System.getProperty("lambdas.jar");
        if(caminho == null) {
            caminho = System.getenv("LAMBDAS_JAR");
        }
        return caminho; // pode ser null se não for usado
    }
}
