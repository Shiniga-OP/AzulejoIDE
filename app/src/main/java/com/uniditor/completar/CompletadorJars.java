package com.uniditor.completar;

import com.uniditor.Buffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * depois de "." lista os membros do tipo da variável (ou os estáticos da classe) lendo o índice dos jars
 * sem ponto, completa com palavras do arquivo e depois com nomes de classe do índice (com pacote e import)
 * enquanto "indice" for null (ainda sendo construído) cai no completador de palavras
 */
public class CompletadorJars implements Completador {
    public final Completador palavras;
    public volatile IndiceJars indice;

    public CompletadorJars(Completador palavras) {
        this.palavras = palavras;
    }

    @Override
    public List<Sugestao> sugerir(Buffer buffer, int linha, int coluna, String prefixo, int max) {
        final IndiceJars ij = indice;
        if(ij == null) return palavras.sugerir(buffer, linha, coluna, prefixo, max);

        final String conteudo = buffer.linha(linha);
        final int ponto = coluna - prefixo.length() - 1;
        if(ponto < 0 || ponto >= conteudo.length() || conteudo.charAt(ponto) != '.') {
            return sugerirNomes(ij, buffer, linha, coluna, prefixo, max);
        }
        int ini = ponto;
        while(ini > 0 && CompletadorPalavras.eIdentificador(conteudo.charAt(ini - 1))) ini--;
        final String receptor = conteudo.substring(ini, ponto);
        if(receptor.isEmpty() || Character.isDigit(receptor.charAt(0))) return new ArrayList<Sugestao>();

        List<Sugestao> membros = null;
        final boolean encadeado = ini > 0 && conteudo.charAt(ini - 1) == '.';
        if(!encadeado) {
            if(receptor.equals("this") || receptor.equals("super")) membros = herdados(ij, buffer, linha);
            else membros = membrosDe(ij, buffer, linha, receptor);
        }
        if(membros == null) {
            if(prefixo.length() < 2) return new ArrayList<Sugestao>();
            return palavras.sugerir(buffer, linha, coluna, prefixo, max);
        }
        return filtrar(membros, prefixo, max);
    }

    // palavras do arquivo primeiro; começando em maiúscula, entram as classes do índice
    // uma palavra do arquivo que também é classe vira sugestão de classe (ganha pacote e import)
    public List<Sugestao> sugerirNomes(IndiceJars ij, Buffer buffer, int linha, int coluna, String prefixo, int max) {
        final List<Sugestao> r = new ArrayList<Sugestao>(palavras.sugerir(buffer, linha, coluna, prefixo, max));
        final List<Sugestao> herdados = herdados(ij, buffer, linha);
        if(herdados != null && !prefixo.isEmpty()) misturar(r, filtrar(herdados, prefixo, max), max);
        if(prefixo.isEmpty() || !Character.isUpperCase(prefixo.charAt(0))) return r;

        final Declaracoes.Imports imports = Declaracoes.lerImports(buffer);
        final List<Sugestao> classes = new ArrayList<Sugestao>();
        for(String interno : ij.buscarPrefixo(prefixo)) {
            if(interno.indexOf('$') >= 0) continue;
            final String simples = Descritor.simples(interno);
            if(simples.length() <= prefixo.length() || !simples.startsWith(prefixo)) continue;
            classes.add(new Sugestao(simples, Sugestao.CLASSE, pacoteDe(interno), importar(interno, imports), false, false));
        }
        Collections.sort(classes, new Comparator<Sugestao>() {
				@Override
				public int compare(Sugestao a, Sugestao b) {
					if(a.texto.length() != b.texto.length()) return a.texto.length() - b.texto.length();
					final int nome = a.texto.compareTo(b.texto);
					if(nome != 0) return nome;
					// mesmo nome em pacotes diferentes: a que já é visível sem import primeiro
					if((a.importar == null) != (b.importar == null)) return a.importar == null ? -1 : 1;
					return a.detalhe.compareTo(b.detalhe);
				}
			});
        for(Sugestao classe : classes) {
            final int i = indiceDe(r, classe.texto);
            if(i >= 0 && r.get(i).tipo == Sugestao.PALAVRA) {
                r.set(i, classe);
                continue;
            }
            if(r.size() < max) r.add(classe);
        }
        return r;
    }

    // o que o código da classe enxerga sem receptor: tudo que vem do "extends" (públicos e protegidos)
    public List<Sugestao> herdados(IndiceJars ij, Buffer buffer, int linha) {
        final String tipo = Declaracoes.superclasse(buffer, linha);
        if(tipo == null) return null;
        final String interno = resolver(ij, buffer, tipo);
        if(interno == null) return null;
        return listar(ij, interno, false, true);
    }

    // palavra do arquivo que é membro herdado vira o membro (ganha "()" e assinatura);
    // metade da lista fica reservada pros membros que ainda não aparecem no arquivo
    public void misturar(List<Sugestao> r, List<Sugestao> membros, int max) {
        final List<Sugestao> novos = new ArrayList<Sugestao>();
        for(Sugestao m : membros) {
            final int i = indiceDe(r, m.texto);
            if(i < 0) novos.add(m);
            else if(r.get(i).tipo == Sugestao.PALAVRA) r.set(i, m);
        }
        final int vagas = Math.min(novos.size(), max / 2);
        while(r.size() > max - vagas) r.remove(r.size() - 1);
        for(Sugestao m : novos) {
            if(r.size() >= max) break;
            r.add(m);
        }
    }

    public int indiceDe(List<Sugestao> lista, String texto) {
        for(int i = 0; i < lista.size(); i++) {
            if(lista.get(i).texto.equals(texto)) return i;
        }
        return -1;
    }

    // "android/widget/EditText" -> "android.widget"
    public String pacoteDe(String interno) {
        final int p = interno.lastIndexOf('/');
        return p < 0 ? "" : interno.substring(0, p).replace('/', '.');
    }

    // nome completo pra pôr em "import", ou null se a classe já é visível sem import
    public String importar(String interno, Declaracoes.Imports imports) {
        final String pacote = pacoteDe(interno);
        if(pacote.isEmpty() || pacote.equals("java.lang") || pacote.equals(imports.pacote)) return null;
        final String nome = Descritor.nomeFonte(interno);
        if(imports.exatos.contains(nome) || imports.curingas.contains(pacote + ".")) return null;
        return nome;
    }

    public List<Sugestao> membrosDe(IndiceJars ij, Buffer buffer, int linha, String receptor) {
        String tipo = Declaracoes.acharTipo(buffer, linha, receptor);
        boolean estatico = false;
        if(tipo == null) {
            // não é variável: se começa com maiúscula, trata como nome de classe
            if(!Character.isUpperCase(receptor.charAt(0))) return null;
            tipo = receptor;
            estatico = true;
        }
        if(tipo.equals("[]")) {
            final List<Sugestao> r = new ArrayList<Sugestao>();
            r.add(new Sugestao("length", Sugestao.CAMPO, "int", null, false, false));
            r.add(new Sugestao("clone", Sugestao.METODO, "() Object", null, false, false));
            return r;
        }
        final String interno = resolver(ij, buffer, tipo);
        if(interno == null) return null;
        return listar(ij, interno, estatico, false);
    }

    // própria classe antes das herdadas, não obsoletos antes dos obsoletos
    public List<Sugestao> listar(IndiceJars ij, String interno, boolean estatico, boolean herdeiro) {
        final List<IndiceJars.Entrada> entradas = ij.listarMembros(interno, estatico, herdeiro);
        Collections.sort(entradas, new Comparator<IndiceJars.Entrada>() {
				@Override
				public int compare(IndiceJars.Entrada a, IndiceJars.Entrada b) {
					if(a.obsoleto != b.obsoleto) return a.obsoleto ? 1 : -1;
					if(a.nivel != b.nivel) return a.nivel - b.nivel;
					return a.nome.compareToIgnoreCase(b.nome);
				}
			});
        final List<Sugestao> r = new ArrayList<Sugestao>();
        final Set<String> vistos = new HashSet<String>();
        for(IndiceJars.Entrada e : entradas) {
            if(!vistos.add(e.nome)) continue;
            if(e.metodo) r.add(new Sugestao(e.nome, Sugestao.METODO, assinatura(e), null, e.minParametros > 0, e.obsoleto));
            else r.add(new Sugestao(e.nome, Sugestao.CAMPO, Descritor.nomeTipo(e.descritor), null, false, e.obsoleto));
        }
        if(estatico) {
            final String dono = Descritor.simples(interno);
            for(String interna : ij.aninhadas(interno)) {
                if(vistos.add(interna)) r.add(new Sugestao(interna, Sugestao.CLASSE, dono, null, false, false));
            }
        }
        return r;
    }

    // "(int, String) void" e "+2" quando há outras sobrecargas
    public String assinatura(IndiceJars.Entrada e) {
        final StringBuilder sb = new StringBuilder("(");
        final List<String> params = Descritor.parametros(e.descritor);
        for(int i = 0; i < params.size(); i++) {
            if(i > 0) sb.append(", ");
            sb.append(Descritor.nomeTipo(params.get(i)));
        }
        sb.append(") ").append(Descritor.nomeTipo(Descritor.retorno(e.descritor)));
        if(e.sobrecargas > 1) sb.append(" +").append(e.sobrecargas - 1);
        return sb.toString();
    }

    public List<Sugestao> filtrar(List<Sugestao> lista, String prefixo, int max) {
        final List<Sugestao> achadas = new ArrayList<Sugestao>();
        final int tam = prefixo.length();
        // primeiro a mesma caixa digitada, depois ignorando a caixa
        for(Sugestao s : lista) {
            if(achadas.size() >= max) return achadas;
            if(s.texto.length() > tam && s.texto.startsWith(prefixo)) achadas.add(s);
        }
        for(Sugestao s : lista) {
            if(achadas.size() >= max) break;
            if(s.texto.length() > tam && !s.texto.startsWith(prefixo) && s.texto.regionMatches(true, 0, prefixo, 0, tam)) achadas.add(s);
        }
        return achadas;
    }

    // imports exatos primeiro, depois java.lang, depois os imports com *
    public String resolver(IndiceJars ij, Buffer buffer, String tipo) {
        if(tipo.indexOf('.') >= 0) return internoDeNome(ij, tipo);

        final Declaracoes.Imports imports = Declaracoes.lerImports(buffer);
        for(String exato : imports.exatos) {
            if(!exato.endsWith("." + tipo)) continue;
            final String interno = internoDeNome(ij, exato);
            if(interno != null) return interno;
        }
        final String lang = internoDeNome(ij, "java.lang." + tipo);
        if(lang != null) return lang;
        for(String pacote : imports.curingas) {
            final String interno = internoDeNome(ij, pacote + tipo);
            if(interno != null) return interno;
        }
        return null;
    }

    // "android.view.View.OnClickListener" -> "android/view/View$OnClickListener"
    public String internoDeNome(IndiceJars ij, String nome) {
        String tentativa = nome.replace('.', '/');
        while(true) {
            if(ij.contem(tentativa)) return tentativa;
            final int p = tentativa.lastIndexOf('/');
            if(p < 0) return null;
            tentativa = tentativa.substring(0, p) + "$" + tentativa.substring(p + 1);
        }
    }
}
