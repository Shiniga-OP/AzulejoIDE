package com.uniditor.nucleo.editores;

import com.uniditor.nucleo.entradas.CursorSimples;
import com.uniditor.nucleo.entradas.EntradaTextoSimples;
import com.uniditor.nucleo.entradas.Selecao;
import com.uniditor.nucleo.Buffer;
import com.uniditor.nucleo.BufferSimples;
import com.uniditor.nucleo.graficos.Cor;
import com.uniditor.nucleo.graficos.Renderizador;
import com.uniditor.nucleo.Editor;
import com.uniditor.nucleo.entradas.EntradaTexto;
import java.util.Timer;
import java.util.TimerTask;
import com.uniditor.nucleo.sintaxe.Tokenizador;
import com.uniditor.nucleo.sintaxe.Token;

public class VisaoEditor extends Editor {
    public float ESPACO_ESQ = 12f;

    public boolean cursorVisivel = true;
    public Timer relogio;
    public TimerTask piscaCursor;

    public int COR_FUNDO = 0xFF1E1E1E;
    public int COR_TEXTO = 0xFFE0E0E0;
    public int COR_CURSOR = 0xFFFFFFFF;
    public int COR_NUMERO_LINHA = 0xFF606060;
    public int COR_SARJETA_FUNDO = 0xFF252525;
    public int COR_SELECAO = 0x664FC3FF;

    public Tokenizador tokenizador; // null = sem coloração nem complete

    public VisaoEditor(Renderizador render, Tokenizador tokenizador) {
        this("", render, tokenizador);
    }

    public VisaoEditor(String textoInicial, Renderizador render, Tokenizador tokenizador) {
        buffer = new BufferSimples(textoInicial);
        cursor = new CursorSimples();
        entrada = new EntradaTextoSimples(buffer, cursor);
        this.render = render;
		this.tokenizador = tokenizador;

        if(relogio == null) relogio = new Timer();

        piscaCursor = new TimerTask() {
            @Override
            public void run() {
                cursorVisivel = !cursorVisivel;
            }
        };
        relogio.schedule(piscaCursor, 500, 500);
        rolamentoY = 0f;
    }

    @Override
    public void aoTocar(float x, float y) {
        abrirTeclado();
        moverCursor(x, y);
    }

    @Override
    public void abrirTeclado() {
        entrada.teclado.abrirTeclado();
    }

    @Override
    public int[] posToque(float tX, float tY) {
        float altLinha = render.alturaLinha();
        float sarjetaLarg = larguraSarjeta();

        float yRelativo = tY - ESPACO_TOPO + rolamentoY;
        int linha = (int)(yRelativo / altLinha);
        linha = Math.max(0, Math.min(linha, buffer.totalLinhas() - 1));

        float xRelativo = tX - sarjetaLarg - ESPACO_ESQ;
        String conteudo = buffer.linha(linha);
        int coluna = 0;
        float acumulado = 0f;
        for(int i = 0; i < conteudo.length(); i++) {
            float largCarctere = render.larguraCaractere(conteudo.charAt(i));
            if(acumulado + largCarctere / 2f > xRelativo) break;
            acumulado += largCarctere;
            coluna = i + 1;
        }
        return new int[]{linha, coluna};
    }

    public void moverCursor(float tX, float tY) {
        int[] pos = posToque(tX, tY);
        cursor.def(pos[0], pos[1]);
    }

    public float larguraSarjeta() {
        int digitos = String.valueOf(buffer.totalLinhas()).length();
        return render.larguraTexto("0") * (digitos + 1) + ESPACO_ESQ;
    }

    @Override
    public void att() {
        if(render.pause) return;
        render.iniciarQuadro();
        render.limpar(COR_FUNDO);

        float altLinha = render.alturaLinha();
        float sarjetaLarg = larguraSarjeta();
        float baseline = render.ascente();

        render.addRecorte(sarjetaLarg, 0, render.largura - sarjetaLarg, render.altura);
        render.defPos(0, -rolamentoY);

        int primeiraLinha = Math.max(0, (int)(rolamentoY / altLinha));
        int ultimaLinha = Math.min(buffer.totalLinhas() - 1,
        (int)((rolamentoY + render.altura) / altLinha) + 1);

        // === destaque de seleção ===
        Selecao sel = entrada.selecao();
        if(sel.ativa) {
            int[] ini = sel.inicio();
            int[] fim = sel.fim();
            int lIni = Math.max(ini[0], primeiraLinha);
            int lFim = Math.min(fim[0], ultimaLinha);

            for(int i = lIni; i <= lFim; i++) {
                String linhaStr = buffer.linha(i);
                float y = ESPACO_TOPO + i * altLinha;
                float xInicio = sarjetaLarg + ESPACO_ESQ;
                float xFim = sarjetaLarg + ESPACO_ESQ + render.larguraTexto(linhaStr);

                if(i == ini[0])
                    xInicio += render.larguraTexto(linhaStr.substring(0, Math.min(ini[1], linhaStr.length())));
                if(i == fim[0])
                    xFim = sarjetaLarg + ESPACO_ESQ
						+ render.larguraTexto(linhaStr.substring(0, Math.min(fim[1], linhaStr.length())));
                if(xFim > xInicio)
                    render.renderRetangulo(xInicio, y, xFim - xInicio, altLinha, COR_SELECAO);
            }
        }
        // === texto ===
        if(tokenizador != null) tokenizador.reiniciar();
        for(int i = primeiraLinha; i <= ultimaLinha; i++) {
            float y = ESPACO_TOPO + i * altLinha + baseline;
            String linhaStr = buffer.linha(i);
            if(tokenizador != null) {
                Token[] cores = tokenizador.tokenizar(linhaStr);
                render.renderTextoCor(linhaStr, cores, sarjetaLarg + ESPACO_ESQ, y);
            } else {
                render.defCorTexto(COR_TEXTO);
                render.renderTexto(linhaStr, sarjetaLarg + ESPACO_ESQ, y);
            }
        }
        // === cursor(oculto durante seleção ativa) ===
        if(cursorVisivel && !sel.ativa) {
            float xCursor = sarjetaLarg + ESPACO_ESQ
                + render.larguraTexto(buffer.linha(cursor.linha()).substring(0, cursor.coluna()));
            float yCursor = ESPACO_TOPO + cursor.linha() * altLinha;
            render.renderRetangulo(xCursor, yCursor, 2f, altLinha, COR_CURSOR);
        }
        render.subRecorte();

        // === sarjeta ===
        render.renderRetangulo(0, 0, sarjetaLarg, render.altura, COR_SARJETA_FUNDO);
        render.defCorTexto(COR_NUMERO_LINHA);
        for(int i = primeiraLinha; i <= ultimaLinha; i++) {
            float y = ESPACO_TOPO + i * altLinha + baseline - rolamentoY;
            String num = String.valueOf(i + 1);
            float xNum = sarjetaLarg - render.larguraTexto(num) - ESPACO_ESQ;
            render.renderTexto(num, xNum, y);
        }
        render.fimQuadro();
    }

    public void defTexto(String texto) {
        buffer.defTexto(texto);
        cursor.def(0, 0);
        rolamentoY = 0f;
    }
	
	@Override
	public void liberar() {
		render.liberar();
	}
}

