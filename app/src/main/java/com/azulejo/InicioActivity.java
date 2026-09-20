package com.azulejo;

import com.azulejo.compilacao.Aapt2;
import com.azulejo.compilacao.Processo;
import com.azulejo.projeto.Projeto;
import com.azulejo.projeto.Chave;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import com.azulejo.debug.Logs;

public class InicioActivity extends Activity {
	public Logs logs;
	public String raiz = "";

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.inicio);
		
		logs = new Logs(this);

        System.setOut(logs);
        System.setErr(logs);
		
		raiz = getExternalMediaDirs()[0].getAbsolutePath();
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
}
