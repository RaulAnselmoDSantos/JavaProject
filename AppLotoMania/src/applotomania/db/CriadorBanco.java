package applotomania.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Classe responsável pela criação e manutenção das tabelas do banco de dados
 * Versão corrigida para garantir compatibilidade com todas as funcionalidades
 * 
 * @author Raul 
 */
public class CriadorBanco {
    
    public static void criarTabelas() {
        String sqlJogadores = """
            CREATE TABLE IF NOT EXISTS jogadores (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT NOT NULL,
                telefone TEXT
            );
            """;
        String sqlGruposFixos = """
            CREATE TABLE IF NOT EXISTS grupos_fixos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT NOT NULL UNIQUE
            );
            """;
        // Removido NOT NULL da coluna 'nome' para compatibilidade
        String sqlJogosFixos = """
            CREATE TABLE IF NOT EXISTS jogos_fixos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                grupo_id INTEGER NOT NULL,
                jogador_id INTEGER NOT NULL,
                nome TEXT,
                numeros TEXT NOT NULL,
                jogo_espelho_id INTEGER,
                FOREIGN KEY (grupo_id) REFERENCES grupos_fixos(id),
                FOREIGN KEY (jogador_id) REFERENCES jogadores(id),
                UNIQUE (grupo_id, numeros)
            );
            """;
        String idxJogosFixosNumeros = "CREATE INDEX IF NOT EXISTS idx_jogos_fixos_numeros ON jogos_fixos(grupo_id, numeros);";
        String sqlJogosEspelho = """
            CREATE TABLE IF NOT EXISTS jogos_espelho (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                jogo_base_id INTEGER NOT NULL UNIQUE,
                grupo_id INTEGER NOT NULL,
                numeros TEXT NOT NULL,
                hash TEXT,
                FOREIGN KEY (jogo_base_id) REFERENCES jogos_fixos(id),
                FOREIGN KEY (grupo_id) REFERENCES grupos_fixos(id),
                UNIQUE (grupo_id, numeros)
            );
            """;
        String idxJogosEspelhoNumeros = "CREATE INDEX IF NOT EXISTS idx_jogos_espelho_grupo_numeros ON jogos_espelho(grupo_id, numeros);";
        String idxJogosEspelhoBase = "CREATE INDEX IF NOT EXISTS idx_jogos_espelho_base ON jogos_espelho(jogo_base_id);";
        String sqlConcursos = """
            CREATE TABLE IF NOT EXISTS concursos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                descricao TEXT NOT NULL UNIQUE,
                numeros TEXT
            );
            """;
        // *** CORREÇÃO: Adicionado colunas jogo_fixo_id e espelho ***
        String sqlJogos = """
            CREATE TABLE IF NOT EXISTS jogos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                concurso_id INTEGER NOT NULL,
                jogador_id INTEGER NOT NULL,
                jogo_fixo_id INTEGER NOT NULL,
                numeros TEXT NOT NULL,
                acertos INTEGER DEFAULT -1,
                espelho BOOLEAN DEFAULT 0,
                FOREIGN KEY (concurso_id) REFERENCES concursos(id),
                FOREIGN KEY (jogador_id) REFERENCES jogadores(id),
                FOREIGN KEY (jogo_fixo_id) REFERENCES jogos_fixos(id),
                UNIQUE (concurso_id, numeros, jogador_id)
            );
            """;
        String idxJogosConcurso = "CREATE INDEX IF NOT EXISTS idx_jogos_concurso ON jogos(concurso_id);";
        String idxJogosFixo = "CREATE INDEX IF NOT EXISTS idx_jogos_fixo ON jogos(jogo_fixo_id);";
        
        // *** CORREÇÃO: Adicionado tabela jogos_hash para otimização ***
        String sqlJogosHash = """
            CREATE TABLE IF NOT EXISTS jogos_hash (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                grupo_id INTEGER NOT NULL,
                jogo_id INTEGER NOT NULL,
                hash TEXT NOT NULL,
                numeros TEXT NOT NULL,
                FOREIGN KEY (grupo_id) REFERENCES grupos_fixos(id),
                UNIQUE (grupo_id, hash)
            );
            """;
        String idxJogosHashGrupo = "CREATE INDEX IF NOT EXISTS idx_jogos_hash_grupo ON jogos_hash(grupo_id);";
        String idxJogosHashHash = "CREATE INDEX IF NOT EXISTS idx_jogos_hash_hash ON jogos_hash(hash);";
        
        try (Connection conn = Conexao.conectar();
             Statement stmt = conn.createStatement()) {
            System.out.println("Criando/Verificando tabelas...");
            
            // Verificar se a tabela jogos existe e se tem as colunas necessárias
            boolean recriarTabelaJogos = false;
            try {
                ResultSet rs = stmt.executeQuery("PRAGMA table_info(jogos)");
                boolean temJogoFixoId = false;
                boolean temEspelho = false;
                
                while (rs.next()) {
                    String colName = rs.getString("name");
                    if ("jogo_fixo_id".equals(colName)) temJogoFixoId = true;
                    else if ("espelho".equals(colName)) temEspelho = true;
                }
                if (!temJogoFixoId || !temEspelho) recriarTabelaJogos = true;
                rs.close();
            } catch (SQLException e) {
                recriarTabelaJogos = false;
            }
            
            // Preservar jogos_fixos existentes
            try {
                ResultSet rs = stmt.executeQuery("SELECT 1 FROM jogos_fixos LIMIT 1");
                rs.close();
                System.out.println("Tabela jogos_fixos existente preservada.");
            } catch (SQLException e) {
                System.out.println("Tabela jogos_fixos não existe, será criada.");
            }
            
            if (recriarTabelaJogos) {
                stmt.execute("DROP TABLE IF EXISTS jogos;");
                System.out.println("Tabela jogos antiga removida para adicionar colunas necessárias.");
            }
            
            // Verificar e popular ou recriar jogos_hash
            try {
                ResultSet rs = stmt.executeQuery("PRAGMA table_info(jogos_hash)");
                boolean temJogoId = false, temHash = false, temNumeros = false;
                while (rs.next()) {
                    String col = rs.getString("name");
                    if ("jogo_id".equals(col)) temJogoId = true;
                    if ("hash".equals(col)) temHash = true;
                    if ("numeros".equals(col)) temNumeros = true;
                }
                if (temJogoId && temHash && temNumeros) {
                    System.out.println("Tabela de hash criada com índices otimizados.");
                    rs = stmt.executeQuery("SELECT COUNT(*) FROM jogos_hash");
                    if (rs.next() && rs.getInt(1) == 0) {
                        stmt.execute("INSERT INTO jogos_hash (grupo_id, jogo_id, hash, numeros) SELECT grupo_id, id, hex(randomblob(16)), numeros FROM jogos_fixos");
                        System.out.println("Tabela de hash populada com sucesso.");
                    }
                } else {
                    stmt.execute("DROP TABLE IF EXISTS jogos_hash;");
                    System.out.println("Tabela jogos_hash antiga removida para corrigir estrutura.");
                }
                rs.close();
            } catch (SQLException e) {
                System.out.println("Erro ao verificar/popular jogos_hash: " + e.getMessage());
            }
            
            // Criar tabelas principais
            stmt.execute(sqlJogadores);
            stmt.execute(sqlGruposFixos);
            stmt.execute(sqlJogosFixos);
            stmt.execute(idxJogosFixosNumeros);
            stmt.execute(sqlJogosEspelho);
            stmt.execute(idxJogosEspelhoNumeros);
            stmt.execute(idxJogosEspelhoBase);
            stmt.execute(sqlConcursos);
            stmt.execute(sqlJogos);
            stmt.execute(idxJogosConcurso);
            stmt.execute(idxJogosFixo);
            
            // Criar tabela de hash e índices básicos
            stmt.execute(sqlJogosHash);
            stmt.execute(idxJogosHashGrupo);
            stmt.execute(idxJogosHashHash);
            
            // Índices únicos de hash
            stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_jogos_hash_grupo_hash ON jogos_hash(grupo_id, hash);");
            stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_jogos_espelho_grupo_hash ON jogos_espelho(grupo_id, hash);");
            
            // Índice composto para acelerar agrupamento por acertos
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_jogos_concurso_acertos ON jogos(concurso_id, acertos);");
            
            System.out.println("Tabelas prontas com otimizações.");
        } catch (SQLException e) {
            System.err.println("Erro ao criar tabelas: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        criarTabelas();
    }
}