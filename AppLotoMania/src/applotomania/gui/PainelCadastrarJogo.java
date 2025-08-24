package applotomania.gui;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Raul
 */

public class PainelCadastrarJogo extends JPanel {

    private final LotoService lotoService;
    private JComboBox<GrupoFixoWrapper> cbGrupos;
    private JTextField txtJogoNumeros;
    private JLabel lblStatus;
    private JLabel lblQtdJogos;
    private JButton btnCadastrar;
    private JButton btnLimpar;
    private JButton[] botoesNumeros = new JButton[100];
    private JCheckBox chkEspelho;
    
    // Constante para limitar o número máximo de dezenas
    private static final int MAX_DEZENAS = 50;

    // Wrapper para ComboBox
    private static class GrupoFixoWrapper {
        GrupoFixo grupo;
        GrupoFixoWrapper(GrupoFixo g) { this.grupo = g; }
        @Override public String toString() { return grupo != null ? grupo.getNome() : "Selecione..."; }
    }

    public PainelCadastrarJogo(LotoService service) {
        this.lotoService = service;
        
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titulo = new JLabel("CADASTRAR JOGO", JLabel.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 20));
        add(titulo, BorderLayout.NORTH);

        JPanel painelCentral = new JPanel(new BorderLayout(10, 10));
        
        // Painel de criação de grupo
        JPanel painelCriarGrupo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        painelCriarGrupo.add(new JLabel("Cadastrar Grupo:"));
        JTextField txtNovoGrupo = new JTextField(20);
        painelCriarGrupo.add(txtNovoGrupo);
        JButton btnCriarGrupo = new JButton("Criar Grupo");
        btnCriarGrupo.addActionListener(e -> {
            String nomeGrupo = txtNovoGrupo.getText().trim();
            if (!nomeGrupo.isEmpty()) {
                boolean sucesso = lotoService.criarGrupoFixo(nomeGrupo);
                if (sucesso) {
                    lblStatus.setText("Status: Grupo '" + nomeGrupo + "' criado com sucesso!");
                    lblStatus.setForeground(Color.BLUE);
                    txtNovoGrupo.setText("");
                    carregarGrupos();
                } else {
                    lblStatus.setText("Status: Erro ao criar grupo. Verifique se já existe.");
                    lblStatus.setForeground(Color.RED);
                }
            } else {
                lblStatus.setText("Status: Nome do grupo não pode ser vazio.");
                lblStatus.setForeground(Color.RED);
            }
        });
        painelCriarGrupo.add(btnCriarGrupo);
        
        painelCentral.add(painelCriarGrupo, BorderLayout.NORTH);
        
        // Painel de seleção de grupo
        JPanel painelGrupo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        painelGrupo.add(new JLabel("Grupo:"));
        cbGrupos = new JComboBox<>();
        cbGrupos.setPreferredSize(new Dimension(200, 25));
        painelGrupo.add(cbGrupos);
        
        // Label para quantidade de jogos
        lblQtdJogos = new JLabel("QTD: 0");
        lblQtdJogos.setFont(new Font("Arial", Font.BOLD, 12));
        painelGrupo.add(Box.createHorizontalStrut(20));
        painelGrupo.add(lblQtdJogos);
        
        JPanel painelSuperior = new JPanel(new BorderLayout());
        painelSuperior.add(painelCriarGrupo, BorderLayout.NORTH);
        painelSuperior.add(painelGrupo, BorderLayout.SOUTH);
        
        painelCentral.add(painelSuperior, BorderLayout.NORTH);
        
        // Painel central com grade de números e campo de texto
        JPanel painelJogo = new JPanel(new BorderLayout(10, 10));
        
        // Campo de texto para números
        JPanel painelTexto = new JPanel(new BorderLayout());
        painelTexto.setBorder(BorderFactory.createTitledBorder("Jogo (50 dezenas):"));
        txtJogoNumeros = new JTextField();
        
        // Adicionar filtro para formatação automática de vírgulas
        ((AbstractDocument) txtJogoNumeros.getDocument()).setDocumentFilter(new NumeroDocumentFilter(this));
        
        painelTexto.add(txtJogoNumeros, BorderLayout.CENTER);
        
        // Checkbox para jogo espelho
        chkEspelho = new JCheckBox("Cadastrar Jogo Espelho automaticamente");
        chkEspelho.setSelected(true);
        chkEspelho.setToolTipText("Cria automaticamente um jogo com as dezenas complementares (99-dezena)");
        painelTexto.add(chkEspelho, BorderLayout.SOUTH);
        
        painelJogo.add(painelTexto, BorderLayout.NORTH);
        
        // Grade de números 10x10
        JPanel painelGrade = new JPanel(new GridLayout(10, 10, 2, 2));
        painelGrade.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        for (int i = 0; i < 100; i++) {
            final int numero = i;
            botoesNumeros[i] = new JButton(String.format("%02d", i));
            botoesNumeros[i].setMargin(new Insets(2, 2, 2, 2));
            botoesNumeros[i].setFocusPainted(false);
            botoesNumeros[i].addActionListener(e -> adicionarNumero(numero));
            painelGrade.add(botoesNumeros[i]);
        }
        
        painelJogo.add(painelGrade, BorderLayout.CENTER);
        painelCentral.add(painelJogo, BorderLayout.CENTER);
        
        // Painel inferior com status e botões
        JPanel painelInferior = new JPanel(new BorderLayout());
        
        // Status
        lblStatus = new JLabel("Status: Selecione um grupo e informe as dezenas do jogo.");
        painelInferior.add(lblStatus, BorderLayout.NORTH);
        
        // Botões
        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnLimpar = new JButton("Limpar");
        btnCadastrar = new JButton("Cadastrar Jogo");
        
        painelBotoes.add(btnLimpar);
        painelBotoes.add(btnCadastrar);
        painelInferior.add(painelBotoes, BorderLayout.SOUTH);
        
        painelCentral.add(painelInferior, BorderLayout.SOUTH);
        add(painelCentral, BorderLayout.CENTER);
        
        // Configurar listeners
        configurarListeners();
        
        // Carregar dados iniciais
        atualizarDados();
    }
    
    // Método para atualizar dados do painel
    public void atualizarDados() {
        carregarGrupos();
        atualizarQuantidadeJogos();
    }
    
    private void carregarGrupos() {
        List<GrupoFixo> grupos = lotoService.getGruposFixosList();
        Vector<GrupoFixoWrapper> model = new Vector<>();
        model.add(new GrupoFixoWrapper(null)); // Placeholder
        grupos.forEach(g -> model.add(new GrupoFixoWrapper(g)));
        cbGrupos.setModel(new DefaultComboBoxModel<>(model));
    }
    
    private void atualizarQuantidadeJogos() {
        // Garantir que a atualização ocorra na thread de eventos do Swing
        SwingUtilities.invokeLater(() -> {
            GrupoFixoWrapper wrapper = (GrupoFixoWrapper) cbGrupos.getSelectedItem();
            if (wrapper != null && wrapper.grupo != null) {
                int grupoId = wrapper.grupo.getId();
                System.out.println("[DEBUG] Atualizando QTD para grupo ID: " + grupoId);
                
                int qtd = lotoService.getQuantidadeJogosPorGrupo(grupoId);
                System.out.println("[DEBUG] Quantidade retornada: " + qtd);
                
                // Verificar se o valor retornado é válido
                if (qtd >= 0) {
                    lblQtdJogos.setText("QTD: " + qtd);
                    System.out.println("[DEBUG] QTD atualizado para: " + qtd);
                } else {
                    lblQtdJogos.setText("QTD: 0");
                    System.out.println("[DEBUG] QTD inválido, definido como 0");
                }
                
                // Forçar repaint do componente
                lblQtdJogos.repaint();
            } else {
                lblQtdJogos.setText("QTD: 0");
                System.out.println("[DEBUG] Nenhum grupo selecionado, QTD definido como 0");
            }
        });
    }
    
    private void configurarListeners() {
        // Listener para seleção de grupo
        cbGrupos.addActionListener(e -> {
            System.out.println("[DEBUG] Grupo selecionado: " + cbGrupos.getSelectedItem());
            atualizarQuantidadeJogos();
        });
        
        // Listener para botão Limpar
        btnLimpar.addActionListener(e -> {
            txtJogoNumeros.setText("");
            limparSelecaoBotoes();
            lblStatus.setText("Status: Campos limpos.");
            lblStatus.setForeground(Color.BLACK);
        });
        
        // Listener para botão Cadastrar
        btnCadastrar.addActionListener(e -> cadastrarJogo());
        
        // Listener para campo de texto
        txtJogoNumeros.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { 
                SwingUtilities.invokeLater(() -> atualizarBotoesComTexto()); 
            }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { 
                SwingUtilities.invokeLater(() -> atualizarBotoesComTexto()); 
            }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { 
                SwingUtilities.invokeLater(() -> atualizarBotoesComTexto()); 
            }
        });
    }
    
    private void adicionarNumero(int numero) {
        // Verificar se o número já está no campo
        String texto = txtJogoNumeros.getText();
        String numeroFormatado = String.format("%02d", numero);
        
        // Verificar se o número já está selecionado (verificando a cor do botão)
        if (botoesNumeros[numero].getBackground() != null && 
            botoesNumeros[numero].getBackground().equals(new Color(200, 230, 255))) {
            // Se já está selecionado, remover o número
            removerNumero(numero);
            return;
        }
        
        // Verificar se já atingiu o limite de dezenas
        int dezenasAtuais = contarDezenas(texto);
        if (dezenasAtuais >= MAX_DEZENAS) {
            lblStatus.setText("Status: Limite de " + MAX_DEZENAS + " dezenas atingido.");
            lblStatus.setForeground(Color.RED);
            return;
        }
        
        // Adicionar o número ao campo
        if (texto.isEmpty()) {
            txtJogoNumeros.setText(numeroFormatado);
        } else if (texto.endsWith(",")) {
            txtJogoNumeros.setText(texto + numeroFormatado);
        } else {
            txtJogoNumeros.setText(texto + "," + numeroFormatado);
        }
        
        // Atualizar status
        lblStatus.setText("Status: Dezena " + numeroFormatado + " adicionada.");
        lblStatus.setForeground(Color.BLUE);
        
        // Destacar o botão
        botoesNumeros[numero].setBackground(new Color(200, 230, 255));
        botoesNumeros[numero].setForeground(Color.BLACK);
        botoesNumeros[numero].setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
    }
    
    private void removerNumero(int numero) {
        String texto = txtJogoNumeros.getText();
        String numeroFormatado = String.format("%02d", numero);
        
        // Remover o número do texto
        String novoTexto = removerDezenaDaString(texto, numeroFormatado);
        txtJogoNumeros.setText(novoTexto);
        
        // Atualizar status
        lblStatus.setText("Status: Dezena " + numeroFormatado + " removida.");
        lblStatus.setForeground(Color.BLUE);
        
        // Restaurar cor do botão
        botoesNumeros[numero].setBackground(null);
        botoesNumeros[numero].setForeground(null);
        botoesNumeros[numero].setBorder(UIManager.getBorder("Button.border"));
    }
    
    private String removerDezenaDaString(String texto, String dezena) {
        // Remover a dezena do texto, considerando as vírgulas
        String[] partes = texto.split(",");
        StringBuilder resultado = new StringBuilder();
        
        for (int i = 0; i < partes.length; i++) {
            if (!partes[i].equals(dezena)) {
                if (resultado.length() > 0) {
                    resultado.append(",");
                }
                resultado.append(partes[i]);
            }
        }
        
        return resultado.toString();
    }
    
    private int contarDezenas(String texto) {
        if (texto == null || texto.isEmpty()) {
            return 0;
        }
        
        // Contar vírgulas e adicionar 1
        int count = 1;
        for (int i = 0; i < texto.length(); i++) {
            if (texto.charAt(i) == ',') {
                count++;
            }
        }
        
        return count;
    }
    
    public void atualizarBotoesComTexto() {
        // Limpar todos os botões
        limparSelecaoBotoes();
        
        // Obter números do texto
        String texto = txtJogoNumeros.getText();
        if (texto.isEmpty()) return;
        
        // Extrair números usando regex
        Pattern pattern = Pattern.compile("\\d{2}");
        Matcher matcher = pattern.matcher(texto);
        
        while (matcher.find()) {
            try {
                int numero = Integer.parseInt(matcher.group());
                if (numero >= 0 && numero < 100) {
                    botoesNumeros[numero].setBackground(new Color(200, 230, 255));
                    botoesNumeros[numero].setForeground(Color.BLACK);
                    botoesNumeros[numero].setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
                }
            } catch (NumberFormatException e) {
                // Ignorar
            }
        }
    }
    
    private void limparSelecaoBotoes() {
        for (JButton btn : botoesNumeros) {
            btn.setBackground(null);
            btn.setForeground(null);
            btn.setBorder(UIManager.getBorder("Button.border"));
        }
    }
    
    private void cadastrarJogo() {
        GrupoFixoWrapper wrapper = (GrupoFixoWrapper) cbGrupos.getSelectedItem();
        if (wrapper == null || wrapper.grupo == null) {
            lblStatus.setText("Status: Selecione um grupo válido.");
            lblStatus.setForeground(Color.RED);
            return;
        }
        
        String numerosTexto = txtJogoNumeros.getText().trim();
        if (numerosTexto.isEmpty()) {
            lblStatus.setText("Status: Informe as dezenas do jogo.");
            lblStatus.setForeground(Color.RED);
            return;
        }
        
        // Remover vírgula final se existir
        if (numerosTexto.endsWith(",")) {
            numerosTexto = numerosTexto.substring(0, numerosTexto.length() - 1);
        }
        
        // Verificar se há dezenas repetidas
        Set<String> dezenas = new HashSet<>();
        Pattern pattern = Pattern.compile("\\d{2}");
        Matcher matcher = pattern.matcher(numerosTexto);
        
        while (matcher.find()) {
            String dezena = matcher.group();
            if (!dezenas.add(dezena)) {
                lblStatus.setText("Status: Dezena " + dezena + " repetida. Remova a repetição.");
                lblStatus.setForeground(Color.RED);
                return;
            }
        }
        
        // Verificar limite de dezenas
        if (dezenas.size() > MAX_DEZENAS) {
            lblStatus.setText("Status: Limite de " + MAX_DEZENAS + " dezenas excedido.");
            lblStatus.setForeground(Color.RED);
            return;
        }
        
        boolean gerarEspelho = chkEspelho.isSelected();
        System.out.println("[DEBUG] Cadastrando jogo para grupo ID: " + wrapper.grupo.getId());
        
        // Corrigido para tratar o retorno long como long
        long resultado = lotoService.adicionarJogoFixoIndividual(wrapper.grupo.getId(), numerosTexto, gerarEspelho);
        System.out.println("[DEBUG] Resultado do cadastro: " + resultado);
        
        if (resultado > 0) {
            lblStatus.setText("Status: Jogo cadastrado com sucesso! ID: " + resultado);
            lblStatus.setForeground(Color.BLUE);
            txtJogoNumeros.setText("");
            limparSelecaoBotoes();
            
            // Atualizar QTD após cadastro bem-sucedido
            System.out.println("[DEBUG] Atualizando QTD após cadastro bem-sucedido");
            atualizarQuantidadeJogos();
        } else if (resultado == -1) {
            lblStatus.setText("Status: Formato inválido. Verifique as dezenas.");
            lblStatus.setForeground(Color.RED);
        } else if (resultado == -2) {
            lblStatus.setText("Status: Jogo duplicado. Este jogo já existe no grupo.");
            lblStatus.setForeground(Color.RED);
        } else if (resultado == -3) {
            lblStatus.setText("Status: Erro ao criar jogo espelho. Já existe um espelho para este jogo.");
            lblStatus.setForeground(Color.RED);
        } else {
            lblStatus.setText("Status: Erro ao cadastrar jogo. Código: " + resultado);
            lblStatus.setForeground(Color.RED);
        }
    }
    
    // Filtro para formatação automática de vírgulas no campo de texto
    private static class NumeroDocumentFilter extends DocumentFilter {
        private final PainelCadastrarJogo painel;
        
        public NumeroDocumentFilter(PainelCadastrarJogo painel) {
            this.painel = painel;
        }
        
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            // Permitir apenas dígitos e vírgulas
            String filtrado = string.replaceAll("[^0-9,]", "");
            if (filtrado.isEmpty()) return;
            
            // Obter texto atual
            String textoAtual = fb.getDocument().getText(0, fb.getDocument().getLength());
            String antes = textoAtual.substring(0, offset);
            String depois = textoAtual.substring(offset);
            
            // Inserir texto filtrado com formatação
            String novoTexto = formatarTextoComVirgulas(antes + filtrado + depois);
            
            // Substituir todo o texto
            super.remove(fb, 0, fb.getDocument().getLength());
            super.insertString(fb, 0, novoTexto, attr);
            
            // Atualizar botões após a inserção
            SwingUtilities.invokeLater(() -> painel.atualizarBotoesComTexto());
        }
        
        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            // Permitir apenas dígitos e vírgulas
            String filtrado = text.replaceAll("[^0-9,]", "");
            if (filtrado.isEmpty() && length == 0) return;
            
            // Obter texto atual
            String textoAtual = fb.getDocument().getText(0, fb.getDocument().getLength());
            String antes = textoAtual.substring(0, offset);
            String depois = textoAtual.substring(offset + length);
            
            // Inserir texto filtrado com formatação
            String novoTexto = formatarTextoComVirgulas(antes + filtrado + depois);
            
            // Substituir todo o texto
            super.remove(fb, 0, fb.getDocument().getLength());
            super.insertString(fb, 0, novoTexto, attrs);
            
            // Atualizar botões após a substituição
            SwingUtilities.invokeLater(() -> painel.atualizarBotoesComTexto());
        }
        
        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            // Obter texto atual
            String textoAtual = fb.getDocument().getText(0, fb.getDocument().getLength());
            String antes = textoAtual.substring(0, offset);
            String depois = textoAtual.substring(offset + length);
            
            // Formatar texto após remoção
            String novoTexto = formatarTextoComVirgulas(antes + depois);
            
            // Substituir todo o texto
            super.remove(fb, 0, fb.getDocument().getLength());
            super.insertString(fb, 0, novoTexto, null);
            
            // Atualizar botões após a remoção
            SwingUtilities.invokeLater(() -> painel.atualizarBotoesComTexto());
        }
        
        private String formatarTextoComVirgulas(String texto) {
            // Remover todas as vírgulas existentes
            String apenasNumeros = texto.replaceAll(",", "");
            StringBuilder resultado = new StringBuilder();
            
            // Adicionar vírgulas a cada 2 dígitos
            for (int i = 0; i < apenasNumeros.length(); i++) {
                resultado.append(apenasNumeros.charAt(i));
                if (i % 2 == 1 && i < apenasNumeros.length() - 1) {
                    resultado.append(",");
                }
            }
            
            return resultado.toString();
        }
    }
}
