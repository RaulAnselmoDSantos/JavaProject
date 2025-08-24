
package applotomania.model;

/**
 *
 * @author Raul
 */

public class Jogo {
    private int id;
    private int concursoId;
    private int jogadorId;
    private String numeros; // 50 dezenas separadas por vírgula
    private int acertos;    // Calculado depois do resultado

    public Jogo() {
    }

    public Jogo(int id, int concursoId, int jogadorId, String numeros, int acertos) {
        this.id = id;
        this.concursoId = concursoId;
        this.jogadorId = jogadorId;
        this.numeros = numeros;
        this.acertos = acertos;
    }

    public Jogo(int concursoId, int jogadorId, String numeros) {
        this.concursoId = concursoId;
        this.jogadorId = jogadorId;
        this.numeros = numeros;
        this.acertos = 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getConcursoId() {
        return concursoId;
    }

    public void setConcursoId(int concursoId) {
        this.concursoId = concursoId;
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

    public int getAcertos() {
        return acertos;
    }

    public void setAcertos(int acertos) {
        this.acertos = acertos;
    }

    @Override
    public String toString() {
        return "Jogo ID: " + id + " | Acertos: " + acertos + " | Números: " + numeros;
    }
}
