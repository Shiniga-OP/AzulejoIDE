package com.azulejo.gerenciador;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ArvoreAdapter extends BaseAdapter {
	public static final int RECUO = 16;
	public static final int ALTURA = 36;

	public final Context contexto;
	public final List<No> visiveis = new ArrayList<No>();
	public final float densidade;

	public ArvoreAdapter(Context contexto, File raiz) {
		this.contexto = contexto;
		this.densidade = contexto.getResources().getDisplayMetrics().density;

		No no = new No(raiz, 0);
		no.expandido = true;
		visiveis.add(no);
		visiveis.addAll(no.filhos());
	}

	@Override
	public int getCount() {
		return visiveis.size();
	}

	@Override
	public No getItem(int pos) {
		return visiveis.get(pos);
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
			texto.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
			texto.setTextColor(Color.WHITE);
			texto.setSingleLine(true);
			texto.setLayoutParams(new ViewGroup.LayoutParams(
									  ViewGroup.LayoutParams.MATCH_PARENT,
									  dp(ALTURA)
								  ));
		} else {
			texto = (TextView) reuso;
		}
		No no = visiveis.get(pos);
		texto.setPadding(dp(8 + no.nivel * RECUO), 0, dp(8), 0);
		texto.setText(icone(no) + " " + no.nome());
		return texto;
	}

	// expande ou recolhe a pasta, retorna false se o nó for um arquivo
	public boolean alternar(int pos) {
		No no = visiveis.get(pos);

		if(!no.pasta()) return false;
		
		if(no.expandido) {
			recolher(pos, no);
		} else {
			visiveis.addAll(pos + 1, no.filhos());
			no.expandido = true;
		}
		notifyDataSetChanged();
		return true;
	}

	public void recolher(int pos, No no) {
		int fim = pos + 1;

		while(fim < visiveis.size() && visiveis.get(fim).nivel > no.nivel) {
			fim++;
		}
		visiveis.subList(pos + 1, fim).clear();
		no.expandido = false;
	}

	public String icone(No no) {
		if(!no.pasta()) {
			return "\u2022";
		}
		return no.expandido ? "\u25BE" : "\u25B8";
	}

	public int dp(int valor) {
		return (int) (valor * densidade + 0.5f);
	}
}
