package applotomania.gui;

import javax.swing.*;
import java.awt.*;

/**
 *
 * @author Raul
 */

public class PainelInicio extends JPanel {

    public PainelInicio() {
        setLayout(new BorderLayout());

        // Conteúdo central
        JPanel centro = new JPanel();
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
        centro.setBorder(BorderFactory.createEmptyBorder(60, 80, 60, 80));

        JLabel titulo = new JLabel("🎯 Sistema LotoMania", JLabel.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 28));
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitulo = new JLabel("Gerenciamento completo de concursos, jogos e análises", JLabel.CENTER);
        subtitulo.setFont(new Font("SansSerif", Font.PLAIN, 16));
        subtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitulo.setForeground(Color.DARK_GRAY);

        JLabel versao = new JLabel("v1.0 - Interface Gráfica", JLabel.CENTER);
        versao.setFont(new Font("SansSerif", Font.ITALIC, 12));
        versao.setAlignmentX(Component.CENTER_ALIGNMENT);
        versao.setForeground(Color.GRAY);

        JLabel createdBy = new JLabel("made by Raul A.", JLabel.CENTER);
        createdBy.setFont(new Font("SansSerif", Font.ITALIC, 12));
        createdBy.setAlignmentX(Component.CENTER_ALIGNMENT);
        createdBy.setForeground(Color.GRAY);
        
        centro.add(titulo);
        centro.add(Box.createVerticalStrut(20));
        centro.add(subtitulo);
        centro.add(Box.createVerticalStrut(10));
        centro.add(versao);
        centro.add(createdBy);

        // Painel centralizado
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.add(centro);

        add(wrapper, BorderLayout.CENTER);
    }
}
