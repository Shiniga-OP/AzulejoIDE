package com.azulejo.gerenciador;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import com.azulejo.util.ArquivosUtil;

public class GerenciadorArquivos extends LinearLayout {
	public interface Ouvinte {
		void aoAbrir(File arquivo);
		void aoExcluir(File arquivo);
		void aoNavegar(File pasta); // toda vez que abre uma pasta, pra quem ouve checar se tem modulo.auto nela
	}

	public final TextView caminho;
	public final TextView novo;
	public final ListView lista;
	public final ListaAdapter adapter;

	public File raiz;
	public static File atual;
	public Ouvinte ouvinte;

	public GerenciadorArquivos(Context contexto) {
		super(contexto);
		setOrientation(VERTICAL);
		setBackgroundColor(0xFF252526);

		adapter = new ListaAdapter(contexto);

		caminho = new TextView(contexto);
		caminho.setTextColor(0xFF9CDCFE);
		caminho.setSingleLine(true);
		caminho.setEllipsize(android.text.TextUtils.TruncateAt.START);
		caminho.setGravity(Gravity.CENTER_VERTICAL);
		caminho.setPadding(adapter.dp(12), 0, adapter.dp(12), 0);
		caminho.setBackgroundColor(0xFF333333);
		caminho.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					subir();
				}
			});
		novo = new TextView(contexto);
		novo.setText("+");
		novo.setTextColor(0xFF9CDCFE);
		novo.setTextSize(22);
		novo.setGravity(Gravity.CENTER);
		novo.setBackgroundColor(0xFF333333);
		novo.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					pedirCriar();
				}
			});
		lista = new ListView(contexto);
		lista.setDivider(null);
		lista.setDividerHeight(0);
		lista.setSelector(new ColorDrawable(Color.TRANSPARENT));
		lista.setAdapter(adapter);
		lista.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> pai, View v, int pos, long id) {
					File item = adapter.getItem(pos);

					if(item.isDirectory()) {
						abrirPasta(item);
					} else if(ouvinte != null) {
						ouvinte.aoAbrir(item);
					}
				}
			});
		lista.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> pai, View v, int pos, long id) {
					confirmarExcluir(adapter.getItem(pos));
					return true;
				}
			});

		LinearLayout barra = new LinearLayout(contexto);
		barra.setOrientation(HORIZONTAL);
		barra.addView(caminho, new LayoutParams(0, -1, 1f));
		barra.addView(novo, new LayoutParams(adapter.dp(48), -1));

		addView(barra, new LayoutParams(-1, adapter.dp(40)));
		addView(lista, new LayoutParams(-1, 0, 1f));
	}

	public void defRaiz(File raiz) {
		this.raiz = raiz;
		abrirPasta(raiz);
	}

	public void defOuvinte(Ouvinte ouvinte) {
		this.ouvinte = ouvinte;
	}

	public void abrirPasta(File pasta) {
		atual = pasta;
		adapter.listar(pasta);
		lista.setSelection(0);
		caminho.setText(nomeRelativo(pasta));

		if(ouvinte != null) ouvinte.aoNavegar(pasta);
	}

	// sobe um nível, retorna false se já estiver na raiz (o chamador decide o que fazer)
	public boolean subir() {
		if(atual == null || atual.equals(raiz)) return false;

		final File pai = atual.getParentFile();

		if(pai == null) return false;

		abrirPasta(pai);
		return true;
	}

	// recarrega a pasta atual depois de criar ou excluir
	public void atualizar() {
		adapter.listar(atual);
	}

	// pergunta o nome e se é arquivo ou pasta
	public void pedirCriar() {
		if(atual == null) return;

		final EditText nome = new EditText(getContext());
		nome.setSingleLine(true);
		nome.setInputType(InputType.TYPE_CLASS_TEXT);
		nome.setHint("Nome");

		new AlertDialog.Builder(getContext())
			.setTitle("Criar em " + atual.getName())
			.setView(nome)
			.setPositiveButton("Arquivo", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface d, int qual) {
					criar(nome.getText().toString(), false);
				}
			})
			.setNeutralButton("Pasta", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface d, int qual) {
					criar(nome.getText().toString(), true);
				}
			})
			.setNegativeButton("Cancelar", null)
			.show();
	}

	public void criar(String nome, boolean pasta) {
		final String limpo = nome.trim();

		if(limpo.length() == 0 || limpo.indexOf('/') != -1) {
			aviso("Nome inválido");
			return;
		}
		final File novoItem = new File(atual, limpo);

		if(novoItem.exists()) {
			aviso("Já existe: " + limpo);
			return;
		}
		final boolean ok = pasta ? ArquivosUtil.criarPasta(novoItem) : ArquivosUtil.criarArquivo(novoItem);

		aviso(ok ? "Criado: " + limpo : "Falha ao criar");
		atualizar();
	}

	public void confirmarExcluir(final File alvo) {
		final String tipo = alvo.isDirectory() ? "a pasta (com tudo dentro)" : "o arquivo";

		new AlertDialog.Builder(getContext())
			.setTitle("Excluir")
			.setMessage("Excluir " + tipo + " \"" + alvo.getName() + "\"?")
			.setPositiveButton("Excluir", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface d, int qual) {
					excluir(alvo);
				}
			})
			.setNegativeButton("Cancelar", null)
			.show();
	}

	public void excluir(File alvo) {
		final boolean ok = ArquivosUtil.excluir(alvo);

		aviso(ok ? "Excluído: " + alvo.getName() : "Falha ao excluir");
		atualizar();

		if(ouvinte != null && ok) ouvinte.aoExcluir(alvo);
	}

	public void aviso(String texto) {
		Toast.makeText(getContext(), texto, Toast.LENGTH_SHORT).show();
	}

	// mostra "projeto/src/com" em vez do caminho absoluto inteiro
	public String nomeRelativo(File pasta) {
		final String base = raiz.getParent();
		final String completo = pasta.getAbsolutePath();

		if(base != null && completo.startsWith(base)) {
			return completo.substring(base.length() + 1);
		}
		return completo;
	}
}
