package utilidades.modulos.almatallada.velocidades;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;

public class VelocidadesPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// ── Sistema de colores ────────────────────────────────────────────────
	static final Color BG = new Color(245, 245, 247);
	static final Color BG_CARD = new Color(255, 255, 255);
	static final Color BG_INPUT = new Color(250, 250, 252);
	static final Color BG_ALT = new Color(248, 248, 250);
	static final Color BG_SEL = new Color(210, 225, 255);
	static final Color TEXT = new Color(20, 20, 20); // TEXT
	static final Color TEXT_SEC = new Color(130, 130, 140);
	static final Color BORDER = new Color(210, 225, 255);
	static final Color BORDER_LT = new Color(190, 190, 195);
	static final Color VERDE = new Color(100, 200, 120);
	static final Color ROJO = new Color(220, 80, 80);
	static final File ARCHIVO = new File(System.getProperty("user.home"), "almatallada_velocidades.csv");

	static final String[] COLUMNAS = { "Descripcion", "Vel. (mm/min)", "Potencia (%)", "Pot. constante", "Soplado aire",
			"Modo", "Pasadas", "Perforacion", "Guia entrada" };

	DefaultTableModel modelo;
	JTable tabla;
	JComboBox<String> cbPotConst, cbSoplado, cbModo, cbPerf;

	public VelocidadesPanel() {
		setLayout(new BorderLayout(8, 8));
		setBackground(BG);
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		add(crearTabla(), BorderLayout.CENTER);
		add(crearFormulario(), BorderLayout.SOUTH);
		cargarDesdeArchivo();
	}

	JPanel crearTabla() {
		JPanel p = new JPanel(new BorderLayout(0, 8));
		p.setBackground(BG);

		JLabel tit = new JLabel("TABLA DE VELOCIDADES Y PARAMETROS");
		tit.setFont(new Font("SansSerif", Font.BOLD, 11));
		tit.setForeground(TEXT);
		tit.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, BORDER_LT),
				BorderFactory.createEmptyBorder(0, 0, 8, 0)));
		p.add(tit, BorderLayout.NORTH);

		modelo = new DefaultTableModel(COLUMNAS, 0) {
			public boolean isCellEditable(int r, int c) {
				return true;
			}
		};

		tabla = new JTable(modelo);
		tabla.setFont(new Font("SansSerif", Font.PLAIN, 12));
		tabla.setForeground(TEXT);
		tabla.setBackground(BG_CARD);
		tabla.setRowHeight(26);
		tabla.setGridColor(BORDER);
		tabla.setSelectionBackground(BG_SEL);
		tabla.setSelectionForeground(TEXT);
		tabla.getTableHeader().setReorderingAllowed(false);

		// Header: fondo gris claro con texto negro
		tabla.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
			public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row,
					int col) {
				JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, row, col);
				l.setBackground(new Color(235, 235, 240));
				l.setForeground(new Color(20, 20, 20));
				l.setFont(new Font("SansSerif", Font.BOLD, 11));
				l.setOpaque(true);
				l.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
				return l;
			}
		});

		// Renderer filas
		tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
			public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row,
					int col) {
				Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
				c.setForeground(TEXT);
				c.setBackground(sel ? BG_SEL : (row % 2 == 0 ? BG_CARD : BG_ALT));
				return c;
			}
		});

		int[] anchos = { 180, 110, 90, 110, 100, 80, 70, 90, 150 };
		for (int i = 0; i < anchos.length; i++)
			tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);

		JScrollPane sp = new JScrollPane(tabla);
		sp.setBorder(BorderFactory.createLineBorder(BORDER));
		sp.getViewport().setBackground(BG_CARD);
		p.add(sp, BorderLayout.CENTER);

		// Botones de acción
		JPanel bots = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
		bots.setBackground(BG);

		JButton btnEliminar = btnDestructivo("Eliminar");
		JButton btnDuplicar = btnSecundario("Duplicar");
		JButton btnLimpiar = btnSecundario("Limpiar todo");
		JButton btnExportar = btnPrimario("Exportar CSV");

		btnEliminar.addActionListener(e -> {
			int row = tabla.getSelectedRow();
			if (row >= 0) {
				modelo.removeRow(row);
				guardarEnArchivo();
			} else
				JOptionPane.showMessageDialog(this, "Selecciona una fila primero.");
		});
		btnDuplicar.addActionListener(e -> {
			int row = tabla.getSelectedRow();
			if (row >= 0) {
				Object[] datos = new Object[COLUMNAS.length];
				for (int i = 0; i < COLUMNAS.length; i++)
					datos[i] = modelo.getValueAt(row, i);
				modelo.addRow(datos);
				guardarEnArchivo();
			} else
				JOptionPane.showMessageDialog(this, "Selecciona una fila primero.");
		});
		btnLimpiar.addActionListener(e -> {
			if (JOptionPane.showConfirmDialog(this, "¿Borrar toda la tabla?", "Confirmar",
					JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				modelo.setRowCount(0);
				guardarEnArchivo();
			}
		});
		btnExportar.addActionListener(e -> exportarCSV());

		bots.add(btnEliminar);
		bots.add(btnDuplicar);
		bots.add(btnLimpiar);
		bots.add(btnExportar);
		p.add(bots, BorderLayout.SOUTH);
		return p;
	}

	JPanel crearFormulario() {
		JPanel outer = new JPanel(new BorderLayout(0, 8));
		outer.setBackground(BG_CARD);
		outer.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_LT),
				BorderFactory.createEmptyBorder(10, 14, 10, 14)));

		JLabel tit = new JLabel("AGREGAR / EDITAR CONFIGURACION");
		tit.setFont(new Font("SansSerif", Font.BOLD, 10));
		tit.setForeground(TEXT_SEC);
		outer.add(tit, BorderLayout.NORTH);

		JPanel form = new JPanel(new GridBagLayout());
		form.setBackground(BG_CARD);
		GridBagConstraints g = new GridBagConstraints();
		g.insets = new Insets(4, 6, 4, 6);
		g.fill = GridBagConstraints.HORIZONTAL;

		JTextField tfDesc = cf(18);
		JTextField tfVel = cf(7);
		JTextField tfPot = cf(5);
		JTextField tfPasadas = cf(4);
		JTextField tfGuia = cf(14);

		cbPotConst = combo("OFF", "ON");
		cbSoplado = combo("OFF", "ON");
		cbModo = combo("LINEA", "RELLENO", "OFFSET");
		cbPerf = combo("OFF", "ON");

		g.gridy = 0;
		ag(form, g, 0, "Descripcion:", tfDesc);
		ag(form, g, 2, "Vel. mm/min:", tfVel);
		ag(form, g, 4, "Potencia %:", tfPot);
		ag(form, g, 6, "Pasadas:", tfPasadas);

		g.gridy = 1;
		ag(form, g, 0, "Pot. constante:", cbPotConst);
		ag(form, g, 2, "Soplado aire:", cbSoplado);
		ag(form, g, 4, "Modo:", cbModo);
		ag(form, g, 6, "Perforacion:", cbPerf);

		g.gridy = 2;
		g.gridx = 0;
		g.gridwidth = 1;
		form.add(lbl("Guia entrada:"), g);
		g.gridx = 1;
		g.gridwidth = 7;
		form.add(tfGuia, g);

		outer.add(form, BorderLayout.CENTER);

		JButton btnAgregar = btnPrimario("+ Agregar fila");
		JButton btnCargar = btnSecundario("Cargar fila seleccionada");

		btnAgregar.addActionListener(e -> {
			modelo.addRow(new Object[] { tfDesc.getText(), tfVel.getText(), tfPot.getText(),
					cbPotConst.getSelectedItem(), cbSoplado.getSelectedItem(), cbModo.getSelectedItem(),
					tfPasadas.getText(), cbPerf.getSelectedItem(), tfGuia.getText() });
			guardarEnArchivo();
			tfDesc.setText("");
			tfVel.setText("");
			tfPot.setText("");
			tfPasadas.setText("");
			tfGuia.setText("");
		});

		btnCargar.addActionListener(e -> {
			int row = tabla.getSelectedRow();
			if (row < 0) {
				JOptionPane.showMessageDialog(this, "Selecciona una fila.");
				return;
			}
			tfDesc.setText(s(row, 0));
			tfVel.setText(s(row, 1));
			tfPot.setText(s(row, 2));
			cbPotConst.setSelectedItem(s(row, 3));
			cbSoplado.setSelectedItem(s(row, 4));
			cbModo.setSelectedItem(s(row, 5));
			tfPasadas.setText(s(row, 6));
			cbPerf.setSelectedItem(s(row, 7));
			tfGuia.setText(s(row, 8));
		});

		JPanel rb = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		rb.setBackground(BG_CARD);
		rb.add(btnAgregar);
		rb.add(btnCargar);
		outer.add(rb, BorderLayout.SOUTH);
		return outer;
	}

	void guardarEnArchivo() {
		try (PrintWriter pw = new PrintWriter(new FileWriter(ARCHIVO))) {
			for (int i = 0; i < modelo.getRowCount(); i++) {
				StringBuilder sb = new StringBuilder();
				for (int j = 0; j < COLUMNAS.length; j++) {
					if (j > 0)
						sb.append("|");
					Object v = modelo.getValueAt(i, j);
					sb.append(v != null ? v.toString().replace("|", "/") : "");
				}
				pw.println(sb);
			}
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(this, "Error guardando: " + ex.getMessage());
		}
	}

	void cargarDesdeArchivo() {
		if (!ARCHIVO.exists()) {
			cargarDatosEjemplo();
			return;
		}
		try (BufferedReader br = new BufferedReader(new FileReader(ARCHIVO))) {
			String linea;
			while ((linea = br.readLine()) != null) {
				if (linea.isBlank())
					continue;
				String[] partes = linea.split("\\|", -1);
				Object[] fila = new Object[COLUMNAS.length];
				for (int i = 0; i < COLUMNAS.length; i++)
					fila[i] = i < partes.length ? partes[i] : "";
				modelo.addRow(fila);
			}
		} catch (IOException ex) {
			cargarDatosEjemplo();
		}
	}

	void cargarDatosEjemplo() {
		Object[][] ej = { { "QR - grabado", "2500", "30", "OFF", "ON", "RELLENO", "1", "OFF", "OFF" },
				{ "Detalles pequenos - corte", "700", "70", "ON", "ON", "LINEA", "3", "OFF", "OFF" },
				{ "Cortes grandes - corte", "850", "85", "ON", "ON", "LINEA", "3", "OFF", "ON - 45/0.5 linea" },
				{ "Letras - grabado", "5000", "75", "OFF", "ON", "RELLENO", "2", "OFF", "OFF" },
				{ "Cuero/piel - grabado", "2500", "5", "ON", "ON", "RELLENO", "1", "OFF", "OFF" }, };
		for (Object[] f : ej)
			modelo.addRow(f);
		guardarEnArchivo();
	}

	void exportarCSV() {
		JFileChooser fc = new JFileChooser();
		fc.setSelectedFile(new File("velocidades.csv"));
		if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
			return;
		try (PrintWriter pw = new PrintWriter(new FileWriter(fc.getSelectedFile()))) {
			pw.println(String.join(",", COLUMNAS));
			for (int i = 0; i < modelo.getRowCount(); i++) {
				StringBuilder sb = new StringBuilder();
				for (int j = 0; j < COLUMNAS.length; j++) {
					if (j > 0)
						sb.append(",");
					sb.append("\"").append(s(i, j)).append("\"");
				}
				pw.println(sb);
			}
			JOptionPane.showMessageDialog(this, "Exportado:\n" + fc.getSelectedFile().getAbsolutePath());
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
		}
	}

	String s(int r, int c) {
		Object v = modelo.getValueAt(r, c);
		return v != null ? v.toString() : "";
	}

	JTextField cf(int cols) {
		JTextField tf = new JTextField(cols);
		tf.setFont(new Font("SansSerif", Font.PLAIN, 12));
		tf.setForeground(TEXT);
		tf.setBackground(BG_INPUT);
		tf.setCaretColor(TEXT);
		tf.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_LT),
				BorderFactory.createEmptyBorder(3, 6, 3, 6)));
		return tf;
	}

	JComboBox<String> combo(String... ops) {
		JComboBox<String> cb = new JComboBox<>(ops);
		cb.setFont(new Font("SansSerif", Font.PLAIN, 12));
		cb.setBackground(BG_INPUT);
		cb.setForeground(TEXT);
		return cb;
	}

	JLabel lbl(String t) {
		JLabel l = new JLabel(t);
		l.setFont(new Font("SansSerif", Font.PLAIN, 12));
		l.setForeground(TEXT); // BLANCO siempre
		return l;
	}

	void ag(JPanel p, GridBagConstraints g, int col, String lbl, JComponent comp) {
		g.gridx = col;
		g.weightx = 0;
		p.add(lbl(lbl), g);
		g.gridx = col + 1;
		g.weightx = 1;
		p.add(comp, g);
	}

	JButton btnPrimario(String txt) {
		JButton b = new JButton(txt);
		b.setFont(new Font("SansSerif", Font.BOLD, 11));
		b.setBackground(new Color(60, 120, 220));
		b.setForeground(Color.WHITE);
		b.setFocusPainted(false);
		b.setOpaque(true);
		b.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
		b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		return b;
	}

	JButton btnSecundario(String txt) {
		JButton b = new JButton(txt);
		b.setFont(new Font("SansSerif", Font.PLAIN, 11));
		b.setBackground(new Color(238, 238, 242));
		b.setForeground(new Color(20, 20, 20));
		b.setFocusPainted(false);
		b.setOpaque(true);
		b.setBorder(new CompoundBorder(new LineBorder(new Color(190, 190, 195), 1),
				BorderFactory.createEmptyBorder(5, 12, 5, 12)));
		b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		return b;
	}

	JButton btnDestructivo(String txt) {
		JButton b = new JButton(txt);
		b.setFont(new Font("SansSerif", Font.PLAIN, 11));
		b.setBackground(new Color(238, 238, 242));
		b.setForeground(ROJO);
		b.setFocusPainted(false);
		b.setOpaque(true);
		b.setBorder(new CompoundBorder(new LineBorder(ROJO, 1), BorderFactory.createEmptyBorder(5, 12, 5, 12)));
		b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		return b;
	}
}