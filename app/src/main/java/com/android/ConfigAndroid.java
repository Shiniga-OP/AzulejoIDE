package com.android;

import android.content.Context;
import com.uniditor.Util;
import com.uniditor.util.Assets;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;

public class ConfigAndroid {
	public ConfigAndroid(final Context ctx) {
		Util.assets = new Assets() {
			@Override
				public InputStream obter(String caminho) {
				try {
					return ctx.getAssets().open(caminho);
				} catch(IOException e) {
					return null;
				}
			}
			@Override
			public File obterCache() {
				return ctx.getCacheDir();
			}
		};
	}
}
