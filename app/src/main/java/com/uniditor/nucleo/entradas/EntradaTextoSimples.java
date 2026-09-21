package com.uniditor.nucleo.entradas;

import com.uniditor.nucleo.Buffer;

public class EntradaTextoSimples implements EntradaTexto {
    public final Buffer buffer;
    public final Cursor cursor;
    public Teclado teclado;

    public final Selecao selecao = new Selecao();

    public EntradaTextoSimples(Buffer buffer, Cursor cursor) {
        this.buffer = buffer;
        this.cursor = cursor;
    }

    @Override
    public void add(String texto) {
        buffer.add(cursor.linha(), cursor.coluna(), texto);

        if(texto.contains("\n")) {
            String[] partes = texto.split("\n", -1);
            int novaLinha = cursor.linha() + partes.length - 1;
            int novaColuna = partes[partes.length - 1].length();
            cursor.def(novaLinha, novaColuna);
        } else {
            cursor.def(cursor.linha(), cursor.coluna() + texto.length());
        }
    }

    @Override
    public void rmAntes() {
        if(selecao.ativa) {
			rmSelecao();
			return;
		}
        int linha = cursor.linha();
        int coluna = cursor.coluna();

        if(coluna > 0) {
            buffer.rm(linha, coluna - 1, 1);
            cursor.def(linha, coluna - 1);
        } else if(linha > 0) {
            int colunaAnterior = buffer.linha(linha - 1).length();
            buffer.rm(linha - 1, colunaAnterior, 1);
            cursor.def(linha - 1, colunaAnterior);
        }
    }

    @Override
    public void rmDepois() {
        if(selecao.ativa) {
			rmSelecao();
			return;
		}
        int linha = cursor.linha();
        int coluna = cursor.coluna();
        String linhaAtual = buffer.linha(linha);

        if(coluna < linhaAtual.length()) {
            buffer.rm(linha, coluna, 1);
        } else if(linha < buffer.totalLinhas() - 1) {
            buffer.rm(linha, coluna, 1);
        }
        cursor.clampar(buffer);
    }

    // === seleção ===

    @Override
    public Selecao selecao() { return selecao; }

    @Override
    public void iniciarSelecao(int linha, int coluna) {
        selecao.iniciar(linha, coluna);
    }

    @Override
    public void attSelecao(int linha, int coluna) {
        selecao.att(linha, coluna);
    }

    @Override
    public void limparSelecao() {
        selecao.limpar();
    }

    @Override
    public void selecionarTudo(Buffer buffer) {
        int ultimaLinha = buffer.totalLinhas() - 1;
        int ultimaColuna = buffer.linha(ultimaLinha).length();
        selecao.iniciar(0, 0);
        selecao.att(ultimaLinha, ultimaColuna);
    }

    @Override
    public String copiar() {
        return selecao.extrair(buffer);
    }

    @Override
    public void rmSelecao() {
        if(!selecao.ativa) return;
        int[] ini = selecao.inicio();
        int[] fim = selecao.fim();

        int maxLinha = buffer.totalLinhas() - 1;
        ini[0] = Math.min(ini[0], maxLinha);
        fim[0] = Math.min(fim[0], maxLinha);
        ini[1] = Math.min(ini[1], buffer.linha(ini[0]).length());
        fim[1] = Math.min(fim[1], buffer.linha(fim[0]).length());

        String prefixo = buffer.linha(ini[0]).substring(0, ini[1]);
        String sufixo = buffer.linha(fim[0]).substring(fim[1]);

        for(int i = fim[0]; i > ini[0]; i--) {
            buffer.rm(i - 1, buffer.linha(i - 1).length(), 1);
        }
        String linhaAtual = buffer.linha(ini[0]);
        buffer.rm(ini[0], 0, linhaAtual.length());
        buffer.add(ini[0], 0, prefixo + sufixo);

        selecao.limpar();
        cursor.def(ini[0], ini[1]);
        cursor.clampar(buffer);
    }

	@Override
	public void aoDigitar(String texto) {
		if(texto.contains("\n")) {
			String linhaAtual = buffer.linha(cursor.linha());
			// pega apenas o texto antes do cursor para não copiar indentação acidental
			String antesCursor = linhaAtual.substring(0, cursor.coluna());
			StringBuilder indentar = new StringBuilder();

			for(int i = 0; i < antesCursor.length(); i++) {
				char c = antesCursor.charAt(i);
				if(c == ' ') indentar.append(c);
				else if(c == '\t') indentar.append(c);
				else break;
			}
			// se estiver abrindo um bloco, aumenta a indentação
			if(antesCursor.trim().endsWith("{")) {
				String indentacaoAtual = indentar.toString();
				String novaIndentacao = indentacaoAtual + "\t";
				if(proxCaractere().equals("}")) {
					// caso especial: enter no meio de {}
					add("\n" + novaIndentacao + "\n" + indentacaoAtual);
					cursor.mover(-1, 0, buffer);
					cursor.def(cursor.linha(), novaIndentacao.length());
					return;
				} else {
					add("\n" + novaIndentacao);
					return;
				}
			}
			add("\n" + indentar.toString());
			return;
		}
		if(texto.equals("{")) {
			add("{}");
			cursor.mover(0, -1, buffer);
			return;
		}
		if(texto.equals("}")) {
			if(proxCaractere().equals("}")) {
				cursor.mover(0, 1, buffer);
			} else {
				add("}");
			}
			return;
		}
		if(texto.equals("\"")) {
			if(proxCaractere().equals("\"")) {
				cursor.mover(0, 1, buffer);
			} else {
				add("\"\"");
				cursor.mover(0, -1, buffer);
			}
			return;
		}
		if(texto.equals("'")) {
			if(proxCaractere().equals("'")) {
				cursor.mover(0, 1, buffer);
			} else {
				add("''");
				cursor.mover(0, -1, buffer);
			}
			return;
		}
		add(texto);
	}

	public String proxCaractere() {
		String linha = buffer.linha(cursor.linha());
		int col = cursor.coluna();
		if(col < linha.length()) return String.valueOf(linha.charAt(col));
		return "";
	}
}
