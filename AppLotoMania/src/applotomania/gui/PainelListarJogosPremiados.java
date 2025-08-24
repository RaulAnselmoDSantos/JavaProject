package applotomania.gui;

import applotomania.model.Concurso;
import applotomania.model.GrupoFixo;
import applotomania.service.LotoService;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import applotomania.db.Conexao;
import java.util.List;
import java.util.ArrayList;

/**
 * Painel para listar jogos premiados
 * Exibe o nome do grupo e a cartela (seq id dentro do grupo)
 * Colore acertos conforme a faixa (20 e 00 = azul, etc.)
 * @author Raul
 */
public class PainelListarJogosPremiados extends JPanel {
    private LotoService lotoService;
    private Map<Integer, String> grupoNomes;
    private JComboBox<ConcursoWrapper> cbConcursos;
    private JComboBox<String> cbFiltroAcertos;
    private JTable tblJogos;
    private DefaultTableModel modelJogos;
    private JLabel lblJogosEncontrados;
    private JButton btnFechar;

    public PainelListarJogosPremiados() {
        this.lotoService = new LotoService();
        this.grupoNomes = lotoService.getGruposFixosList()
            .stream().collect(Collectors.toMap(GrupoFixo::getId, GrupoFixo::getNome));
        initComponents();
        carregarConcursos();
    }

    public PainelListarJogosPremiados(LotoService service) {
        this.lotoService = service;
        this.grupoNomes = lotoService.getGruposFixosList()
            .stream().collect(Collectors.toMap(GrupoFixo::getId, GrupoFixo::getNome));
        initComponents();
        carregarConcursos();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        JLabel lblTitulo = new JLabel("LISTAR JOGOS PREMIADOS");
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 24));
        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);
        add(lblTitulo, BorderLayout.NORTH);

        JPanel painelFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        JLabel lblConcurso = new JLabel("Selecionar Concurso:");
        cbConcursos = new JComboBox<>();
        cbConcursos.setPreferredSize(new Dimension(400, 30));
        cbConcursos.addActionListener(e -> {
            ConcursoWrapper wrapper = (ConcursoWrapper) cbConcursos.getSelectedItem();
            if (wrapper != null && wrapper.concurso != null) carregarJogosPremiados(wrapper.concurso.getId());
            else limparTabela();
        });
        JLabel lblFiltro = new JLabel("Filtrar por Acertos:");
        cbFiltroAcertos = new JComboBox<>(new String[]{"Todos","15 Acertos","16 Acertos","17 Acertos","18 Acertos","19 Acertos","20 Acertos","00 Acertos"});
        cbFiltroAcertos.setPreferredSize(new Dimension(150,30));
        cbFiltroAcertos.addActionListener(e -> {
            ConcursoWrapper wrapper = (ConcursoWrapper) cbConcursos.getSelectedItem();
            if (wrapper!=null && wrapper.concurso!=null) carregarJogosPremiados(wrapper.concurso.getId());
        });
        painelFiltros.add(lblConcurso); painelFiltros.add(cbConcursos);
        painelFiltros.add(lblFiltro); painelFiltros.add(cbFiltroAcertos);

        JPanel painelCentral = new JPanel(new BorderLayout(10,10));
        painelCentral.add(painelFiltros, BorderLayout.NORTH);
        lblJogosEncontrados = new JLabel("0 jogos premiados encontrados");
        lblJogosEncontrados.setFont(new Font("Arial", Font.BOLD, 12));
        lblJogosEncontrados.setForeground(Color.BLUE);
        painelCentral.add(lblJogosEncontrados, BorderLayout.CENTER);

        String[] colunas = {"Grupo","Cartela","Jogo","Acertos","Espelho"};
        modelJogos = new DefaultTableModel(colunas,0) { @Override public boolean isCellEditable(int r,int c){return false;} };
        tblJogos = new JTable(modelJogos);
        tblJogos.getColumnModel().getColumn(0).setPreferredWidth(100);
        tblJogos.getColumnModel().getColumn(1).setPreferredWidth(50);
        tblJogos.getColumnModel().getColumn(2).setPreferredWidth(500);
        tblJogos.getColumnModel().getColumn(3).setPreferredWidth(80);
        tblJogos.getColumnModel().getColumn(4).setPreferredWidth(80);

        DefaultTableCellRenderer acertosRenderer = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table,Object value,boolean isSelected,boolean hasFocus,int row,int column){
                Component c = super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
                if(column==3 && value!=null){
                    int a=(int)value; Color bg;
                    switch(a){case 20: case 0: bg=Color.BLUE; break;
                                case 19: bg=Color.GREEN; break;
                                case 18: bg=new Color(102,204,255); break;
                                case 17: bg=new Color(153,255,153); break;
                                case 16: bg=new Color(255,255,102); break;
                                case 15: bg=Color.ORANGE; break;
                                default: bg=table.getBackground();}
                    c.setBackground(bg); c.setForeground(Color.BLACK);
                } else { c.setBackground(table.getBackground()); c.setForeground(table.getForeground()); }
                return c;
            }
        };
        acertosRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tblJogos.getColumnModel().getColumn(3).setCellRenderer(acertosRenderer);
        DefaultTableCellRenderer center = new DefaultTableCellRenderer(); center.setHorizontalAlignment(SwingConstants.CENTER);
        tblJogos.getColumnModel().getColumn(4).setCellRenderer(center);

        JScrollPane scroll = new JScrollPane(tblJogos); scroll.setPreferredSize(new Dimension(800,400));
        painelCentral.add(scroll, BorderLayout.SOUTH);

        JPanel painelInf = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnRec = new JButton("Recarregar Jogos"); btnRec.setPreferredSize(new Dimension(150,40));
        btnRec.addActionListener(e->{ConcursoWrapper w=(ConcursoWrapper)cbConcursos.getSelectedItem();if(w!=null&&w.concurso!=null)carregarJogosPremiados(w.concurso.getId());});
        painelInf.add(btnRec);
        btnFechar=new JButton("Fechar"); btnFechar.setPreferredSize(new Dimension(120,40));
        btnFechar.addActionListener(e->{Container p=getParent();if(p instanceof JPanel) ((CardLayout)p.getLayout()).show(p,"PainelConcursosUnificado");});
        painelInf.add(btnFechar);

        add(painelCentral,BorderLayout.CENTER);
        add(painelInf,BorderLayout.SOUTH);
    }

    /**
     * Carrega a lista de concursos
     */
    public void atualizarDados() {
        carregarConcursos();
        ConcursoWrapper wrapper = (ConcursoWrapper) cbConcursos.getSelectedItem();
        if (wrapper!=null && wrapper.concurso!=null) carregarJogosPremiados(wrapper.concurso.getId()); else limparTabela();
        System.out.println("DEBUG: Dados do painel de jogos premiados atualizados");
    }

    private void carregarConcursos(){
        cbConcursos.removeAllItems();
        for(Concurso c: lotoService.getConcursosComResultado()) cbConcursos.addItem(new ConcursoWrapper(c));
        if(cbConcursos.getItemCount()>0) cbConcursos.setSelectedIndex(0);
    }

    private void carregarJogosPremiados(int concursoId) {
        limparTabela();
        System.out.println("DEBUG: Carregando jogos premiados para concurso " + concursoId);

        // 1) Montar map de cartelas para TODOS os jogos_fixos
        Map<Integer,Integer> cartelaMap   = new HashMap<>();
        Map<Integer,Integer> seqMaster    = new HashMap<>();
        String sqlCart = "SELECT id, grupo_id FROM jogos_fixos ORDER BY grupo_id, id";
        try (java.sql.Connection conn = Conexao.conectar();
             java.sql.Statement stmt  = conn.createStatement();
             java.sql.ResultSet rs    = stmt.executeQuery(sqlCart)) {
            while (rs.next()) {
                int jogoFixoId = rs.getInt("id");
                int grupoId    = rs.getInt("grupo_id");
                int seq        = seqMaster.getOrDefault(grupoId, 0) + 1;
                seqMaster.put(grupoId, seq);
                cartelaMap.put(jogoFixoId, seq);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 2) Pegar do serviço só os jogos premiados já filtrados por acerto (0 e 15–20)
        List<Map<String,Object>> todos = lotoService.listarJogosPremiados(concursoId);

        // 3) Aplicar filtro de faixa, se houver
        String sel = (String) cbFiltroAcertos.getSelectedItem();
        List<Map<String,Object>> filt = new ArrayList<>();
        if (sel != null && !sel.equals("Todos")) {
            int faixa = sel.equals("00 Acertos") ? 0 : Integer.parseInt(sel.split(" ")[0]);
            for (Map<String,Object> j : todos) {
                if ((int) j.get("acertos") == faixa) {
                    filt.add(j);
                }
            }
        } else {
            filt = todos;
        }

        // 4) Preencher a tabela usando o cartelaMap
        for (Map<String,Object> j : filt) {
            int grupoId     = (int) j.get("grupo_id");
            String grpName  = grupoNomes.getOrDefault(grupoId, "");
            int jogoFixoId  = (int) j.get("jogo_fixo_id");       // precisa estar vindo do service!
            int cartela     = cartelaMap.getOrDefault(jogoFixoId, 0);
            String numeros  = (String) j.get("numeros");
            int acertos     = (int) j.get("acertos");
            boolean espelho = (boolean) j.get("espelho");

            modelJogos.addRow(new Object[]{
                grpName,
                cartela,
                formatarNumerosParaExibicao(numeros),
                acertos,
                espelho ? "Sim" : "Não"
            });
            System.out.println("DEBUG: Grupo=" + grpName +
                               " cartela=" + cartela +
                               " acertos=" + acertos +
                               " espelho=" + espelho);
        }

        lblJogosEncontrados.setText(filt.size() + " jogos premiados encontrados");
        System.out.println("DEBUG: " + filt.size() + " jogos carregados");
    }

    private void limparTabela(){ modelJogos.setRowCount(0); lblJogosEncontrados.setText("0 jogos premiados encontrados"); }

    private static class ConcursoWrapper{ public final Concurso concurso; public ConcursoWrapper(Concurso c){this.concurso=c;} @Override public String toString(){ if(concurso==null) return "Selecione..."; String d=concurso.getDescricao(); return (d==null||d.isEmpty())?"Concurso "+concurso.getId():d+" (Com Resultado)";} }

    private String formatarNumerosParaExibicao(String numeros) { return numeros.replace(",", " "); }
}