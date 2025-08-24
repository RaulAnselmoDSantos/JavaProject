package applotomania.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Classe para gerenciamento de cache global de grupos
 * Implementação compatível com o LotoService
 * 
 * @author Raul
 */

public class GrupoCache {
    private final Map<Integer, Integer> quantidadePorGrupo = new HashMap<>();
    
    /**
     * Construtor padrão sem parâmetros
     */
    public GrupoCache() {
        // Inicialização vazia
    }
    
    /**
     * Obtém a quantidade de jogos para um grupo específico
     * 
     * @param grupoId ID do grupo
     * @return Quantidade de jogos ou null se não estiver em cache
     */
    public Integer getQuantidadeJogos(int grupoId) {
        return quantidadePorGrupo.get(grupoId);
    }
    
    /**
     * Atualiza a quantidade de jogos para um grupo específico
     * 
     * @param grupoId ID do grupo
     * @param quantidade Nova quantidade de jogos
     */
    public void atualizarQuantidadeJogos(int grupoId, int quantidade) {
        quantidadePorGrupo.put(grupoId, quantidade);
    }
    
    /**
     * Limpa todo o cache
     */
    public void limparCache() {
        quantidadePorGrupo.clear();
    }
    
    /**
     * Remove um grupo específico do cache
     * 
     * @param grupoId ID do grupo a ser removido
     */
    public void removerGrupo(int grupoId) {
        quantidadePorGrupo.remove(grupoId);
    }
}
