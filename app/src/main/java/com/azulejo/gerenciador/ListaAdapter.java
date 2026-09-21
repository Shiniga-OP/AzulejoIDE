package com.azulejo.gerenciador;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class ListaAdapter extends BaseAdapter {
	public static final int ALTURA = 44;

	public final Context contexto;
	public final List<File> itens = new ArrayList<File>();
	public final float densidade;

	public ListaAdapter(Context contexto) {
		this.contexto = contexto;
		this.densidade = contexto.getResources().getDisplayMetrics().density;
	}

	@Override
	public int getCount() {
		return itens.size();
	}

	@Override
	public File getItem(int pos) {
		return itens.get(pos);
	}

	@Override
	public long getItemId(int pos) {
		return pos;
	}

	@Override
	public View getView(int pos, View reuso, ViewGroup pai) {
		TextView texto;

		if(reuso == null) {
			texto = new TextView(contexto);
			texto.setGravity(Gravity.CENTER_VERTICAL);
			texto.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
			texto.setTextColor(0xFFFFFFFF);
			texto.setSingleLine(true);
			texto.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
			texto.setPadding(dp(12), 0, dp(12), 0);
			texto.setLayoutParams(new ViewGroup.LayoutParams(
									  ViewGroup.LayoutParams.MATCH_PARENT,
									  dp(ALTURA)
								  ));
		} else {
			texto = (TextView) reuso;
		}
		File arquivo = itens.get(pos);
		texto.setText(arquivo.isDirectory() ? "\u25B8 " + arquivo.getName() : arquivo.getName());
		return texto;
	}

	// troca o conteúdo pelos filhos da pasta: pastas primeiro, depois arquivos, ambos em ordem alfabética
	public void listar(File pasta) {
		itens.clear();
		File[] filhos = pasta.listFiles();

		if(filhos != null) {
			Arrays.sort(filhos, new Comparator<File>() {
					@Override
					public int compare(File a, File b) {
						if(a.isDirectory() != b.isDirectory()) {
							return a.isDirectory() ? -1 : 1;
						}
						return a.getName().compareToIgnoreCase(b.getName());
					}
				});
			itens.addAll(Arrays.asList(filhos));
		}
		notifyDataSetChanged();
	}

	public int dp(int valor) {
		return (int) (valor * densidade + 0.5f);
	}
}
