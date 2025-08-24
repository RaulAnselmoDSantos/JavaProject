
package applotomania.db;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
        
        
/**
 *
 * @author Raul
 */
public class Conexao {
    
    private static String arquivo = "loto.db"; // padrão

    public static void setArquivo(String nome) {
        arquivo = nome;
    }

    public static Connection conectar() {
        String url = "jdbc:sqlite:" + arquivo;
        try {
            return DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.out.println("❌ Erro ao conectar com o banco de dados SQLite:");
            e.printStackTrace();
            return null;
        }
    }
    
}
