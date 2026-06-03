package utilidades.modulos.finanzas;

import utilidades.modulos.finanzas.modelo.Categoria;
import utilidades.modulos.finanzas.modelo.Gasto;
import utilidades.modulos.finanzas.util.FinanzasManager;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;
import java.util.stream.*;

public class FinanzasPanel extends JPanel {

    // ── Paleta ────────────────────────────────────────────────
    private static final Color BG_APP       = new Color(242, 242, 247);
    private static final Color BG_PANEL     = new Color(255, 255, 255);
    private static final Color BG_SIDEBAR   = new Color(248, 248, 250);
    private static final Color ACCENT       = new Color(60, 120, 220);
    private static final Color BORDER_LIGHT = new Color(210, 210, 215);
    private static final Color TEXT_MAIN    = new Color(20, 20, 20);
    private static final Color TEXT_DIM     = new Color(130, 130, 140);
    private static final Color SEL_BG       = new Color(210, 225, 255);
    private static final Color COLOR_ING    = new Color(40, 180, 100);
    private static final Color COLOR_GASTO  = new Color(220, 70, 70);
    private static final Color COLOR_OK     = new Color(40, 180, 100);
    private static final Color COLOR_WARN   = new Color(230, 160, 30);
    private static final Color COLOR_OVER   = new Color(220, 70, 70);

    private static final String[] COLORES_PRESET = {
        "#FF6B6B","#FF9F43","#FECA57","#48DBFB","#1DD1A1",
        "#A29BFE","#FD79A8","#6C5CE7","#00B894","#E17055"
    };

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_ISO   = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DecimalFormat     FMT_DINERO = new DecimalFormat("#,##0.00");

    // ── Datos ─────────────────────────────────────────────────
    private final List<Categoria> categorias = new ArrayList<>();
    private final List<Gasto>     gastos     = new ArrayList<>();
    private double ingresoMensual = 0;

    // ── Período activo ────────────────────────────────────────
    private enum Periodo { SEMANA, MES, TODO }
    private Periodo periodoActual = Periodo.MES;

    // ── UI ────────────────────────────────────────────────────
    private JPanel      sidebarCats;
    private DefaultTableModel tableModel;
    private JTable      tablaGastos;
    private JPanel      resumenPanel;
    private JLabel      lblTotalGasto;
    private JLabel      lblDisponible;
    private JTextField  tfIngreso;
    private JComboBox<String> cbPeriodo;

    public FinanzasPanel() {
        setLayout(new BorderLayout());
        setBackground(BG_APP);
        cargarDatos();

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            buildSidebar(), buildMain());
        split.setDividerLocation(240);
        split.setDividerSize(1);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        actualizarTodo();
    }

    // ── SIDEBAR ───────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(BG_SIDEBAR);
        sidebar.setBorder(new MatteBorder(0, 0, 0, 1, BORDER_LIGHT));
        sidebar.setPreferredSize(new Dimension(240, 0));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_SIDEBAR);
        header.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER_LIGHT),
            BorderFactory.createEmptyBorder(10, 14, 10, 10)));

        JLabel lbl = new JLabel("Categorías");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        lbl.setForeground(TEXT_MAIN);

        JButton btnNuevaCat = new JButton("+");
        btnNuevaCat.setFont(new Font("SansSerif", Font.BOLD, 17));
        btnNuevaCat.setForeground(ACCENT);
        btnNuevaCat.setContentAreaFilled(false);
        btnNuevaCat.setBorderPainted(false);
        btnNuevaCat.setFocusPainted(false);
        btnNuevaCat.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnNuevaCat.setToolTipText("Nueva categoría");
        btnNuevaCat.addActionListener(e -> dlgCategoria(null));

        header.add(lbl, BorderLayout.WEST);
        header.add(btnNuevaCat, BorderLayout.EAST);

        // Período selector
        JPanel periodoPanel = new JPanel(new BorderLayout(6, 0));
        periodoPanel.setBackground(BG_SIDEBAR);
        periodoPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JLabel lblPer = new JLabel("Período:");
        lblPer.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblPer.setForeground(TEXT_DIM);

        cbPeriodo = new JComboBox<>(new String[]{"Este mes", "Esta semana", "Todo"});
        cbPeriodo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cbPeriodo.addActionListener(e -> {
            periodoActual = switch (cbPeriodo.getSelectedIndex()) {
                case 0 -> Periodo.MES;
                case 1 -> Periodo.SEMANA;
                default -> Periodo.TODO;
            };
            actualizarTodo();
        });

        periodoPanel.add(lblPer, BorderLayout.WEST);
        periodoPanel.add(cbPeriodo, BorderLayout.CENTER);

        // Panel de categorías (scrollable)
        sidebarCats = new JPanel();
        sidebarCats.setLayout(new BoxLayout(sidebarCats, BoxLayout.Y_AXIS));
        sidebarCats.setBackground(BG_SIDEBAR);

        JScrollPane scroll = new JScrollPane(sidebarCats);
        scroll.setBorder(null);
        scroll.setBackground(BG_SIDEBAR);
        scroll.getViewport().setBackground(BG_SIDEBAR);

        // Total sidebar bottom
        JPanel totalPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        totalPanel.setBackground(BG_SIDEBAR);
        totalPanel.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, BORDER_LIGHT),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        lblTotalGasto = new JLabel("Total: $0.00");
        lblTotalGasto.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblTotalGasto.setForeground(COLOR_GASTO);

        lblDisponible = new JLabel("Disponible: $0.00");
        lblDisponible.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblDisponible.setForeground(COLOR_OK);

        totalPanel.add(lblTotalGasto);
        totalPanel.add(lblDisponible);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(BG_SIDEBAR);
        top.add(header,      BorderLayout.NORTH);
        top.add(periodoPanel, BorderLayout.SOUTH);

        sidebar.add(top,        BorderLayout.NORTH);
        sidebar.add(scroll,     BorderLayout.CENTER);
        sidebar.add(totalPanel, BorderLayout.SOUTH);
        return sidebar;
    }

    // ── MAIN PANEL ────────────────────────────────────────────
    private JPanel buildMain() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(BG_APP);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tabs.setBackground(BG_APP);

        tabs.addTab("💸 Gastos",  buildGastosTab());
        tabs.addTab("📊 Resumen", buildResumenTab());

        main.add(tabs, BorderLayout.CENTER);
        return main;
    }

    // ── TAB GASTOS ────────────────────────────────────────────
    private JPanel buildGastosTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(BG_PANEL);

        // Top bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG_PANEL);
        topBar.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER_LIGHT),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        JLabel title = new JLabel("Registro de Gastos");
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        title.setForeground(TEXT_MAIN);

        JButton btnAgregar = accentBtn("+ Agregar gasto");
        btnAgregar.addActionListener(e -> dlgGasto(null));

        topBar.add(title,     BorderLayout.WEST);
        topBar.add(btnAgregar, BorderLayout.EAST);

        // Tabla
        String[] cols = {"Fecha", "Descripción", "Categoría", "Monto"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tablaGastos = new JTable(tableModel);
        tablaGastos.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tablaGastos.setForeground(TEXT_MAIN);
        tablaGastos.setBackground(BG_PANEL);
        tablaGastos.setRowHeight(30);
        tablaGastos.setGridColor(new Color(235, 235, 238));
        tablaGastos.setShowHorizontalLines(true);
        tablaGastos.setShowVerticalLines(false);
        tablaGastos.setSelectionBackground(SEL_BG);
        tablaGastos.setSelectionForeground(TEXT_MAIN);
        tablaGastos.setIntercellSpacing(new Dimension(0, 0));
        tablaGastos.setFillsViewportHeight(true);

        // Header
        JTableHeader header = tablaGastos.getTableHeader();
        header.setBackground(new Color(248, 248, 250));
        header.setForeground(TEXT_DIM);
        header.setFont(new Font("SansSerif", Font.BOLD, 11));
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_LIGHT));
        header.setReorderingAllowed(false);

        // Ancho columnas
        tablaGastos.getColumnModel().getColumn(0).setPreferredWidth(90);
        tablaGastos.getColumnModel().getColumn(1).setPreferredWidth(220);
        tablaGastos.getColumnModel().getColumn(2).setPreferredWidth(120);
        tablaGastos.getColumnModel().getColumn(3).setPreferredWidth(90);

        // Monto en rojo, alineado a la derecha
        tablaGastos.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setHorizontalAlignment(SwingConstants.RIGHT);
                setForeground(sel ? TEXT_MAIN : COLOR_GASTO);
                setFont(new Font("SansSerif", Font.BOLD, 13));
                return this;
            }
        });

        // Doble click → editar, click derecho → eliminar
        tablaGastos.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int row = tablaGastos.rowAtPoint(e.getPoint());
                if (row < 0) return;
                Gasto g = gastoEnFila(row);
                if (g == null) return;
                if (e.getClickCount() == 2)
                    dlgGasto(g);
                else if (SwingUtilities.isRightMouseButton(e)) {
                    tablaGastos.setRowSelectionInterval(row, row);
                    JPopupMenu menu = new JPopupMenu();
                    JMenuItem editar   = new JMenuItem("✏  Editar");
                    JMenuItem eliminar = new JMenuItem("🗑  Eliminar");
                    eliminar.setForeground(COLOR_GASTO);
                    editar.addActionListener(ev -> dlgGasto(g));
                    eliminar.addActionListener(ev -> {
                        if (JOptionPane.showConfirmDialog(FinanzasPanel.this,
                                "¿Eliminar gasto \"" + g.getDescripcion() + "\"?",
                                "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                            gastos.remove(g);
                            guardar();
                            actualizarTodo();
                        }
                    });
                    menu.add(editar); menu.addSeparator(); menu.add(eliminar);
                    menu.show(tablaGastos, e.getX(), e.getY());
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tablaGastos);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_PANEL);

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // ── TAB RESUMEN ───────────────────────────────────────────
    private JPanel buildResumenTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(BG_PANEL);

        // Top: ingreso editable
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 10));
        topBar.setBackground(BG_PANEL);
        topBar.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_LIGHT));

        JLabel lblIng = new JLabel("Ingreso mensual: $");
        lblIng.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblIng.setForeground(TEXT_MAIN);

        tfIngreso = new JTextField(String.valueOf((int) ingresoMensual), 10);
        tfIngreso.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tfIngreso.setBorder(new CompoundBorder(
            new LineBorder(BORDER_LIGHT, 1, true),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        tfIngreso.addActionListener(e -> {
            try {
                ingresoMensual = Double.parseDouble(tfIngreso.getText().replace(",", "").trim());
                guardar();
                actualizarTodo();
            } catch (NumberFormatException ignored) {}
        });
        tfIngreso.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) {
                tfIngreso.getActionListeners()[0].actionPerformed(null);
            }
        });

        topBar.add(lblIng);
        topBar.add(tfIngreso);

        // Panel de barras (se rebuilds en actualizarResumen)
        resumenPanel = new JPanel();
        resumenPanel.setLayout(new BoxLayout(resumenPanel, BoxLayout.Y_AXIS));
        resumenPanel.setBackground(BG_PANEL);
        resumenPanel.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JScrollPane scroll = new JScrollPane(resumenPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_PANEL);
        scroll.getVerticalScrollBar().setUnitIncrement(12);

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // ── ACTUALIZAR TODO ───────────────────────────────────────
    private void actualizarTodo() {
        actualizarSidebar();
        actualizarTabla();
        actualizarResumen();
    }

    private List<Gasto> gastosFiltrados() {
        LocalDate hoy = LocalDate.now();
        return gastos.stream().filter(g -> {
            try {
                LocalDate fecha = LocalDate.parse(g.getFecha(), FMT_ISO);
                return switch (periodoActual) {
                    case MES    -> fecha.getMonth() == hoy.getMonth() && fecha.getYear() == hoy.getYear();
                    case SEMANA -> !fecha.isBefore(hoy.with(java.time.DayOfWeek.MONDAY)) && !fecha.isAfter(hoy);
                    case TODO   -> true;
                };
            } catch (Exception e) { return false; }
        }).sorted(Comparator.comparing(Gasto::getFecha).reversed())
          .collect(Collectors.toList());
    }

    private void actualizarSidebar() {
        sidebarCats.removeAll();
        List<Gasto> filtrados = gastosFiltrados();

        double totalGasto = filtrados.stream().mapToDouble(Gasto::getMonto).sum();

        for (Categoria cat : categorias) {
            double gastoCat = filtrados.stream()
                .filter(g -> g.getCategoriaId().equals(cat.getId()))
                .mapToDouble(Gasto::getMonto).sum();

            sidebarCats.add(buildCatCard(cat, gastoCat, totalGasto));
        }
        sidebarCats.add(Box.createVerticalGlue());
        sidebarCats.revalidate();
        sidebarCats.repaint();

        // Totales — siempre ingreso completo menos lo gastado en el período
        lblTotalGasto.setText("Gastado: $" + FMT_DINERO.format(totalGasto));
        double disponible = ingresoMensual - totalGasto;
        lblDisponible.setText("Disponible: $" + FMT_DINERO.format(disponible));
        lblDisponible.setForeground(disponible >= 0 ? COLOR_OK : COLOR_OVER);
    }

    private JPanel buildCatCard(Categoria cat, double gastado, double totalGeneral) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(BG_SIDEBAR);
        card.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Fila nombre + monto
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(BG_SIDEBAR);

        Color catColor = parseColor(cat.getColor());
        JLabel dot = new JLabel("●  " + cat.getNombre());
        dot.setFont(new Font("SansSerif", Font.BOLD, 12));
        dot.setForeground(catColor);

        JLabel monto = new JLabel("$" + FMT_DINERO.format(gastado));
        monto.setFont(new Font("SansSerif", Font.PLAIN, 12));
        monto.setForeground(TEXT_MAIN);

        top.add(dot,   BorderLayout.WEST);
        top.add(monto, BorderLayout.EAST);

        // Barra de progreso
        double pct = cat.getPresupuesto() > 0
            ? gastado / cat.getPresupuesto()
            : (totalGeneral > 0 ? gastado / totalGeneral : 0);
        final double pctFinal = Math.min(pct, 1.0);

        Color barColor = cat.getPresupuesto() > 0
            ? (pct >= 1.0 ? COLOR_OVER : pct >= 0.8 ? COLOR_WARN : COLOR_OK)
            : catColor;

        JPanel barBg = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(220, 220, 225));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                g2.setColor(barColor);
                g2.fillRoundRect(0, 0, (int)(getWidth() * pctFinal), getHeight(), 4, 4);
            }
        };
        barBg.setPreferredSize(new Dimension(0, 6));
        barBg.setOpaque(false);

        // Presupuesto label
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(BG_SIDEBAR);
        bottom.add(barBg, BorderLayout.CENTER);
        if (cat.getPresupuesto() > 0) {
            JLabel lblPres = new JLabel("  $" + FMT_DINERO.format(cat.getPresupuesto()));
            lblPres.setFont(new Font("SansSerif", Font.PLAIN, 10));
            lblPres.setForeground(TEXT_DIM);
            bottom.add(lblPres, BorderLayout.EAST);
        }

        card.add(top,    BorderLayout.NORTH);
        card.add(bottom, BorderLayout.CENTER);

        // Click derecho → editar / eliminar categoría
        MouseAdapter ctxMenu = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    JPopupMenu menu = new JPopupMenu();
                    JMenuItem edit = new JMenuItem("✏  Editar categoría");
                    JMenuItem del  = new JMenuItem("🗑  Eliminar");
                    del.setForeground(COLOR_GASTO);
                    edit.addActionListener(ev -> dlgCategoria(cat));
                    del.addActionListener(ev -> {
                        if (JOptionPane.showConfirmDialog(FinanzasPanel.this,
                                "¿Eliminar categoría \"" + cat.getNombre() + "\"?\n" +
                                "Los gastos asociados quedarán sin categoría.",
                                "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                            categorias.remove(cat);
                            guardar(); actualizarTodo();
                        }
                    });
                    menu.add(edit); menu.addSeparator(); menu.add(del);
                    menu.show(card, e.getX(), e.getY());
                } else if (e.getClickCount() == 2) {
                    dlgCategoria(cat);
                }
            }
        };
        card.addMouseListener(ctxMenu);
        top.addMouseListener(ctxMenu);

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(230, 230, 233));
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_SIDEBAR);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(card, BorderLayout.CENTER);
        wrapper.add(sep,  BorderLayout.SOUTH);
        return wrapper;
    }

    private void actualizarTabla() {
        tableModel.setRowCount(0);
        for (Gasto g : gastosFiltrados()) {
            String nombreCat = categorias.stream()
                .filter(c -> c.getId().equals(g.getCategoriaId()))
                .map(Categoria::getNombre).findFirst().orElse("Sin categoría");
            String fechaFmt = g.getFecha();
            try { fechaFmt = LocalDate.parse(g.getFecha(), FMT_ISO).format(FMT_FECHA); }
            catch (Exception ignored) {}
            tableModel.addRow(new Object[]{
                fechaFmt, g.getDescripcion(), nombreCat,
                "$" + FMT_DINERO.format(g.getMonto())
            });
        }
    }

    private void actualizarResumen() {
        resumenPanel.removeAll();
        List<Gasto> filtrados = gastosFiltrados();
        double total = filtrados.stream().mapToDouble(Gasto::getMonto).sum();

        if (total == 0) {
            JLabel vacio = new JLabel("No hay gastos en este período.");
            vacio.setFont(new Font("SansSerif", Font.ITALIC, 13));
            vacio.setForeground(TEXT_DIM);
            vacio.setAlignmentX(Component.LEFT_ALIGNMENT);
            resumenPanel.add(vacio);
            resumenPanel.revalidate(); resumenPanel.repaint();
            return;
        }

        // Ingreso y disponible — siempre el ingreso completo menos lo gastado en el período
        double ingresoRef = ingresoMensual;
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 10, 0));
        statsRow.setBackground(BG_PANEL);
        statsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        statsRow.add(statCard("Gastado",    "$" + FMT_DINERO.format(total),            COLOR_GASTO));
        statsRow.add(statCard("Ingreso",    "$" + FMT_DINERO.format(ingresoRef),        COLOR_ING));
        statsRow.add(statCard("Disponible", "$" + FMT_DINERO.format(ingresoRef - total),
            ingresoRef - total >= 0 ? COLOR_OK : COLOR_OVER));
        resumenPanel.add(statsRow);
        resumenPanel.add(Box.createVerticalStrut(18));

        // Título barras
        JLabel lblBarras = new JLabel("Gasto por categoría");
        lblBarras.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblBarras.setForeground(TEXT_MAIN);
        lblBarras.setAlignmentX(Component.LEFT_ALIGNMENT);
        resumenPanel.add(lblBarras);
        resumenPanel.add(Box.createVerticalStrut(10));

        // Ordenar categorías por gasto descendente
        List<Categoria> ordenadas = categorias.stream()
            .filter(c -> filtrados.stream().anyMatch(g -> g.getCategoriaId().equals(c.getId())))
            .sorted((a, b) -> {
                double ga = filtrados.stream().filter(g -> g.getCategoriaId().equals(a.getId())).mapToDouble(Gasto::getMonto).sum();
                double gb = filtrados.stream().filter(g -> g.getCategoriaId().equals(b.getId())).mapToDouble(Gasto::getMonto).sum();
                return Double.compare(gb, ga);
            }).collect(Collectors.toList());

        double maxCat = ordenadas.isEmpty() ? 1 : filtrados.stream()
            .filter(g -> g.getCategoriaId().equals(ordenadas.get(0).getId()))
            .mapToDouble(Gasto::getMonto).sum();

        for (Categoria cat : ordenadas) {
            double gastoCat = filtrados.stream()
                .filter(g -> g.getCategoriaId().equals(cat.getId()))
                .mapToDouble(Gasto::getMonto).sum();
            double pctTotal = total > 0 ? gastoCat / total : 0;
            double pctBar   = maxCat > 0 ? gastoCat / maxCat : 0;
            Color  col      = parseColor(cat.getColor());

            resumenPanel.add(buildBarRow(cat.getNombre(), gastoCat, pctTotal, pctBar, col,
                cat.getPresupuesto()));
            resumenPanel.add(Box.createVerticalStrut(8));
        }

        resumenPanel.revalidate();
        resumenPanel.repaint();
    }

    private JPanel buildBarRow(String nombre, double monto, double pctTotal,
                                double pctBar, Color color, double presupuesto) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(BG_PANEL);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        JLabel lblNom = new JLabel(nombre);
        lblNom.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblNom.setForeground(color);
        lblNom.setPreferredSize(new Dimension(130, 0));

        // Barra
        final double pct = pctBar;
        JPanel barra = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int h = getHeight(), w = getWidth();
                int barH = 14, barY = (h - barH) / 2;
                g2.setColor(new Color(230, 230, 233));
                g2.fillRoundRect(0, barY, w, barH, 6, 6);
                g2.setColor(color);
                g2.fillRoundRect(0, barY, (int)(w * pct), barH, 6, 6);
            }
        };
        barra.setOpaque(false);

        String textoMonto = "$" + FMT_DINERO.format(monto) +
            "  (" + String.format("%.1f", pctTotal * 100) + "%)";
        if (presupuesto > 0) textoMonto += "  /  $" + FMT_DINERO.format(presupuesto);
        JLabel lblMonto = new JLabel(textoMonto);
        lblMonto.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblMonto.setForeground(TEXT_DIM);
        lblMonto.setPreferredSize(new Dimension(220, 0));
        lblMonto.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(lblNom,   BorderLayout.WEST);
        row.add(barra,    BorderLayout.CENTER);
        row.add(lblMonto, BorderLayout.EAST);
        return row;
    }

    private JPanel statCard(String titulo, String valor, Color color) {
        JPanel card = new JPanel(new GridLayout(2, 1, 0, 2));
        card.setBackground(new Color(248, 248, 250));
        card.setBorder(new CompoundBorder(
            new LineBorder(BORDER_LIGHT, 1, true),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        JLabel lblT = new JLabel(titulo);
        lblT.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblT.setForeground(TEXT_DIM);

        JLabel lblV = new JLabel(valor);
        lblV.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblV.setForeground(color);

        card.add(lblT); card.add(lblV);
        return card;
    }

    // ── DIÁLOGO GASTO ─────────────────────────────────────────
    private void dlgGasto(Gasto gastoEditar) {
        boolean nuevo = gastoEditar == null;
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this) instanceof Frame f ? f : null,
            nuevo ? "Agregar gasto" : "Editar gasto", true);
        dlg.setSize(420, 300);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG_PANEL);
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 10, 16));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.fill = GridBagConstraints.HORIZONTAL;

        JTextField tfMonto = dlgField(nuevo ? "" : String.valueOf(gastoEditar.getMonto()));
        JTextField tfDesc  = dlgField(nuevo ? "" : gastoEditar.getDescripcion());
        JTextField tfFecha = dlgField(nuevo ? LocalDate.now().format(FMT_ISO)
            : gastoEditar.getFecha());

        String[] catNombres = categorias.stream().map(Categoria::getNombre).toArray(String[]::new);
        JComboBox<String> cbCat = new JComboBox<>(catNombres);
        if (!nuevo) {
            String catNom = categorias.stream().filter(c -> c.getId().equals(gastoEditar.getCategoriaId()))
                .map(Categoria::getNombre).findFirst().orElse("");
            cbCat.setSelectedItem(catNom);
        }

        g.gridy=0; g.gridx=0; g.weightx=0; form.add(dlgLabel("Monto ($):"),        g);
        g.gridx=1; g.weightx=1;             form.add(tfMonto,                         g);
        g.gridy=1; g.gridx=0; g.weightx=0; form.add(dlgLabel("Descripción:"),       g);
        g.gridx=1; g.weightx=1;             form.add(tfDesc,                          g);
        g.gridy=2; g.gridx=0; g.weightx=0; form.add(dlgLabel("Categoría:"),         g);
        g.gridx=1; g.weightx=1;             form.add(cbCat,                           g);
        g.gridy=3; g.gridx=0; g.weightx=0; form.add(dlgLabel("Fecha (yyyy-MM-dd):"),g);
        g.gridx=1; g.weightx=1;             form.add(tfFecha,                         g);

        JPanel bots = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        bots.setBackground(BG_PANEL);
        bots.setBorder(new MatteBorder(1, 0, 0, 0, BORDER_LIGHT));
        JButton btnCancel = grayBtn("Cancelar");
        JButton btnOk     = accentBtn(nuevo ? "Agregar" : "Guardar");

        btnCancel.addActionListener(e -> dlg.dispose());
        btnOk.addActionListener(e -> {
            try {
                double monto = Double.parseDouble(tfMonto.getText().trim());
                int catIdx   = cbCat.getSelectedIndex();
                if (catIdx < 0 || categorias.isEmpty()) {
                    JOptionPane.showMessageDialog(dlg, "Selecciona una categoría.");
                    return;
                }
                String catId = categorias.get(catIdx).getId();
                String desc  = tfDesc.getText().trim();
                String fecha = tfFecha.getText().trim();
                LocalDate.parse(fecha, FMT_ISO); // validar formato

                if (nuevo) {
                    gastos.add(new Gasto(monto, catId, desc, fecha));
                } else {
                    gastoEditar.setMonto(monto);
                    gastoEditar.setCategoriaId(catId);
                    gastoEditar.setDescripcion(desc);
                    gastoEditar.setFecha(fecha);
                }
                guardar(); actualizarTodo(); dlg.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dlg, "El monto debe ser un número.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Fecha inválida. Usa yyyy-MM-dd");
            }
        });

        bots.add(btnCancel); bots.add(btnOk);
        dlg.add(form, BorderLayout.CENTER);
        dlg.add(bots, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // ── DIÁLOGO CATEGORÍA ─────────────────────────────────────
    private void dlgCategoria(Categoria catEditar) {
        boolean nuevo = catEditar == null;
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this) instanceof Frame f ? f : null,
            nuevo ? "Nueva categoría" : "Editar categoría", true);
        dlg.setSize(380, 230);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG_PANEL);
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 10, 16));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.fill = GridBagConstraints.HORIZONTAL;

        JTextField tfNombre = dlgField(nuevo ? "" : catEditar.getNombre());
        JTextField tfPres   = dlgField(nuevo ? "0" : String.valueOf((int)catEditar.getPresupuesto()));

        // Color selector
        JComboBox<String> cbColor = new JComboBox<>(COLORES_PRESET);
        cbColor.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v,
                    int i, boolean sel, boolean foc) {
                super.getListCellRendererComponent(l, v, i, sel, foc);
                setText("   ");
                setBackground(parseColor((String) v));
                setOpaque(true);
                return this;
            }
        });
        if (!nuevo) {
            for (int i = 0; i < COLORES_PRESET.length; i++)
                if (COLORES_PRESET[i].equals(catEditar.getColor())) cbColor.setSelectedIndex(i);
        }

        g.gridy=0; g.gridx=0; g.weightx=0; form.add(dlgLabel("Nombre:"),          g);
        g.gridx=1; g.weightx=1;             form.add(tfNombre,                      g);
        g.gridy=1; g.gridx=0; g.weightx=0; form.add(dlgLabel("Color:"),            g);
        g.gridx=1; g.weightx=1;             form.add(cbColor,                       g);
        g.gridy=2; g.gridx=0; g.weightx=0; form.add(dlgLabel("Límite ($, 0=sin):"),g);
        g.gridx=1; g.weightx=1;             form.add(tfPres,                        g);

        JPanel bots = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        bots.setBackground(BG_PANEL);
        bots.setBorder(new MatteBorder(1, 0, 0, 0, BORDER_LIGHT));
        JButton btnCancel = grayBtn("Cancelar");
        JButton btnOk     = accentBtn(nuevo ? "Crear" : "Guardar");

        btnCancel.addActionListener(e -> dlg.dispose());
        btnOk.addActionListener(e -> {
            String nombre = tfNombre.getText().trim();
            if (nombre.isBlank()) { JOptionPane.showMessageDialog(dlg, "Escribe un nombre."); return; }
            double pres = 0;
            try { pres = Double.parseDouble(tfPres.getText().trim()); } catch (Exception ignored) {}
            String color = COLORES_PRESET[cbColor.getSelectedIndex()];

            if (nuevo) {
                categorias.add(new Categoria(nombre, color, pres));
            } else {
                catEditar.setNombre(nombre);
                catEditar.setColor(color);
                catEditar.setPresupuesto(pres);
            }
            guardar(); actualizarTodo(); dlg.dispose();
        });

        bots.add(btnCancel); bots.add(btnOk);
        dlg.add(form, BorderLayout.CENTER);
        dlg.add(bots, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // ── HELPERS UI ────────────────────────────────────────────
    private JTextField dlgField(String val) {
        JTextField tf = new JTextField(val);
        tf.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tf.setForeground(TEXT_MAIN);
        tf.setBorder(new CompoundBorder(new LineBorder(BORDER_LIGHT, 1, true),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        return tf;
    }
    private JLabel dlgLabel(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        l.setForeground(TEXT_DIM);
        return l;
    }
    private JButton accentBtn(String txt) {
        JButton b = new JButton(txt);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setForeground(Color.WHITE);
        b.setBackground(ACCENT);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorder(new CompoundBorder(new LineBorder(new Color(45,95,190),1,true),
            BorderFactory.createEmptyBorder(5,14,5,14)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(new Color(45,95,190)); }
            @Override public void mouseExited(MouseEvent e)  { b.setBackground(ACCENT); }
        });
        return b;
    }
    private JButton grayBtn(String txt) {
        JButton b = new JButton(txt);
        b.setFont(new Font("SansSerif", Font.PLAIN, 12));
        b.setForeground(TEXT_MAIN);
        b.setBackground(new Color(238,238,242));
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorder(new CompoundBorder(new LineBorder(BORDER_LIGHT,1,true),
            BorderFactory.createEmptyBorder(5,14,5,14)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
    private Color parseColor(String hex) {
        try { return Color.decode(hex); }
        catch (Exception e) { return Color.GRAY; }
    }
    private Gasto gastoEnFila(int row) {
        List<Gasto> f = gastosFiltrados();
        return row < f.size() ? f.get(row) : null;
    }

    // ── PERSISTENCIA ─────────────────────────────────────────
    private void cargarDatos() {
        String raw = FinanzasManager.leerArchivo();
        if (raw == null) { cargarDefaults(); return; }
        ingresoMensual = FinanzasManager.cargarIngreso(raw)[0];
        categorias.addAll(FinanzasManager.cargarCategorias(raw));
        gastos.addAll(FinanzasManager.cargarGastos(raw));
        if (categorias.isEmpty()) cargarDefaults();
    }

    private void cargarDefaults() {
        String[][] defaults = {
            {"Comida",         "#FF6B6B"},
            {"Transporte",     "#FF9F43"},
            {"Entretenimiento","#FECA57"},
            {"Salud",          "#1DD1A1"},
            {"Servicios",      "#48DBFB"},
            {"Otros",          "#A29BFE"},
        };
        for (String[] d : defaults)
            categorias.add(new Categoria(d[0], d[1], 0));
    }

    private void guardar() {
        FinanzasManager.guardar(ingresoMensual, categorias, gastos);
    }
}