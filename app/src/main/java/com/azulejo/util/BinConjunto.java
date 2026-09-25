package com.azulejo.util;

import java.io.DataOutputStream;
import java.util.Map;
import java.io.IOException;
import java.util.HashMap;
import java.io.DataInputStream;
import java.util.List;
import java.util.ArrayList;

public class BinConjunto {
	public Map<String, Object> valores = new HashMap<>();

	public void defBoolean(String chave, boolean valor) {
        valores.put(chave, valor);
    }

	public boolean lerBoolean(String chave) {
        final Boolean valor = (Boolean)valores.get(chave);
        return valor != null ? valor.booleanValue() : false;
    }

	public void defLong(String chave, long valor) {
        valores.put(chave, valor);
    }

	public long lerLong(String chave) {
        final Long valor = (Long)valores.get(chave);
        return valor != null ? valor.longValue() : 0L;
    }

	public void defDouble(String chave, double valor) {
        valores.put(chave, valor);
    }

	public double lerDouble(String chave) {
        final Double valor = (Double)valores.get(chave);
        return valor != null ? valor.doubleValue() : 0D;
    }

    public void defFloat(String chave, float valor) {
        valores.put(chave, valor);
    }

    public float lerFloat(String chave) {
        final Float valor = (Float)valores.get(chave);
        return valor != null ? valor.floatValue() : 0f;
    }

    public void defInt(String chave, int valor) {
        valores.put(chave, valor);
    }

    public int lerInt(String chave) {
        final Integer valor = (Integer)valores.get(chave);
        return valor != null ? valor.intValue() : 0;
    }

    public void defString(String chave, String valor) {
        valores.put(chave, valor);
    }

    public String lerString(String chave) {
        final Object valor = valores.get(chave);
        return valor != null ? (String)valor : "";
    }

    public void defConjunto(String chave, BinConjunto valor) {
        valores.put(chave, valor);
    }

    public BinConjunto lerConjunto(String chave) {
        return (BinConjunto)valores.get(chave);
    }

    public boolean temChave(String chave) {
        return valores.containsKey(chave);
    }

    public void defIntArr(String chave, int[] valor) {
        valores.put(chave, valor);
    }

    public int[] lerIntArr(String chave) {
        final int[] valor = (int[])valores.get(chave);
        return valor != null ? valor : new int[0];
    }

    public void defShortArr(String chave, short[] valor) {
        valores.put(chave, valor);
    }

    public short[] lerShortArr(String chave) {
        final short[] valor = (short[])valores.get(chave);
        return valor != null ? valor : new short[0];
    }

    public void defByteArr(String chave, byte[] valor) {
        valores.put(chave, valor);
    }

    public byte[] lerByteArr(String chave) {
        final byte[] valor = (byte[])valores.get(chave);
        return valor != null ? valor : new byte[0];
    }

    public void defLongArr(String chave, long[] valor) {
        valores.put(chave, valor);
    }

    public long[] lerLongArr(String chave) {
        final long[] valor = (long[])valores.get(chave);
        return valor != null ? valor : new long[0];
    }

    // lista de conjuntos: uso pontual, nao pra dados em massa
    public void defLista(String chave, List<BinConjunto> valor) {
        valores.put(chave, valor);
    }

    public List<BinConjunto> lerLista(String chave) {
        final Object valor = valores.get(chave);
        return valor != null ? (List<BinConjunto>)valor : new ArrayList<BinConjunto>();
    }

	public void salvar(DataOutputStream saida) throws IOException {
		saida.writeInt(valores.size());
		for(Map.Entry<String, Object> par : valores.entrySet()) {
			final Object valor = par.getValue();
			saida.writeUTF(par.getKey());
			if(valor instanceof Float) {
				saida.writeByte(1);
				saida.writeFloat(((Float)valor).floatValue());
			} else if(valor instanceof Integer) {
				saida.writeByte(2);
				saida.writeInt(((Integer)valor).intValue());
			} else if(valor instanceof Long) {
				saida.writeByte(3);
				saida.writeLong(((Long)valor).longValue());
			} else if(valor instanceof Double) {
				saida.writeByte(4);
				saida.writeDouble(((Double)valor).doubleValue());
			} else if(valor instanceof Boolean) {
				saida.writeByte(5);
				saida.writeBoolean((Boolean)valor);
			} else if(valor instanceof String) {
				saida.writeByte(6);
				saida.writeUTF((String)valor);
			} else if(valor instanceof BinConjunto) {
				saida.writeByte(7);
				((BinConjunto)valor).salvar(saida);
			} else if(valor instanceof int[]) {
				saida.writeByte(8);
				final int[] arr = (int[])valor;
				saida.writeInt(arr.length);
				for(int i = 0; i < arr.length; i++) saida.writeInt(arr[i]);
			} else if(valor instanceof short[]) {
				saida.writeByte(9);
				final short[] arr = (short[])valor;
				saida.writeInt(arr.length);
				for(int i = 0; i < arr.length; i++) saida.writeShort(arr[i]);
			} else if(valor instanceof byte[]) {
				saida.writeByte(10);
				final byte[] arr = (byte[])valor;
				saida.writeInt(arr.length);
				saida.write(arr);
			} else if(valor instanceof long[]) {
				saida.writeByte(11);
				final long[] arr = (long[])valor;
				saida.writeInt(arr.length);
				for(int i = 0; i < arr.length; i++) saida.writeLong(arr[i]);
			} else if(valor instanceof List) {
				saida.writeByte(12);
				final List<BinConjunto> lista = (List<BinConjunto>)valor;
				saida.writeInt(lista.size());
				for(int i = 0; i < lista.size(); i++) lista.get(i).salvar(saida);
			}
		}
	}

	public void carregar(DataInputStream entrada) throws IOException {
		final int tam = entrada.readInt();
		for(int i = 0; i < tam; i++) {
			final String chave = entrada.readUTF();
			final int tipo = entrada.readByte();
			if(tipo == 1) {
				valores.put(chave, entrada.readFloat());
			} else if(tipo == 2) {
				valores.put(chave, entrada.readInt());
			} else if(tipo == 3) {
				valores.put(chave, entrada.readLong());
			} else if(tipo == 4) {
				valores.put(chave, entrada.readDouble());
			} else if(tipo == 5) {
				valores.put(chave, entrada.readBoolean());
			} else if(tipo == 6) {
				valores.put(chave, entrada.readUTF());
			} else if(tipo == 7) {
				final BinConjunto b = new BinConjunto();
				b.carregar(entrada);
				valores.put(chave, b);
			} else if(tipo == 8) {
				final int n = entrada.readInt();
				final int[] arr = new int[n];
				for(int j = 0; j < n; j++) arr[j] = entrada.readInt();
				valores.put(chave, arr);
			} else if(tipo == 9) {
				final int n = entrada.readInt();
				final short[] arr = new short[n];
				for(int j = 0; j < n; j++) arr[j] = entrada.readShort();
				valores.put(chave, arr);
			} else if(tipo == 10) {
				final int n = entrada.readInt();
				final byte[] arr = new byte[n];
				entrada.readFully(arr);
				valores.put(chave, arr);
			} else if(tipo == 11) {
				final int n = entrada.readInt();
				final long[] arr = new long[n];
				for(int j = 0; j < n; j++) arr[j] = entrada.readLong();
				valores.put(chave, arr);
			} else if(tipo == 12) {
				final int n = entrada.readInt();
				final List<BinConjunto> lista = new ArrayList<>(n);
				for(int j = 0; j < n; j++) {
					final BinConjunto b = new BinConjunto();
					b.carregar(entrada);
					lista.add(b);
				}
				valores.put(chave, lista);
			}
		}
	}

	public void reutilizar() {
		valores.clear();
	}
}
