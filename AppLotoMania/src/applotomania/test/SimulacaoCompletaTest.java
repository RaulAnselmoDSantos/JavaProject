package applotomania.test;

import applotomania.service.LotoService;
import applotomania.util.HashUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Simulação completa modificada:
 * 1) Cadastra 60k jogos no grupo usando adicionarJogoFixoIndividual (inclui espelho)
 * 2) Cria um concurso e aplica o grupo
 * 3) Registra resultado aleatório de 20 dezenas
 * 4) Calcula acertos
 *
 * Use a UI para listar e verificar manualmente.
 */
public class SimulacaoCompletaTest {
    public static void main(String[] args) throws Exception {
        LotoService service = new LotoService();
        // Ajuste este ID para o seu grupo de teste (deve existir antes)
        int grupoId = 1;

        // 1) Gerar e cadastrar 60.000 jogos no grupo (base + espelho)
        Random rand = new Random();
        List<String> listaNumeros = new ArrayList<>(60_000);
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
            listaNumeros.add(numeros);
        }

        System.out.println("Iniciando cadastro de 60k jogos no grupo, incluindo espelhos...");
        long t0 = System.currentTimeMillis();
        for (String numeros : listaNumeros) {
            long result = service.adicionarJogoFixoIndividual(grupoId, numeros, true);
            if (result < 0) {
                System.err.println("Falha ao cadastrar jogo: retorno " + result);
            }
        }
        long t1 = System.currentTimeMillis();
        System.out.printf("📋 Cadastro concluído em %d ms%n", (t1 - t0));

        // 2) Criar concurso e aplicar grupo
        String descricao = "ConcursoTeste60k_" + System.currentTimeMillis();
        int concursoId = service.criarConcursoEObterID(descricao);
        System.out.println("Concurso criado: " + concursoId + " (" + descricao + ")");
        int[] aplicados = service.aplicarGrupoFixoAoConcurso(grupoId, concursoId);
        System.out.printf("Jogos aplicados ao concurso: %d base+espelho, ignorados: %d%n", aplicados[0] + aplicados[1], 0);

        // 3) Gerar e registrar resultado aleatório de 20 dezenas
        List<Integer> all = IntStream.rangeClosed(0, 99)
                                     .boxed()
                                     .collect(Collectors.toList());
        Collections.shuffle(all, rand);
        List<Integer> resultado = all.subList(0, 20).stream()
                                     .sorted()
                                     .collect(Collectors.toList());
        String resStr = resultado.stream()                                 .map(d -> String.format("%02d", d))
                                 .collect(Collectors.joining(","));
        boolean ok = service.registrarResultadoConcurso(concursoId, resStr);
        System.out.println("Resultado registrado: " + ok + " -> " + resStr);

        // 4) Calcular acertos
        int processados = service.calcularAcertosDoConcurso(concursoId);
        System.out.println("Acertos calculados para " + processados + " jogos");

        System.out.println("Simulação completa pronta. Abra a UI e selecione o concurso '" + descricao + "'.");
    }
}
