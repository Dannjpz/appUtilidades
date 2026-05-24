package utilidades;

import utilidades.modulos.almatallada.AlmaTalladaPanel;
import utilidades.modulos.contrasenas.ContrasenasPanel;
import utilidades.modulos.wow.WowPanel;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final Color BG = new Color(245, 245, 245);

    public MainFrame() {
        setTitle("Utilidades");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setPreferredSize(new Dimension(950, 640));
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        // ── Barra superior persistente ────────────────────────
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(new MatteBorder(0, 0, 1, 0, new Color(210, 210, 215)));

        JPanel btnWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        btnWrapper.setBackground(Color.WHITE);
        btnWrapper.add(buildWowButton());
        btnWrapper.add(buildUrlButton("🌐  Bot-Traiding",    "http://139.59.182.150:8501/"));
        btnWrapper.add(buildUrlButton("🤖  IA-CLAUDE",  "https://claude.ai/"));
        topBar.add(btnWrapper, BorderLayout.WEST);

        // ── Tabs de módulos ───────────────────────────────────
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(BG);
        tabs.setForeground(new Color(30, 30, 30));
        tabs.setFont(new Font("SansSerif", Font.BOLD, 13));

        WowPanel wowPanel = new WowPanel();
        tabs.addTab("⚔ WoW", wowPanel);
        tabs.addTab("💎 Alma Tallada", new AlmaTalladaPanel());
        tabs.addTab("🔑 Contraseñas",  new ContrasenasPanel());
        // Futuros módulos:
        // tabs.addTab("🎮 Otro juego", new OtroPanel());

        add(topBar, BorderLayout.NORTH);
        add(tabs,   BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                wowPanel.guardar();
                System.exit(0);
            }
        });

        pack();
        setLocationRelativeTo(null);
    }

    private JButton buildWowButton() {
        JButton btn = new JButton();
        btn.setToolTipText("Abrir World of Warcraft");
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);

        java.io.File exe = new java.io.File("C:\\Naerzone 3.3.5a\\Wow.exe");
        if (exe.exists()) {
            try {
                Icon icon = javax.swing.filechooser.FileSystemView
                    .getFileSystemView().getSystemIcon(exe);
                Image img = ((ImageIcon) icon).getImage()
                    .getScaledInstance(32, 32, Image.SCALE_SMOOTH);
                btn.setIcon(new ImageIcon(img));
                btn.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            } catch (Exception ignored) {}
        }

        if (btn.getIcon() == null) {
            btn.setText("WoW");
            btn.setFont(new Font("SansSerif", Font.BOLD, 12));
            btn.setForeground(new Color(180, 50, 50));
            btn.setBorder(new CompoundBorder(
                new LineBorder(new Color(180, 50, 50), 1, true),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
            ));
            btn.setContentAreaFilled(true);
            btn.setOpaque(true);
            btn.setBackground(Color.WHITE);
        }

        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btn.setBorderPainted(true);
                btn.setBorder(new CompoundBorder(
                    new LineBorder(new Color(180, 50, 50), 1, true),
                    BorderFactory.createEmptyBorder(3, 7, 3, 7)
                ));
            }
            @Override public void mouseExited(MouseEvent e) {
                btn.setBorderPainted(false);
                btn.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            }
        });

        btn.addActionListener(e -> {
            try {
                new ProcessBuilder("C:\\Naerzone 3.3.5a\\Wow.exe").start();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                    "No se pudo abrir WoW:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        return btn;
    }

    private JButton buildUrlButton(String label, String url) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(60, 120, 220));
        btn.setOpaque(true);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new CompoundBorder(
            new LineBorder(new Color(45, 95, 190), 1),
            BorderFactory.createEmptyBorder(5, 12, 5, 12)
        ));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(45, 95, 190)); }
            @Override public void mouseExited(MouseEvent e)  { btn.setBackground(new Color(60, 120, 220)); }
        });
        btn.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new java.net.URI(url));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "No se pudo abrir:\n" + url, "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        return btn;
    }
}