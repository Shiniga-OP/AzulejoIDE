package com.uniditor.sintaxe;

import com.uniditor.graficos.Cor;
import com.uniditor.sintaxe.Token;
import com.uniditor.sintaxe.Tokenizador;
import java.util.ArrayList;
import java.util.List;

public class TokenizadorJava implements Tokenizador {
    // cores do tema
    public int COR_PRE_PROCESSADOR = Cor.ROXO;
    public int COR_IDENTIFICADOR = 0xFFABB2BF;
    public int COR_STRING = Cor.VERDE;
    public int COR_NUMERO = 0xFFD19A66;
    public int COR_TIPO = Cor.ROXO;
    public int COR_OPERADOR = 0xFF56B6C2;
    public int COR_COMENTARIO = 0xFF5C6370;
    public int COR_PALAVRA_CHAVE = Cor.ROXO;
    public int COR_SEPARADOR = 0xFFABB2BF;
    public int COR_CLASSE = 0xFFE5C07B;
    public int COR_CONSTANTE = 0xFFD19A66;
    public int COR_ANOTACAO = 0xFFE06C75;
    public final List<Token> cores = new ArrayList<>();

    public final String[] TIPOS = {
        "void", "int", "float", "double", "long", "char", "boolean",
        "short", "byte"
    };
    public final String[] PALAVRAS_CHAVE = {
        "if", "else", "for", "while", "do", "switch", "case", "break",
        "continue", "return", "class", "import", "package", "enum",
        "public", "private", "protected", "abstract",
        "new", "this", "final", "volatile", "static", "override",
        "true", "false", "null", "instanceof", "inline", "implements", "extends"
    };
    public boolean emComentarioBloco = false;

    @Override
    public void reiniciar() {
        emComentarioBloco = false;
    }

    @Override
    public Token[] tokenizar(String linha) {
        cores.clear();
        final int n = linha.length();
        int i = 0;

        if(emComentarioBloco) {
            final int fim = linha.indexOf("*/");
            if(fim == -1) {
                cores.add(new Token(0, n, COR_COMENTARIO, false, true));
                return cores.toArray(new Token[0]);
            } else {
                cores.add(new Token(0, fim + 2, COR_COMENTARIO, false, true));
                emComentarioBloco = false;
                i = fim + 2;
            }
        }
        while(i < n) {
            final char c = linha.charAt(i);

            if(c == ' ' || c == '\t') {
                i++;
                continue;
            }
            // comentario de linha
            if(c == '/' && i + 1 < n && linha.charAt(i + 1) == '/') {
                cores.add(new Token(i, n, COR_COMENTARIO, false, true)); // italico mas não negrito
                return cores.toArray(new Token[0]);
            }
            // comentario de bloco
            if(c == '/' && i + 1 < n && linha.charAt(i + 1) == '*') {
                int fim = linha.indexOf("*/", i + 2);
                if(fim == -1) {
                    cores.add(new Token(i, n, COR_COMENTARIO, false, true));
                    emComentarioBloco = true;
                    return cores.toArray(new Token[0]);
                } else {
                    cores.add(new Token(i, fim + 2, COR_COMENTARIO, false, true));
                    i = fim + 2;
                    continue;
                }
            }
            // anotação: @Override, @SuppressWarnings, @android.annotation.TargetApi
            if(c == '@' && i + 1 < n && Character.isLetter(linha.charAt(i + 1))) {
                final int inicio = i;
                i++;
                while(i < n && (Character.isLetterOrDigit(linha.charAt(i)) || linha.charAt(i) == '_' || linha.charAt(i) == '.')) i++;
                cores.add(new Token(inicio, i, COR_ANOTACAO, false, true));
                continue;
            }
            // string aspas duplas
            if(c == '"') {
                int fim = i + 1;
                while(fim < n) {
                    if(linha.charAt(fim) == '\\') {
                        fim += 2;
                        continue;
                    }
                    if(linha.charAt(fim) == '"') {
                        fim++;
                        break;
                    }
                    fim++;
                }
                cores.add(new Token(i, fim, COR_STRING));
                i = fim;
                continue;
            }
            // char aspas simples
            if(c == '\'') {
                int fim = i + 1;
                while(fim < n) {
                    if(linha.charAt(fim) == '\\') {
                        fim += 2;
                        continue;
                    }
                    if(linha.charAt(fim) == '\'') {
                        fim++;
                        break;
                    }
                    fim++;
                }
                cores.add(new Token(i, fim, COR_STRING));
                i = fim;
                continue;
            }
            // numero
            if(Character.isDigit(c)) {
                int inicio = i;
                if(c == '0' && i + 1 < n && linha.charAt(i + 1) == 'x') {
                    i += 2;
                    while(i < n && Character.isLetterOrDigit(linha.charAt(i))) i++;
                } else {
                    while(i < n && (Character.isDigit(linha.charAt(i)) || linha.charAt(i) == '.')) i++;
                    if(i < n && "fFlLuU".indexOf(linha.charAt(i)) >= 0) i++;
                }
                cores.add(new Token(inicio, i, COR_NUMERO));
                continue;
            }
            // identificador/palavra-chave/tipo/classe/função
            if(Character.isLetter(c) || c == '_') {
                int inicio = i;
                while(i < n && (Character.isLetterOrDigit(linha.charAt(i)) || linha.charAt(i) == '_')) i++;
                String palavra = linha.substring(inicio, i);

                // pula espaços para ver o que vem depois
                int proximo = i;
                while(proximo < n && (linha.charAt(proximo) == ' ' || linha.charAt(proximo) == '\t')) proximo++;

                if(eTipo(palavra)) {
                    cores.add(new Token(inicio, i, COR_TIPO, false, false));
                } else if(ePalavraChave(palavra)) {
                    cores.add(new Token(inicio, i, COR_PALAVRA_CHAVE, true, false));
                } else if(Character.isUpperCase(palavra.charAt(0))) {
                    cores.add(new Token(inicio, i, eConstante(palavra) ? COR_CONSTANTE : COR_CLASSE));
                } else if(proximo < n && linha.charAt(proximo) == '(') {
                    cores.add(new Token(inicio, i, Cor.AZUL)); // azul para funções
                } else {
                    cores.add(new Token(inicio, i, COR_IDENTIFICADOR));
                }
                continue;
            }
            // separadores
            if("(){}[];,".indexOf(c) >= 0) {
                cores.add(new Token(i, i + 1, COR_SEPARADOR));
                i++;
                continue;
            }
            // operadores de dois caracteres
            if("+-*/%=<>!&|^~?:.".indexOf(c) >= 0) {
                int inicio = i;
                if(i + 1 < n) {
                    char p = linha.charAt(i + 1);
                    if((c == '=' && p == '=') || (c == '!' && p == '=') ||
					   (c == '<' && p == '=') || (c == '>' && p == '=') ||
					   (c == '&' && p == '&') || (c == '|' && p == '|') ||
					   (c == '+' && p == '+') || (c == '-' && p == '-') ||
					   (c == ':' && p == ':') || (c == '-' && p == '>')) {
                        cores.add(new Token(inicio, i + 2, COR_OPERADOR));
                        i += 2;
                        continue;
                    }
                }
                cores.add(new Token(inicio, i + 1, COR_OPERADOR));
                i++;
                continue;
            }
            i++;
        }
        return cores.toArray(new Token[0]);
    }

    // duas ou mais letras, todas maiusculas(digitos e _ valem): MAX_SUGESTOES, ID
    public boolean eConstante(String s) {
        if(s.length() < 2) return false;
        for(int i = 0; i < s.length(); i++) {
            if(Character.isLowerCase(s.charAt(i))) return false;
        }
        return true;
    }

    public boolean eTipo(String s) {
        for(String t : TIPOS) if(t.equals(s)) return true;
        return false;
    }

    public boolean ePalavraChave(String s) {
        for(String c : PALAVRAS_CHAVE) {
            if(c.equals(s)) return true;
        }
        return false;
    }
}
