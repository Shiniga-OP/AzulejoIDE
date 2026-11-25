package com.uniditor.completar;

import com.uniditor.Buffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// o que dá pra descobrir lendo o próprio arquivo: package, imports e tipo declarado de uma variável
public class Declaracoes {
    public static class Imports {
        public String pacote = ""; // "com.azulejo", vazio = pacote padrão
        public final Set<String> exatos = new HashSet<String>(); // "android.widget.EditText"
        public final List<String> curingas = new ArrayList<String>(); // com o ponto no fim: "java.util."
    }

    public static final String[] NAO_TIPOS = {
        "return", "new", "else", "throw", "case", "instanceof", "import", "package", "assert", "do", "goto"
    };

    public static Imports lerImports(Buffer buffer) {
        final Imports r = new Imports();
        final int total = buffer.totalLinhas();
        for(int i = 0; i < total; i++) {
            final String linha = buffer.linha(i);
            if(linha.startsWith("package ")) {
                r.pacote = semPontoVirgula(linha.substring(8));
                continue;
            }
            if(!linha.startsWith("import ")) continue;
            final String nome = semPontoVirgula(linha.substring(7));
            if(nome.startsWith("static ")) continue;
            if(nome.endsWith(".*")) r.curingas.add(nome.substring(0, nome.length() - 1));
            else r.exatos.add(nome);
        }
        return r;
    }

    public static String semPontoVirgula(String s) {
        final int fim = s.indexOf(';');
        if(fim >= 0) s = s.substring(0, fim);
        return s.trim();
    }

    // o que vem depois de "extends" na declaração de classe mais próxima acima do cursor
    // "public class InicioActivity extends Activity {" -> "Activity"; sem extends -> null
    public static String superclasse(Buffer buffer, int linhaCursor) {
        final int total = buffer.totalLinhas();
        for(int i = Math.min(linhaCursor, total - 1); i >= 0; i--) {
            final String linha = buffer.linha(i);
            final String limpa = linha.trim();
            if(limpa.startsWith("//") || limpa.startsWith("*")) continue;
            final int c = acharPalavra(linha, "class", 0);
            if(c < 0) continue;
            // a declaração pode quebrar linha antes do "{"
            final StringBuilder sb = new StringBuilder(linha.substring(c));
            for(int j = i + 1; j < total && j <= i + 5 && sb.indexOf("{") < 0; j++) sb.append(' ').append(buffer.linha(j));
            final String decl = sb.toString();
            final int e = acharPalavra(decl, "extends", 0);
            if(e < 0) return null;
            int ini = e + 7;
            while(ini < decl.length() && Character.isWhitespace(decl.charAt(ini))) ini++;
            int fim = ini;
            while(fim < decl.length() && (CompletadorPalavras.eIdentificador(decl.charAt(fim)) || decl.charAt(fim) == '.')) fim++;
            return fim > ini ? decl.substring(ini, fim) : null;
        }
        return null;
    }

    // posição de "palavra" inteira em "s" (não vale ".class" nem pedaço de outro nome), ou -1
    public static int acharPalavra(String s, String palavra, int de) {
        while(true) {
            final int p = s.indexOf(palavra, de);
            if(p < 0) return -1;
            final int fim = p + palavra.length();
            final boolean antesOk = p == 0 || (!CompletadorPalavras.eIdentificador(s.charAt(p - 1)) && s.charAt(p - 1) != '.');
            final boolean depoisOk = fim >= s.length() || !CompletadorPalavras.eIdentificador(s.charAt(fim));
            if(antesOk && depoisOk) return p;
            de = fim;
        }
    }

    public static boolean eNaoTipo(String s) {
        for(String palavra : NAO_TIPOS) if(palavra.equals(s)) return true;
        return false;
    }

    // procura a declaração de "nome" da linha do cursor para cima e depois para baixo
    // devolve o tipo como está escrito, ou "[]" se for array
    public static String acharTipo(Buffer buffer, int linhaCursor, String nome) {
        final int total = buffer.totalLinhas();
        for(int i = Math.min(linhaCursor, total - 1); i >= 0; i--) {
            final String tipo = tipoNaLinha(buffer.linha(i), nome);
            if(tipo != null) return tipo;
        }
        for(int i = linhaCursor + 1; i < total; i++) {
            final String tipo = tipoNaLinha(buffer.linha(i), nome);
            if(tipo != null) return tipo;
        }
        return null;
    }

    // reconhece "Tipo nome", "Tipo<A, B> nome", "Tipo[] nome" seguidos de = ; , ) : ou fim da linha
    public static String tipoNaLinha(String linha, String nome) {
        final int n = linha.length();
        int de = 0;
        while(true) {
            final int p = linha.indexOf(nome, de);
            if(p < 0) return null;
            de = p + nome.length();
            if(p > 0 && CompletadorPalavras.eIdentificador(linha.charAt(p - 1))) continue;
            if(de < n && CompletadorPalavras.eIdentificador(linha.charAt(de))) continue;

            int depois = de;
            while(depois < n && (linha.charAt(depois) == ' ' || linha.charAt(depois) == '\t')) depois++;
            if(depois < n && "=;,):".indexOf(linha.charAt(depois)) < 0) continue;

            int antes = p - 1;
            while(antes >= 0 && (linha.charAt(antes) == ' ' || linha.charAt(antes) == '\t')) antes--;
            boolean matriz = false;
            while(antes >= 1 && linha.charAt(antes) == ']' && linha.charAt(antes - 1) == '[') {
                matriz = true;
                antes -= 2;
            }
            if(antes >= 0 && linha.charAt(antes) == '>') {
                int nivel = 0;
                while(antes >= 0) {
                    final char c = linha.charAt(antes);
                    if(c == '>') nivel++;
                    else if(c == '<') nivel--;
                    antes--;
                    if(nivel == 0) break;
                }
            }
            final int fimTipo = antes + 1;
            while(antes >= 0 && (CompletadorPalavras.eIdentificador(linha.charAt(antes)) || linha.charAt(antes) == '.')) antes--;
            final String tipo = linha.substring(antes + 1, fimTipo);
            if(tipo.isEmpty() || tipo.charAt(0) == '.' || Character.isDigit(tipo.charAt(0)) || eNaoTipo(tipo)) continue;
            return matriz ? "[]" : tipo;
        }
    }
}
