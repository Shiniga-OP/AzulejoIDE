package com.uniditor.completar;

public class Sugestao {
    public static final int PALAVRA = 0;
    public static final int CLASSE = 1;
    public static final int METODO = 2;
    public static final int CAMPO = 3;

    public final String texto; // o que é inserido(metodo ganha "()" na hora de aceitar)
    public final int tipo;
    public final String detalhe; // mostrado em cinza ao lado (pacote, assinatura...), pode ser null
    public final String importar; // nome completo pra inserir em "import", null = não precisa
    public final boolean temParametros;
    public final boolean obsoleto;

    public Sugestao(String texto, int tipo, String detalhe, String importar, boolean temParametros, boolean obsoleto) {
        this.texto = texto;
        this.tipo = tipo;
        this.detalhe = detalhe;
        this.importar = importar;
        this.temParametros = temParametros;
        this.obsoleto = obsoleto;
    }

    public Sugestao(String texto, int tipo) {
        this(texto, tipo, null, null, false, false);
    }
}
