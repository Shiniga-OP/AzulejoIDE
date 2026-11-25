package com.uniditor.editores;

import com.uniditor.entradas.CursorSimples;
import com.uniditor.entradas.EntradaTextoSimples;
import com.uniditor.entradas.Selecao;
import com.uniditor.Buffer;
import com.uniditor.BufferSimples;
import com.uniditor.graficos.Cor;
import com.uniditor.graficos.Renderizador;
import com.uniditor.Editor;
import com.uniditor.entradas.EntradaTexto;
import java.util.Timer;
import java.util.TimerTask;
import com.uniditor.sintaxe.Tokenizador;
import com.uniditor.sintaxe.Token;
import com.uniditor.completar.Completador;
import com.uniditor.completar.Sugestao;
import java.util.List;

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

    public Completador completador; // null = sem sugestões

    public int COR_POPUP_FUNDO = 0xFF2D2D30;
    public int COR_POPUP_BORDA = 0xFF454545;
    public int COR_POPUP_TEXTO = 0xFFE0E0E0;
    public int COR_POPUP_PREFIXO = 0xFF4FC3FF;
    public int COR_POPUP_DETALHE = 0xFF808080;
    public int MIN_PREFIXO = 2;
    public static final int MAX_SUGESTOES = 8;

    public final Sugestao[] sugestoes = new Sugestao[MAX_SUGESTOES];
    public final String[] sugParte1 = new String[MAX_SUGESTOES]; // parte já digitada
    public final String[] sugParte2 = new String[MAX_SUGESTOES]; // parte que falta
    public int totalSugestoes = 0;
    public int linhaChave = -1;
    public int colChave = 0;
    public String prefixoChave = "";
    public boolean suprimir = false;
    public boolean popVisivel = false;
    public float popX, popY, popLarg, popAlt, popItemAlt;

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
        if(toqueEmSugestao(x, y)) return;
        abrirTeclado();
        moverCursor(x, y);
        // tocar no texto não deve abrir sugestões, só digitar
        suprimir = true;
        atualizarSugestoes();
        suprimir = false;
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

        float xRelativo = tX - sarjetaLarg - ESPACO_ESQ + rolamentoX;
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

    @Override
    public float larguraSarjeta() {
        int digitos = String.valueOf(buffer.totalLinhas()).length();
        return render.larguraTexto("0") * (digitos + 1) + ESPACO_ESQ;
    }

    @Override
    public void att() {
        if(render.pause) return;
        render.iniciarQuadro();
        render.limpar(COR_FUNDO);
        atualizarSugestoes();

        float altLinha = render.alturaLinha();
        float sarjetaLarg = larguraSarjeta();
        float baseline = render.ascente();

        render.addRecorte(sarjetaLarg, 0, render.largura - sarjetaLarg, render.altura);
        render.defPos(-rolamentoX, -rolamentoY);

        int primeiraLinha = Math.max(0, (int)(rolamentoY / altLinha));
        int ultimaLinha = Math.min(buffer.totalLinhas() - 1, (int)((rolamentoY + render.altura) / altLinha) + 1);

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

                if(i == ini[0]) xInicio += render.larguraTexto(linhaStr.substring(0, Math.min(ini[1], linhaStr.length())));
                if(i == fim[0]) xFim = sarjetaLarg + ESPACO_ESQ + render.larguraTexto(linhaStr.substring(0, Math.min(fim[1], linhaStr.length())));
                if(xFim > xInicio) render.renderRetangulo(xInicio, y, xFim - xInicio, altLinha, COR_SELECAO);
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
            float xCursor = sarjetaLarg + ESPACO_ESQ + render.larguraTexto(buffer.linha(cursor.linha()).substring(0, cursor.coluna()));
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
        desenharSugestoes(sarjetaLarg, altLinha, baseline);
        render.fimQuadro();
    }

    public boolean eIdentificador(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    // recalcula só quando a posição ou o prefixo mudam
    public void atualizarSugestoes() {
        if(completador == null) {
            totalSugestoes = 0;
            return;
        }
        final int l = Math.min(cursor.linha(), buffer.totalLinhas() - 1);
        final String conteudo = buffer.linha(l);
        final int c = Math.min(cursor.coluna(), conteudo.length());
        int ini = c;
        while(ini > 0 && eIdentificador(conteudo.charAt(ini - 1))) ini--;
        final String prefixo = conteudo.substring(ini, c);
        if(l == linhaChave && ini == colChave && prefixo.equals(prefixoChave)) return;

        linhaChave = l;
        colChave = ini;
        prefixoChave = prefixo;
        totalSugestoes = 0;
        if(suprimir) return;
        // depois de "." sugere membros mesmo sem digitar nada
        final boolean aposPonto = ini > 0 && conteudo.charAt(ini - 1) == '.';
        if(prefixo.length() < MIN_PREFIXO && !aposPonto) return;
        if(prefixo.length() > 0 && Character.isDigit(prefixo.charAt(0))) return;
        // cursor no meio de uma palavra: não sugere
        if(c < conteudo.length() && eIdentificador(conteudo.charAt(c))) return;

        final List<Sugestao> achadas = completador.sugerir(buffer, l, c, prefixo, MAX_SUGESTOES);
        final int n = Math.min(achadas.size(), MAX_SUGESTOES);
        for(int i = 0; i < n; i++) {
            final Sugestao s = achadas.get(i);
            sugestoes[i] = s;
            sugParte1[i] = s.texto.substring(0, prefixo.length());
            sugParte2[i] = s.texto.substring(prefixo.length());
        }
        totalSugestoes = n;
    }

    public boolean toqueEmSugestao(float tX, float tY) {
        if(!popVisivel) return false;
        if(tX < popX || tX > popX + popLarg || tY < popY || tY > popY + popAlt) return false;
        int indice = (int)((tY - popY) / popItemAlt);
        indice = Math.max(0, Math.min(indice, totalSugestoes - 1));
        aceitarSugestao(indice);
        return true;
    }

    public void aceitarSugestao(int indice) {
        final Sugestao s = sugestoes[indice];
        buffer.rm(linhaChave, colChave, prefixoChave.length());
        cursor.def(linhaChave, colChave);
        entrada.add(s.texto);
        if(s.tipo == Sugestao.METODO) {
            final String conteudo = buffer.linha(cursor.linha());
            final int c = cursor.coluna();
            // se já tem "(" logo depois não duplica
            if(c >= conteudo.length() || conteudo.charAt(c) != '(') {
                entrada.add("()");
                // com parâmetros o cursor fica dentro dos parênteses
                if(s.temParametros) cursor.def(cursor.linha(), cursor.coluna() - 1);
            }
        }
        if(s.importar != null) adicionarImport(s.importar);
        // depois de aceitar não reabre o popup com palavras maiores
        suprimir = true;
        atualizarSugestoes();
        suprimir = false;
        if(aoAceitarSugestao != null) aoAceitarSugestao.run();
    }

    // Enter/Tab com o popup aberto aceita a primeira sugestão, como o Tab do PC
    // devolve false quando não tem popup: aí a tecla segue normal
    public boolean aceitarPrimeira() {
        if(entrada.selecao().ativa) return false;
        atualizarSugestoes();
        if(totalSugestoes == 0 || !popVisivel) return false;
        aceitarSugestao(0);
        return true;
    }

    // insere "import nome;" depois do último import (ou do package) sem a tela pular
    public void adicionarImport(String nome) {
        final String linhaImport = "import " + nome + ";";
        final int total = buffer.totalLinhas();
        int ultimoImport = -1;
        int pacote = -1;
        for(int i = 0; i < total; i++) {
            final String linha = buffer.linha(i).trim();
            if(linha.equals(linhaImport)) return;
            if(linha.startsWith("import ")) ultimoImport = i;
            else if(linha.startsWith("package ")) pacote = i;
        }
        final int linhaCursor = cursor.linha();
        final int colCursor = cursor.coluna();
        int alvo;
        String texto;
        if(ultimoImport >= 0) {
            alvo = ultimoImport;
            texto = "\n" + linhaImport;
        } else if(pacote >= 0) {
            alvo = pacote;
            texto = "\n\n" + linhaImport;
        } else {
            alvo = -1;
            texto = linhaImport + "\n\n";
        }
        if(alvo >= 0) cursor.def(alvo, buffer.linha(alvo).length());
        else cursor.def(0, 0);
        entrada.add(texto);

        // linhas novas acima do cursor empurram ele (e a tela) pra baixo
        int novas = 0;
        if(alvo < linhaCursor) {
            for(int i = 0; i < texto.length(); i++) if(texto.charAt(i) == '\n') novas++;
        }
        cursor.def(linhaCursor + novas, colCursor);
        rolamentoY += novas * render.alturaLinha();
    }

    public void desenharSugestoes(float sarjetaLarg, float altLinha, float baseline) {
        popVisivel = false;
        if(totalSugestoes == 0 || linhaChave < 0 || entrada.selecao().ativa) return;
        final float yLinha = ESPACO_TOPO + linhaChave * altLinha - rolamentoY;
        if(yLinha + altLinha < 0f || yLinha > render.altura) return;

        final float folga = altLinha * 0.5f;
        popItemAlt = altLinha + folga;
        popAlt = popItemAlt * totalSugestoes;
        float maior = 0f;
        for(int i = 0; i < totalSugestoes; i++) {
            float larg = render.larguraTexto(sugestoes[i].texto);
            if(sugestoes[i].detalhe != null) larg += folga * 2f + render.larguraTexto(sugestoes[i].detalhe);
            maior = Math.max(maior, larg);
        }
        popLarg = Math.min(maior + folga * 2f, render.largura);

        final String conteudo = buffer.linha(linhaChave);
        float x = sarjetaLarg + ESPACO_ESQ + render.larguraTexto(conteudo.substring(0, Math.min(colChave, conteudo.length()))) - rolamentoX;
        if(x + popLarg > render.largura) x = render.largura - popLarg;
        if(x < 0f) x = 0f;
        // abaixo da linha do cursor; se não couber, acima
        float y = yLinha + altLinha;
        if(y + popAlt > render.altura && yLinha - popAlt >= 0f) y = yLinha - popAlt;

        render.renderRetangulo(x, y, popLarg, popAlt, COR_POPUP_FUNDO);
        // detalhe comprido demais é cortado na borda do popup
        render.addRecorte(x, y, popLarg, popAlt);
        for(int i = 0; i < totalSugestoes; i++) {
            final Sugestao s = sugestoes[i];
            final float yBase = y + i * popItemAlt + folga / 2f + baseline;
            final float xTexto = x + folga;
            final float larg1 = render.larguraTexto(sugParte1[i]);
            final float largTexto = larg1 + render.larguraTexto(sugParte2[i]);
            render.defCorTexto(COR_POPUP_PREFIXO);
            render.renderTexto(sugParte1[i], xTexto, yBase);
            render.defCorTexto(COR_POPUP_TEXTO);
            render.renderTexto(sugParte2[i], xTexto + larg1, yBase);
            if(s.obsoleto) render.renderRetangulo(xTexto, yBase - baseline * 0.35f, largTexto, 2f, COR_POPUP_TEXTO);
            if(s.detalhe != null) {
                // alinhado à direita, mas nunca por cima do nome
                final float xDetalhe = Math.max(x + popLarg - folga - render.larguraTexto(s.detalhe), xTexto + largTexto + folga * 2f);
                render.defCorTexto(COR_POPUP_DETALHE);
                render.renderTexto(s.detalhe, xDetalhe, yBase);
            }
        }
        render.subRecorte();
        render.renderRetanguloContorno(x, y, popLarg, popAlt, COR_POPUP_BORDA, 2f);

        popX = x;
        popY = y;
        popVisivel = true;
    }

    public void defTexto(String texto) {
        buffer.defTexto(texto);
        cursor.def(0, 0);
        rolamentoY = 0f;
        rolamentoX = 0f;
        totalSugestoes = 0;
        linhaChave = -1;
    }

    @Override
    public float larguraMaxLinha() {
        float max = 0f;
        for(int i = 0; i < buffer.totalLinhas(); i++) {
            final float larg = render.larguraTexto(buffer.linha(i));
            if(larg > max) max = larg;
        }
        return max + ESPACO_ESQ;
    }

    @Override
    public void liberar() {
        render.liberar();
    }
}
