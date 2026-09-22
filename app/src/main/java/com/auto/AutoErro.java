package com.auto;

public class AutoErro extends RuntimeException {
	public AutoErro(String msg, int linha) {
		super(msg + " (linha " + linha + ")");
	}
}
