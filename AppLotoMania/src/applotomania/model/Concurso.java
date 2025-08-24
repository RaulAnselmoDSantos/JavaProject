package applotomania.model;

/**
 *
 * @author Raul
 */

public class Concurso {
    private int id;
    private int numero;       // Número do concurso (ex: 2767)
    private String data;      // Data do concurso (ex: "07/05/2025")
    private String resultado; // 20 dezenas sorteadas (formato: "03,22,...") ou null
    private String descricao; // Ex: "Concurso 2767 - 07/05/2025"

    public Concurso() {
    }

    public Concurso(int id, int numero, String data, String resultado) {
        this.id = id;
        this.numero = numero;
        this.data = data;
        this.resultado = resultado;
        this.descricao = "Concurso " + numero + " - " + data;
    }

    public Concurso(int id, String descricao, String resultado) {
        this.id = id;
        this.descricao = descricao;
        this.resultado = resultado;
    }

    public Concurso(String descricao) {
        this.descricao = descricao;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
        atualizarDescricao();
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
        atualizarDescricao();
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    // Para compatibilidade com código existente
    public String getNumeros() {
        return resultado;
    }

    public void setNumeros(String numeros) {
        this.resultado = numeros;
    }

    private void atualizarDescricao() {
        if (numero > 0 && data != null && !data.isEmpty()) {
            this.descricao = "Concurso " + numero + " - " + data;
        }
    }

    @Override
    public String toString() {
        return descricao;
    }
}
