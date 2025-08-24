package applotomania.model;

/**
 *
 * @author Raul
 */

public class JogoFixo {
    private long id;
    private int grupoId;
    private int jogadorId;
    private String nome;
    private String numeros; // 50 dezenas separadas por vírgula (ex: "01,02,03,...,99")
    private String dezenas;
    private int acertos; // Adicionado para compatibilidade
    private boolean espelho; // Adicionado para compatibilidade
    private int jogoEspelhoId; // Adicionado para compatibilidade

    public JogoFixo() {
    }

    public JogoFixo(long id, int grupoId, int jogadorId, String nome, String numeros) {
        this.id = id;
        this.grupoId = grupoId;
        this.jogadorId = jogadorId;
        this.nome = nome;
        this.numeros = numeros;
    }

    public JogoFixo(int grupoId, int jogadorId, String nome, String numeros) {
        this.grupoId = grupoId;
        this.jogadorId = jogadorId;
        this.nome = nome;
        this.numeros = numeros;
    }
    
    public JogoFixo(String nome, String numeros, int grupoId, int jogadorId) {
        this.nome = nome;
        this.numeros = numeros;
        this.grupoId = grupoId;
        this.jogadorId = jogadorId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getGrupoId() {
        return grupoId;
    }

    public void setGrupoId(int grupoId) {
        this.grupoId = grupoId;
    }

    public int getJogadorId() {
        return jogadorId;
    }

    public void setJogadorId(int jogadorId) {
        this.jogadorId = jogadorId;
    }

    public String getNumeros() {
        return numeros;
    }

    public void setNumeros(String numeros) {
        this.numeros = numeros;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }
    
    // Métodos adicionados para compatibilidade
    
    public int getAcertos() {
        return acertos;
    }
    
    public void setAcertos(int acertos) {
        this.acertos = acertos;
    }
    
    public boolean isEspelho() {
        return espelho;
    }
    
    public void setEspelho(boolean espelho) {
        this.espelho = espelho;
    }
    
    public int getJogoEspelhoId() {
        return jogoEspelhoId;
    }
    
    public void setJogoEspelhoId(int jogoEspelhoId) {
        this.jogoEspelhoId = jogoEspelhoId;
    }

    @Override
    public String toString() {
        return nome + " [" + numeros + "]";
    }
}
