package com.uniditor.nucleo.util;

import java.io.InputStream;
import java.io.File;

public interface Assets {
	public InputStream obter(String caminho);
	public File obterCache();
}
