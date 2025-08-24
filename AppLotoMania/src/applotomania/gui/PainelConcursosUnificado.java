package applotomania.gui;

import applotomania.model.Concurso;
import applotomania.model.GrupoFixo;
import applotomania.service.LotoService;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.HashSet;

/**
 *
 * @author Raul
 */

public class PainelConcursosUnificado extends JPanel {

    private final LotoService lotoService;
    private final MainFrame mainFrame; // Para navegação

    // Componentes da UI
    private JTextField txtNomeConcurso, txtResultado;
    private JComboBox<GrupoFixoWrapper> cbGrupos;
    private JComboBox<ConcursoWrapper> cbConcursosVinculo, cbConcursosResultado, cbConcursosAcertos;
    private JLabel lblStatusCriacao, lblStatusResultado, lblStatusVinculo, lblStatusCalculo;
    private JLabel lblConcursoAcertosInfo;
    private JLabel[] lblAcertosContagem = new JLabel[7]; // Para 15, 16, 17, 18, 19, 20, 00 acertos
    private JButton btnCriar, btnVincular, btnRegistrar, btnCalcular, btnListarJogos;

    // Wrappers para ComboBox
    private static class GrupoFixoWrapper {
        GrupoFixo grupo;
        GrupoFixoWrapper(GrupoFixo g) { this.grupo = g; }
        @Override public String toString() { return grupo != null ? grupo.getNome() : "Selecione..."; }
    }

    private static class ConcursoWrapper {
        Concurso concurso;
        boolean temResultado;
        ConcursoWrapper(Concurso c, boolean temResultado) { this.concurso = c; this.temResultado = temResultado; }
        @Override public String toString() {
            if (concurso == null) return "Selecione...";
            return concurso.getDescricao() + (temResultado ? " (R)" : "");
        }
    }

    public PainelConcursosUnificado(LotoService service, MainFrame frame) {
        this.lotoService = service;
        this.mainFrame = frame;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titulo = new JLabel("GERENCIAMENTO DE CONCURSOS", JLabel.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 20));
        add(titulo, BorderLayout.NORTH);

        JPanel painelCentral = new JPanel();
        painelCentral.setLayout(new BoxLayout(painelCentral, BoxLayout.Y_AXIS));

        painelCentral.add(criarSecaoCriarConcurso());
        painelCentral.add(Box.createVerticalStrut(15));
        painelCentral.add(criarSecaoVinculoGrupo());
        painelCentral.add(Box.createVerticalStrut(15));
        painelCentral.add(criarSecaoResultado());
        painelCentral.add(Box.createVerticalStrut(15));
        painelCentral.add(criarSecaoAcertos());

        // Adiciona um painel vazio ao final para empurrar o conteúdo para cima
        painelCentral.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(painelCentral);
        scroll.setBorder(BorderFactory.createEmptyBorder()); // Remove borda do scrollpane
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        add(scroll, BorderLayout.CENTER);

        // Carregar dados iniciais
        carregarGrupos();
        carregarConcursos();
        configurarListeners();
        atualizarEstadosBotoes();
    }

    // --- Métodos de Criação das Seções da UI ---

    private JPanel criarSecaoCriarConcurso() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBorder(BorderFactory.createTitledBorder("1. Criar Novo Concurso"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; painel.add(new JLabel("Descrição:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        txtNomeConcurso = new JTextField(30);
        painel.add(txtNomeConcurso, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        painel.add(new JLabel("Status:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        lblStatusCriacao = new JLabel("-");
        painel.add(lblStatusCriacao, gbc);

        gbc.gridx = 2; gbc.gridy = 1; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0; gbc.anchor = GridBagConstraints.EAST;
        btnCriar = new JButton("Criar");
        painel.add(btnCriar, gbc);

        return painel;
    }

    private JPanel criarSecaoVinculoGrupo() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBorder(BorderFactory.createTitledBorder("2. Vincular Grupo de Jogos ao Concurso"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; painel.add(new JLabel("Grupo de Jogos:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        cbGrupos = new JComboBox<>();
        painel.add(cbGrupos, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        painel.add(new JLabel("Concurso:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        cbConcursosVinculo = new JComboBox<>();
        painel.add(cbConcursosVinculo, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        painel.add(new JLabel("Status:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        lblStatusVinculo = new JLabel("-");
        painel.add(lblStatusVinculo, gbc);

        gbc.gridx = 2; gbc.gridy = 2; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0; gbc.anchor = GridBagConstraints.EAST;
        btnVincular = new JButton("Vincular");
        painel.add(btnVincular, gbc);

        return painel;
    }

    private JPanel criarSecaoResultado() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBorder(BorderFactory.createTitledBorder("3. Registrar Resultado do Concurso"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; painel.add(new JLabel("Concurso:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        cbConcursosResultado = new JComboBox<>();
        painel.add(cbConcursosResultado, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        painel.add(new JLabel("Resultado (20 dezenas):"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        txtResultado = new JTextField(45); // Aumentado para acomodar 20 dezenas + separadores
        // Adicionar filtro para formatação automática mais permissivo
        ((AbstractDocument) txtResultado.getDocument()).setDocumentFilter(new ResultadoDocumentFilter(this));
        painel.add(txtResultado, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        painel.add(new JLabel("Status:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        lblStatusResultado = new JLabel("-");
        painel.add(lblStatusResultado, gbc);

        gbc.gridx = 2; gbc.gridy = 2; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0; gbc.anchor = GridBagConstraints.EAST;
        btnRegistrar = new JButton("Registrar");
        painel.add(btnRegistrar, gbc);

        return painel;
    }

    private JPanel criarSecaoAcertos() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBorder(BorderFactory.createTitledBorder("4. Calcular e Visualizar Acertos"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Linha 1: Seleção de Concurso e Botão Calcular
        gbc.gridx = 0; gbc.gridy = 0; painel.add(new JLabel("Concurso:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        cbConcursosAcertos = new JComboBox<>();
        painel.add(cbConcursosAcertos, gbc);
        gbc.gridx = 2; gbc.gridy = 0; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0; gbc.anchor = GridBagConstraints.EAST;
        btnCalcular = new JButton("Calcular Acertos");
        painel.add(btnCalcular, gbc);

        // Linha 2: Status do Cálculo
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 3; gbc.anchor = GridBagConstraints.WEST;
        lblStatusCalculo = new JLabel("Status: Selecione um concurso com resultado e clique em Calcular.");
        painel.add(lblStatusCalculo, gbc);

        // Linha 3: Informação do Concurso Calculado
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 3; gbc.anchor = GridBagConstraints.CENTER;
        lblConcursoAcertosInfo = new JLabel("Acertos para: -");
        lblConcursoAcertosInfo.setFont(lblConcursoAcertosInfo.getFont().deriveFont(Font.BOLD));
        painel.add(lblConcursoAcertosInfo, gbc);

        // Linha 4: Tabela de Acertos
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel painelTabelaAcertos = new JPanel(new GridLayout(2, 7, 2, 2));
        painelTabelaAcertos.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        String[] faixas = {"15", "16", "17", "18", "19", "20", "00"};
        Font headerFont = new Font("Arial", Font.BOLD, 12);
        Font dataFont = new Font("Arial", Font.PLAIN, 12);
        for (String faixa : faixas) {
            JLabel header = new JLabel(faixa, JLabel.CENTER);
            header.setFont(headerFont);
            painelTabelaAcertos.add(header);
        }
        for (int i = 0; i < 7; i++) {
            lblAcertosContagem[i] = new JLabel("-", JLabel.CENTER);
            lblAcertosContagem[i].setFont(dataFont);
            painelTabelaAcertos.add(lblAcertosContagem[i]);
        }
        painel.add(painelTabelaAcertos, gbc);

        // Linha 5: Botão Listar Jogos
        gbc.gridx = 2; gbc.gridy = 4; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST;
        btnListarJogos = new JButton("Listar Jogos Premiados");
        painel.add(btnListarJogos, gbc);

        return painel;
    }

    // --- Métodos de Carregamento de Dados ---

    private void carregarGrupos() {
        List<GrupoFixo> grupos = lotoService.getGruposFixosList();
        Vector<GrupoFixoWrapper> model = new Vector<>();
        model.add(new GrupoFixoWrapper(null)); // Placeholder
        grupos.forEach(g -> model.add(new GrupoFixoWrapper(g)));
        cbGrupos.setModel(new DefaultComboBoxModel<>(model));
    }

    private void carregarConcursos() {
        List<Concurso> concursos = lotoService.getConcursosList();
        Vector<ConcursoWrapper> modelVinculo = new Vector<>();
        Vector<ConcursoWrapper> modelResultado = new Vector<>();
        Vector<ConcursoWrapper> modelAcertos = new Vector<>();

        modelVinculo.add(new ConcursoWrapper(null, false));
        modelResultado.add(new ConcursoWrapper(null, false));
        modelAcertos.add(new ConcursoWrapper(null, false));

        for (Concurso c : concursos) {
            boolean temResultado = c.getNumeros() != null && !c.getNumeros().isEmpty();
            ConcursoWrapper wrapper = new ConcursoWrapper(c, temResultado);
            modelVinculo.add(wrapper);
            modelResultado.add(wrapper);
            modelAcertos.add(wrapper);
        }

        cbConcursosVinculo.setModel(new DefaultComboBoxModel<>(modelVinculo));
        cbConcursosResultado.setModel(new DefaultComboBoxModel<>(modelResultado));
        cbConcursosAcertos.setModel(new DefaultComboBoxModel<>(modelAcertos));
    }

    // --- Configuração de Listeners e Lógica de Eventos ---

    private void configurarListeners() {
        // Listener para Criar Concurso
        btnCriar.addActionListener(e -> criarNovoConcurso());

        // Listener para Vincular Grupo
        btnVincular.addActionListener(e -> vincularGrupoAoConcurso());

        // Listener para Registrar Resultado
        btnRegistrar.addActionListener(e -> registrarResultado());
        // Listener para validar resultado enquanto digita
        txtResultado.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { 
                SwingUtilities.invokeLater(() -> validarEntradaResultado()); 
            }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { 
                SwingUtilities.invokeLater(() -> validarEntradaResultado()); 
            }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { 
                SwingUtilities.invokeLater(() -> validarEntradaResultado()); 
            }
        });
        cbConcursosResultado.addActionListener(e -> atualizarEstadosBotoes()); // Atualiza status do botão ao mudar concurso

        // Listener para Calcular Acertos
        btnCalcular.addActionListener(e -> calcularEExibirAcertos());
        cbConcursosAcertos.addActionListener(e -> {
             // Limpa tabela de acertos ao mudar concurso
             limparTabelaAcertos();
             lblConcursoAcertosInfo.setText("Acertos para: -");
             lblStatusCalculo.setText("Status: Selecione um concurso com resultado e clique em Calcular.");
             atualizarEstadosBotoes();
        });

        // Listener para Listar Jogos
        btnListarJogos.addActionListener(e -> listarJogosPremiados());

        // Listeners para habilitar/desabilitar botões baseado na seleção
        cbGrupos.addActionListener(e -> atualizarEstadosBotoes());
        cbConcursosVinculo.addActionListener(e -> atualizarEstadosBotoes());
        txtNomeConcurso.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
             public void insertUpdate(javax.swing.event.DocumentEvent e) { atualizarEstadosBotoes(); }
             public void removeUpdate(javax.swing.event.DocumentEvent e) { atualizarEstadosBotoes(); }
             public void changedUpdate(javax.swing.event.DocumentEvent e) { atualizarEstadosBotoes(); }
        });
    }

    private void criarNovoConcurso() {
        String nome = txtNomeConcurso.getText().trim();
        if (nome.isEmpty()) {
            lblStatusCriacao.setText("Status: Nome não pode ser vazio.");
            lblStatusCriacao.setForeground(Color.RED);
            return;
        }
        int id = lotoService.criarConcursoEObterID(nome);
        if (id > 0) { // ID positivo indica sucesso
            lblStatusCriacao.setText("Status: Concurso criado com sucesso (ID: " + id + ").");
            lblStatusCriacao.setForeground(Color.BLUE);
            txtNomeConcurso.setText(""); // Limpa campo
            carregarConcursos(); // Recarrega dropdowns
        } else if (id == -1) { // -1 indica erro ou já existente
            lblStatusCriacao.setText("Status: Erro ao criar ou concurso já existe.");
            lblStatusCriacao.setForeground(Color.RED);
        } else {
             lblStatusCriacao.setText("Status: Erro desconhecido ao criar concurso.");
             lblStatusCriacao.setForeground(Color.RED);
        }
    }

    private void vincularGrupoAoConcurso() {
        GrupoFixoWrapper grupoWrapper = (GrupoFixoWrapper) cbGrupos.getSelectedItem();
        ConcursoWrapper concursoWrapper = (ConcursoWrapper) cbConcursosVinculo.getSelectedItem();
        
        if (grupoWrapper == null || grupoWrapper.grupo == null || concursoWrapper == null || concursoWrapper.concurso == null) {
            lblStatusVinculo.setText("Status: Selecione um grupo e um concurso.");
            lblStatusVinculo.setForeground(Color.RED);
            return;
        }
        
        lblStatusVinculo.setText("Status: Vinculando... aguarde.");
        lblStatusVinculo.setForeground(Color.BLUE);
        
        // Atualiza UI para mostrar que está processando
        SwingUtilities.invokeLater(() -> {
            int grupoId = grupoWrapper.grupo.getId();
            int concursoId = concursoWrapper.concurso.getId();
            
            int[] resultado = lotoService.aplicarGrupoFixoAoConcurso(grupoId, concursoId);
            
            if (resultado != null && resultado.length >= 2) {
                int jogosAplicados = resultado[0];
                int jogosIgnorados = resultado[1];
                
                if (jogosAplicados >= 0) {
                    lblStatusVinculo.setText("Status: " + jogosAplicados + " jogos aplicados, " + 
                                            jogosIgnorados + " ignorados.");
                    lblStatusVinculo.setForeground(Color.BLUE);
                } else {
                    lblStatusVinculo.setText("Status: Erro ao vincular grupo ao concurso.");
                    lblStatusVinculo.setForeground(Color.RED);
                }
            } else {
                lblStatusVinculo.setText("Status: Erro ao vincular grupo ao concurso.");
                lblStatusVinculo.setForeground(Color.RED);
            }
        });
    }

    private void registrarResultado() {
        ConcursoWrapper concursoWrapper = (ConcursoWrapper) cbConcursosResultado.getSelectedItem();
        if (concursoWrapper == null || concursoWrapper.concurso == null) {
            lblStatusResultado.setText("Status: Selecione um concurso.");
            lblStatusResultado.setForeground(Color.RED);
            return;
        }
        
        String resultado = txtResultado.getText().trim();
        if (!validarResultado(resultado)) {
            lblStatusResultado.setText("Status: Resultado inválido. Informe 20 dezenas (00-99).");
            lblStatusResultado.setForeground(Color.RED);
            return;
        }
        
        int concursoId = concursoWrapper.concurso.getId();
        boolean sucesso = lotoService.registrarResultadoConcurso(concursoId, resultado);
        
        if (sucesso) {
            lblStatusResultado.setText("Status: Resultado registrado com sucesso!");
            lblStatusResultado.setForeground(Color.BLUE);
            carregarConcursos(); // Recarrega para atualizar status (R)
            txtResultado.setText(""); // Limpa campo
        } else {
            lblStatusResultado.setText("Status: Erro ao registrar resultado.");
            lblStatusResultado.setForeground(Color.RED);
        }
    }

    private void calcularEExibirAcertos() {
        ConcursoWrapper concursoWrapper = (ConcursoWrapper) cbConcursosAcertos.getSelectedItem();
        if (concursoWrapper == null || concursoWrapper.concurso == null || !concursoWrapper.temResultado) {
            lblStatusCalculo.setText("Status: Selecione um concurso com resultado.");
            lblStatusCalculo.setForeground(Color.RED);
            return;
        }

        int concursoId = concursoWrapper.concurso.getId();
        btnCalcular.setEnabled(false);
        lblStatusCalculo.setText("Status: Calculando... aguarde.");
        lblStatusCalculo.setForeground(Color.BLUE);

        // Executa o cálculo em segundo plano
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                return lotoService.calcularAcertosDoConcurso(concursoId);
            }

            @Override
            protected void done() {
                try {
                    int jogosAtualizados = get();
                    if (jogosAtualizados >= 0) {
                        lblStatusCalculo.setText("Status: Acertos calculados para " + jogosAtualizados + " jogos.");
                        lblStatusCalculo.setForeground(Color.BLUE);
                        lblConcursoAcertosInfo.setText("Acertos para: " + concursoWrapper.concurso.getDescricao());
                        atualizarTabelaAcertos(concursoId);
                        btnListarJogos.setEnabled(true);
                    } else {
                        lblStatusCalculo.setText("Status: Erro ao calcular acertos.");
                        lblStatusCalculo.setForeground(Color.RED);
                        limparTabelaAcertos();
                        btnListarJogos.setEnabled(false);
                    }
                } catch (Exception ex) {
                    lblStatusCalculo.setText("Status: Erro ao calcular acertos.");
                    lblStatusCalculo.setForeground(Color.RED);
                    limparTabelaAcertos();
                    btnListarJogos.setEnabled(false);
                    ex.printStackTrace();
                } finally {
                    btnCalcular.setEnabled(true);
                }
            }
        }.execute();
    }

    private void atualizarTabelaAcertos(int concursoId) {
        int[] acertosPorFaixa = lotoService.getAcertosPorFaixa(concursoId);
        
        if (acertosPorFaixa != null && acertosPorFaixa.length >= 7) {
            for (int i = 0; i < 7; i++) {
                lblAcertosContagem[i].setText(String.valueOf(acertosPorFaixa[i]));
            }
        } else {
            limparTabelaAcertos();
        }
    }

    private void limparTabelaAcertos() {
        for (int i = 0; i < 7; i++) {
            lblAcertosContagem[i].setText("-");
        }
    }

    private void listarJogosPremiados() {
        ConcursoWrapper concursoWrapper = (ConcursoWrapper) cbConcursosAcertos.getSelectedItem();
        if (concursoWrapper != null && concursoWrapper.concurso != null) {
            mainFrame.mostrarPainel("PainelListarJogosPremiados");
        }
    }

    // --- Validação e Formatação ---

    private boolean validarResultado(String resultado) {
        if (resultado == null || resultado.isEmpty()) return false;
        
        // Remove espaços e verifica formato
        String limpo = resultado.replaceAll("\\s+", "");
        
        // Verifica se tem 20 dezenas separadas por vírgula
        String[] dezenas = limpo.split(",");
        if (dezenas.length != 20) return false;
        
        // Verifica se todas são números válidos (00-99)
        Set<Integer> numerosUnicos = new HashSet<>();
        for (String dezena : dezenas) {
            try {
                int numero = Integer.parseInt(dezena);
                if (numero < 0 || numero > 99) return false;
                if (!numerosUnicos.add(numero)) return false; // Verifica duplicidade
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        return true;
    }

    private void validarEntradaResultado() {
        String texto = txtResultado.getText().trim();
        if (texto.isEmpty()) {
            btnRegistrar.setEnabled(false);
            txtResultado.setForeground(Color.BLACK);
            return;
        }
        
        boolean valido = validarResultado(texto);
        btnRegistrar.setEnabled(valido);
        
        if (valido) {
            txtResultado.setForeground(Color.BLACK);
        } else {
            // Não mudar a cor para vermelho para permitir digitação parcial
            txtResultado.setForeground(Color.BLACK);
        }
    }

    // --- Atualização de Estado dos Botões ---

    public void atualizarEstadosBotoes() {
        // Botão Criar
        btnCriar.setEnabled(!txtNomeConcurso.getText().trim().isEmpty());
        
        // Botão Vincular
        GrupoFixoWrapper grupoWrapper = (GrupoFixoWrapper) cbGrupos.getSelectedItem();
        ConcursoWrapper concursoVinculoWrapper = (ConcursoWrapper) cbConcursosVinculo.getSelectedItem();
        btnVincular.setEnabled(grupoWrapper != null && grupoWrapper.grupo != null && 
                              concursoVinculoWrapper != null && concursoVinculoWrapper.concurso != null);
        
        // Botão Registrar
        ConcursoWrapper concursoResultadoWrapper = (ConcursoWrapper) cbConcursosResultado.getSelectedItem();
        btnRegistrar.setEnabled(concursoResultadoWrapper != null && concursoResultadoWrapper.concurso != null && 
                               validarResultado(txtResultado.getText().trim()));
        
        // Botão Calcular
        ConcursoWrapper concursoAcertosWrapper = (ConcursoWrapper) cbConcursosAcertos.getSelectedItem();
        btnCalcular.setEnabled(concursoAcertosWrapper != null && concursoAcertosWrapper.concurso != null && 
                              concursoAcertosWrapper.temResultado);
        
        // Botão Listar Jogos
        btnListarJogos.setEnabled(concursoAcertosWrapper != null && concursoAcertosWrapper.concurso != null); 
                                 
    }

    // --- Classe para Formatação Automática do Campo de Resultado ---

    private class ResultadoDocumentFilter extends DocumentFilter {
        private final PainelConcursosUnificado painel;
        private final Pattern pattern = Pattern.compile("\\d{2}");
        
        public ResultadoDocumentFilter(PainelConcursosUnificado painel) {
            this.painel = painel;
        }
        
        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
            // Permitir dígitos, vírgulas e espaços (mais permissivo)
            String filtrado = text.replaceAll("[^0-9,\\s]", "");
            if (filtrado.isEmpty()) return;
            
            super.insertString(fb, offset, filtrado, attr);
            
            // Formatar texto após inserção, mas de forma mais permissiva
            SwingUtilities.invokeLater(() -> {
                try {
                    formatarTextoPermissivo(fb);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            });
        }
        
        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            // Permitir dígitos, vírgulas e espaços (mais permissivo)
            String filtrado = text.replaceAll("[^0-9,\\s]", "");
            if (filtrado.isEmpty() && length == 0) return;
            
            super.replace(fb, offset, length, filtrado, attrs);
            
            // Formatar texto após substituição, mas de forma mais permissiva
            SwingUtilities.invokeLater(() -> {
                try {
                    formatarTextoPermissivo(fb);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            });
        }
        
        private void formatarTextoPermissivo(FilterBypass fb) throws BadLocationException {
            String texto = fb.getDocument().getText(0, fb.getDocument().getLength());
            
            // Remover espaços
            texto = texto.replaceAll("\\s+", "");
            
            // Normalizar vírgulas (substituir múltiplas vírgulas por uma)
            texto = texto.replaceAll(",+", ",");
            
            // Remover vírgula no início ou fim
            texto = texto.replaceAll("^,|,$", "");
            
            // Adicionar vírgulas a cada 2 dígitos se não houver vírgula
            StringBuilder resultado = new StringBuilder();
            int digitCount = 0;
            boolean adicionarVirgula = false;
            
            for (int i = 0; i < texto.length(); i++) {
                char c = texto.charAt(i);
                
                if (c == ',') {
                    // Resetar contagem de dígitos após vírgula
                    digitCount = 0;
                    adicionarVirgula = false;
                    resultado.append(c);
                } else {
                    // É um dígito
                    resultado.append(c);
                    digitCount++;
                    
                    // Adicionar vírgula após cada 2 dígitos
                    if (digitCount == 2 && i < texto.length() - 1 && texto.charAt(i + 1) != ',') {
                        resultado.append(',');
                        digitCount = 0;
                    }
                }
            }
            
            // Substituir texto apenas se for diferente
            String novoTexto = resultado.toString();
            if (!novoTexto.equals(texto)) {
                fb.replace(0, fb.getDocument().getLength(), novoTexto, null);
            }
            
            // Atualizar estado dos botões
            SwingUtilities.invokeLater(() -> painel.validarEntradaResultado());
        }
    }

    // Método para atualizar dados do painel quando necessário
    public void atualizarDados() {
        carregarGrupos();
        carregarConcursos();
        atualizarEstadosBotoes();
    }
    
}
