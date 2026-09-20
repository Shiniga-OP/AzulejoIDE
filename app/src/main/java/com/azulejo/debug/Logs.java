package com.azulejo.debug;

import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import android.app.Activity;
import android.widget.ScrollView;
import android.widget.EditText;
import android.view.View;
import com.azulejo.R;

public class Logs extends PrintStream {
	public static Activity ctx;
	public static ScrollView div;
	public static EditText texto;
	
	public Logs(final Activity ctx) {
		super(new ByteArrayOutputStream());
		this.ctx = ctx;
		
		ctx.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				texto = ctx.findViewById(R.id.logs);
				div = ctx.findViewById(R.id.div);
			}
		});
	}

	@Override
	public void write(byte[] buf, int pos, int tam) {
		final String msg = new String(buf, pos, tam);
		ctx.runOnUiThread(new Runnable() {
				public void run() {
					texto.append(msg);
					div.fullScroll(View.FOCUS_DOWN);
				}
			});
	}

	@Override
	public void write(int b) {
		write(new byte[]{(byte) b}, 0, 1);
	}
}
