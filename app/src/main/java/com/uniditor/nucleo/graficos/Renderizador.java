package com.uniditor.nucleo.graficos;

import com.uniditor.nucleo.sintaxe.Token;
import java.io.File;

public interface Renderizador {
	public int altura, largura;
	public boolean pause;
    // === ciclo de vida ===
    void liberar();
    void iniciarQuadro(); // chamado no inicio de cada qudro
    void fimQuadro(); // chamado no fim
	void defAPI(Object renderizacao);

    // === fundo ===
    void limpar(int cor);

    // === texto ===
	void defTextoTam(float tamanho);
	void defTabTam(int espacos);
    void defFonte(File fonte);
    void defCorTexto(int cor);
    void renderTexto(String texto, float x, float y);
    void renderTextoCor(String texto, Token[] cores, float x, float y);

    // === formas ===
    void renderRetangulo(float x, float y, float largura, float altura, int cor);
    void renderRetanguloContorno(float x, float y, float largura, float altura, int cor, float espessura);

    // === metricas(necessario pra VisaoEditor calcular tela) ===
    float larguraTexto(String texto);
    float larguraCaractere(char c);
    float alturaLinha();
    float ascente();
    float descente();

    // === recorte/rolamento ===
    void addRecorte(float x, float y, float largura, float altura);
    void subRecorte();
    void defPos(float dx, float dy); // translação global(rolamento)

    // === dimensões da superficie ===
	void ajustar(int altura, int largura);
    int larguraTela();
    int alturaTela();
}
