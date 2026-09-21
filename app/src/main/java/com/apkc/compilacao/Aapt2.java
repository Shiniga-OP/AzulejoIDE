package com.apkc.compilacao;

import java.io.BufferedReader;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.InputStreamReader;

public class Aapt2 {
	public final List<String> cmd = new ArrayList<>();
	public String bin = "nulo";
	
	public Aapt2(String bin) {
		this.bin = bin;
	}
	
	public boolean exec(String... args) throws Exception {
		cmd.add(this.bin);
		cmd.addAll(Arrays.asList(args));

		final ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.redirectErrorStream(true);
		final Process p = pb.start();

		final BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String linha;
		while((linha = r.readLine()) != null) System.out.println(linha + "\n");
		r.close();
		
		cmd.clear();
		return p.waitFor() == 0;
	}
}
