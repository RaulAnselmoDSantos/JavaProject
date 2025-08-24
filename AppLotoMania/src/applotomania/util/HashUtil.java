package applotomania.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Classe utilitária para cálculo de hash MD5
 * 
 * @author Raul
 */
public class HashUtil {
    
    /**
     * Calcula o hash MD5 para uma string de números
     * 
     * @param numeros String contendo os números do jogo
     * @return String com o hash MD5 em formato hexadecimal, ou null em caso de erro
     */
    public static String calcularHashMD5(String numeros) {
        if (numeros == null || numeros.isEmpty()) {
            return null;
        }
        
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(numeros.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Erro ao calcular hash MD5: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Verifica se dois jogos são iguais comparando seus hashes
     * 
     * @param numeros1 String contendo os números do primeiro jogo
     * @param numeros2 String contendo os números do segundo jogo
     * @return true se os jogos são iguais, false caso contrário ou em caso de erro
     */
    public static boolean jogosIguais(String numeros1, String numeros2) {
        String hash1 = calcularHashMD5(numeros1);
        String hash2 = calcularHashMD5(numeros2);
        
        if (hash1 == null || hash2 == null) {
            return false;
        }
        
        return hash1.equals(hash2);
    }
}
