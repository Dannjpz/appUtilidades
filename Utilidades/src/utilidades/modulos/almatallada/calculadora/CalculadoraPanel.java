package utilidades.modulos.almatallada.calculadora;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Properties;

public class CalculadoraPanel extends JPanel {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// ── Sistema de colores ────────────────────────────────────────────────
    // Fondos
    static final Color BG        = new Color(245, 245, 247);   // fondo principal
    static final Color BG_CARD   = new Color(255, 255, 255);   // tarjetas
    static final Color BG_INPUT  = new Color(250, 250, 252);   // inputs
    static final Color BG_ALT    = new Color(248, 248, 250);   // filas alternas
    static final Color BG_SEL    = new Color(210, 225, 255);   // selección tabla

    // Textos
    static final Color TEXT      = new Color(20, 20, 20); // TEXT              // texto principal
    static final Color TEXT_SEC  = new Color(130, 130, 140);// texto secundario
    static final Color TEXT_HINT = new Color(160, 160, 170);// hints/placeholders

    // Bordes
    static final Color BORDER    = new Color(210, 225, 255);   // bordes normales
    static final Color BORDER_LT = new Color(190, 190, 195);   // bordes claros

    // Acciones
    static final Color VERDE     = new Color(100, 200, 120);
    static final Color ROJO      = new Color(220, 80, 80);

    static final File CONFIG = new File(System.getProperty("user.home"), "almatallada_calc.properties");

    JTextField tfLuzHora, tfMDFHoja, tfDesgaste, tfManoObra;
    JTextField tfDescripcion, tfNumPiezas;
    JTextField tfHorasLaserPieza, tfHorasDisenoTotal, tfHojasMDF;
    JCheckBox  cbPintura;
    JTextField tfPintura;
    JTextField tfOtros, tfGanancia;

    JLabel lCostoTotal, lPrecioUnitario, lPrecioProyecto, lGananciaTotal, lResumen;
    DefaultTableModel modelDesglose;
    DefaultTableModel historialModel;

    CardLayout cardLayout;
    JPanel     cardPanel;
    JPanel     panelIndicador;
    JButton    btnSiguiente, btnAnterior;
    JLabel[]   lblPasos = new JLabel[4];
    int        pasoActual = 0;
    static final int TOTAL_PASOS = 4;
    static final String[] NOMBRES_PASOS = {"1 · Producto", "2 · Material", "3 · Precio", "4 · Resultados"};

    public CalculadoraPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(BG);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(crearTopBar(),    BorderLayout.NORTH);
        add(crearWizard(),    BorderLayout.CENTER);
        add(crearHistorial(), BorderLayout.SOUTH);
        cargarConfig();
    }

    // ── Barra costos base ─────────────────────────────────────────────────
    JPanel crearTopBar() {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setBackground(BG_CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(8, 14, 8, 14)));

        JLabel lbl = new JLabel("Costos base");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        lbl.setForeground(TEXT);
        p.add(lbl, BorderLayout.WEST);

        JPanel campos = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        campos.setOpaque(false);

        tfLuzHora  = campoTop("4.00");
        tfMDFHoja  = campoTop("20.00");
        tfDesgaste = campoTop("5.00");
        tfManoObra = campoTop("0.00");

        campos.add(lblTop("Luz/hr $"));       campos.add(tfLuzHora);
        campos.add(lblSep());
        campos.add(lblTop("MDF/hoja $"));     campos.add(tfMDFHoja);
        campos.add(lblSep());
        campos.add(lblTop("Desgaste/hr $"));  campos.add(tfDesgaste);
        campos.add(lblSep());
        campos.add(lblTop("Mano obra/hr $")); campos.add(tfManoObra);

        FocusListener fl = new FocusAdapter() {
            public void focusLost(FocusEvent e) { guardarConfig(); }
        };
        for (JTextField tf : new JTextField[]{tfLuzHora, tfMDFHoja, tfDesgaste, tfManoObra})
            tf.addFocusListener(fl);

        p.add(campos, BorderLayout.CENTER);
        return p;
    }

    // ── Wizard ────────────────────────────────────────────────────────────
    JPanel crearWizard() {
        JPanel outer = new JPanel(new BorderLayout(0, 8));
        outer.setBackground(BG);
        outer.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        outer.add(crearIndicador(), BorderLayout.NORTH);

        cardLayout = new CardLayout();
        cardPanel  = new JPanel(cardLayout);
        cardPanel.setBackground(BG);
        cardPanel.add(crearPaso1(), "0");
        cardPanel.add(crearPaso2(), "1");
        cardPanel.add(crearPaso3(), "2");
        cardPanel.add(crearPaso4(), "3");
        outer.add(cardPanel, BorderLayout.CENTER);

        JPanel nav = new JPanel(new BorderLayout());
        nav.setBackground(BG);
        nav.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        btnAnterior  = btnSecundario("◀  Anterior");
        btnSiguiente = btnPrimario("Siguiente  ▶");
        btnAnterior.setEnabled(false);
        btnAnterior.addActionListener(e -> irPaso(pasoActual - 1));
        btnSiguiente.addActionListener(e -> {
            if (pasoActual == TOTAL_PASOS - 1) guardarEnHistorial();
            else { if (pasoActual == TOTAL_PASOS - 2) recalcular(); irPaso(pasoActual + 1); }
        });
        nav.add(btnAnterior,  BorderLayout.WEST);
        nav.add(btnSiguiente, BorderLayout.EAST);
        outer.add(nav, BorderLayout.SOUTH);
        return outer;
    }

    JPanel crearIndicador() {
        panelIndicador = new JPanel(new GridLayout(1, 4, 6, 0));
        panelIndicador.setBackground(BG);
        panelIndicador.setPreferredSize(new Dimension(0, 38));

        for (int i = 0; i < 4; i++) {
            lblPasos[i] = new PasoLabel(NOMBRES_PASOS[i], i == 0);
            panelIndicador.add(lblPasos[i]);
        }
        return panelIndicador;
    }

    void irPaso(int paso) {
        if (paso == TOTAL_PASOS - 1) recalcular();
        pasoActual = paso;
        cardLayout.show(cardPanel, String.valueOf(paso));
        btnAnterior.setEnabled(paso > 0);

        for (int i = 0; i < 4; i++) {
            PasoLabel pl = (PasoLabel) lblPasos[i];
            if (i == paso)       pl.setEstado(PasoLabel.Estado.ACTIVO);
            else if (i < paso)   pl.setEstado(PasoLabel.Estado.COMPLETO);
            else                 pl.setEstado(PasoLabel.Estado.PENDIENTE);
        }

        boolean esUltimo = paso == TOTAL_PASOS - 1;
        btnSiguiente.setText(esUltimo ? "Guardar en historial" : "Siguiente  ▶");
    }

    // ── Paso con resplandor ───────────────────────────────────────────────
    static class PasoLabel extends JLabel {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		enum Estado { ACTIVO, COMPLETO, PENDIENTE }

        private static final Color GLOW_COLOR   = new Color(60, 120, 220);
        private static final Color ACTIVO_BG    = new Color(60, 120, 220);
        private static final Color COMPLETO_BG  = new Color(220, 235, 255);
        private static final Color PENDIENTE_BG = new Color(245, 245, 247);

        private Estado estado;
        private float  glowAlpha = 0f;
        private javax.swing.Timer glowTimer;

        PasoLabel(String texto, boolean activo) {
            super(texto, SwingConstants.CENTER);
            setOpaque(false);
            setFont(new Font("SansSerif", Font.BOLD, 11));
            setEstado(activo ? Estado.ACTIVO : Estado.PENDIENTE);
        }

        void setEstado(Estado nuevoEstado) {
            this.estado = nuevoEstado;
            if (glowTimer != null) glowTimer.stop();

            if (nuevoEstado == Estado.ACTIVO) {
                setForeground(Color.WHITE);
                // Animar entrada del glow
                glowAlpha = 0f;
                glowTimer = new javax.swing.Timer(16, null);
                glowTimer.addActionListener(e -> {
                    glowAlpha = Math.min(1f, glowAlpha + 0.08f);
                    repaint();
                    if (glowAlpha >= 1f) glowTimer.stop();
                });
                glowTimer.start();
            } else if (nuevoEstado == Estado.COMPLETO) {
                setForeground(new Color(60, 120, 220));
                glowAlpha = 0f;
            } else {
                setForeground(new Color(150, 150, 160));
                glowAlpha = 0f;
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int arc = 6;

            if (estado == Estado.ACTIVO && glowAlpha > 0) {
                // Capas de resplandor exterior
                for (int i = 8; i >= 1; i--) {
                    float alpha = glowAlpha * (0.06f * (9 - i));
                    g2.setColor(new Color(
                        GLOW_COLOR.getRed(), GLOW_COLOR.getGreen(), GLOW_COLOR.getBlue(),
                        (int)(alpha * 255)));
                    int pad = i * 2;
                    g2.fillRoundRect(-pad, -pad, w + pad * 2, h + pad * 2, arc + pad, arc + pad);
                }
                // Fondo sólido activo
                g2.setColor(ACTIVO_BG);
                g2.fillRoundRect(0, 0, w, h, arc, arc);

            } else if (estado == Estado.COMPLETO) {
                g2.setColor(COMPLETO_BG);
                g2.fillRoundRect(0, 0, w, h, arc, arc);
                g2.setColor(new Color(60, 120, 220, 120));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

            } else {
                g2.setColor(PENDIENTE_BG);
                g2.fillRoundRect(0, 0, w, h, arc, arc);
                g2.setColor(new Color(200, 200, 205));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ── Pasos ─────────────────────────────────────────────────────────────
    JPanel crearPaso1() {
        JPanel p = contenedor();
        p.add(Box.createVerticalStrut(14));
        p.add(campoGuiado("Nombre o descripción del producto",
            "Ej: Ángeles navideños MDF 3mm  /  Centro de mesa boda",
            tfDescripcion = campo("")));
        p.add(Box.createVerticalStrut(16));
        p.add(campoGuiado("¿Cuántas piezas vas a hacer en total?",
            "Si haces el mismo diseño varias veces, pon el total. Los costos se dividen automáticamente.",
            tfNumPiezas = campo("1")));
        p.add(Box.createVerticalStrut(14));
        p.add(nota("Si haces 24 ángeles, pon 24. Si es un solo letrero, pon 1."));
        p.add(Box.createVerticalGlue());
        return p;
    }

    JPanel crearPaso2() {
        JPanel p = contenedor();
        p.add(Box.createVerticalStrut(14));
        p.add(campoGuiado("Horas de láser — POR PIEZA individual",
            "¿Cuánto tarda el láser en UNA pieza?  0.08 = 5min  ·  0.25 = 15min  ·  0.5 = 30min  ·  1.0 = 1hr",
            tfHorasLaserPieza = campo("0.25")));
        p.add(Box.createVerticalStrut(12));
        p.add(campoGuiado("Horas de diseño — del proyecto completo",
            "Tiempo total que tardaste diseñando en la computadora. Se reparte entre todas las piezas.",
            tfHorasDisenoTotal = campo("1.0")));
        p.add(Box.createVerticalStrut(12));
        p.add(campoGuiado("Hojas de MDF — en total",
            "Número de planillas 60×60 para todo el pedido. Puede ser decimal. Ej: 2.5",
            tfHojasMDF = campo("1.0")));
        p.add(Box.createVerticalStrut(12));

        JPanel rowPint = new JPanel(new BorderLayout(8, 0));
        rowPint.setOpaque(false);
        rowPint.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        cbPintura = new JCheckBox("¿Usas pintura?  —  Costo total del lote ($)");
        cbPintura.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cbPintura.setForeground(TEXT);
        cbPintura.setBackground(BG_CARD);
        tfPintura = campo("0.00");
        tfPintura.setEnabled(false);
        tfPintura.setMaximumSize(new Dimension(90, 28));
        tfPintura.setPreferredSize(new Dimension(90, 28));
        cbPintura.addActionListener(e -> tfPintura.setEnabled(cbPintura.isSelected()));
        rowPint.add(cbPintura, BorderLayout.WEST);
        rowPint.add(tfPintura, BorderLayout.EAST);
        p.add(rowPint);

        p.add(Box.createVerticalStrut(12));
        p.add(nota("Las horas de láser son POR PIEZA. La app multiplica por el número de piezas automáticamente."));
        p.add(Box.createVerticalGlue());
        return p;
    }

    JPanel crearPaso3() {
        JPanel p = contenedor();
        p.add(Box.createVerticalStrut(14));
        p.add(campoGuiado("Otros costos del proyecto ($)",
            "Empaque, traslado, consumibles extra. Pon 0 si no aplica.",
            tfOtros = campo("0.00")));
        p.add(Box.createVerticalStrut(16));
        p.add(campoGuiado("Multiplicador de ganancia",
            "¿Cuántas veces el costo quieres cobrar? Ej: 3.0 = cobras 3 veces lo que te costó.",
            tfGanancia = campo("3.0")));
        p.add(Box.createVerticalStrut(16));

        JPanel tabRef = new JPanel(new GridLayout(4, 2, 10, 6));
        tabRef.setBackground(new Color(248, 248, 250));
        tabRef.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_LT),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        tabRef.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        String[][] refs = {{"2.0","100% de ganancia"},{"2.5","150% de ganancia"},
                           {"3.0","200% de ganancia"},{"4.0","300% de ganancia"}};
        for (String[] r : refs) {
            JLabel lm = new JLabel(r[0]);
            lm.setFont(new Font("SansSerif", Font.BOLD, 12));
            lm.setForeground(Color.WHITE);
            JLabel ld = new JLabel(r[1]);
            ld.setFont(new Font("SansSerif", Font.PLAIN, 11));
            ld.setForeground(TEXT_SEC);
            tabRef.add(lm); tabRef.add(ld);
        }
        p.add(tabRef);
        p.add(Box.createVerticalGlue());
        return p;
    }

    JPanel crearPaso4() {
        JPanel p = contenedor();

        lResumen = new JLabel("", SwingConstants.CENTER);
        lResumen.setFont(new Font("SansSerif", Font.BOLD, 13));
        lResumen.setForeground(TEXT);
        lResumen.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        p.add(Box.createVerticalStrut(8));
        p.add(lResumen);
        p.add(Box.createVerticalStrut(12));

        JPanel cards = new JPanel(new GridLayout(2, 2, 8, 8));
        cards.setOpaque(false);
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        lCostoTotal     = lbRes(TEXT_SEC);
        lPrecioUnitario = lbRes(TEXT);
        lPrecioProyecto = lbRes(TEXT);
        lGananciaTotal  = lbRes(VERDE);

        cards.add(tarjeta("Costo total",         lCostoTotal,     new Color(252, 252, 255)));
        cards.add(tarjeta("Precio por pieza",     lPrecioUnitario, new Color(248, 248, 252)));
        cards.add(tarjeta("Precio del proyecto",  lPrecioProyecto, new Color(248, 248, 252)));
        cards.add(tarjeta("Tu ganancia",          lGananciaTotal,  new Color(252, 252, 255)));
        p.add(cards);

        p.add(Box.createVerticalStrut(10));
        JLabel lblDes = new JLabel("Desglose:");
        lblDes.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblDes.setForeground(TEXT_SEC);
        lblDes.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        p.add(lblDes);
        p.add(Box.createVerticalStrut(4));

        modelDesglose = new DefaultTableModel(new String[]{"Concepto","Detalle","Subtotal"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tDes = tabla(modelDesglose);
        JScrollPane sp = new JScrollPane(tDes);
        sp.setBorder(BorderFactory.createLineBorder(BORDER));
        sp.getViewport().setBackground(BG_CARD);
        sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        p.add(sp);
        p.add(Box.createVerticalGlue());
        return p;
    }

    // ── Recalcular ─────────────────────────────────────────────────────────
    void recalcular() {
        try {
            double luz      = d(tfLuzHora);
            double mdf      = d(tfMDFHoja);
            double desgaste = d(tfDesgaste);
            double manoObra = d(tfManoObra);
            int    piezas   = Math.max(1, i(tfNumPiezas));

            double hLaserPieza  = d(tfHorasLaserPieza);
            double hLaserTotal  = hLaserPieza * piezas;
            double hDisenoTotal = d(tfHorasDisenoTotal);
            double hojasMDF     = d(tfHojasMDF);
            double pintura      = cbPintura.isSelected() ? d(tfPintura) : 0;
            double otros        = d(tfOtros);
            double mult         = Math.max(1.0, d(tfGanancia));

            double cLuz      = hLaserTotal * luz;
            double cDesgaste = hLaserTotal * desgaste;
            double cMO       = (hLaserTotal + hDisenoTotal) * manoObra;
            double cMDF      = hojasMDF * mdf;
            double costoTotal = cLuz + cDesgaste + cMO + cMDF + pintura + otros;

            double precioProyecto = costoTotal * mult;
            double precioUnitario = precioProyecto / piezas;
            double ganancia       = precioProyecto - costoTotal;

            String desc = tfDescripcion.getText().isBlank() ? "Proyecto" : tfDescripcion.getText();
            lResumen.setText(desc + "  ·  " + piezas + " pieza(s)");

            lCostoTotal.setText("$" + f(costoTotal));
            lPrecioUnitario.setText("$" + f(precioUnitario));
            lPrecioProyecto.setText("$" + f(precioProyecto));
            lGananciaTotal.setText("$" + f(ganancia));
            lGananciaTotal.setForeground(ganancia >= 0 ? VERDE : ROJO);

            modelDesglose.setRowCount(0);
            modelDesglose.addRow(new Object[]{"Luz",            f(hLaserTotal)+" hr",               "$"+f(cLuz)});
            modelDesglose.addRow(new Object[]{"Desgaste laser", f(hLaserTotal)+" hr",               "$"+f(cDesgaste)});
            modelDesglose.addRow(new Object[]{"Mano de obra",   f(hLaserTotal+hDisenoTotal)+" hr",  "$"+f(cMO)});
            modelDesglose.addRow(new Object[]{"MDF",            f(hojasMDF)+" hojas",               "$"+f(cMDF)});
            if (cbPintura.isSelected())
                modelDesglose.addRow(new Object[]{"Pintura","lote","$"+f(pintura)});
            if (otros > 0)
                modelDesglose.addRow(new Object[]{"Otros","---","$"+f(otros)});
            modelDesglose.addRow(new Object[]{"TOTAL","","$"+f(costoTotal)});
        } catch (Exception ignored) {}
    }

    void guardarEnHistorial() {
        recalcular();
        String desc = tfDescripcion.getText().isBlank() ? "Sin descripcion" : tfDescripcion.getText();
        historialModel.addRow(new Object[]{desc, tfNumPiezas.getText(),
            lCostoTotal.getText(), lPrecioUnitario.getText(), lGananciaTotal.getText()});
        guardarConfig();
        int op = JOptionPane.showConfirmDialog(this,
            "Guardado en historial.\n¿Calcular otro producto?", "Listo", JOptionPane.YES_NO_OPTION);
        if (op == JOptionPane.YES_OPTION) { limpiar(); irPaso(0); }
    }

    void limpiar() {
        tfDescripcion.setText(""); tfNumPiezas.setText("1");
        tfHorasLaserPieza.setText("0.25"); tfHorasDisenoTotal.setText("1.0");
        tfHojasMDF.setText("1.0"); tfPintura.setText("0.00");
        tfOtros.setText("0.00"); tfGanancia.setText("3.0");
        cbPintura.setSelected(false); tfPintura.setEnabled(false);
    }

    // ── Historial ─────────────────────────────────────────────────────────
    JPanel crearHistorial() {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(BG);
        p.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, BORDER),
            BorderFactory.createEmptyBorder(8, 0, 0, 0)));
        p.setPreferredSize(new Dimension(0, 130));

        JLabel lbl = new JLabel("HISTORIAL DE PROYECTOS");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        lbl.setForeground(TEXT_SEC);
        p.add(lbl, BorderLayout.NORTH);

        historialModel = new DefaultTableModel(
                new String[]{"Descripcion","Piezas","Costo total","Precio/pieza","Ganancia"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tHist = tabla(historialModel);
        JScrollPane sp = new JScrollPane(tHist);
        sp.setBorder(BorderFactory.createLineBorder(BORDER));
        sp.getViewport().setBackground(BG_CARD);
        p.add(sp, BorderLayout.CENTER);

        JButton btnBorrar = btnDestructivo("Borrar seleccionado");
        btnBorrar.addActionListener(e -> {
            int row = tHist.getSelectedRow();
            if (row >= 0) { historialModel.removeRow(row); guardarConfig(); }
        });
        JPanel rb = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        rb.setBackground(BG);
        rb.add(btnBorrar);
        p.add(rb, BorderLayout.SOUTH);
        return p;
    }

    // ── Guardar / Cargar ───────────────────────────────────────────────────
    void guardarConfig() {
        Properties p = new Properties();
        p.setProperty("luz",      tfLuzHora.getText());
        p.setProperty("mdf",      tfMDFHoja.getText());
        p.setProperty("desgaste", tfDesgaste.getText());
        p.setProperty("manoObra", tfManoObra.getText());
        p.setProperty("hist.rows", String.valueOf(historialModel.getRowCount()));
        for (int i = 0; i < historialModel.getRowCount(); i++)
            for (int j = 0; j < 5; j++) {
                Object v = historialModel.getValueAt(i, j);
                p.setProperty("h."+i+"."+j, v != null ? v.toString() : "");
            }
        try (FileOutputStream o = new FileOutputStream(CONFIG)) { p.store(o, "AT"); }
        catch (IOException ignored) {}
    }

    void cargarConfig() {
        if (!CONFIG.exists()) return;
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(CONFIG)) {
            p.load(in);
            tfLuzHora.setText(p.getProperty("luz",      "4.00"));
            tfMDFHoja.setText(p.getProperty("mdf",      "20.00"));
            tfDesgaste.setText(p.getProperty("desgaste","5.00"));
            tfManoObra.setText(p.getProperty("manoObra","0.00"));
            int rows = Integer.parseInt(p.getProperty("hist.rows","0"));
            for (int i = 0; i < rows; i++) {
                Object[] row = new Object[5];
                for (int j = 0; j < 5; j++) row[j] = p.getProperty("h."+i+"."+j,"");
                historialModel.addRow(row);
            }
        } catch (Exception ignored) {}
    }

    // ── Helpers UI ─────────────────────────────────────────────────────────
    JPanel contenedor() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG_CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(12, 18, 12, 18)));
        return p;
    }

    JPanel campoGuiado(String etiqueta, String ayuda, JTextField tf) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));

        JLabel lE = new JLabel(etiqueta);
        lE.setFont(new Font("SansSerif", Font.BOLD, 12));
        lE.setForeground(TEXT);                        // BLANCO puro
        lE.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));

        JLabel lA = new JLabel(ayuda);
        lA.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lA.setForeground(TEXT_HINT);                   // gris medio visible
        lA.setMaximumSize(new Dimension(Integer.MAX_VALUE, 15));

        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        tf.setFont(new Font("SansSerif", Font.PLAIN, 13));

        p.add(lE);
        p.add(Box.createVerticalStrut(2));
        p.add(lA);
        p.add(Box.createVerticalStrut(4));
        p.add(tf);
        return p;
    }

    JPanel nota(String texto) {
        JLabel l = new JLabel("<html>" + texto + "</html>");
        l.setFont(new Font("SansSerif", Font.PLAIN, 11));
        l.setForeground(TEXT_SEC);                     // gris claro legible
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(248, 248, 250));
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 3, 0, 0, Color.WHITE),
            BorderFactory.createEmptyBorder(7, 10, 7, 10)));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        p.add(l);
        return p;
    }

    JPanel tarjeta(String titulo, JLabel valor, Color bg) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(bg);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_LT),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        JLabel lt = new JLabel(titulo, SwingConstants.CENTER);
        lt.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lt.setForeground(TEXT_SEC);                    // gris claro
        valor.setFont(new Font("SansSerif", Font.BOLD, 18));
        p.add(lt, BorderLayout.NORTH);
        p.add(valor, BorderLayout.CENTER);
        return p;
    }

    JLabel lbRes(Color c) {
        JLabel l = new JLabel("$0.00", SwingConstants.CENTER);
        l.setFont(new Font("SansSerif", Font.BOLD, 18));
        l.setForeground(c);
        return l;
    }

    JTable tabla(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setFont(new Font("SansSerif", Font.PLAIN, 12));
        t.setForeground(TEXT);
        t.setBackground(BG_CARD);
        t.setRowHeight(24);
        t.setGridColor(BORDER);
        t.setSelectionBackground(BG_SEL);
        t.setSelectionForeground(TEXT);
        t.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable tbl, Object v,
                    boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tbl, v, sel, foc, row, col);
                l.setBackground(new Color(240, 240, 242));
                l.setForeground(Color.WHITE);
                l.setFont(new Font("SansSerif", Font.BOLD, 11));
                l.setOpaque(true);
                l.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                return l;
            }
        });
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable tbl, Object v,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, v, sel, foc, row, col);
                c.setForeground(sel ? TEXT : TEXT);
                c.setBackground(sel ? BG_SEL : (row%2==0 ? BG_CARD : BG_ALT));
                return c;
            }
        });
        return t;
    }

    JTextField campo(String v) {
        JTextField tf = new JTextField(v);
        tf.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tf.setForeground(TEXT);
        tf.setBackground(BG_INPUT);
        tf.setCaretColor(TEXT);

        // Glow inicial: amarillo si vacío/default, verde si ya tiene valor
        actualizarGlow(tf, v);

        tf.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { actualizarGlow(tf, tf.getText()); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { actualizarGlow(tf, tf.getText()); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { actualizarGlow(tf, tf.getText()); }
        });
        return tf;
    }

    void actualizarGlow(JTextField tf, String valor) {
        boolean lleno = valor != null && !valor.isBlank();
        Color color   = lleno ? new Color(60, 200, 100) : new Color(255, 190, 0);
        int   alpha1  = lleno ? 20  : 30;
        int   alpha2  = lleno ? 50  : 70;
        float stroke1 = lleno ? 4f  : 5f;
        float stroke2 = lleno ? 2f  : 3f;

        tf.setBorder(new javax.swing.border.Border() {
            @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Capa exterior difusa
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha1));
                g2.setStroke(new BasicStroke(stroke1));
                g2.drawRoundRect(x+2, y+2, w-4, h-4, 5, 5);
                // Capa media
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha2));
                g2.setStroke(new BasicStroke(stroke2));
                g2.drawRoundRect(x+1, y+1, w-3, h-3, 5, 5);
                // Borde sólido
                g2.setColor(color);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(x+1, y+1, w-3, h-3, 5, 5);
                g2.dispose();
            }
            @Override public Insets getBorderInsets(Component c) { return new Insets(6, 9, 6, 9); }
            @Override public boolean isBorderOpaque() { return false; }
        });
        tf.repaint();
    }

    void aplicarBordeGlow(JTextField tf, boolean activo) {
        // Mantener compatibilidad — delegar a actualizarGlow
        actualizarGlow(tf, tf.getText());
    }

    JTextField campoTop(String v) {
        JTextField tf = new JTextField(v, 5);
        tf.setFont(new Font("SansSerif", Font.PLAIN, 11));
        tf.setForeground(TEXT);
        tf.setBackground(BG_INPUT);
        tf.setCaretColor(TEXT);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_LT),
            BorderFactory.createEmptyBorder(2, 5, 2, 5)));
        tf.setPreferredSize(new Dimension(65, 24));
        return tf;
    }

    JLabel lblTop(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("SansSerif", Font.PLAIN, 11));
        l.setForeground(TEXT_SEC);
        return l;
    }

    JLabel lblSep() {
        JLabel l = new JLabel("·");
        l.setForeground(BORDER_LT);
        return l;
    }

    // Botón primario: fondo BLANCO, texto NEGRO — máximo contraste
    JButton btnPrimario(String txt) {
        JButton b = new JButton(txt);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setBackground(Color.WHITE);
        b.setForeground(Color.BLACK);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 22, 8, 22));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // Botón secundario: fondo oscuro, borde y texto grises
    JButton btnSecundario(String txt) {
        JButton b = new JButton(txt);
        b.setFont(new Font("SansSerif", Font.PLAIN, 12));
        b.setBackground(BG_CARD);
        b.setForeground(TEXT_SEC);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_LT),
            BorderFactory.createEmptyBorder(7, 20, 7, 20)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // Botón destructivo: borde rojo, texto rojo
    JButton btnDestructivo(String txt) {
        JButton b = new JButton(txt);
        b.setFont(new Font("SansSerif", Font.PLAIN, 11));
        b.setBackground(BG_CARD);
        b.setForeground(ROJO);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ROJO),
            BorderFactory.createEmptyBorder(4, 12, 4, 12)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    String f(double v) { return String.format("%.2f", v); }
    double d(JTextField tf) {
        try { return Double.parseDouble(tf.getText().replace(",",".")); } catch(Exception e){return 0;}
    }
    int i(JTextField tf) {
        try { return Integer.parseInt(tf.getText().trim()); } catch(Exception e){return 1;}
    }
}