package com.azulejo.gerenciador;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.io.File;

public class GerenciadorArquivos extends LinearLayout {
	public interface Ouvinte {
		void aoAbrir(File arquivo);
	}

	public final TextView caminho;
	public final ListView lista;
	public final ListaAdapter adapter;

	public File raiz;
	public File atual;
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

		addView(caminho, new LayoutParams(-1, adapter.dp(40)));
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
	}

	// sobe um nível, retorna false se já estiver na raiz (o chamador decide o que fazer)
	public boolean subir() {
		if(atual == null || atual.equals(raiz)) {
			return false;
		}

		File pai = atual.getParentFile();

		if(pai == null) {
			return false;
		}

		abrirPasta(pai);
		return true;
	}

	// mostra "projeto/src/com" em vez do caminho absoluto inteiro
	public String nomeRelativo(File pasta) {
		String base = raiz.getParent();
		String completo = pasta.getAbsolutePath();

		if(base != null && completo.startsWith(base)) {
			return completo.substring(base.length() + 1);
		}
		return completo;
	}
}
