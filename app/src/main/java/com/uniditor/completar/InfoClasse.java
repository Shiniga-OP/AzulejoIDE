package com.uniditor.completar;

// o que interessa de um .class: quem é, de quem herda e seus campos e métodos
public class InfoClasse {
    public static class Membro {
        public final String nome;
        public final String descritor;
        public final int marcas;
        public final boolean obsoleto;

        public Membro(String nome, String descritor, int marcas, boolean obsoleto) {
            this.nome = nome;
            this.descritor = descritor;
            this.marcas = marcas;
            this.obsoleto = obsoleto;
        }

        public boolean ePublico() {
            return (marcas & 0x0001) != 0;
        }

        public boolean eProtegido() {
            return (marcas & 0x0004) != 0;
        }

        public boolean eEstatico() {
            return (marcas & 0x0008) != 0;
        }

        public boolean eSintetico() {
            return (marcas & 0x1000) != 0;
        }

        // só vale pra método (em campo o mesmo bit é volatile)
        public boolean eBridge() {
            return (marcas & 0x0040) != 0;
        }
    }

    public String nome; // interno: "android/widget/EditText"
    public String superNome;
    public int marcas;
    public String[] interfaces = new String[0];
    public Membro[] campos = new Membro[0];
    public Membro[] metodos = new Membro[0];
}
