package com.apkc.otimizadores.analise;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AnalisadorClasse {
	public static final int CONST_TEXTO_UTF = 1;
	public static final int CONST_INTEIRO = 3;
	public static final int CONST_DECIMAL = 4;
	public static final int CONST_LONGO = 5;
	public static final int CONST_DUPLO = 6;
	public static final int CONST_CLASSE = 7;
	public static final int CONST_TEXTO = 8;
	public static final int CONST_CAMPO = 9;
	public static final int CONST_METODO = 10;
	public static final int CONST_METODO_INTERFACE = 11;
	public static final int CONST_NOME_TIPO = 12;
	public static final int CONST_METODO_REF = 15;
	public static final int CONST_METODO_TIPO = 16;
	public static final int CONST_DINAMICO = 17;
	public static final int CONST_INVOCA_DINAMICO = 18;
	public static final int CONST_MODULO = 19;
	public static final int CONST_PACOTE = 20;

	public static class Constante {
		public int tipo;
		public String texto;
		public int a;
		public int b;
	}

	public static class Instrucao {
		public int pos;
		public int codigo;
		public int tam;
	}

	public static class Metodo {
		public int acesso;
		public String nome;
		public String descritor;
		public int inicioCodigo = -1;
		public int tamCodigo;
		public final List<Instrucao> instrucoes = new ArrayList<Instrucao>();
	}

	public byte[] dados;
	public int versaoMenor;
	public int versaoMaior;
	public Constante[] constantes;
	public int acesso;
	public String nomeClasse;
	public String nomeSuper;
	public final List<Metodo> metodos = new ArrayList<Metodo>();

	public AnalisadorClasse(byte[] dados) throws IOException {
		this.dados = dados;
		analisar();
	}

	public class Contador extends ByteArrayInputStream {
		public Contador(byte[] dados) {
			super(dados);
		}

		public int pos() {
			return pos;
		}
	}

	public void analisar() throws IOException {
		final Contador contador = new Contador(dados);
		final DataInputStream entrada = new DataInputStream(contador);

		if(entrada.readInt() != 0xCAFEBABE) throw new IOException("Não é um arquivo .class");
		versaoMenor = entrada.readUnsignedShort();
		versaoMaior = entrada.readUnsignedShort();

		final int qtdConstantes = entrada.readUnsignedShort();
		constantes = new Constante[qtdConstantes];
		for(int i = 1; i < qtdConstantes; i++) {
			final Constante c = new Constante();
			c.tipo = entrada.readUnsignedByte();
			switch(c.tipo) {
				case CONST_TEXTO_UTF:
					c.texto = entrada.readUTF();
					break;
				case CONST_INTEIRO:
				case CONST_DECIMAL:
					c.a = entrada.readInt();
					break;
				case CONST_LONGO:
				case CONST_DUPLO:
					entrada.readLong();
					constantes[i] = c;
					i++;
					continue;
				case CONST_CLASSE:
				case CONST_TEXTO:
				case CONST_METODO_TIPO:
				case CONST_MODULO:
				case CONST_PACOTE:
					c.a = entrada.readUnsignedShort();
					break;
				case CONST_CAMPO:
				case CONST_METODO:
				case CONST_METODO_INTERFACE:
				case CONST_NOME_TIPO:
				case CONST_DINAMICO:
				case CONST_INVOCA_DINAMICO:
					c.a = entrada.readUnsignedShort();
					c.b = entrada.readUnsignedShort();
					break;
				case CONST_METODO_REF:
					c.a = entrada.readUnsignedByte();
					c.b = entrada.readUnsignedShort();
					break;
				default:
					throw new IOException("Tipo de constante desconhecido: " + c.tipo);
			}
			constantes[i] = c;
		}
		acesso = entrada.readUnsignedShort();
		nomeClasse = nomeDaClasse(entrada.readUnsignedShort());
		nomeSuper = nomeDaClasse(entrada.readUnsignedShort());

		final int qtdInterfaces = entrada.readUnsignedShort();
		for(int i = 0; i < qtdInterfaces; i++) entrada.readUnsignedShort();

		final int qtdCampos = entrada.readUnsignedShort();
		for(int i = 0; i < qtdCampos; i++) {
			entrada.readUnsignedShort();
			entrada.readUnsignedShort();
			entrada.readUnsignedShort();
			pularAtributos(entrada);
		}
		final int qtdMetodos = entrada.readUnsignedShort();
		for(int i = 0; i < qtdMetodos; i++) {
			final Metodo m = new Metodo();
			m.acesso = entrada.readUnsignedShort();
			m.nome = texto(entrada.readUnsignedShort());
			m.descritor = texto(entrada.readUnsignedShort());

			final int qtdAtributos = entrada.readUnsignedShort();
			for(int j = 0; j < qtdAtributos; j++) {
				final String nomeAtributo = texto(entrada.readUnsignedShort());
				final int tamAtributo = entrada.readInt();
				if("Code".equals(nomeAtributo)) {
					entrada.readUnsignedShort();
					entrada.readUnsignedShort();
					final int tamCodigo = entrada.readInt();
					m.inicioCodigo = contador.pos();
					m.tamCodigo = tamCodigo;
					final byte[] codigo = new byte[tamCodigo];
					entrada.readFully(codigo);
					lerInstrucoes(codigo, m);
					pular(entrada, tamAtributo - 8 - tamCodigo);
				} else {
					pular(entrada, tamAtributo);
				}
			}
			metodos.add(m);
		}
	}

	public void pularAtributos(DataInputStream entrada) throws IOException {
		final int qtd = entrada.readUnsignedShort();
		for(int i = 0; i < qtd; i++) {
			entrada.readUnsignedShort();
			pular(entrada, entrada.readInt());
		}
	}

	public void pular(DataInputStream entrada, int quantidade) throws IOException {
		int restante = quantidade;
		while(restante > 0) {
			final int pulados = entrada.skipBytes(restante);
			if(pulados <= 0) throw new IOException("Fim inesperado do arquivo");
			restante -= pulados;
		}
	}

	public String texto(int indice) {
		if(indice <= 0 || indice >= constantes.length) return null;
		final Constante c = constantes[indice];
		return c == null ? null : c.texto;
	}

	public String nomeDaClasse(int indice) {
		if(indice <= 0 || indice >= constantes.length) return null;
		final Constante c = constantes[indice];
		if(c == null || c.tipo != CONST_CLASSE) return null;
		return texto(c.a);
	}

	public String[] referenciaMetodo(int indice) {
		final Constante c = constantes[indice];
		if(c == null) return null;
		if(c.tipo != CONST_METODO && c.tipo != CONST_METODO_INTERFACE) return null;
		final Constante nomeTipo = constantes[c.b];
		return new String[] {
			nomeDaClasse(c.a),
			texto(nomeTipo.a),
			texto(nomeTipo.b)
		};
	}

	public void lerInstrucoes(byte[] codigo, Metodo m) throws IOException {
		int i = 0;
		while(i < codigo.length) {
			final Instrucao ins = new Instrucao();
			ins.pos = i;
			ins.codigo = codigo[i] & 0xFF;
			final int tam = tamInstrucao(codigo, i);
			if(tam <= 0 || i + tam > codigo.length) {
				throw new IOException("Instrução inválida na posição " + i + " (código " + ins.codigo + ")");
			}
			ins.tam = tam;
			m.instrucoes.add(ins);
			i += tam;
		}
	}

	public static int tamInstrucao(byte[] c, int i) {
		final int op = c[i] & 0xFF;

		if(op <= 0x0F) return 1;
		if(op == 0x10) return 2;
		if(op == 0x11) return 3;
		if(op == 0x12) return 2;
		if(op == 0x13 || op == 0x14) return 3;
		if(op >= 0x15 && op <= 0x19) return 2;
		if(op >= 0x1A && op <= 0x35) return 1;
		if(op >= 0x36 && op <= 0x3A) return 2;
		if(op >= 0x3B && op <= 0x83) return 1;
		if(op == 0x84) return 3;
		if(op >= 0x85 && op <= 0x98) return 1;
		if(op >= 0x99 && op <= 0xA8) return 3;
		if(op == 0xA9) return 2;
		if(op == 0xAA) return tamTabela(c, i);
		if(op == 0xAB) return tamBusca(c, i);
		if(op >= 0xAC && op <= 0xB1) return 1;
		if(op >= 0xB2 && op <= 0xB8) return 3;
		if(op == 0xB9 || op == 0xBA) return 5;
		if(op == 0xBB) return 3;
		if(op == 0xBC) return 2;
		if(op == 0xBD) return 3;
		if(op == 0xBE || op == 0xBF) return 1;
		if(op == 0xC0 || op == 0xC1) return 3;
		if(op == 0xC2 || op == 0xC3) return 1;
		if(op == 0xC4) return c[i + 1] == (byte) 0x84 ? 6 : 4;
		if(op == 0xC5) return 4;
		if(op == 0xC6 || op == 0xC7) return 3;
		if(op == 0xC8 || op == 0xC9) return 5;
		return -1;
	}

	public static int alinhar(int pos) {
		return (pos + 4) & ~3;
	}

	public static int lerInt(byte[] c, int pos) {
		return ((c[pos] & 0xFF) << 24)
			| ((c[pos + 1] & 0xFF) << 16)
			| ((c[pos + 2] & 0xFF) << 8)
			| (c[pos + 3] & 0xFF);
	}

	public static int tamTabela(byte[] c, int i) {
		final int inicio = alinhar(i);
		final int menor = lerInt(c, inicio + 4);
		final int maior = lerInt(c, inicio + 8);
		final int qtd = maior - menor + 1;
		return inicio + 12 + qtd * 4 - i;
	}

	public static int tamBusca(byte[] c, int i) {
		final int inicio = alinhar(i);
		final int qtd = lerInt(c, inicio + 4);
		return inicio + 8 + qtd * 8 - i;
	}
}
