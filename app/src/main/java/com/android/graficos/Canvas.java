package com.android.graficos;

import android.graphics.Paint;
import android.graphics.Typeface;

import com.uniditor.graficos.Renderizador;

import com.uniditor.sintaxe.Token;
import java.io.File;

public class Canvas implements Renderizador {
    public android.graphics.Canvas canvas;

    public final Paint pincelTexto = new Paint(Paint.ANTI_ALIAS_FLAG);
    public final Paint pincelForma = new Paint(Paint.ANTI_ALIAS_FLAG);
    public final Paint pincelFundo = new Paint();

    public int TAM_TAB = 4;

    // profundidade de recortes abertos, pra subRecorte nao restaurar a mais
    public int profundidadeRecorte = 0;

    // cache de fonte: 0 normal, 1 negrito, 2 italico, 3 negrito+italico
    public final Typeface[] estilos = new Typeface[4];

    // cache de medidas, recalculado so quando a fonte ou o tamanho mudam
    public float larguraEspaco = 0f;
    public float larguraTab = 0f;
    public float alturaCache = 0f;
    public float ascenteCache = 0f;
    public float descenteCache = 0f;

    // reaproveitados pra nao alocar dentro do desenho
    public final Paint.FontMetrics metricas = new Paint.FontMetrics();
    public final char[] unico = new char[1];

    public Canvas(int corForma, int corFundo) {
        pause = true;
        pincelForma.setColor(corForma);
        pincelFundo.setColor(corFundo);
        montarEstilos(pincelTexto.getTypeface());
        defTextoTam(25f);
    }

    public void defAPI(Object canvas) {
        if(canvas instanceof android.graphics.Canvas) {
            this.canvas = (android.graphics.Canvas)canvas;
            pause = false;
        }
    }

    public void montarEstilos(Typeface base) {
        estilos[0] = Typeface.create(base, Typeface.NORMAL);
        estilos[1] = Typeface.create(base, Typeface.BOLD);
        estilos[2] = Typeface.create(base, Typeface.ITALIC);
        estilos[3] = Typeface.create(base, Typeface.BOLD_ITALIC);
    }

    // chamado quando muda fonte, tamanho ou tab
    public void atualizarMedidas() {
        pincelTexto.getFontMetrics(metricas);
        ascenteCache = -metricas.ascent;
        descenteCache = metricas.descent;
        alturaCache = metricas.descent - metricas.ascent;
        larguraEspaco = pincelTexto.measureText(" ");
        larguraTab = larguraEspaco * TAM_TAB;
    }

    @Override
    public void defTextoTam(float tam) {
        pincelTexto.setTextSize(tam);
        atualizarMedidas();
    }

    @Override
    public void ajustar(int h, int v) {
        this.altura = v;
        this.largura = h;
    }

    @Override
    public void liberar() {
        canvas = null;
        profundidadeRecorte = 0;
    }

    @Override
    public void iniciarQuadro() {
        profundidadeRecorte = 0;
        canvas.save();
    }

    @Override
    public void fimQuadro() {
        canvas.restore();
    }

    @Override
    public void limpar(int cor) {
        pincelFundo.setColor(cor);
        canvas.drawRect(0, 0, largura, altura, pincelFundo);
    }

    @Override
    public void defFonte(File fonte) {
        Typeface tf = Typeface.createFromFile(fonte);
        pincelTexto.setTypeface(tf);
        montarEstilos(tf);
        atualizarMedidas();
    }

    @Override
    public void defCorTexto(int cor) {
        pincelTexto.setColor(cor);
    }

    // desenha o trecho de inicio ate fim (exclusivo) e devolve a largura desenhada, com tab
    public float desenharTrecho(String texto, int inicio, int fim, float x, float y) {
        if(inicio >= fim) return 0f;
        float ox = x;
        int ini = inicio;
        for(int i = inicio; i <= fim; i++) {
            if(i == fim || texto.charAt(i) == '\t') {
                if(i > ini) {
                    canvas.drawText(texto, ini, i, ox, y, pincelTexto);
                    ox += pincelTexto.measureText(texto, ini, i);
                }
                if(i < fim) ox += larguraTab;
                ini = i + 1;
            }
        }
        return ox - x;
    }

    // mede o trecho de inicio ate fim (exclusivo) sem alocar, com tab
    public float medirTrecho(String texto, int inicio, int fim) {
        if(inicio >= fim) return 0f;
        float total = 0f;
        int ini = inicio;
        for(int i = inicio; i <= fim; i++) {
            if(i == fim || texto.charAt(i) == '\t') {
                if(i > ini) total += pincelTexto.measureText(texto, ini, i);
                if(i < fim) total += larguraTab;
                ini = i + 1;
            }
        }
        return total;
    }

    @Override
    public void renderTexto(String texto, float x, float y) {
        if(texto.indexOf('\t') < 0) {
            canvas.drawText(texto, x, y, pincelTexto);
            return;
        }
        desenharTrecho(texto, 0, texto.length(), x, y);
    }

    @Override
    public void renderTextoCor(String texto, Token[] cores, float x, float y) {
        if(cores == null || cores.length == 0) {
            renderTexto(texto, x, y);
            return;
        }
        int n = texto.length();
        int corOriginal = pincelTexto.getColor();
        Typeface tfOriginal = pincelTexto.getTypeface();
        float ox = x;
        int pos = 0;

        for(int k = 0; k < cores.length; k++) {
            Token token = cores[k];
            if(token.inicio >= token.fim || token.fim > n || token.inicio < pos) continue;

            // trecho entre o token anterior e este (espacos que o tokenizador pulou)
            if(token.inicio > pos) {
                pincelTexto.setTypeface(estilos[0]);
                pincelTexto.setColor(corOriginal);
                ox += desenharTrecho(texto, pos, token.inicio, ox, y);
            }
            int estilo = (token.negrito ? 1 : 0) | (token.italico ? 2 : 0);
            pincelTexto.setTypeface(estilos[estilo]);
            pincelTexto.setColor(token.cor);
            ox += desenharTrecho(texto, token.inicio, token.fim, ox, y);
            pos = token.fim;
        }
        if(pos < n) {
            pincelTexto.setTypeface(estilos[0]);
            pincelTexto.setColor(corOriginal);
            desenharTrecho(texto, pos, n, ox, y);
        }
        pincelTexto.setColor(corOriginal);
        pincelTexto.setTypeface(tfOriginal);
    }

    @Override
    public void renderRetangulo(float x, float y, float largura, float altura, int cor) {
        pincelForma.setStyle(Paint.Style.FILL);
        pincelForma.setColor(cor);
        canvas.drawRect(x, y, x + largura, y + altura, pincelForma);
    }

    @Override
    public void renderRetanguloContorno(float x, float y, float largura, float altura, int cor, float espessura) {
        pincelForma.setStyle(Paint.Style.STROKE);
        pincelForma.setStrokeWidth(espessura);
        pincelForma.setColor(cor);
        canvas.drawRect(x, y, x + largura, y + altura, pincelForma);
    }

    @Override
    public float larguraTexto(String texto) {
        return medirTrecho(texto, 0, texto.length());
    }

    @Override
    public float larguraCaractere(char c) {
        if(c == '\t') return larguraTab;
        unico[0] = c;
        return pincelTexto.measureText(unico, 0, 1);
    }

    @Override
    public float alturaLinha() {
        return alturaCache;
    }

    @Override
    public float ascente() {
        return ascenteCache;
    }

    @Override
    public float descente() {
        return descenteCache;
    }

    @Override
    public void defTabTam(int espacos) {
        TAM_TAB = espacos;
        larguraTab = larguraEspaco * TAM_TAB;
    }

    @Override
    public void addRecorte(float x, float y, float largura, float altura) {
        canvas.save();
        profundidadeRecorte++;
        canvas.clipRect(x, y, x + largura, y + altura);
    }

    @Override
    public void subRecorte() {
        if(profundidadeRecorte > 0) {
            profundidadeRecorte--;
            canvas.restore();
        }
    }

    @Override
    public void defPos(float dx, float dy) {
        canvas.translate(dx, dy);
    }

    @Override
    public int larguraTela() {
        return largura;
    }

    @Override
    public int alturaTela() {
        return altura;
    }
}
