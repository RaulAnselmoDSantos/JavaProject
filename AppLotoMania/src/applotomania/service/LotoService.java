package applotomania.service;

import applotomania.db.Conexao;
import applotomania.model.Concurso;
import applotomania.model.Jogador;
import applotomania.model.GrupoFixo;
import applotomania.model.JogoFixo;
import applotomania.util.HashUtil;
import applotomania.util.GrupoCache;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Classe de serviço para operações relacionadas à loteria
 * Corrigida para compatibilidade com todas as interfaces
 * CORREÇÃO FINAL: Removidas todas as referências à coluna 'espelho' em jogos_fixos
 * 
 * @author Raul 
 */
public class LotoService {
    
    // Constantes
    public static final String JOGADOR_VICENTE_ID = "1";
    private final GrupoCache grupoCache = new GrupoCache();
    
    public LotoService() {
        // Garante que as tabelas e o jogador padrão existam ao instanciar
        applotomania.db.CriadorBanco.criarTabelas();
    }
    
    /**
     * Cria um novo grupo fixo
     * Método necessário para PainelCadastrarJogo
     */
    public boolean criarGrupoFixo(String nomeGrupo) {
        if (nomeGrupo == null || nomeGrupo.trim().isEmpty()) {
            return false;
        }
        
        try (Connection conn = Conexao.conectar()) {
            String sql = "INSERT INTO grupos_fixos (nome) VALUES (?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, nomeGrupo.trim());
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                // Limpar cache para forçar recarga
                grupoCache.limparCache();
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Erro ao criar grupo fixo: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Obtém a lista de grupos fixos
     * Método necessário para PainelCadastrarJogo e PainelConcursosUnificado
     */
    public List<GrupoFixo> getGruposFixosList() {
        List<GrupoFixo> grupos = new ArrayList<>();
        
        try (Connection conn = Conexao.conectar()) {
            String sql = "SELECT id, nome FROM grupos_fixos ORDER BY id";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                GrupoFixo grupo = new GrupoFixo();
                grupo.setId(rs.getInt("id"));
                grupo.setNome(rs.getString("nome"));
                grupos.add(grupo);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar grupos fixos: " + e.getMessage());
        }
        
        return grupos;
    }
    
    /**
     * Obtém a quantidade de jogos por grupo
     * Método necessário para PainelCadastrarJogo
     */
    public int getQuantidadeJogosPorGrupo(int grupoId) {
        // Primeiro tenta obter do cache
        Integer qtdCache = grupoCache.getQuantidadeJogos(grupoId);
        if (qtdCache != null) {
            System.out.println("[DEBUG] Quantidade obtida do cache: " + qtdCache);
            return qtdCache;
        }
        
        // Se não estiver no cache, consulta o banco
        try (Connection conn = Conexao.conectar()) {
            String sql = "SELECT COUNT(*) as qtd FROM jogos_fixos WHERE grupo_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, grupoId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                int qtd = rs.getInt("qtd");
                // Atualiza o cache
                grupoCache.atualizarQuantidadeJogos(grupoId, qtd);
                System.out.println("[DEBUG] Quantidade retornada: " + qtd);
                return qtd;
            }
        } catch (SQLException e) {
            System.err.println("Erro ao obter quantidade de jogos: " + e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Adiciona um jogo fixo individual com suporte a espelho
     * Método compatível com a interface PainelCadastrarJogo
     * CORRIGIDO: Removida referência à coluna 'espelho' em jogos_fixos
     */
    public long adicionarJogoFixoIndividual(int grupoId, String numerosInput, boolean cadastrarEspelho) {
        // Validação e formatação do jogo
        List<Integer> dezenasBase = validarEFormatarJogo(numerosInput);
        if (dezenasBase == null) {
            return -1; // Erro de validação de formato
        }
        
        String numerosBaseFormatados = formatarDezenasParaString(dezenasBase);
        
        Connection conn = null;
        long jogoBaseId = -3;
        
        try {
            conn = Conexao.conectar();
            conn.setAutoCommit(false); // Iniciar transação
            
            // Verificar duplicidade
            if (verificarJogoDuplicado(conn, grupoId, numerosBaseFormatados)) {
                System.err.println("Erro: Jogo base já existe neste grupo.");
                conn.rollback();
                return -2; // Jogo duplicado
            }
            
            // Calcular hash para o jogo base
            String hashBase = HashUtil.calcularHashMD5(numerosBaseFormatados);
            
            // Verificar duplicidade por hash
            if (verificarHashDuplicado(conn, grupoId, hashBase)) {
                System.out.println("[DEBUG] Hash duplicado encontrado para jogo base: " + hashBase);
                System.out.println("[DEBUG] Números do jogo: " + numerosBaseFormatados);
                conn.rollback();
                return -2; // Hash duplicado
            }
            
            // Inserir jogo base - CORRIGIDO: Removida coluna 'espelho'
            String sqlInsertBase = "INSERT INTO jogos_fixos (grupo_id, jogador_id, numeros) VALUES (?, ?, ?)";
            try (PreparedStatement stmtInsertBase = conn.prepareStatement(sqlInsertBase, Statement.RETURN_GENERATED_KEYS)) {
                stmtInsertBase.setInt(1, grupoId);
                stmtInsertBase.setInt(2, Integer.parseInt(JOGADOR_VICENTE_ID)); // Jogador padrão
                stmtInsertBase.setString(3, numerosBaseFormatados);
                stmtInsertBase.executeUpdate();
                ResultSet rsKeys = stmtInsertBase.getGeneratedKeys();
                if (rsKeys.next()) {
                    jogoBaseId = rsKeys.getLong(1);
                } else {
                    throw new SQLException("Falha ao obter ID do jogo base inserido.");
                }
            }
            
            // Inserir na tabela de hash
            String sqlInsertHash = "INSERT OR IGNORE INTO jogos_hash (grupo_id, jogo_id, hash, numeros) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmtInsertHash = conn.prepareStatement(sqlInsertHash)) {
                stmtInsertHash.setInt(1, grupoId);
                stmtInsertHash.setLong(2, jogoBaseId);
                stmtInsertHash.setString(3, hashBase);
                stmtInsertHash.setString(4, numerosBaseFormatados);
                stmtInsertHash.executeUpdate();
            }
            
            // Inserir jogo espelho (se aplicável)
            if (cadastrarEspelho) {
                // Calcular espelho
                List<Integer> dezenasEspelho = calcularDezenasEspelho(dezenasBase);
                if (dezenasEspelho == null) {
                    System.err.println("Erro interno ao calcular jogo espelho.");
                    conn.rollback();
                    return -1; // Erro interno
                }
                String numerosEspelhoFormatados = formatarDezenasParaString(dezenasEspelho);
                
                // Verificar duplicidade do espelho
                if (verificarJogoDuplicado(conn, grupoId, numerosEspelhoFormatados)) {
                    System.err.println("Erro: Jogo espelho correspondente já existe neste grupo.");
                    conn.rollback();
                    return -2; // Jogo duplicado (espelho)
                }
                
                // Calcular hash para o jogo espelho
                String hashEspelho = HashUtil.calcularHashMD5(numerosEspelhoFormatados);
                
                // Verificar duplicidade por hash para o espelho
                if (verificarHashDuplicado(conn, grupoId, hashEspelho)) {
                    System.out.println("[DEBUG] Hash duplicado encontrado para jogo espelho: " + hashEspelho);
                    System.out.println("[DEBUG] Números do espelho: " + numerosEspelhoFormatados);
                    conn.rollback();
                    return -2; // Hash duplicado
                }
                
                // Inserir na tabela jogos_espelho
                String sqlInsertEspelho = "INSERT INTO jogos_espelho (jogo_base_id, grupo_id, numeros) VALUES (?, ?, ?)";
                long jogoEspelhoId = -1;
                try (PreparedStatement stmtInsertEspelho = conn.prepareStatement(sqlInsertEspelho, Statement.RETURN_GENERATED_KEYS)) {
                    stmtInsertEspelho.setLong(1, jogoBaseId);
                    stmtInsertEspelho.setInt(2, grupoId);
                    stmtInsertEspelho.setString(3, numerosEspelhoFormatados);
                    stmtInsertEspelho.executeUpdate();
                    ResultSet rsKeys = stmtInsertEspelho.getGeneratedKeys();
                    if (rsKeys.next()) {
                        jogoEspelhoId = rsKeys.getLong(1);
                    }
                }
                
                // Inserir na tabela de hash para o espelho
                if (jogoEspelhoId > 0) {
                    try (PreparedStatement stmtInsertHashEspelho = conn.prepareStatement(sqlInsertHash)) {
                        stmtInsertHashEspelho.setInt(1, grupoId);
                        stmtInsertHashEspelho.setLong(2, jogoEspelhoId);
                        stmtInsertHashEspelho.setString(3, hashEspelho);
                        stmtInsertHashEspelho.setString(4, numerosEspelhoFormatados);
                        stmtInsertHashEspelho.executeUpdate();
                    }
                }
                
                // Atualizar jogo base com referência ao espelho
                String sqlUpdateBase = "UPDATE jogos_fixos SET jogo_espelho_id = ? WHERE id = ?";
                try (PreparedStatement stmtUpdateBase = conn.prepareStatement(sqlUpdateBase)) {
                    stmtUpdateBase.setLong(1, jogoEspelhoId);
                    stmtUpdateBase.setLong(2, jogoBaseId);
                    stmtUpdateBase.executeUpdate();
                }
            }
            
            // Atualizar cache de quantidade
            int qtdAtual = getQuantidadeJogosPorGrupo(grupoId);
            grupoCache.atualizarQuantidadeJogos(grupoId, qtdAtual + 1);
            System.out.println("[DEBUG] Atualizando QTD após cadastro bem-sucedido");
            
            conn.commit();
            return jogoBaseId;
            
        } catch (SQLException e) {
            System.err.println("Erro ao adicionar jogo: " + e.getMessage());
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                System.err.println("Erro ao fazer rollback: " + ex.getMessage());
            }
            return -3; // Erro de banco de dados
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.err.println("Erro ao fechar conexão: " + e.getMessage());
            }
        }
    }
    
    /**
     * Verifica se um hash já existe para o grupo
     */
    private boolean verificarHashDuplicado(Connection conn, int grupoId, String hash) throws SQLException {
        String sql = "SELECT jogo_id, numeros FROM jogos_hash WHERE grupo_id = ? AND hash = ? LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, grupoId);
            stmt.setString(2, hash);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                long jogoId = rs.getLong("jogo_id");
                String numeros = rs.getString("numeros");
                System.out.println("[DEBUG] Hash duplicado encontrado: " + hash);
                System.out.println("[DEBUG] Jogo ID: " + jogoId + ", Números: " + numeros);
                return true;
            }
            return false;
        }
    }
    
    /**
     * Verificação SQL direta para duplicidade de jogo
     */
    private boolean verificarJogoDuplicado(Connection conn, int grupoId, String numeros) throws SQLException {
        String sql = "SELECT id FROM jogos_fixos WHERE grupo_id = ? AND numeros = ? LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, grupoId);
            stmt.setString(2, numeros);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                long jogoId = rs.getLong("id");
                System.out.println("[DEBUG] Jogo duplicado encontrado: ID " + jogoId + ", Números: " + numeros);
                return true;
            }
            
            // Verificar também na tabela de espelhos
            String sqlEspelho = "SELECT id FROM jogos_espelho WHERE grupo_id = ? AND numeros = ? LIMIT 1";
            try (PreparedStatement stmtEspelho = conn.prepareStatement(sqlEspelho)) {
                stmtEspelho.setInt(1, grupoId);
                stmtEspelho.setString(2, numeros);
                ResultSet rsEspelho = stmtEspelho.executeQuery();
                
                if (rsEspelho.next()) {
                    long jogoEspelhoId = rsEspelho.getLong("id");
                    System.out.println("[DEBUG] Espelho duplicado encontrado: ID " + jogoEspelhoId + ", Números: " + numeros);
                    return true;
                }
            }
            
            return false;
        }
    }
    
    /**
     * Verifica se um jogo é igual a outro ou ao seu espelho
     */
    private boolean verificarJogoIgualOuEspelho(String numeros1, String numeros2) {
        if (numeros1.equals(numeros2)) {
            return true;
        }
        
        // Verificar se é espelho
        List<Integer> dezenas1 = Arrays.stream(numeros1.split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        
        List<Integer> dezenas2 = Arrays.stream(numeros2.split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        
        List<Integer> espelho1 = calcularDezenasEspelho(dezenas1);
        if (espelho1 == null) {
            return false;
        }
        
        String numerosEspelho1 = formatarDezenasParaString(espelho1);
        return numerosEspelho1.equals(numeros2);
    }
    
    /**
    * Valida e formata um jogo a partir de uma string de entrada,
    * garantindo que tenha exatamente 50 dezenas entre 0 e 99,
    * sem duplicatas, formatadas como "00,01,…".
    */
   private List<Integer> validarEFormatarJogo(String numerosInput) {
       if (numerosInput == null || numerosInput.trim().isEmpty()) {
           System.err.println("Entrada vazia");
           return null;
       }

       // Divide em partes
       String[] partes = numerosInput
           .trim()
           .replaceAll("\\s+", ",")
           .replaceAll("[.\\-]", ",")
           .split(",");

       // Converte e valida intervalo
       List<Integer> dezenas = new ArrayList<>();
       try {
           for (String parte : partes) {
               if (parte.isEmpty()) continue;
               int dez = Integer.parseInt(parte.trim());
               if (dez < 0 || dez > 99) {
                   System.err.println("Dezena fora do intervalo: " + dez);
                   return null;
               }
               dezenas.add(dez);
           }
       } catch (NumberFormatException e) {
           System.err.println("Formato inválido de dezena: " + e.getMessage());
           return null;
       }

       // Remove duplicatas
       Set<Integer> set = new HashSet<>(dezenas);
       if (set.size() != dezenas.size()) {
           System.err.println("Há dezenas repetidas no jogo");
           return null;
       }

       // Verifica tamanho exato
       if (set.size() != 50) {
           System.err.println("Jogo inválido: deve conter exatamente 50 dezenas, mas encontrou " + set.size());
           return null;
       }

       // Ordena e retorna
       List<Integer> resultado = new ArrayList<>(set);
       Collections.sort(resultado);
       return resultado;
   }
    
    /**
     * Formata uma lista de dezenas para string no formato "00,01,02,..."
     */
    private String formatarDezenasParaString(List<Integer> dezenas) {
        return dezenas.stream()
                .map(d -> String.format("%02d", d))
                .collect(Collectors.joining(","));
    }
    
    /**
    * Calcula o jogo “espelho” como o complemento de 0..99 em relação ao jogo base.
    */
   private List<Integer> calcularDezenasEspelho(List<Integer> dezenasBase) {
       // Constrói a lista 0,1,2,…,99
       List<Integer> espelho = IntStream.range(0, 100)
                                         .boxed()
                                         .collect(Collectors.toList());
       // Remove todas as dezenas já presentes no jogo base
       espelho.removeAll(dezenasBase);
       // Ordena para manter consistência
       Collections.sort(espelho);
       return espelho;
   }
    
    /**
     * Cria um novo concurso e retorna seu ID
     * Método necessário para PainelConcursosUnificado
     */
    public int criarConcursoEObterID(String descricao) {
        if (descricao == null || descricao.trim().isEmpty()) {
            return -1;
        }
        
        try (Connection conn = Conexao.conectar()) {
            String sql = "INSERT INTO concursos (descricao) VALUES (?)";
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, descricao.trim());
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return -1;
        } catch (SQLException e) {
            System.err.println("Erro ao criar concurso: " + e.getMessage());
            return -1;
        }
    }
    
    /**
     * Aplica um grupo fixo a um concurso
     * Método necessário para PainelConcursosUnificado
     * CORRIGIDO: Retorna int[] para compatibilidade com interface
     */
    public int[] aplicarGrupoFixoAoConcurso(int grupoId, int concursoId) {
        int jogosAplicados = 0;
        int jogosIgnorados = 0;
        
        try (Connection conn = Conexao.conectar()) {
            conn.setAutoCommit(false);
            
            // Obter todos os jogos do grupo que ainda não foram aplicados ao concurso
            String sqlJogosNaoAplicados = """
                SELECT jf.id, jf.jogador_id, jf.numeros, jf.jogo_espelho_id
                FROM jogos_fixos jf
                LEFT JOIN jogos j ON j.jogo_fixo_id = jf.id AND j.concurso_id = ? AND j.espelho = 0
                WHERE jf.grupo_id = ? AND j.id IS NULL
                """;
            
            try (PreparedStatement stmtJogosNaoAplicados = conn.prepareStatement(sqlJogosNaoAplicados)) {
                stmtJogosNaoAplicados.setInt(1, concursoId);
                stmtJogosNaoAplicados.setInt(2, grupoId);
                ResultSet rs = stmtJogosNaoAplicados.executeQuery();
                
                // Inserir jogos base não aplicados
                String sqlInsertJogo = """
                    INSERT INTO jogos (concurso_id, jogador_id, jogo_fixo_id, numeros, espelho)
                    VALUES (?, ?, ?, ?, 0)
                    """;
                
                try (PreparedStatement stmtInsertJogo = conn.prepareStatement(sqlInsertJogo)) {
                    while (rs.next()) {
                        long jogoFixoId = rs.getLong("id");
                        int jogadorId = rs.getInt("jogador_id");
                        String numeros = rs.getString("numeros");
                        
                        // Verificar se já existe este jogo para este concurso
                        String sqlCheckExistente = """
                            SELECT id FROM jogos
                            WHERE concurso_id = ? AND numeros = ? AND jogador_id = ?
                            LIMIT 1
                            """;
                        
                        boolean jogoJaExiste = false;
                        try (PreparedStatement stmtCheckExistente = conn.prepareStatement(sqlCheckExistente)) {
                            stmtCheckExistente.setInt(1, concursoId);
                            stmtCheckExistente.setString(2, numeros);
                            stmtCheckExistente.setInt(3, jogadorId);
                            ResultSet rsCheck = stmtCheckExistente.executeQuery();
                            jogoJaExiste = rsCheck.next();
                        }
                        
                        if (jogoJaExiste) {
                            jogosIgnorados++;
                            continue;
                        }
                        
                        // Inserir jogo base
                        stmtInsertJogo.setInt(1, concursoId);
                        stmtInsertJogo.setInt(2, jogadorId);
                        stmtInsertJogo.setLong(3, jogoFixoId);
                        stmtInsertJogo.setString(4, numeros);
                        stmtInsertJogo.executeUpdate();
                        jogosAplicados++;
                        
                        System.out.println("Jogo base ID " + jogoFixoId + " aplicado ao concurso " + concursoId);
                        
                        // Verificar se tem espelho
                        Long jogoEspelhoId = rs.getObject("jogo_espelho_id") != null ? rs.getLong("jogo_espelho_id") : null;
                        if (jogoEspelhoId != null) {
                            // Obter dados do espelho
                            String sqlEspelho = "SELECT numeros FROM jogos_espelho WHERE id = ?";
                            try (PreparedStatement stmtEspelho = conn.prepareStatement(sqlEspelho)) {
                                stmtEspelho.setLong(1, jogoEspelhoId);
                                ResultSet rsEspelho = stmtEspelho.executeQuery();
                                
                                if (rsEspelho.next()) {
                                    String numerosEspelho = rsEspelho.getString("numeros");
                                    
                                    // Verificar se o espelho já existe para este concurso
                                    boolean espelhoJaExiste = false;
                                    try (PreparedStatement stmtCheckEspelho = conn.prepareStatement(sqlCheckExistente)) {
                                        stmtCheckEspelho.setInt(1, concursoId);
                                        stmtCheckEspelho.setString(2, numerosEspelho);
                                        stmtCheckEspelho.setInt(3, jogadorId);
                                        ResultSet rsCheckEspelho = stmtCheckEspelho.executeQuery();
                                        espelhoJaExiste = rsCheckEspelho.next();
                                    }
                                    
                                    if (!espelhoJaExiste) {
                                        // Inserir jogo espelho
                                        String sqlInsertEspelho = """
                                            INSERT INTO jogos (concurso_id, jogador_id, jogo_fixo_id, numeros, espelho)
                                            VALUES (?, ?, ?, ?, 1)
                                            """;
                                        
                                        try (PreparedStatement stmtInsertEspelho = conn.prepareStatement(sqlInsertEspelho)) {
                                            stmtInsertEspelho.setInt(1, concursoId);
                                            stmtInsertEspelho.setInt(2, jogadorId);
                                            stmtInsertEspelho.setLong(3, jogoFixoId);
                                            stmtInsertEspelho.setString(4, numerosEspelho);
                                            stmtInsertEspelho.executeUpdate();
                                            jogosAplicados++;
                                            
                                            System.out.println("Jogo espelho para ID " + jogoFixoId + " aplicado ao concurso " + concursoId);
                                        }
                                    } else {
                                        jogosIgnorados++;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            conn.commit();
            System.out.println("Status: " + jogosAplicados + " jogos aplicados, " + jogosIgnorados + " ignorados.");
            return new int[]{jogosAplicados, jogosIgnorados};
            
        } catch (SQLException e) {
            System.err.println("Erro ao aplicar grupo fixo ao concurso: " + e.getMessage());
            return new int[]{-1, -1};
        }
    }
    
    /**
     * Registra o resultado de um concurso
     * Método necessário para PainelConcursosUnificado
     */
    public boolean registrarResultadoConcurso(int concursoId, String numerosInput) {
        List<Integer> dezenas = validarEFormatarResultado(numerosInput);
        if (dezenas == null) {
            return false;
        }
        
        String numerosFormatados = formatarDezenasParaString(dezenas);
        
        try (Connection conn = Conexao.conectar()) {
            String sql = "UPDATE concursos SET numeros = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, numerosFormatados);
            stmt.setInt(2, concursoId);
            int result = stmt.executeUpdate();
            
            return result > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao registrar resultado: " + e.getMessage());
            return false;
        }
    }
    
    /**
    * Calcula acertos para todos os jogos de um concurso
    * Com debug por jogo base e espelho
    */
   public int calcularAcertosDoConcurso(int concursoId) {
       int jogosProcessados = 0;
       int jogosBase = 0;
       int jogosEspelho = 0;

       try (Connection conn = Conexao.conectar()) {
           conn.setAutoCommit(false);

           // 1) Buscar resultado do concurso
           String sqlResultado = "SELECT numeros FROM concursos WHERE id = ?";
           String numerosResultado;
           try (PreparedStatement stmtResultado = conn.prepareStatement(sqlResultado)) {
               stmtResultado.setInt(1, concursoId);
               ResultSet rsRes = stmtResultado.executeQuery();
               if (!rsRes.next()) {
                   System.err.println("Concurso não encontrado ou sem resultado registrado");
                   return -1;
               }
               numerosResultado = rsRes.getString("numeros");
           }

           // Converte resultado para Set<Integer>
           Set<Integer> dezenasResultado = Arrays.stream(numerosResultado.split(","))
                   .map(Integer::parseInt)
                   .collect(Collectors.toSet());

           // 2) Selecionar todos os jogos base
           String sqlJogosBase = """
               SELECT j.id, j.jogo_fixo_id, j.jogador_id, j.numeros, jf.jogo_espelho_id
               FROM jogos j
               JOIN jogos_fixos jf ON j.jogo_fixo_id = jf.id
               WHERE j.concurso_id = ? AND j.espelho = 0
               """;
           try (PreparedStatement stmtJogosBase = conn.prepareStatement(sqlJogosBase)) {
               stmtJogosBase.setInt(1, concursoId);
               ResultSet rs = stmtJogosBase.executeQuery();

               while (rs.next()) {
                   long jogoId       = rs.getLong("id");
                   long jogoFixoId   = rs.getLong("jogo_fixo_id");
                   String numeros    = rs.getString("numeros");
                   Long espelhoId    = rs.getObject("jogo_espelho_id", Long.class);

                   // Calcular e logar acertos do jogo base
                   int acertos = calcularAcertos(numeros, dezenasResultado);
                   System.out.println("DEBUG_CALC Base: jogoId=" + jogoId +
                                      " (fixo " + jogoFixoId + ") -> " + acertos + " acertos");

                   // Atualizar acertos do jogo base
                   try (PreparedStatement stmtUpd = conn.prepareStatement(
                           "UPDATE jogos SET acertos = ? WHERE id = ?")) {
                       stmtUpd.setInt(1, acertos);
                       stmtUpd.setLong(2, jogoId);
                       stmtUpd.executeUpdate();
                   }
                   jogosProcessados++;
                   jogosBase++;

                   // 3) Processar espelho
                   if (espelhoId != null) {
                       // Ler números do espelho
                       String numerosEspelho;
                       try (PreparedStatement stmtEsp = conn.prepareStatement(
                               "SELECT numeros FROM jogos_espelho WHERE id = ?")) {
                           stmtEsp.setLong(1, espelhoId);
                           ResultSet rsEsp = stmtEsp.executeQuery();
                           if (!rsEsp.next()) continue;
                           numerosEspelho = rsEsp.getString("numeros");
                       }

                       // Calcular e logar acertos do espelho
                       int acertosEspelho = calcularAcertos(numerosEspelho, dezenasResultado);
                       System.out.println("DEBUG_CALC Espelho: jogoFixoId=" + jogoFixoId +
                                          " -> " + acertosEspelho + " acertos");

                       // Atualizar acertos do espelho na tabela jogos
                       // Primeiro buscar o ID do registro de espelho na tabela jogos
                       long idEsp = -1;
                       try (PreparedStatement stmtFind = conn.prepareStatement(
                               "SELECT id FROM jogos WHERE concurso_id = ? AND jogo_fixo_id = ? AND espelho = 1 LIMIT 1")) {
                           stmtFind.setInt(1, concursoId);
                           stmtFind.setLong(2, jogoFixoId);
                           ResultSet rsFind = stmtFind.executeQuery();
                           if (rsFind.next()) {
                               idEsp = rsFind.getLong("id");
                           }
                       }
                       if (idEsp > 0) {
                           try (PreparedStatement stmtUpdEsp = conn.prepareStatement(
                                   "UPDATE jogos SET acertos = ? WHERE id = ?")) {
                               stmtUpdEsp.setInt(1, acertosEspelho);
                               stmtUpdEsp.setLong(2, idEsp);
                               stmtUpdEsp.executeUpdate();
                           }
                           jogosProcessados++;
                           jogosEspelho++;
                       }
                   }
               }
           }

           conn.commit();
           System.out.println("Acertos calculados para " + jogosProcessados +
                              " jogos do concurso " + concursoId +
                              " (" + jogosBase + " base e " + jogosEspelho + " espelhos)");
           return jogosProcessados;

       } catch (SQLException e) {
           System.err.println("Erro ao calcular acertos: " + e.getMessage());
           return -1;
       }
   }


    
    /**
     * Calcula acertos de um jogo em relação ao resultado
     */
    private int calcularAcertos(String numeros, Set<Integer> dezenasResultado) {
        Set<Integer> dezenasJogo = Arrays.stream(numeros.split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
        
        // Interseção dos conjuntos
        Set<Integer> acertos = new HashSet<>(dezenasJogo);
        acertos.retainAll(dezenasResultado);
        
        return acertos.size();
    }
    
    /**
    * Lista jogos premiados filtrando apenas acertos de 15 a 20 e 0 (especial),
    * retornando também o ID do jogo principal (jogo_fixo_id).
    */
   public List<Map<String, Object>> listarJogosPremiados(int concursoId) {
       List<Map<String, Object>> jogosPremiados = new ArrayList<>();
       String sql = """
           SELECT j.id,
                  j.jogo_fixo_id,
                  j.numeros,
                  j.acertos,
                  j.espelho,
                  jf.grupo_id
           FROM jogos j
           JOIN jogos_fixos jf ON j.jogo_fixo_id = jf.id
           WHERE j.concurso_id = ?
             AND (j.acertos BETWEEN 15 AND 20 OR j.acertos = 0)
           ORDER BY j.acertos DESC, jf.grupo_id, j.id
       """;

       try (Connection conn = Conexao.conectar();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
           stmt.setInt(1, concursoId);

           try (ResultSet rs = stmt.executeQuery()) {
               while (rs.next()) {
                   int realId      = rs.getInt("id");
                   int jogoFixoId  = rs.getInt("jogo_fixo_id");
                   String numeros  = rs.getString("numeros");
                   int acertos     = rs.getInt("acertos");
                   boolean espelho = rs.getBoolean("espelho");
                   int grupoId     = rs.getInt("grupo_id");

                   Map<String, Object> jogo = new HashMap<>();
                   jogo.put("jogo_fixo_id", jogoFixoId);   // ID principal para exibição
                   jogo.put("real_id",      realId);       // ID interno do registro
                   jogo.put("numeros",      numeros);
                   jogo.put("acertos",      acertos);
                   jogo.put("espelho",      espelho);
                   jogo.put("grupo_id",     grupoId);

                   jogosPremiados.add(jogo);
                   System.out.println("DEBUG: Jogo fixo ID " + jogoFixoId +
                                      " (registro " + realId + ") acertos=" + acertos +
                                      " espelho=" + espelho);
               }
           }
       } catch (SQLException e) {
           System.err.println("Erro ao listar jogos premiados: " + e.getMessage());
       }

       return jogosPremiados;
   }
    
    /**
     * Obtém a lista de concursos com resultado
     * Método necessário para PainelListarJogosPremiados
     */
    public List<Concurso> getConcursosComResultado() {
        List<Concurso> concursos = new ArrayList<>();
        
        try (Connection conn = Conexao.conectar()) {
            String sql = "SELECT id, descricao, numeros FROM concursos WHERE numeros IS NOT NULL ORDER BY id DESC";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                Concurso concurso = new Concurso();
                concurso.setId(rs.getInt("id"));
                concurso.setDescricao(rs.getString("descricao"));
                concurso.setNumeros(rs.getString("numeros"));
                concursos.add(concurso);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar concursos: " + e.getMessage());
        }
        
        return concursos;
    }
    
    /**
     * Obtém a lista de concursos
     * Método necessário para PainelConcursosUnificado
     */
    public List<Concurso> getConcursosList() {
        List<Concurso> concursos = new ArrayList<>();
        
        try (Connection conn = Conexao.conectar()) {
            String sql = "SELECT id, descricao, numeros FROM concursos ORDER BY id DESC";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                Concurso concurso = new Concurso();
                concurso.setId(rs.getInt("id"));
                concurso.setDescricao(rs.getString("descricao"));
                concurso.setNumeros(rs.getString("numeros"));
                concursos.add(concurso);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar concursos: " + e.getMessage());
        }
        
        return concursos;
    }
    
    /**
     * Insere em lote uma lista de jogos_hash no SQLite, evitando duplicatas
     * por grupo (incluindo o espelho).
     */
    public void inserirJogosHashEmLote(List<JogoHash> jogos, int grupoId) throws SQLException {
        String sqlCheck = "SELECT 1 FROM jogos_hash WHERE grupo_id = ? AND hash = ? LIMIT 1";
        String sqlCheckMirror = "SELECT 1 FROM jogos_espelho WHERE grupo_id = ? AND hash = ? LIMIT 1";
        String sqlInsert = "INSERT INTO jogos_hash(grupo_id, jogo_id, hash, numeros) VALUES (?, ?, ?, ?)";

        try (Connection conn = Conexao.conectar();
             PreparedStatement psCheck = conn.prepareStatement(sqlCheck);
             PreparedStatement psCheckMirror = conn.prepareStatement(sqlCheckMirror);
             PreparedStatement psInsert = conn.prepareStatement(sqlInsert)) {

            conn.setAutoCommit(false);
            int batchSize = 1000;
            int countInserted = 0;
            int countSkipped = 0;

            for (JogoHash j : jogos) {
                // checa duplicidade base
                psCheck.setInt(1, grupoId);
                psCheck.setString(2, j.getHash());
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next()) { countSkipped++; continue; }
                }
                // checa duplicidade espelho
                psCheckMirror.setInt(1, grupoId);
                psCheckMirror.setString(2, j.getMirrorHash());
                try (ResultSet rs2 = psCheckMirror.executeQuery()) {
                    if (rs2.next()) { countSkipped++; continue; }
                }
                // adiciona ao batch
                psInsert.setInt(1, grupoId);
                psInsert.setInt(2, j.getJogoId());
                psInsert.setString(3, j.getHash());
                psInsert.setString(4, j.getNumeros());
                psInsert.addBatch();
                countInserted++;
                if (countInserted % batchSize == 0) {
                    psInsert.executeBatch();
                }
            }
            psInsert.executeBatch();
            conn.commit();
            System.out.printf("✅ Inserção concluída: %d adicionados, %d pulados.%n", countInserted, countSkipped);
        }
    }
     /**
     * Retorna um array com a contagem de jogos por faixa de acertos:
     * índices 0 a 5 = acertos de 15,16,17,18,19,20; índice 6 = acertos 0 (especial).
     */
    public int[] getAcertosPorFaixa(int concursoId) {
        int[] faixas = new int[7];
        String sql = """
            SELECT acertos, COUNT(*) AS cnt
            FROM jogos
            WHERE concurso_id = ?
              AND (acertos BETWEEN 15 AND 20 OR acertos = 0)
            GROUP BY acertos
            """;

        try (Connection conn = Conexao.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, concursoId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int ac = rs.getInt("acertos");
                int cnt = rs.getInt("cnt");
                if (ac >= 15 && ac <= 20) {
                    faixas[ac - 15] = cnt;
                } else if (ac == 0) {
                    faixas[6] = cnt;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao obter acertos por faixa: " + e.getMessage());
        }
        return faixas;
    }

    /**
     * Classe de apoio para inserção em lote de hashes.
     */
    public static class JogoHash {
        private final int jogoId;
        private final String numeros;
        private final String hash;
        private final String mirrorHash;

        public JogoHash(int jogoId, String numeros, String hash, String mirrorHash) {
            this.jogoId = jogoId;
            this.numeros = numeros;
            this.hash = hash;
            this.mirrorHash = mirrorHash;
        }
        public int getJogoId() { return jogoId; }
        public String getNumeros() { return numeros; }
        public String getHash() { return hash; }
        public String getMirrorHash() { return mirrorHash; }
    }
    
    /**
    * Valida e formata o resultado de um concurso,
    * garantindo que contenha exatamente 20 dezenas únicas entre 0 e 99.
    */
   private List<Integer> validarEFormatarResultado(String numerosInput) {
       if (numerosInput == null || numerosInput.trim().isEmpty()) {
           System.err.println("Resultado vazio");
           return null;
       }

       String[] partes = numerosInput
           .trim()
           .replaceAll("\\s+", ",")
           .replaceAll("[.\\-]", ",")
           .split(",");

       List<Integer> dezenas = new ArrayList<>();
       try {
           for (String parte : partes) {
               if (parte.isEmpty()) continue;
               int dez = Integer.parseInt(parte.trim());
               if (dez < 0 || dez > 99) {
                   System.err.println("Dezena de resultado fora do intervalo: " + dez);
                   return null;
               }
               dezenas.add(dez);
           }
       } catch (NumberFormatException e) {
           System.err.println("Formato inválido de dezena no resultado: " + e.getMessage());
           return null;
       }

       // Sem duplicatas
       Set<Integer> set = new HashSet<>(dezenas);
       if (set.size() != dezenas.size()) {
           System.err.println("Há dezenas repetidas no resultado");
           return null;
       }

       // Exatamente 20 dezenas obrigatórias
       if (dezenas.size() != 20) {
           System.err.println("Resultado inválido: deve conter exatamente 20 dezenas, mas encontrou " + dezenas.size());
           return null;
       }

       List<Integer> resultado = new ArrayList<>(set);
       Collections.sort(resultado);
       return resultado;
   }
}