package com.uniditor.completar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
/*
 * índice das classes de uma lista de jars (android.jar, bibliotecas do projeto)
 * o construtor só lista os nomes (rápido, rodar numa thread); cada .class só é lido
 * quando alguém pede os membros dele, e fica em cache
 * depois de pronto só a thread do editor mexe nele
 */
public class IndiceJars {
    // um nome de membro com todas as suas sobrecargas juntas
    public static class Entrada {
        public String nome;
        public boolean metodo;
        public int nivel; // 0 = da própria classe, cresce a cada herança
        public int minParametros;
        public String descritor; // da sobrecarga com menos parâmetros
        public int sobrecargas;
        public boolean obsoleto = true; // só é obsoleto se todas forem
        public final Set<String> assinaturas = new HashSet<String>();
    }

    public final TreeMap<String, ZipFile> origem = new TreeMap<String, ZipFile>(); // interno -> jar
    public final TreeMap<String, List<String>> porSimples = new TreeMap<String, List<String>>(); // simples em minúsculo -> internos
    public final Map<String, InfoClasse> cache = new HashMap<String, InfoClasse>();
    public final Set<String> falhas = new HashSet<String>();

    public IndiceJars(List<File> jars) {
        for(File jar : jars) {
            if(jar == null || !jar.isFile()) continue;
            try {
                indexar(new ZipFile(jar));
            } catch(IOException e) {
                System.out.println("[ERRO]: ao indexar " + jar.getName() + ": " + e.getMessage());
            }
        }
    }

    public void indexar(ZipFile zip) {
        final Enumeration<? extends ZipEntry> en = zip.entries();
        while(en.hasMoreElements()) {
            final String nome = en.nextElement().getName();
            if(!nome.endsWith(".class")) continue;
            final String interno = nome.substring(0, nome.length() - 6);
            if(!nomeValido(interno) || origem.containsKey(interno)) continue;
            origem.put(interno, zip);
            final String chave = Descritor.simples(interno).toLowerCase();
            List<String> lista = porSimples.get(chave);
            if(lista == null) {
                lista = new ArrayList<String>(2);
                porSimples.put(chave, lista);
            }
            lista.add(interno);
        }
    }

    // descarta anônimas (Foo$1), R$id, package-info e nomes ofuscados: toda parte precisa começar em maiúscula
    public static boolean nomeValido(String interno) {
        final String base = interno.substring(interno.lastIndexOf('/') + 1);
        if(base.isEmpty()) return false;
        for(String parte : base.split("\\$", -1)) {
            if(parte.isEmpty() || !Character.isUpperCase(parte.charAt(0))) return false;
        }
        return true;
    }

    public boolean contem(String interno) {
        return origem.containsKey(interno);
    }

    // classes com esse nome simples exato (pode ter em vários pacotes)
    public List<String> internosDe(String simples) {
        final List<String> r = new ArrayList<String>();
        final List<String> lista = porSimples.get(simples.toLowerCase());
        if(lista == null) return r;
        for(String interno : lista) {
            if(Descritor.simples(interno).equals(simples)) r.add(interno);
        }
        return r;
    }

    // classes cujo nome simples começa com o prefixo (ignora a caixa)
    public List<String> buscarPrefixo(String prefixo) {
        final String minusculo = prefixo.toLowerCase();
        final List<String> r = new ArrayList<String>();
        for(Map.Entry<String, List<String>> par : porSimples.tailMap(minusculo).entrySet()) {
            if(!par.getKey().startsWith(minusculo)) break;
            r.addAll(par.getValue());
            if(r.size() > 300) break;
        }
        return r;
    }

    // nomes das classes internas diretas de "interno"
    public List<String> aninhadas(String interno) {
        final List<String> r = new ArrayList<String>();
        final String base = interno + "$";
        for(String nome : origem.subMap(base, base + "\uffff").keySet()) {
            final String resto = nome.substring(base.length());
            if(resto.indexOf('$') < 0) r.add(resto);
        }
        return r;
    }

    public InfoClasse info(String interno) {
        final InfoClasse guardada = cache.get(interno);
        if(guardada != null) return guardada;
        if(falhas.contains(interno)) return null;

        final ZipFile zip = origem.get(interno);
        if(zip != null) {
            InputStream entrada = null;
            try {
                final ZipEntry e = zip.getEntry(interno + ".class");
                if(e != null) {
                    entrada = zip.getInputStream(e);
                    final InfoClasse info = LeitorClasse.ler(entrada);
                    cache.put(interno, info);
                    return info;
                }
            } catch(IOException e) {
                System.out.println("[ERRO]: ao ler " + interno + ": " + e.getMessage());
            } catch(RuntimeException e) {
                System.out.println("[ERRO]: classe inválida " + interno + ": " + e);
            } finally {
                if(entrada != null) {
                    try {
                        entrada.close();
                    } catch(IOException e) {
                        // ignora
                    }
                }
            }
        }
        falhas.add(interno);
        return null;
    }

    // a classe e tudo que ela herda, em largura, com o nível de cada uma (Object fica por último)
    public Map<String, Integer> hierarquia(String interno) {
        final Map<String, Integer> r = new LinkedHashMap<String, Integer>();
        final LinkedList<String> fila = new LinkedList<String>();
        r.put(interno, 0);
        fila.add(interno);
        while(!fila.isEmpty()) {
            final String atual = fila.removeFirst();
            final InfoClasse info = info(atual);
            if(info == null) continue;
            final int nivel = r.get(atual) + 1;
            if(info.superNome != null && !r.containsKey(info.superNome)) {
                r.put(info.superNome, nivel);
                fila.add(info.superNome);
            }
            for(String interface_ : info.interfaces) {
                if(!r.containsKey(interface_)) {
                    r.put(interface_, nivel);
                    fila.add(interface_);
                }
            }
        }
        if(r.containsKey("java/lang/Object")) r.put("java/lang/Object", 99);
        return r;
    }

    // primeiro membro público com esse nome; entre sobrecargas prefere a de mesma quantidade de argumentos
    public InfoClasse.Membro acharMembro(String interno, String nome, boolean metodo, int nArgs) {
        InfoClasse.Membro qualquer = null;
        for(String classe : hierarquia(interno).keySet()) {
            final InfoClasse info = info(classe);
            if(info == null) continue;
            final InfoClasse.Membro[] lista = metodo ? info.metodos : info.campos;
            for(InfoClasse.Membro m : lista) {
                if(!m.nome.equals(nome) || !m.ePublico() || m.eSintetico()) continue;
                if(metodo && m.eBridge()) continue;
                if(!metodo || nArgs < 0 || Descritor.contarParametros(m.descritor) == nArgs) return m;
                if(qualquer == null) qualquer = m;
            }
        }
        return qualquer;
    }

    // membros públicos acessíveis: estáticos (receptor é uma classe) ou de instância
    public List<Entrada> listarMembros(String interno, boolean estatico) {
        return listarMembros(interno, estatico, false);
    }

    // herdeiro = código de dentro de uma subclasse: vê os protegidos e os estáticos junto com os de instância
    public List<Entrada> listarMembros(String interno, boolean estatico, boolean herdeiro) {
        final Map<String, Entrada> mapa = new HashMap<String, Entrada>();
        for(Map.Entry<String, Integer> par : hierarquia(interno).entrySet()) {
            final InfoClasse info = info(par.getKey());
            if(info == null) continue;
            for(InfoClasse.Membro m : info.metodos) {
                if(!permitido(m, estatico, herdeiro) || m.eBridge()) continue;
                adicionar(mapa, m, true, par.getValue());
            }
            for(InfoClasse.Membro m : info.campos) {
                if(!permitido(m, estatico, herdeiro)) continue;
                adicionar(mapa, m, false, par.getValue());
            }
        }
        return new ArrayList<Entrada>(mapa.values());
    }

    public boolean permitido(InfoClasse.Membro m, boolean estatico, boolean herdeiro) {
        if(m.eSintetico() || m.nome.charAt(0) == '<') return false;
        if(herdeiro) return m.ePublico() || m.eProtegido();
        return m.ePublico() && m.eEstatico() == estatico;
    }

    public void adicionar(Map<String, Entrada> mapa, InfoClasse.Membro m, boolean metodo, int nivel) {
        final String chave = (metodo ? "m:" : "c:") + m.nome;
        Entrada e = mapa.get(chave);
        if(e == null) {
            e = new Entrada();
            e.nome = m.nome;
            e.metodo = metodo;
            e.nivel = nivel;
            mapa.put(chave, e);
        } else if(nivel < e.nivel) {
            e.nivel = nivel;
        }
        if(!m.obsoleto) e.obsoleto = false;
        final int np = metodo ? Descritor.contarParametros(m.descritor) : 0;
        // sobrescrita em subclasse tem a mesma assinatura: não conta como sobrecarga
        final String assinatura = metodo ? m.descritor.substring(0, m.descritor.indexOf(')') + 1) : "";
        if(e.assinaturas.add(assinatura)) e.sobrecargas++;
        if(e.descritor == null || np < e.minParametros) {
            e.minParametros = np;
            e.descritor = m.descritor;
        }
    }
}
