package com.azulejo.util;
import java.io.File;

public class ArquivosUtil {
	public static boolean existe(String c) {
		return (new File(c)).exists();
	}
}
