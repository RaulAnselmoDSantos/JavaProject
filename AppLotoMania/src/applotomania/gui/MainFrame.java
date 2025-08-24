package applotomania.gui;

import applotomania.service.LotoService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 *
 * @author Raul
 */
public class MainFrame extends JFrame {

    private final LotoService lotoService;
    private final CardLayout cardLayout;
    private final JPanel painelPrincipal;
    
    // Referências aos painéis para reutilização
    private PainelInicio painelInicio;
    private PainelCadastrarJogo painelCadastrarJogo;
    private PainelConcursosUnificado painelConcursosUnificado;
    private PainelListarJogosPremiados painelListarJogosPremiados;

    public MainFrame() {
        super("AppLotoMania");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        
        // Inicializar serviço
        lotoService = new LotoService();
        
        // Configurar layout principal
        setLayout(new BorderLayout());
        
        // Painel lateral (menu)
        JPanel painelLateral = criarPainelLateral();
        add(painelLateral, BorderLayout.WEST);
        
        // Painel principal com CardLayout
        cardLayout = new CardLayout();
        painelPrincipal = new JPanel(cardLayout);
        add(painelPrincipal, BorderLayout.CENTER);
        
        // Inicializar painéis
        inicializarPaineis();
        
        // Mostrar painel inicial por padrão
        cardLayout.show(painelPrincipal, "PainelInicio");
    }
    
    private JPanel criarPainelLateral() {
        JPanel painel = new JPanel();
        painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));
        painel.setBackground(new Color(50, 50, 50));
        painel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        painel.setPreferredSize(new Dimension(200, getHeight()));
        
        // Título do menu
        JLabel titulo = new JLabel("MENU");
        titulo.setForeground(Color.WHITE);
        titulo.setFont(new Font("Arial", Font.BOLD, 18));
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        painel.add(titulo);
        painel.add(Box.createVerticalStrut(20));
        
        // Botões do menu - ORDEM CORRIGIDA
        JButton btnInicio = criarBotaoMenu("Início");
        JButton btnCadastrarJogo = criarBotaoMenu("Cadastrar Jogo");
        JButton btnGerenciarConcursos = criarBotaoMenu("Gerenciar Concursos");
        
        // Adicionar botões ao painel na ordem correta
        painel.add(btnInicio);
        painel.add(Box.createVerticalStrut(10));
        painel.add(btnCadastrarJogo);
        painel.add(Box.createVerticalStrut(10));
        painel.add(btnGerenciarConcursos);
        
        // Adicionar espaço flexível para empurrar botões para cima
        painel.add(Box.createVerticalGlue());
        
        // Configurar ações dos botões
        btnInicio.addActionListener(e -> mostrarPainel("PainelInicio"));
        btnCadastrarJogo.addActionListener(e -> mostrarPainel("PainelCadastrarJogo"));
        btnGerenciarConcursos.addActionListener(e -> mostrarPainel("PainelConcursosUnificado"));
        
        return painel;
    }
    
    private JButton criarBotaoMenu(String texto) {
        JButton botao = new JButton(texto);
        botao.setAlignmentX(Component.CENTER_ALIGNMENT);
        botao.setMaximumSize(new Dimension(180, 40));
        botao.setFont(new Font("Arial", Font.BOLD, 14));
        botao.setFocusPainted(false);
        botao.setBackground(new Color(70, 70, 70));
        botao.setForeground(Color.WHITE);
        botao.setBorderPainted(false);
        
        // Efeito hover
        botao.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                botao.setBackground(new Color(100, 100, 100));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                botao.setBackground(new Color(70, 70, 70));
            }
        });
        
        return botao;
    }
    
    private void inicializarPaineis() {
        // Criar instâncias dos painéis
        painelInicio = new PainelInicio();
        painelCadastrarJogo = new PainelCadastrarJogo(lotoService);
        painelConcursosUnificado = new PainelConcursosUnificado(lotoService, this);
        painelListarJogosPremiados = new PainelListarJogosPremiados(lotoService);
        
        // Adicionar painéis ao CardLayout
        painelPrincipal.add(painelInicio, "PainelInicio");
        painelPrincipal.add(painelCadastrarJogo, "PainelCadastrarJogo");
        painelPrincipal.add(painelConcursosUnificado, "PainelConcursosUnificado");
        painelPrincipal.add(painelListarJogosPremiados, "PainelListarJogosPremiados");
    }
    
    // Método público para navegação entre painéis
    public void mostrarPainel(String nomePainel) {
        // Atualizar dados do painel antes de exibi-lo
        switch (nomePainel) {
            case "PainelCadastrarJogo":
                painelCadastrarJogo.atualizarDados();
                break;
            case "PainelConcursosUnificado":
                painelConcursosUnificado.atualizarDados();
                break;
            case "PainelListarJogosPremiados":
                painelListarJogosPremiados.atualizarDados();
                break;
        }
        
        // Mostrar o painel
        cardLayout.show(painelPrincipal, nomePainel);
    }
    
    public static void main(String[] args) {
        // Configurar look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Iniciar aplicação na thread de eventos do Swing
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
