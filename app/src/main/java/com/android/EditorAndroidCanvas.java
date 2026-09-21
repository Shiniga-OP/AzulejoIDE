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

import com.uniditor.nucleo.Editor;
import com.uniditor.nucleo.entradas.Teclado;
import android.os.Handler;

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
	public float ultimoY = 0f;
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

    public EditorAndroidCanvas(Context ctx, final Editor editor) {
        super(ctx);
        this.editor = editor;

        this.ISSO = this;
        this.editor.entrada.teclado = new Teclado() {
            @Override
            public void abrirTeclado() {
                InputMethodManager imm = (InputMethodManager) getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
                if(imm != null) imm.showSoftInput(ISSO, InputMethodManager.SHOW_IMPLICIT);
            }
        };

        gestureDetector = new GestureDetector(ctx, new GestureDetector.SimpleOnGestureListener() {
				@Override
				public void onLongPress(MotionEvent e) {
					pressionado = true;
					int[] pos = editor.posToque(e.getX(), e.getY());
					selecionarPalavra(pos[0], pos[1]);
					abrirModoAcao();
					invalidate();
				}
			});

        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    public void selecionarPalavra(int linha, int coluna) {
        String conteudo = editor.buffer.linha(linha);
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
        int total = editor.buffer.totalLinhas();
        if(linha >= total) linha = total - 1;
        if(linha < 0) linha = 0;
        int soma = 0;
        for(int i = 0; i < linha; i++) soma += editor.buffer.linha(i).length() + 1;
        int tam = editor.buffer.linha(linha).length();
        return soma + Math.max(0, Math.min(coluna, tam));
    }

    public int[] linhaColunaDe(int absoluta) {
        int total = editor.buffer.totalLinhas();
        int restante = Math.max(0, absoluta);
        for(int i = 0; i < total; i++) {
            int tam = editor.buffer.linha(i).length();
            if(restante <= tam) return new int[] {i, restante};
            restante -= tam + 1;
        }
        int ultima = total - 1;
        return new int[] {ultima, editor.buffer.linha(ultima).length()};
    }

    public int inicioSelecaoAbs() {
        if(!editor.entrada.selecao().ativa) return posAbsoluta(editor.cursor.linha(), editor.cursor.coluna());
        int[] ini = editor.entrada.selecao().inicio();
        return posAbsoluta(ini[0], ini[1]);
    }

    public int fimSelecaoAbs() {
        if(!editor.entrada.selecao().ativa) return posAbsoluta(editor.cursor.linha(), editor.cursor.coluna());
        int[] fim = editor.entrada.selecao().fim();
        return posAbsoluta(fim[0], fim[1]);
    }

    public ClipboardManager areaTransferencia() {
        return (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
    }

    public void copiarSelecao() {
        ClipboardManager clip = areaTransferencia();
        String texto = editor.entrada.copiar();
        if(!texto.isEmpty() && clip != null)
            clip.setPrimaryClip(ClipData.newPlainText("texto", texto));
    }

    public void recortarSelecao() {
        ClipboardManager clip = areaTransferencia();
        String texto = editor.entrada.copiar();
        if(!texto.isEmpty() && clip != null) {
            clip.setPrimaryClip(ClipData.newPlainText("texto", texto));
            editor.entrada.rmSelecao();
        }
    }

    public void colarTexto() {
        ClipboardManager clip = areaTransferencia();
        if(clip == null || !clip.hasPrimaryClip()) return;
        ClipData dados = clip.getPrimaryClip();
        if(dados == null || dados.getItemCount() == 0) return;
        CharSequence texto = dados.getItemAt(0).coerceToText(getContext());
        if(texto != null && texto.length() > 0)
            editor.entrada.add(texto.toString());
    }

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
            // desabilita Copiar e Recortar se não há seleção
            boolean temSelecao = editor.entrada.selecao().ativa && !editor.entrada.copiar().isEmpty();
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
					int[] posAtual = editor.posToque(e.getX(), e.getY());
					editor.entrada.attSelecao(posAtual[0], posAtual[1]);

					float zona = editor.render.alturaLinha() * 2f;
					float yTela = e.getY();
					if(yTela < zona) {
						iniciarAutoRolamento(-(zona - yTela) * 0.4f, posAtual);
					} else if(yTela > editor.render.altura - zona) {
						iniciarAutoRolamento((yTela - (editor.render.altura - zona)) * 0.4f, posAtual);
					} else {
						pararAutoRolamento();
					}
					break;
				}
				float dyAbs = Math.abs(e.getY() - toqueInicioY);
				float dxAbs = Math.abs(e.getX() - toqueInicioX);
				if(!arrastando && (dxAbs > LIMIAR_DRAG || dyAbs > LIMIAR_DRAG)) {
					arrastando = true;
				}
				if(arrastando) {
					float delta = ultimoY - e.getY();
					long dt = e.getEventTime() - ultimoTempo;
					if(dt > 0) veloRolamento = veloRolamento * 0.6f + (delta / dt * 16f) * 0.4f;

					aplicarRolamento(delta);
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

	public void aplicarRolamento(float delta) {
		editor.rolamentoY += delta;
		float altTotal = editor.render.alturaLinha() * editor.buffer.totalLinhas() + editor.ESPACO_TOPO;
		float maxRolamento = Math.max(0, altTotal - editor.render.altura);
		editor.rolamentoY = Math.max(0, Math.min(editor.rolamentoY, maxRolamento));
	}

	public void iniciarInercia() {
		pararInercia();
		loopInercia = new Runnable() {
			@Override
			public void run() {
				if(Math.abs(veloRolamento) < 0.5f) {
					veloRolamento = 0f;
					loopInercia = null;
					return;
				}
				aplicarRolamento(veloRolamento);
				veloRolamento *= 0.92f;
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
				aplicarRolamento(autoRolamentoDelta);
				// atualiza a seleção para a posição visível mais próxima do dedo
				int[] pos = posReferencia;
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
            // adicione este metodo para manter o teclado sincronizado
            @Override
            public CharSequence getTextBeforeCursor(int n, int m) {
                int linha = editor.cursor.linha();
                int col = editor.cursor.coluna();
                if(col > 0) {
                    String linhaAtual = editor.buffer.linha(linha);
                    int fim = Math.min(col, linhaAtual.length());
                    return linhaAtual.substring(Math.max(0, fim - n), fim);
                } else if(linha > 0) {
                    return "\n"; // avisa o teclado sobre a quebra de linha
                }
                return "";
            }

            @Override
            public boolean commitText(CharSequence texto, int novoCursor) {
                String t = texto.toString();
                if(t.length() > 1 && !t.equals("{}") && !t.equals("\"\"") && !t.equals("''")) {
                    editor.entrada.add(t);
                } else {
                    editor.entrada.aoDigitar(t);
                }
                invalidate();
                return true;
            }

            @Override
            public boolean setComposingText(CharSequence texto, int novoCursor) {
                return true;
            }

            @Override
            public boolean finishComposingText() {
                return true;
            }

            @Override
            public boolean deleteSurroundingText(int antes, int depois) {
                for(int i = 0; i < antes;  i++) editor.entrada.rmAntes();
                for(int i = 0; i < depois; i++) editor.entrada.rmDepois();
                return true;
            }

            @Override
            public CharSequence getSelectedText(int flags) {
                if(!editor.entrada.selecao().ativa) return null;
                String texto = editor.entrada.copiar();
                return texto.isEmpty() ? null : texto;
            }

            @Override
            public CharSequence getTextAfterCursor(int n, int flags) {
                int linha = editor.cursor.linha();
                int col = editor.cursor.coluna();
                String linhaAtual = editor.buffer.linha(linha);
                int ini = Math.min(col, linhaAtual.length());
                int fim = Math.min(linhaAtual.length(), ini + n);
                return linhaAtual.substring(ini, fim);
            }

            @Override
            public ExtractedText getExtractedText(ExtractedTextRequest pedido, int flags) {
                ExtractedText et = new ExtractedText();
                et.text = editor.buffer.texto();
                et.startOffset = 0;
                et.selectionStart = inicioSelecaoAbs();
                et.selectionEnd = fimSelecaoAbs();
                if(editor.entrada.selecao().ativa) et.flags |= ExtractedText.FLAG_SELECTING;
                return et;
            }

            @Override
            public boolean setSelection(int inicio, int fim) {
                int[] a = linhaColunaDe(inicio);
                int[] b = linhaColunaDe(fim);
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
