package com.apkc.otimizadores.passes;

import com.apkc.otimizadores.analise.AnalisadorClasse;

public class PasseDupArmazena implements Passe {
	public static final byte DUP = (byte)0x59;
	public static final byte DUP2 = (byte)0x5C;

	@Override
	public String nome() {
		return "dup-armazena";
	}

	@Override
	public long aplicar(byte[] dados, AnalisadorClasse classe) {
		long qtd = 0;

		for(AnalisadorClasse.Metodo m : classe.metodos) {
			if(m.inicioCodigo < 0) continue;

			for(int i = 0; i + 1 < m.instrucoes.size(); i++) {
				final AnalisadorClasse.Instrucao a = m.instrucoes.get(i);
				final AnalisadorClasse.Instrucao b = m.instrucoes.get(i + 1);

				final int slotA = slotDoStore(a.codigo);
				if(slotA < 0) continue;
				if(slotDoLoad(b.codigo) != slotA) continue;

				// xstore_N seguido de xload_N da mesma variável, sem nada no meio:
				// dup (ou dup2 p/ long/double) antes do store deixa a copia na pilha,
				// dispensando o load. Troca [store][load] por [dup][store]:
				// a posição de 'a' vira dup/dup2, a posição de 'b' recebe o store original.
				final boolean categoria2 = slotA >= 0x200;
				final byte storeOriginal = dados[m.inicioCodigo + a.pos];

				dados[m.inicioCodigo + b.pos] = storeOriginal;
				dados[m.inicioCodigo + a.pos] = categoria2 ? DUP2 : DUP;

				qtd++;
			}
		}
		return qtd;
	}

	public static int slotDoStore(int codigo) {
		if(codigo >= 0x3B && codigo <= 0x3E) return codigo - 0x3B;         // istore_N
		if(codigo >= 0x43 && codigo <= 0x46) return 0x100 + (codigo - 0x43); // fstore_N
		if(codigo >= 0x3F && codigo <= 0x42) return 0x200 + (codigo - 0x3F); // lstore_N
		if(codigo >= 0x47 && codigo <= 0x4A) return 0x300 + (codigo - 0x47); // dstore_N
		return -1;
	}

	public static int slotDoLoad(int codigo) {
		if(codigo >= 0x1A && codigo <= 0x1D) return codigo - 0x1A;         // iload_N
		if(codigo >= 0x22 && codigo <= 0x25) return 0x100 + (codigo - 0x22); // fload_N
		if(codigo >= 0x1E && codigo <= 0x21) return 0x200 + (codigo - 0x1E); // lload_N
		if(codigo >= 0x26 && codigo <= 0x29) return 0x300 + (codigo - 0x26); // dload_N
		return -1;
	}
}
