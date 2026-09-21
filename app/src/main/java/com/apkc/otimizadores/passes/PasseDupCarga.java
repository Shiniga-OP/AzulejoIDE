package com.apkc.otimizadores.passes;

import com.apkc.otimizadores.analise.AnalisadorClasse;

public class PasseDupCarga implements Passe {
	public static final byte DUP2 = (byte)0x5C;
	public static final byte NOP = (byte)0x00;

	@Override
	public String nome() {
		return "dup-carga";
	}

	@Override
	public long aplicar(byte[] dados, AnalisadorClasse classe) {
		long qtd = 0;

		for(AnalisadorClasse.Metodo m : classe.metodos) {
			if(m.inicioCodigo < 0) continue;

			for(int i = 0; i + 3 < m.instrucoes.size(); i++) {
				final AnalisadorClasse.Instrucao a = m.instrucoes.get(i);
				final AnalisadorClasse.Instrucao b = m.instrucoes.get(i + 1);
				final AnalisadorClasse.Instrucao c = m.instrucoes.get(i + 2);
				final AnalisadorClasse.Instrucao d = m.instrucoes.get(i + 3);

				// aload_N (0x2A-0x2D) seguido de iload_M (0x1A-0x1D), repetido igual:
				// dup2 reproduz o mesmo estado de pilha sem recarregar as duas variaveis.
				if(a.codigo >= 0x2A && a.codigo <= 0x2D
				   && b.codigo >= 0x1A && b.codigo <= 0x1D
				   && c.codigo == a.codigo
				   && d.codigo == b.codigo) {
					dados[m.inicioCodigo + c.pos] = DUP2;
					dados[m.inicioCodigo + d.pos] = NOP;
					qtd++;
				}
			}
		}
		return qtd;
	}
}
