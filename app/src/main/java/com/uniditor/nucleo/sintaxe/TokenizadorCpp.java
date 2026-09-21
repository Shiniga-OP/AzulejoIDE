package com.uniditor.nucleo.sintaxe;

import com.uniditor.nucleo.graficos.Cor;
import com.uniditor.nucleo.sintaxe.Token;
import com.uniditor.nucleo.sintaxe.Tokenizador;
import java.util.ArrayList;
import java.util.List;

public class TokenizadorCpp implements Tokenizador {
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

    public final String[] TIPOS = {
        "void", "int", "float", "double", "long", "char", "bool",
        "short", "unsigned", "signed", "wchar_t", "auto", "size_t"
    };
    public final String[] PALAVRAS_CHAVE = {
        "if", "else", "for", "while", "do", "switch", "case", "break",
        "continue", "return", "class", "struct", "enum", "namespace",
        "using", "template", "typename", "public", "private", "protected",
        "new", "delete", "this", "const", "static", "virtual", "override",
        "true", "false", "nullptr", "sizeof", "typedef", "inline", "extern", "explicit"
    };
    public boolean emComentarioBloco = false;

    @Override
    public void reiniciar() {
        emComentarioBloco = false;
    }

    @Override
    public Token[] tokenizar(String linha) {
        List<Token> cores = new ArrayList<>();
        int n = linha.length();
        int i = 0;

        if(emComentarioBloco) {
            int fim = linha.indexOf("*/");
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
            char c = linha.charAt(i);

            if(c == ' ' || c == '\t') {
				i++;
				continue;
			}
            // pré-processador
            if(c == '#') {
                cores.add(new Token(i, i + 8, COR_PRE_PROCESSADOR, false, true));
                // <leitor> ou "leitor" dentro do #include
                String resto = linha.substring(i).trim();
                if(resto.startsWith("#include")) {
                    int lt = linha.indexOf('<', i);
                    int gt = lt != -1 ? linha.indexOf('>', lt) : -1;
                    if(lt != -1 && gt != -1) {
                        cores.add(new Token(lt, gt + 1, Cor.AZUL, false, true));
                    } else {
                        int qt = linha.indexOf('"', i);
                        if(qt != -1) {
                            int qf = linha.indexOf('"', qt + 1);
                            if(qf != -1) cores.add(new Token(qt, qf + 1, Cor.AZUL));
                        }
                    }
                }
                return cores.toArray(new Token[0]);
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
            // string aspas duplas
            if(c == '"') {
                int fim = i + 1;
                while(fim < n) {
                    if(linha.charAt(fim) == '\\') {
						fim += 2;
						continue;
					}
                    if(linha.charAt(fim) == '"')  {
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
                while (fim < n) {
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
            // identificador/palavra-chave/tipo
            // identificador/palavra-chave/tipo/função
			if(Character.isLetter(c) || c == '_') {
				int inicio = i;
				while(i < n && (Character.isLetterOrDigit(linha.charAt(i)) || linha.charAt(i) == '_')) i++;
				String palavra = linha.substring(inicio, i);

				// pula espaços para ver o que vem depois
				int proximo = i;
				while(proximo < n && (linha.charAt(proximo) == ' ' || linha.charAt(proximo) == '\t')) proximo++;

				if(eTipo(palavra)) {
					cores.add(new Token(inicio, i, COR_TIPO, true, false));
					continue;
				} else if(ePalavraChave(palavra)) {
					cores.add(new Token(inicio, i, COR_PALAVRA_CHAVE, true, false));
					continue;
				} else if(proximo < n && linha.charAt(proximo) == '(') {
					cores.add(new Token(inicio, i, Cor.AZUL)); // azul para funções
					continue;
				} else {
					cores.add(new Token(inicio, i, COR_IDENTIFICADOR));
					continue;
				}
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

    public boolean eTipo(String s) {
        for(String t : TIPOS) if(t.equals(s)) return true;
        return false;
    }

    public boolean ePalavraChave(String s) {
        for(String c : PALAVRAS_CHAVE) if(c.equals(s)) return true;
        return false;
    }
}
