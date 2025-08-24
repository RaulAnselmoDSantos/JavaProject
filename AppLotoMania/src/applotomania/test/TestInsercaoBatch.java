package applotomania.test;

import applotomania.service.LotoService;
import applotomania.service.LotoService.JogoHash;
import applotomania.util.HashUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Teste de performance: insere em lote 60.000 jogos para verificar tempo
 */
public class TestInsercaoBatch {
    public static void main(String[] args) throws Exception {
        LotoService service = new LotoService();
        int grupoId = 1; // ajuste para o ID do grupo de teste

        // 1) Gerar 60.000 jogos com 50 dezenas cada e seus espelhos
        List<JogoHash> lista = new ArrayList<>(60_000);
        Random rand = new Random();
        for (int i = 1; i <= 60_000; i++) {
            // Gera 50 dezenas únicas aleatórias de 0..99
            List<Integer> dezenas = IntStream.rangeClosed(0, 99)
                                             .boxed()
                                             .collect(Collectors.toList());
            Collections.shuffle(dezenas, rand);
            List<Integer> base = dezenas.subList(0, 50).stream()
                             .sorted()
                             .collect(Collectors.toList());
            String numeros = base.stream()
                                 .map(d -> String.format("%02d", d))
                                 .collect(Collectors.joining(","));
            String hash = HashUtil.calcularHashMD5(numeros);

            // Gera o espelho (complemento de 0..99)
            List<Integer> espelho = IntStream.rangeClosed(0, 99)
                                             .boxed()
                                             .filter(d -> !base.contains(d))
                                             .sorted()
                                             .collect(Collectors.toList());
            String mirrorNums = espelho.stream()                           .map(d -> String.format("%02d", d))
                           .collect(Collectors.joining(","));
            String mirrorHash = HashUtil.calcularHashMD5(mirrorNums);

            lista.add(new JogoHash(i, numeros, hash, mirrorHash));
        }

        // 2) Executar inserção em lote e medir tempo
        long start = System.currentTimeMillis();
        service.inserirJogosHashEmLote(lista, grupoId);
        long end = System.currentTimeMillis();
        System.out.printf("⏱ Tempo para inserir 60.000 jogos: %d ms%n", (end - start));
    }
}