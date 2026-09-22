package com.android;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.os.Handler;

import com.uniditor.Editor;
import com.uniditor.entradas.Teclado;

public class EditorAndroidCanvas extends View {
    public Editor editor;
    public View ISSO;

    public static final float LIMIAR_DRAG = 8f;

    public float toqueInicioX, toqueInicioY;
    public boolean arrastando = false;
    public boolean pressionado = false;

    public ActionMode modoAcao = null;

    public static final int ID_COPIAR = 1;
    public static final int ID_RECORTAR = 2;
    public static final int ID_COLAR = 3;
    public static final int ID_SELECIONAR_TUDO = 4;

    public GestureDetector gestureDetector;

	public float veloRolamento = 0f;
	public float veloRolamentoX = 0f;
	public float ultimoY = 0f;
	public float ultimoX = 0f;
	public long ultimoTempo = 0L;
	public Handler geren = new Handler();
	public Runnable loopInercia = null;
	public Runnable loopAutoRolamento = null;
	public float autoRolamentoDelta = 0f;
	public boolean redesenhoAgendado = false;
	public final Runnable loopRedesenho = new Runnable() {
		@Override
		public void run() {
			redesenhoAgendado = false;
			invalidate();
		}
	};
	public final ActionMode.Callback padraoAcao = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode modo, Menu menu) {
            menu.add(0, ID_COPIAR, 0, "Copiar");
            menu.add(0, ID_RECORTAR, 1, "Recortar");
            menu.add(0, ID_COLAR, 2, "Colar");
            menu.add(0, ID_SELECIONAR_TUDO, 3, "Selecionar tudo");
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode modo, Menu menu) {
            // desabilita Copiar e Recortar se não ha seleção
            final boolean temSelecao = editor.entrada.selecao().ativa && !editor.entrada.copiar().isEmpty();
            menu.findItem(ID_COPIAR).setEnabled(temSelecao);
            menu.findItem(ID_RECORTAR).setEnabled(temSelecao);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode modo, MenuItem item) {
            switch(item.getItemId()) {
                case ID_COPIAR:
                    copiarSelecao();
                    modo.finish();
                    return true;
                case ID_RECORTAR:
                    recortarSelecao();
                    modo.finish();
                    return true;
                case ID_COLAR:
                    colarTexto();
                    modo.finish();
                    return true;
                case ID_SELECIONAR_TUDO:
                    editor.entrada.selecionarTudo(editor.buffer);
                    modo.invalidate();
                    invalidate();
                    return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode modo) {
            modoAcao = null;
            editor.entrada.limparSelecao();
            invalidate();
        }
    };

    public EditorAndroidCanvas(Context ctx, final Editor editor) {
        super(ctx);
        this.editor = editor;

        this.ISSO = this;
        this.editor.entrada.teclado = new Teclado() {
            @Override
            public void abrirTeclado() {
                final InputMethodManager imm = (InputMethodManager)getContext()
				.getSystemService(Context.INPUT_METHOD_SERVICE);
                if(imm != null) imm.showSoftInput(ISSO, InputMethodManager.SHOW_IMPLICIT);
            }
        };
        gestureDetector = new GestureDetector(ctx, new GestureDetector.SimpleOnGestureListener() {
				@Override
				public void onLongPress(MotionEvent e) {
					pressionado = true;
					final int[] pos = editor.posToque(e.getX(), e.getY());
					selecionarPalavra(pos[0], pos[1]);
					abrirModoAcao();
					invalidate();
				}
			});
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    public void selecionarPalavra(int linha, int coluna) {
        final String conteudo = editor.buffer.linha(linha);
        if(conteudo.isEmpty()) return;
        coluna = Math.min(coluna, conteudo.length());

        int ini = coluna;
        int fim = coluna;

        while(ini > 0 && eSeparador(conteudo.charAt(ini - 1))) ini--;
        if(ini == coluna && coluna < conteudo.length() && eSeparador(conteudo.charAt(coluna))) {
            fim = coluna + 1;
            editor.entrada.iniciarSelecao(linha, ini);
            editor.entrada.attSelecao(linha, fim);
            return;
        }
        while(ini > 0 && !eSeparador(conteudo.charAt(ini - 1))) ini--;
        while(fim < conteudo.length() && !eSeparador(conteudo.charAt(fim))) fim++;

        editor.entrada.iniciarSelecao(linha, ini);
        editor.entrada.attSelecao(linha, fim);
    }

    public static boolean eSeparador(char c) {
        return Character.isWhitespace(c)
            || c == '.' || c == ',' || c == ';' || c == ':'
            || c == '!' || c == '?' || c == '(' || c == ')'
            || c == '[' || c == ']' || c == '{' || c == '}'
            || c == '"' || c == '\'' || c == '/' || c == '\\'
            || c == '-' || c == '+' || c == '=' || c == '<'
            || c == '>' || c == '&' || c == '|' || c == '@';
    }

    public int posAbsoluta(int linha, int coluna) {
        final int total = editor.buffer.totalLinhas();
        if(linha >= total) linha = total - 1;
        if(linha < 0) linha = 0;
        int soma = 0;
        for(int i = 0; i < linha; i++) soma += editor.buffer.linha(i).length() + 1;
        final int tam = editor.buffer.linha(linha).length();
        return soma + Math.max(0, Math.min(coluna, tam));
    }

    public int[] linhaColunaDe(int absoluta) {
        final int total = editor.buffer.totalLinhas();
        int restante = Math.max(0, absoluta);
        for(int i = 0; i < total; i++) {
            final int tam = editor.buffer.linha(i).length();
            if(restante <= tam) return new int[]{i, restante};
            restante -= tam + 1;
        }
        final int ultima = total - 1;
        return new int[]{ultima, editor.buffer.linha(ultima).length()};
    }

    public int inicioSelecaoAbs() {
        if(!editor.entrada.selecao().ativa) return posAbsoluta(editor.cursor.linha(), editor.cursor.coluna());
        final int[] ini = editor.entrada.selecao().inicio();
        return posAbsoluta(ini[0], ini[1]);
    }

    public int fimSelecaoAbs() {
        if(!editor.entrada.selecao().ativa) return posAbsoluta(editor.cursor.linha(), editor.cursor.coluna());
        final int[] fim = editor.entrada.selecao().fim();
        return posAbsoluta(fim[0], fim[1]);
    }

    public ClipboardManager areaTransferencia() {
        return (ClipboardManager)getContext().getSystemService(Context.CLIPBOARD_SERVICE);
    }

    public void copiarSelecao() {
        final ClipboardManager clip = areaTransferencia();
        final String texto = editor.entrada.copiar();
        if(!texto.isEmpty() && clip != null)
            clip.setPrimaryClip(ClipData.newPlainText("texto", texto));
    }

    public void recortarSelecao() {
        final ClipboardManager clip = areaTransferencia();
        final String texto = editor.entrada.copiar();
        if(!texto.isEmpty() && clip != null) {
            clip.setPrimaryClip(ClipData.newPlainText("texto", texto));
            editor.entrada.rmSelecao();
        }
    }

    public void colarTexto() {
        final ClipboardManager clip = areaTransferencia();
        if(clip == null || !clip.hasPrimaryClip()) return;
        final ClipData dados = clip.getPrimaryClip();
        if(dados == null || dados.getItemCount() == 0) return;
        final CharSequence texto = dados.getItemAt(0).coerceToText(getContext());
        if(texto != null && texto.length() > 0)
            editor.entrada.add(texto.toString());
    }

    public void abrirModoAcao() {
        if(modoAcao == null) modoAcao = startActionMode(padraoAcao);
        else modoAcao.invalidate();
    }

    @Override
    protected void onDraw(android.graphics.Canvas canvas) {
        editor.render.defAPI(canvas);
        editor.render.ajustar(getWidth(), getHeight());
        editor.att();
        agendarRedesenho();
    }

    public void agendarRedesenho() {
        if(redesenhoAgendado) return;
        redesenhoAgendado = true;
        geren.postDelayed(loopRedesenho, 33);
    }

    @Override
	public boolean onTouchEvent(MotionEvent e) {
		requestFocus();
		gestureDetector.onTouchEvent(e);

		switch(e.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				toqueInicioX = e.getX();
				toqueInicioY = e.getY();
				ultimoX = e.getX();
				ultimoY = e.getY();
				ultimoTempo = e.getEventTime();

				arrastando = false;
				pressionado = false;
				pararInercia();
				pararAutoRolamento();
				if(modoAcao != null) modoAcao.finish();
				editor.entrada.limparSelecao();
				break;
			case MotionEvent.ACTION_MOVE:
				if(pressionado) {
					// segurar + arrastar -> expande seleção com auto-rolamento
					final int[] posAtual = editor.posToque(e.getX(), e.getY());
					editor.entrada.attSelecao(posAtual[0], posAtual[1]);

					final float zona = editor.render.alturaLinha() * 2f;
					final float yTela = e.getY();
					if(yTela < zona) {
						iniciarAutoRolamento(-(zona - yTela) * 0.4f, posAtual);
					} else if(yTela > editor.render.altura - zona) {
						iniciarAutoRolamento((yTela - (editor.render.altura - zona)) * 0.4f, posAtual);
					} else {
						pararAutoRolamento();
					}
					break;
				}
				final float dyAbs = Math.abs(e.getY() - toqueInicioY);
				final float dxAbs = Math.abs(e.getX() - toqueInicioX);
				if(!arrastando && (dxAbs > LIMIAR_DRAG || dyAbs > LIMIAR_DRAG)) {
					arrastando = true;
				}
				if(arrastando) {
					final float deltaY = ultimoY - e.getY();
					final float deltaX = ultimoX - e.getX();
					final long dt = e.getEventTime() - ultimoTempo;
					if(dt > 0) {
						veloRolamento = veloRolamento * 0.6f + (deltaY / dt * 16f) * 0.4f;
						veloRolamentoX = veloRolamentoX * 0.6f + (deltaX / dt * 16f) * 0.4f;
					}
					aplicarRolamento(deltaY, deltaX);
					ultimoX = e.getX();
					ultimoY = e.getY();
					ultimoTempo = e.getEventTime();
				}
				break;
			case MotionEvent.ACTION_UP:
				pararAutoRolamento();
				if(pressionado) {
					pressionado = false;
					break;
				}
				if(arrastando) {
					arrastando = false;
					iniciarInercia();
				} else {
					editor.aoTocar(e.getX(), e.getY());
				}
				break;
		}
		return true;
	}

	public void aplicarRolamento(float deltaY, float deltaX) {
		editor.rolamentoY += deltaY;
		final float altTotal = editor.render.alturaLinha() * editor.buffer.totalLinhas() + editor.ESPACO_TOPO;
		final float maxRolamentoY = Math.max(0, altTotal - editor.render.altura);
		editor.rolamentoY = Math.max(0, Math.min(editor.rolamentoY, maxRolamentoY));

		editor.rolamentoX += deltaX;
		final float sarjetaLarg = editor.larguraSarjeta();
		final float largTotal = editor.larguraMaxLinha();
		final float larguraVisivel = editor.render.largura - sarjetaLarg;
		final float maxRolamentoX = Math.max(0, largTotal - larguraVisivel);
		editor.rolamentoX = Math.max(0, Math.min(editor.rolamentoX, maxRolamentoX));
	}

	public void iniciarInercia() {
		pararInercia();
		loopInercia = new Runnable() {
			@Override
			public void run() {
				if(Math.abs(veloRolamento) < 0.5f && Math.abs(veloRolamentoX) < 0.5f) {
					veloRolamento = 0f;
					veloRolamentoX = 0f;
					loopInercia = null;
					return;
				}
				aplicarRolamento(veloRolamento, veloRolamentoX);
				veloRolamento *= 0.92f;
				veloRolamentoX *= 0.92f;
				invalidate();
				geren.postDelayed(this, 16);
			}
		};
		geren.post(loopInercia);
	}

	public void pararInercia() {
		if(loopInercia != null) {
			geren.removeCallbacks(loopInercia);
			loopInercia = null;
		}
	}

	public void iniciarAutoRolamento(final float velocidade, final int[] posReferencia) {
		if(loopAutoRolamento != null) {
			autoRolamentoDelta = velocidade;
			return;
		}
		autoRolamentoDelta = velocidade;
		loopAutoRolamento = new Runnable() {
			@Override
			public void run() {
				if(loopAutoRolamento == null) return;
				aplicarRolamento(autoRolamentoDelta, 0f);
				// atualiza a seleção para a posição visivel mais proxima do dedo
				final int[] pos = posReferencia;
				editor.entrada.attSelecao(pos[0], pos[1]);
				invalidate();
				geren.postDelayed(this, 16);
			}
		};
		geren.post(loopAutoRolamento);
	}

	public void pararAutoRolamento() {
		if(loopAutoRolamento != null) {
			geren.removeCallbacks(loopAutoRolamento);
			loopAutoRolamento = null;
		}
		autoRolamentoDelta = 0f;
	}

    @Override
    public InputConnection onCreateInputConnection(EditorInfo info) {
        info.inputType = android.text.InputType.TYPE_CLASS_TEXT
            | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
        info.imeOptions = EditorInfo.IME_FLAG_NO_ENTER_ACTION;

        return new BaseInputConnection(this, false) {
            // pos absoluta na linha atual; -1 = sem composicao em andamento
            public int composInicio = -1;
            public int composFim = -1;

            // guarda a ultima pos de composicao mesmo apos finishComposingText,
            // pra commitText poder substituir o trecho se o teclado mandar o
            // texto final da sugestao sem reabrir a composicao antes
            public int linhaUltComposicao = -1;
            public int ultComposInicio = -1;
            public int ultComposFim = -1;

            public int rmIntervalo(int linha, int colIni, int colFim) {
                editor.buffer.rm(linha, colIni, colFim - colIni);
                return colIni;
            }
            // usado pelo teclado para reconstruir o contexto antes do cursor;
            // precisa devolver o texto de verdade(nao um "\n" generico), senao
            // o teclado perde a nocao de quantos caracteres tem antes do cursor
            // em textos com varias linhas e calcula errado onde a composicao
            // deve ser inserida/substituida
            @Override
            public CharSequence getTextBeforeCursor(int n, int m) {
                final StringBuilder sb = new StringBuilder();
                int linha = editor.cursor.linha();
                final int col = editor.cursor.coluna();
                final String linhaAtual = editor.buffer.linha(linha);
                final int fimAtual = Math.min(col, linhaAtual.length());
                sb.append(linhaAtual, 0, fimAtual);

                while(sb.length() < n && linha > 0) {
                    linha--;
                    sb.insert(0, "\n");
                    sb.insert(0, editor.buffer.linha(linha));
                }
                final int tam = sb.length();
                return sb.substring(Math.max(0, tam - n), tam);
            }

            @Override
            public boolean commitText(CharSequence texto, int novoCursor) {
                final String t = texto.toString();
                final int linha = editor.cursor.linha();
                if(composInicio >= 0) {
                    // finaliza a composicao: troca o trecho sugerido pelo texto final
                    final int colIni = rmIntervalo(linha, composInicio, composFim);
                    editor.cursor.def(linha, colIni);
                    limparComposicao();
                    editor.entrada.add(t);
                } else if(linhaUltComposicao == linha &&
				ultComposInicio >= 0&&
				ultComposFim == editor.cursor.coluna()) {
                    // o teclado finalizou a composicao(ex: finishComposingText) e ja
                    // manda o texto definitivo da sugestao sem reabrir setComposingText;
                    // ainda assim precisa substituir o trecho antigo, senao ele fica
                    // duplicado com o texto da sugestao inserido por cima
                    final int colIni = rmIntervalo(linha, ultComposInicio, ultComposFim);
                    editor.cursor.def(linha, colIni);
                    limparComposicao();
                    editor.entrada.add(t);
                } else if(t.length() > 1 && !t.equals("{}") && !t.equals("\"\"") && !t.equals("''")) {
                    limparComposicao();
                    editor.entrada.add(t);
                } else {
                    limparComposicao();
                    editor.entrada.aoDigitar(t);
                }
                invalidate();
                return true;
            }

            public void limparComposicao() {
                composInicio = -1;
                composFim = -1;
                linhaUltComposicao = -1;
                ultComposInicio = -1;
                ultComposFim = -1;
            }

            @Override
            public boolean setComposingText(CharSequence texto, int novoCursor) {
                // substitui o trecho em composicao pelo texto sugerido, sem
                // deixar o par automatico de aspas/chaves reagir a cada letra
                final String t = texto.toString();
                final int linha = editor.cursor.linha();
                int colIni;
                if(composInicio >= 0) {
                    colIni = rmIntervalo(linha, composInicio, composFim);
                } else {
                    colIni = editor.cursor.coluna();
                }
                editor.buffer.add(linha, colIni, t);
                composInicio = colIni;
                composFim = colIni + t.length();
                editor.cursor.def(linha, composFim);
                invalidate();
                return true;
            }

            @Override
            public boolean finishComposingText() {
                // guarda onde a composicao terminou, pra um commitText logo em
                // seguida ainda saber o que substituir (ver comentario em commitText)
                if(composInicio >= 0) {
                    linhaUltComposicao = editor.cursor.linha();
                    ultComposInicio = composInicio;
                    ultComposFim = composFim;
                }
                composInicio = -1;
                composFim = -1;
                return true;
            }

            @Override
            public boolean deleteSurroundingText(int antes, int depois) {
                if(composInicio >= 0) {
                    // durante composicao, antes/depois contam a partir das bordas dela
                    final int linha = editor.cursor.linha();
                    final int colIni = Math.max(0, composInicio - antes);
                    final int colFim = composFim + depois;
                    rmIntervalo(linha, colIni, colFim);
                    editor.cursor.def(linha, colIni);
                    limparComposicao();
                    invalidate();
                    return true;
                }
                limparComposicao();
                for(int i = 0; i < antes;  i++) editor.entrada.rmAntes();
                for(int i = 0; i < depois; i++) editor.entrada.rmDepois();
                return true;
            }

            @Override
            public CharSequence getSelectedText(int flags) {
                if(!editor.entrada.selecao().ativa) return null;
                final String texto = editor.entrada.copiar();
                return texto.isEmpty() ? null : texto;
            }

            @Override
            public CharSequence getTextAfterCursor(int n, int flags) {
                final StringBuilder sb = new StringBuilder();
                int linha = editor.cursor.linha();
                final int col = editor.cursor.coluna();
                final String linhaAtual = editor.buffer.linha(linha);
                final int ini = Math.min(col, linhaAtual.length());
                sb.append(linhaAtual, ini, linhaAtual.length());

                final int totalLinhas = editor.buffer.totalLinhas();
                while(sb.length() < n && linha < totalLinhas - 1) {
                    linha++;
                    sb.append("\n");
                    sb.append(editor.buffer.linha(linha));
                }
                return sb.substring(0, Math.min(n, sb.length()));
            }

            @Override
            public ExtractedText getExtractedText(ExtractedTextRequest pedido, int flags) {
                final ExtractedText et = new ExtractedText();
                et.text = editor.buffer.texto();
                et.startOffset = 0;
                et.selectionStart = inicioSelecaoAbs();
                et.selectionEnd = fimSelecaoAbs();
                if(editor.entrada.selecao().ativa) et.flags |= ExtractedText.FLAG_SELECTING;
                return et;
            }

            @Override
            public boolean setSelection(int inicio, int fim) {
                limparComposicao();
                final int[] a = linhaColunaDe(inicio);
                final int[] b = linhaColunaDe(fim);
                if(inicio == fim) {
                    editor.entrada.limparSelecao();
                    editor.cursor.def(a[0], a[1]);
                } else {
                    editor.entrada.iniciarSelecao(a[0], a[1]);
                    editor.entrada.attSelecao(b[0], b[1]);
                    editor.cursor.def(b[0], b[1]);
                }
                invalidate();
                return true;
            }

            @Override
            public boolean performContextMenuAction(int id) {
                switch(id) {
                    case android.R.id.paste:
                        colarTexto();
                        invalidate();
                        return true;
                    case android.R.id.copy:
                        copiarSelecao();
                        return true;
                    case android.R.id.cut:
                        recortarSelecao();
                        invalidate();
                        return true;
                    case android.R.id.selectAll:
                        editor.entrada.selecionarTudo(editor.buffer);
                        invalidate();
                        return true;
                }
                return super.performContextMenuAction(id);
            }

            @Override
            public boolean sendKeyEvent(KeyEvent evento) {
                if(evento.getAction() == KeyEvent.ACTION_DOWN) {
                    switch(evento.getKeyCode()) {
                        case KeyEvent.KEYCODE_DPAD_UP:
                            editor.cursor.mover(-1, 0, editor.buffer); break;
                        case KeyEvent.KEYCODE_DPAD_DOWN:
                            editor.cursor.mover(1,  0, editor.buffer); break;
                        case KeyEvent.KEYCODE_DPAD_LEFT:
                            editor.cursor.mover(0, -1, editor.buffer); break;
                        case KeyEvent.KEYCODE_DPAD_RIGHT:
                            editor.cursor.mover(0,  1, editor.buffer); break;
                        case KeyEvent.KEYCODE_DEL:
                            editor.entrada.rmAntes(); break;
                        case KeyEvent.KEYCODE_FORWARD_DEL:
                            editor.entrada.rmDepois(); break;
                        default:
                            return super.sendKeyEvent(evento);
                    }
                    return true;
                }
                return super.sendKeyEvent(evento);
            }
        };
    }

    @Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		pararInercia();
		pararAutoRolamento();
		geren.removeCallbacks(loopRedesenho);
		redesenhoAgendado = false;
		if(modoAcao != null) modoAcao.finish();
		editor.liberar();
	}
}
