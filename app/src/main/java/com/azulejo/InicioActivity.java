package com.azulejo;

import com.apkc.compilacao.Aapt2;
import com.apkc.compilacao.Processo;
import com.apkc.projeto.Projeto;
import com.apkc.projeto.Chave;
import com.azulejo.util.ArquivosUtil;
import com.auto.ArquivoAuto;
import com.auto.No;
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
import com.android.graficos.Canvas;
import com.android.EditorAndroidCanvas;
import com.azulejo.debug.Logs;
import com.azulejo.gerenciador.GerenciadorArquivos;
import com.uniditor.editores.VisaoEditor;
import com.uniditor.sintaxe.TokenizadorJava;
import com.uniditor.Util;
import java.io.File;

public class InicioActivity extends Activity {
	public Logs logs;
	public static String raiz;
	public VisaoEditor visaoEditor;
	public GerenciadorArquivos gerenciador;
	public FrameLayout tela;
	public View editor;
	public File arquivoAberto;
	public String androidJar;
	public String lambdasJar;
	public static File moduloAtual; // pasta com o modulo.auto aberto no momento(null = nenhum ainda)
	public Button abrirProjeto;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);

		raiz = getExternalMediaDirs()[0].getAbsolutePath();
		androidJar = raiz + "/android.jar";
		lambdasJar = null; // raiz + "/core-lambda-stubs.jar";

		visaoEditor = new VisaoEditor(
			new Canvas(
				0xFFFFFFFF, // cor das formas
				0xFF1E1E1E // cor do fundo
			), // renderizador
			new TokenizadorJava() // regras de sintaxe
		);
		editor = new EditorAndroidCanvas(this, visaoEditor);

		visaoEditor.render.defFonte(
			Util.arquivo.copiarAssets("firacode-regular.ttf")
		);
		gerenciador = new GerenciadorArquivos(this);
		gerenciador.defRaiz(new File(raiz));
		ArquivosUtil.carregarPreferencias();
		gerenciador.abrirPasta(gerenciador.atual);
		
		abrirProjeto = new Button(this);
		abrirProjeto.setText("Abrir projeto");
		abrirProjeto.setVisibility(View.GONE);
		abrirProjeto.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					moduloAtual = gerenciador.atual;
					abrirProjeto.setVisibility(View.GONE);
					Toast.makeText(InicioActivity.this, "Projeto aberto: " + moduloAtual.getName(), Toast.LENGTH_SHORT).show();
				}
			});
		gerenciador.addView(abrirProjeto, new LinearLayout.LayoutParams(-1, -2));

		gerenciador.defOuvinte(new GerenciadorArquivos.Ouvinte() {
				@Override
				public void aoAbrir(File arquivo) {
					abrir(arquivo);
				}

				@Override
				public void aoExcluir(File arquivo) {
					fechouExcluido(arquivo);
				}

				@Override
				public void aoNavegar(File pasta) {
					if(
						ArquivoAuto.temModulo(pasta) &&
						!moduloAtual.getAbsolutePath().equals(pasta.getAbsolutePath())
					) abrirProjeto.setVisibility(View.VISIBLE);
					else abrirProjeto.setVisibility(View.GONE);
				}
			});
		Button compilar = new Button(this);
		compilar.setText("Compilar");
		compilar.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					logs.texto.setText("");
					compilarModulo();
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

	// compila so o modulo aberto pelo botao "Abrir projeto" (le direto o modulo.auto dele, sem projeto.auto)
	public void compilarModulo() {
		if(moduloAtual == null) {
			Toast.makeText(this, "Nenhum projeto aberto: navegue até uma pasta com modulo.auto", Toast.LENGTH_SHORT).show();
			return;
		}
		No config = ArquivoAuto.carregarModulo(moduloAtual);
		No android = config.pos("android");

		final String modulo = moduloAtual.getAbsolutePath() + "/";
		final String java = modulo + config.posTexto("java");
		final String res = modulo + android.posTexto("res");
		final String manifest = modulo + android.posTexto("androidManifest");

		No assinar = android.pos("assinar");

		// pega uma chave que ja existe ou cria uma se não existir
		Chave chave = new Chave(
			modulo + assinar.posTexto("chave"),
			assinar.posTexto("nome"),
			assinar.posTexto("dono"),
			assinar.posTexto("senha")
		);

		Projeto projeto = new Projeto(modulo, java, res, manifest, chave);
		projeto.versaoJava = config.posTexto("versaoJava");
		projeto.versaoAlvo = config.posTexto("versaoAlvo");

		Aapt2 aapt2 = new Aapt2(getApplicationInfo().nativeLibraryDir + "/libaapt2.so");
		Processo processo = new Processo(projeto);
		processo.compilarAPK(androidJar, lambdasJar, aapt2);
	}

	// deixa so uma das tres views visiveis
	public void mostrar(View alvo) {
		for(int i = 0; i < tela.getChildCount(); i++) {
			final View filho = tela.getChildAt(i);
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
		final String texto = ArquivosUtil.ler(arquivo);

		if(texto == null) return;

		arquivoAberto = arquivo;
		visaoEditor.entrada.limparSelecao();
		visaoEditor.defTexto(texto);
		mostrar(editor);
	}

	// se o arquivo aberto foi excluido(ou está dentro da pasta excluida), solta ele pra o Salvar não recriar
	public void fechouExcluido(File excluido) {
		if(arquivoAberto == null) return;

		final String aberto = arquivoAberto.getAbsolutePath();
		final String alvo = excluido.getAbsolutePath();

		if(aberto.equals(alvo) || aberto.startsWith(alvo + "/")) {
			arquivoAberto = null;
			visaoEditor.entrada.limparSelecao();
			visaoEditor.defTexto("");
		}
	}

	// monta o texto linha a linha(so uso os metodos do buffer que ja vi sendo usados)
	public String textoEditor() {
		final StringBuilder sb = new StringBuilder();
		final int total = visaoEditor.buffer.totalLinhas();

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
		final boolean ok = ArquivosUtil.salvar(arquivoAberto, textoEditor());
		Toast.makeText(this, ok ? "Salvo: " + arquivoAberto.getName() : "Falha ao salvar", Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onPause() {
		ArquivosUtil.salvarPreferencias();
	}
}
