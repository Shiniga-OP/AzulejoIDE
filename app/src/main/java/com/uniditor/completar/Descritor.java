package com.uniditor.completar;

import java.util.ArrayList;
import java.util.List;

// funções sobre descritores da JVM: "(ILjava/lang/String;)V", "Landroid/view/View;", "[I"
public class Descritor {
    // lê o próximo tipo a partir de pos[0] e avança pos[0]
    public static String proximoTipo(String d, int[] pos) {
        final int ini = pos[0];
        int i = ini;
        while(d.charAt(i) == '[') i++;
        if(d.charAt(i) == 'L') i = d.indexOf(';', i);
        pos[0] = i + 1;
        return d.substring(ini, i + 1);
    }

    public static List<String> parametros(String d) {
        final List<String> r = new ArrayList<String>();
        final int[] pos = {1};
        while(d.charAt(pos[0]) != ')') r.add(proximoTipo(d, pos));
        return r;
    }

    public static int contarParametros(String d) {
        int n = 0;
        final int[] pos = {1};
        while(d.charAt(pos[0]) != ')') {
            proximoTipo(d, pos);
            n++;
        }
        return n;
    }

    public static String retorno(String d) {
        return d.substring(d.indexOf(')') + 1);
    }

    // "Ljava/util/List;" -> "java/util/List"; primitivo, void e array -> null
    public static String internoDe(String t) {
        if(t.length() > 2 && t.charAt(0) == 'L') return t.substring(1, t.length() - 1);
        return null;
    }

    // "java/util/Map$Entry" -> "Entry"
    public static String simples(String interno) {
        final int corte = Math.max(interno.lastIndexOf('/'), interno.lastIndexOf('$'));
        return interno.substring(corte + 1);
    }

    // "java/util/Map$Entry" -> "java.util.Map.Entry"
    public static String nomeFonte(String interno) {
        return interno.replace('/', '.').replace('$', '.');
    }

    // nome legível de um tipo: "[I" -> "int[]", "Ljava/lang/String;" -> "String"
    public static String nomeTipo(String t) {
        int dims = 0;
        while(t.charAt(dims) == '[') dims++;
        String base;
        switch(t.charAt(dims)) {
            case 'B': base = "byte"; break;
            case 'C': base = "char"; break;
            case 'D': base = "double"; break;
            case 'F': base = "float"; break;
            case 'I': base = "int"; break;
            case 'J': base = "long"; break;
            case 'S': base = "short"; break;
            case 'Z': base = "boolean"; break;
            case 'V': base = "void"; break;
            default: base = simples(t.substring(dims + 1, t.length() - 1));
        }
        final StringBuilder sb = new StringBuilder(base);
        for(int i = 0; i < dims; i++) sb.append("[]");
        return sb.toString();
    }
}
