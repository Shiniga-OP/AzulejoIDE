package com.azulejo;

import com.apkc.compilacao.Aapt2;
import com.apkc.compilacao.Processo;
import com.apkc.projeto.Projeto;
import com.apkc.projeto.Chave;
import com.apkc.util.ArquivosUtil;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ScrollView;
import com.android.ConfigAndroid;
import com.android.graficos.Canvas;
import com.android.EditorAndroidCanvas;
import com.azulejo.debug.Logs;
import com.azulejo.gerenciador.GerenciadorArquivos;
import com.uniditor.nucleo.editores.VisaoEditor;
import com.uniditor.nucleo.sintaxe.TokenizadorJava;
import com.uniditor.nucleo.Util;

import java.io.File;

public class InicioActivity extends Activity {
	public Logs logs;
	public String raiz = "";
	public VisaoEditor visaoEditor;
	public GerenciadorArquivos gerenciador;
	public FrameLayout tela;
	public View editor;
	public File arquivoAberto;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
		new ConfigAndroid(this);

		raiz = getExternalMediaDirs()[0].getAbsolutePath();

		visaoEditor = new VisaoEditor(
			new Canvas(
				0xFFFFFFFF, // cor das formas
				0xFF1E1E1E // cor do fundo
			), // renderizador
			new TokenizadorJava() // regras de sintaxe
		);

		visaoEditor.render.defFonte(
			Util.arquivo.copiarAssets("firacode-regular.ttf")
		);
		editor = new EditorAndroidCanvas(this, visaoEditor);

		gerenciador = new GerenciadorArquivos(this);
		gerenciador.defRaiz(new File(raiz, "projeto"));
		gerenciador.defOuvinte(new GerenciadorArquivos.Ouvinte() {
				@Override
				public void aoAbrir(File arquivo) {
					abrir(arquivo);
				}
			});

		Button compilar = new Button(this);
		compilar.setText("Compilar");
		compilar.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					iniciar(v);
				}
			});

		EditText texto = new EditText(this);
		texto.setBackgroundColor(Color.WHITE);
		texto.setTextColor(Color.BLACK);
		texto.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

		ScrollView div = new ScrollView(this);
		div.addView(texto, new ScrollView.LayoutParams(-1, -2));

		LinearLayout painelLogs = new LinearLayout(this);
		painelLogs.setOrientation(LinearLayout.VERTICAL);
		painelLogs.addView(compilar, new LinearLayout.LayoutParams(-1, -2));
		painelLogs.addView(div, new LinearLayout.LayoutParams(-1, 0, 1f));

		// uma coisa por vez: gerenciador, editor ou logs (celular vertical não comporta três painéis)
		tela = new FrameLayout(this);
		tela.addView(gerenciador);
		tela.addView(editor);
		tela.addView(painelLogs);
		mostrar(gerenciador);

		LinearLayout barra = new LinearLayout(this);
		barra.setOrientation(LinearLayout.HORIZONTAL);
		barra.setBackgroundColor(0xFF333333);
		barra.addView(aba("Arquivos", gerenciador), new LinearLayout.LayoutParams(0, -2, 1f));
		barra.addView(aba("Editor", editor), new LinearLayout.LayoutParams(0, -2, 1f));
		barra.addView(aba("Logs", painelLogs), new LinearLayout.LayoutParams(0, -2, 1f));

		Button salvar = new Button(this);
		salvar.setText("Salvar");
		salvar.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					salvar();
				}
			});
		barra.addView(salvar, new LinearLayout.LayoutParams(0, -2, 1f));

		LinearLayout raizTela = new LinearLayout(this);
		raizTela.setOrientation(LinearLayout.VERTICAL);
		raizTela.addView(tela, new LinearLayout.LayoutParams(-1, 0, 1f));
		raizTela.addView(barra, new LinearLayout.LayoutParams(-1, -2));
		setContentView(raizTela);

		logs = new Logs(this);
		// o Logs procurava R.id.logs e R.id.div, agora as views vêm daqui
		Logs.texto = texto;
		Logs.div = div;

		System.setOut(logs);
		System.setErr(logs);
    }

    public void iniciar(View v) {
		Aapt2 aapt2 = new Aapt2(getApplicationInfo().nativeLibraryDir + "/libaapt2.so");
		final String projeto = raiz + "/projeto/";
		final String java = projeto + "src/com/teste/Inicio.java";
		final String res = projeto + "res";
		final String manifest = projeto + "AndroidManifest.xml";
		final String androidJar = raiz + "/android.jar";

		logs.texto.setText("");

		// pega uma chave que ja existe ou cria uma se não existir
		Chave chave = new Chave(projeto + "teste.p12", "teste", "Teste", "123456");

		Processo processo = new Processo(
			new Projeto(projeto, java, res, manifest, chave)
		);
		processo.compilarAPK(androidJar, aapt2);
	}

	public Button aba(String titulo, final View alvo) {
		Button botao = new Button(this);
		botao.setText(titulo);
		botao.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mostrar(alvo);
				}
			});
		return botao;
	}

	// deixa só uma das três views visíveis
	public void mostrar(View alvo) {
		for(int i = 0; i < tela.getChildCount(); i++) {
			View filho = tela.getChildAt(i);
			filho.setVisibility(filho == alvo ? View.VISIBLE : View.GONE);
		}
	}

	@Override
	public void onBackPressed() {
		if(gerenciador.getVisibility() == View.VISIBLE) {
			if(!gerenciador.subir()) super.onBackPressed();
		} else {
			mostrar(gerenciador);
		}
	}

	public void abrir(File arquivo) {
		String texto = ArquivosUtil.ler(arquivo);

		if(texto == null) return;

		arquivoAberto = arquivo;
		visaoEditor.entrada.limparSelecao();
		visaoEditor.defTexto(texto);
		mostrar(editor);
	}

	// monta o texto linha a linha (só uso os métodos do buffer que já vi sendo usados)
	public String textoDoEditor() {
		StringBuilder sb = new StringBuilder();
		int total = visaoEditor.buffer.totalLinhas();

		for(int i = 0; i < total; i++) {
			if(i > 0) sb.append("\n");
			sb.append(visaoEditor.buffer.linha(i));
		}
		return sb.toString();
	}

	public void salvar() {
		if(arquivoAberto == null) {
			Toast.makeText(this, "Nenhum arquivo aberto", Toast.LENGTH_SHORT).show();
			return;
		}

		boolean ok = ArquivosUtil.salvar(arquivoAberto, textoDoEditor());
		Toast.makeText(this, ok ? "Salvo: " + arquivoAberto.getName() : "Falha ao salvar", Toast.LENGTH_SHORT).show();
	}
}
