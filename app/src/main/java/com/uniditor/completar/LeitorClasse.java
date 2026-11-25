package com.uniditor.completar;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LeitorClasse {
    public static InfoClasse ler(InputStream entrada) throws IOException {
        final DataInputStream in = new DataInputStream(new BufferedInputStream(entrada));
        if(in.readInt() != 0xCAFEBABE) throw new IOException("não é um .class");
        in.readUnsignedShort(); // minor
        in.readUnsignedShort(); // major

        final int n = in.readUnsignedShort();
        final String[] textos = new String[n];
        final int[] nomeClasse = new int[n];
        for(int i = 1; i < n; i++) {
            final int tag = in.readUnsignedByte();
            switch(tag) {
                case 1:
                    textos[i] = in.readUTF();
                    break;
                case 3:
                case 4:
                    in.readInt();
                    break;
                case 5:
                case 6:
                    in.readLong();
                    i++; // long e double ocupam duas posições
                    break;
                case 7:
                    nomeClasse[i] = in.readUnsignedShort();
                    break;
                case 8:
                case 16:
                case 19:
                case 20:
                    in.readUnsignedShort();
                    break;
                case 9:
                case 10:
                case 11:
                case 12:
                case 17:
                case 18:
                    in.readInt();
                    break;
                case 15:
                    in.readUnsignedByte();
                    in.readUnsignedShort();
                    break;
                default:
                    throw new IOException("tag desconhecida: " + tag);
            }
        }
        final InfoClasse info = new InfoClasse();
        info.marcas = in.readUnsignedShort();
        info.nome = textos[nomeClasse[in.readUnsignedShort()]];
        final int sup = in.readUnsignedShort();
        info.superNome = sup == 0 ? null : textos[nomeClasse[sup]];

        final int ni = in.readUnsignedShort();
        info.interfaces = new String[ni];
        for(int i = 0; i < ni; i++) info.interfaces[i] = textos[nomeClasse[in.readUnsignedShort()]];

        info.campos = lerMembros(in, textos);
        info.metodos = lerMembros(in, textos);
        return info;
    }

    public static InfoClasse.Membro[] lerMembros(DataInputStream in, String[] textos) throws IOException {
        final int n = in.readUnsignedShort();
        final InfoClasse.Membro[] r = new InfoClasse.Membro[n];
        for(int i = 0; i < n; i++) {
            final int flags = in.readUnsignedShort();
            final String nome = textos[in.readUnsignedShort()];
            final String descritor = textos[in.readUnsignedShort()];
            boolean obsoleto = false;
            final int atributos = in.readUnsignedShort();
            for(int j = 0; j < atributos; j++) {
                final String nomeAtributo = textos[in.readUnsignedShort()];
                final int tam = in.readInt();
                if("Deprecated".equals(nomeAtributo)) obsoleto = true;
                pular(in, tam);
            }
            r[i] = new InfoClasse.Membro(nome, descritor, flags, obsoleto);
        }
        return r;
    }

    public static void pular(DataInputStream in, int tam) throws IOException {
        while(tam > 0) {
            int pulou = in.skipBytes(tam);
            if(pulou <= 0) {
                in.readByte();
                pulou = 1;
            }
            tam -= pulou;
        }
    }
}
