package utilidades.modulos.wow.cds;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

import utilidades.modulos.wow.cds.modelo.Jugador;
import utilidades.modulos.wow.cds.modelo.TrackerData;
import utilidades.modulos.wow.cds.utiles.DataManager;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.util.EventObject;
import java.util.List;

public class CdsPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// ── Paleta ────────────────────────────────────────────────
	private static final Color BG_APP = new Color(242, 242, 247);
	@SuppressWarnings("unused")
	private static final Color BG_PANEL = new Color(255, 255, 255);
	private static final Color BG_HEADER = new Color(255, 255, 255);
	private static final Color BG_ALIANZA = new Color(232, 240, 255);
	private static final Color BG_HORDA = new Color(255, 232, 232);
	private static final Color BG_ROW_EVEN = new Color(255, 255, 255);
	private static final Color BG_ROW_ODD = new Color(250, 250, 252);
	private static final Color BG_SEP = new Color(200, 200, 210);
	private static final Color ACCENT = new Color(60, 120, 220);
	private static final Color BORDER_LIGHT = new Color(210, 210, 215);
	private static final Color ALIANZA_BLUE = new Color(40, 90, 190);
	private static final Color HORDA_RED = new Color(180, 45, 45);
	private static final Color TEXT_MAIN = new Color(20, 20, 20);
	private static final Color TEXT_DIM = new Color(130, 130, 140);
	private static final Color CHECK_ON_BG = new Color(52, 199, 89);
	private static final Color CHECK_OFF_BG = new Color(220, 220, 225);
	private static final Color SEL_BG = new Color(210, 225, 255);
	private static final Color PROG_LOW = new Color(220, 80, 80);
	private static final Color PROG_MID = new Color(230, 160, 30);
	private static final Color PROG_HIGH = new Color(52, 199, 89);

	private final TrackerData data;
	private JTable table;
	private DefaultTableModel tableModel;
	private JTextField searchField;
	private int dragFromRow = -1;
	private boolean actualizando = false;

	private static final String COL_PROGRESO = "Progreso";
	private static final String COL_NOTA = "Nota";

	private int hoverRow = -1;
	private int hoverCol = -1;
	private static final Color BG_HOVER = new Color(220, 230, 255);

	public CdsPanel(TrackerData data) {
		this.data = data;
		setBackground(BG_APP);
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		add(buildTopBar(), BorderLayout.NORTH);
		add(buildTable(), BorderLayout.CENTER);
		add(buildBottomBar(), BorderLayout.SOUTH);
	}

	public void guardar() {
		try {
			DataManager.guardar(data);
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(this, "Error al guardar: " + ex.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private JPanel buildTopBar() {
		JPanel bar = new JPanel(new BorderLayout(10, 0));
		bar.setBackground(BG_HEADER);
		bar.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, BORDER_LIGHT),
				BorderFactory.createEmptyBorder(10, 14, 10, 14)));

		JLabel title = new JLabel("Tracker de Mazmorras");
		title.setFont(new Font("SansSerif", Font.BOLD, 16));
		title.setForeground(TEXT_MAIN);

		searchField = new JTextField(16);
		searchField.setFont(new Font("SansSerif", Font.PLAIN, 13));
		searchField.setBorder(
				new CompoundBorder(new LineBorder(BORDER_LIGHT, 1, true), BorderFactory.createEmptyBorder(4, 8, 4, 8)));

		searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			public void insertUpdate(javax.swing.event.DocumentEvent e) {
				filtrar();
			}

			public void removeUpdate(javax.swing.event.DocumentEvent e) {
				filtrar();
			}

			public void changedUpdate(javax.swing.event.DocumentEvent e) {
				filtrar();
			}
		});

		JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
		left.setBackground(BG_HEADER);
		left.add(title);

		JLabel lbl = new JLabel("Buscar:");
		lbl.setForeground(TEXT_DIM);
		lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
		left.add(lbl);
		left.add(searchField);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
		btnPanel.setBackground(BG_HEADER);

		JButton btnAgrJug = appButton("+ Jugador", false);
		JButton btnAgrCol = appButton("+ Columna", false);
		JButton btnDelCol = appButton("− Columna", false);
		JButton btnResetAll = appButton("Reset todo", false);
		JButton btnResetNoConq = appButton("Reset (sin Conquista)", false);

		btnAgrJug.addActionListener(e -> agregarJugador());
		btnAgrCol.addActionListener(e -> agregarColumna());
		btnDelCol.addActionListener(e -> eliminarColumna());
		btnResetAll.addActionListener(e -> resetear(false));
		btnResetNoConq.addActionListener(e -> resetear(true));

		btnPanel.add(btnAgrJug);
		btnPanel.add(btnAgrCol);
		btnPanel.add(btnDelCol);
		btnPanel.add(btnResetAll);
		btnPanel.add(btnResetNoConq);

		bar.add(left, BorderLayout.WEST);
		bar.add(btnPanel, BorderLayout.EAST);

		return bar;
	}

	private JScrollPane buildTable() {
		tableModel = new DefaultTableModel() {
			@Override
			public Class<?> getColumnClass(int col) {
				int last = getColumnCount() - 1;
				int prog = getColumnCount() - 2;

				if (col == 0 || col == 1 || col == last || col == prog) {
					return String.class;
				}

				return Boolean.class;
			}

			@Override
			public boolean isCellEditable(int row, int col) {
				int prog = getColumnCount() - 2;
				return col != 1 && col != prog;
			}
		};

		refreshTableModel();

		table = new JTable(tableModel) {
			@Override
			public Component prepareRenderer(TableCellRenderer r, int row, int col) {
				Component c = super.prepareRenderer(r, row, col);
				colorearFila(c, row, col);

				if (c instanceof JComponent) {
					Border sep = esPrimeraDeFaccionDistinta(row) ? new MatteBorder(2, 0, 0, 0, BG_SEP)
							: BorderFactory.createEmptyBorder();

					((JComponent) c).setBorder(sep);
				}

				return c;
			}
		};

		table.setBackground(BG_ROW_EVEN);
		table.setForeground(TEXT_MAIN);
		table.setGridColor(new Color(235, 235, 238));
		table.setRowHeight(32);
		table.setFont(new Font("SansSerif", Font.PLAIN, 13));
		table.setSelectionBackground(SEL_BG);
		table.setSelectionForeground(TEXT_MAIN);
		table.setShowHorizontalLines(true);
		table.setShowVerticalLines(false);
		table.setIntercellSpacing(new Dimension(0, 0));
		table.setFillsViewportHeight(true);

		JTableHeader header = table.getTableHeader();
		header.setBackground(new Color(248, 248, 250));
		header.setForeground(TEXT_DIM);
		header.setFont(new Font("SansSerif", Font.BOLD, 11));
		header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_LIGHT));
		header.setReorderingAllowed(false);
		header.setPreferredSize(new Dimension(0, 30));

		ajustarAnchos();

		table.setDefaultRenderer(Boolean.class, new PillRenderer());
		table.setDefaultEditor(Boolean.class, new PillEditor());

		table.setDefaultRenderer(String.class, new ProgressAwareRenderer());

		tableModel.addTableModelListener(e -> {
			if (actualizando) {
				return;
			}

			int row = e.getFirstRow();
			int col = e.getColumn();

			if (row >= 0 && col >= 0) {
				syncModelToData(row, col);
				actualizarProgreso(row);
			}
		});

		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				int row = table.rowAtPoint(e.getPoint());

				if (row < 0) {
					return;
				}

				if (SwingUtilities.isRightMouseButton(e)) {
					table.setRowSelectionInterval(row, row);
					showContextMenu(e, row);
				} else {
					dragFromRow = row;
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				dragFromRow = -1;
				table.setCursor(Cursor.getDefaultCursor());
			}
		});

		table.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				if (dragFromRow < 0) {
					return;
				}

				int toRow = table.rowAtPoint(e.getPoint());

				if (toRow >= 0 && toRow != dragFromRow) {
					moverFila(dragFromRow, toRow);
					dragFromRow = toRow;
					table.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
				}
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				int row = table.rowAtPoint(e.getPoint());
				int col = table.columnAtPoint(e.getPoint());

				if (row != hoverRow || col != hoverCol) {
					hoverRow = row;
					hoverCol = col;
					table.repaint();
				}
			}
		});

		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseExited(MouseEvent e) {
				hoverRow = -1;
				hoverCol = -1;
				table.repaint();
			}
		});

		JScrollPane scroll = new JScrollPane(table);
		scroll.setBorder(new MatteBorder(1, 1, 1, 1, BORDER_LIGHT));
		scroll.getViewport().setBackground(BG_ROW_EVEN);

		return scroll;
	}

	private JPanel buildBottomBar() {
		JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
		bar.setBackground(BG_APP);
		bar.setBorder(new MatteBorder(1, 0, 0, 0, BORDER_LIGHT));

		JLabel hint = new JLabel(
				"Arrastra filas para reordenar  ·  Click derecho para opciones  ·  Se guarda automáticamente al cerrar");
		hint.setFont(new Font("SansSerif", Font.PLAIN, 11));
		hint.setForeground(TEXT_DIM);

		bar.add(hint);

		return bar;
	}

	private void filtrar() {
		String query = searchField == null ? "" : searchField.getText().trim().toLowerCase();

		tableModel.setRowCount(0);

		List<String> cols = data.getColumnas();
		int n = cols.size() + 4;

		for (Jugador j : data.getJugadores()) {
			if (!query.isEmpty() && !j.getNombre().toLowerCase().contains(query)) {
				continue;
			}

			Object[] row = buildRow(j, cols, n);
			tableModel.addRow(row);
		}
	}

	private Object[] buildRow(Jugador j, List<String> cols, int n) {
		Object[] row = new Object[n];

		row[0] = j.getNombre();
		row[1] = j.getFaccion();

		int done = 0;

		for (int i = 0; i < cols.size(); i++) {
			boolean v = j.getColumna(cols.get(i));
			row[i + 2] = v;

			if (v) {
				done++;
			}
		}

		row[n - 2] = done + "/" + cols.size();
		row[n - 1] = j.getNota();

		return row;
	}

	private void refreshTableModel() {
		List<String> cols = data.getColumnas();
		String[] names = new String[cols.size() + 4];

		names[0] = "Jugador";
		names[1] = "Facción";

		for (int i = 0; i < cols.size(); i++) {
			names[i + 2] = cols.get(i);
		}

		names[names.length - 2] = COL_PROGRESO;
		names[names.length - 1] = COL_NOTA;

		tableModel.setColumnIdentifiers(names);
		filtrar();
		ajustarAnchos();
	}

	private void ajustarAnchos() {
		if (table == null || tableModel.getColumnCount() == 0) {
			return;
		}

		int n = tableModel.getColumnCount();

		table.getColumnModel().getColumn(0).setPreferredWidth(130);
		table.getColumnModel().getColumn(1).setPreferredWidth(72);
		table.getColumnModel().getColumn(n - 2).setPreferredWidth(70);
		table.getColumnModel().getColumn(n - 1).setPreferredWidth(180);

		for (int i = 2; i < n - 2; i++) {
			table.getColumnModel().getColumn(i).setPreferredWidth(72);
		}
	}

	private void actualizarProgreso(int row) {
		if (row >= tableModel.getRowCount()) {
			return;
		}

		String nombre = (String) tableModel.getValueAt(row, 0);
		Jugador j = findJugador(nombre);

		if (j == null) {
			return;
		}

		List<String> cols = data.getColumnas();

		int done = 0;

		for (String col : cols) {
			if (j.getColumna(col)) {
				done++;
			}
		}

		int progCol = tableModel.getColumnCount() - 2;

		actualizando = true;
		tableModel.setValueAt(done + "/" + cols.size(), row, progCol);
		actualizando = false;
	}

	private void syncModelToData(int row, int col) {
		if (row >= tableModel.getRowCount()) {
			return;
		}

		String nombre = (String) tableModel.getValueAt(row, 0);
		Jugador j = findJugador(nombre);

		if (j == null) {
			return;
		}

		List<String> cols = data.getColumnas();

		int last = tableModel.getColumnCount() - 1;
		int prog = tableModel.getColumnCount() - 2;

		if (col == 0) {
			Object v = tableModel.getValueAt(row, 0);

			if (v != null) {
				j.setNombre(v.toString());
			}
		} else if (col >= 2 && col < prog) {
			Object v = tableModel.getValueAt(row, col);

			if (v instanceof Boolean) {
				j.setColumna(cols.get(col - 2), (Boolean) v);
			}
		} else if (col == last) {
			Object v = tableModel.getValueAt(row, col);

			if (v != null) {
				j.setNota(v.toString());
			}
		}
	}

	private Jugador findJugador(String nombre) {
		return data.getJugadores().stream().filter(x -> x.getNombre().equals(nombre)).findFirst().orElse(null);
	}

	private void moverFila(int from, int to) {
		String nFrom = (String) tableModel.getValueAt(from, 0);
		String nTo = (String) tableModel.getValueAt(to, 0);

		List<Jugador> list = data.getJugadores();

		int iF = indexOfJugador(list, nFrom);
		int iT = indexOfJugador(list, nTo);

		if (iF < 0 || iT < 0) {
			return;
		}

		list.add(iT, list.remove(iF));
		tableModel.moveRow(from, from, to);
		table.setRowSelectionInterval(to, to);
	}

	private int indexOfJugador(List<Jugador> list, String nombre) {
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).getNombre().equals(nombre)) {
				return i;
			}
		}

		return -1;
	}

	private void colorearFila(Component c, int row, int col) {
		if (row >= tableModel.getRowCount()) {
			return;
		}

		String faccion = (String) tableModel.getValueAt(row, 1);
		boolean alianza = "Alianza".equals(faccion);

		if (table.isRowSelected(row)) {
			c.setBackground(SEL_BG);
			c.setForeground(TEXT_MAIN);
			return;
		}

		boolean enFila = row == hoverRow;
		boolean enCol = col == hoverCol;

		if (col == 1) {
			c.setBackground(alianza ? BG_ALIANZA : BG_HORDA);
			c.setForeground(alianza ? ALIANZA_BLUE : HORDA_RED);

			if (c instanceof JLabel) {
				((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
				((JLabel) c).setFont(new Font("SansSerif", Font.BOLD, 11));
			}

			return;
		}

		Color base = blend(row % 2 == 0 ? BG_ROW_EVEN : BG_ROW_ODD, alianza ? BG_ALIANZA : BG_HORDA, 0.18f);

		if (enFila && enCol) {
			c.setBackground(blend(base, BG_HOVER, 0.7f));
		} else if (enFila || enCol) {
			c.setBackground(blend(base, BG_HOVER, 0.4f));
		} else {
			c.setBackground(base);
		}

		c.setForeground(TEXT_MAIN);

		if (col == 0 && c instanceof JLabel) {
			((JLabel) c).setFont(new Font("SansSerif", Font.BOLD, 13));
		}
	}

	private boolean esPrimeraDeFaccionDistinta(int row) {
		if (row == 0 || row >= tableModel.getRowCount()) {
			return false;
		}

		String cur = (String) tableModel.getValueAt(row, 1);
		String prev = (String) tableModel.getValueAt(row - 1, 1);

		return cur != null && !cur.equals(prev);
	}

	private Color blend(Color a, Color b, float t) {
		return new Color(clamp((int) (a.getRed() + (b.getRed() - a.getRed()) * t)),
				clamp((int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t)),
				clamp((int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t)));
	}

	private int clamp(int v) {
		return Math.max(0, Math.min(255, v));
	}

	private void agregarJugador() {
		JTextField nombre = new JTextField(14);
		JComboBox<String> faccion = new JComboBox<>(new String[] { "Alianza", "Horda" });

		JPanel panel = buildDialogPanel(new String[] { "Nombre:", "Facción:" }, new JComponent[] { nombre, faccion });

		int ok = JOptionPane.showConfirmDialog(this, panel, "Agregar Jugador", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);

		if (ok == JOptionPane.OK_OPTION && !nombre.getText().isBlank()) {
			data.agregarJugador(new Jugador(nombre.getText().trim(), (String) faccion.getSelectedItem()));
			refreshTableModel();
		}
	}

	private void agregarColumna() {
		String col = JOptionPane.showInputDialog(this, "Nombre de la nueva columna:");

		if (col != null && !col.isBlank()) {
			data.agregarColumna(col.trim());
			refreshTableModel();
		}
	}

	private void eliminarColumna() {
		List<String> cols = data.getColumnas();

		if (cols.isEmpty()) {
			return;
		}

		String[] ops = cols.toArray(new String[0]);

		String sel = (String) JOptionPane.showInputDialog(this, "Columna a eliminar:", "Eliminar Columna",
				JOptionPane.PLAIN_MESSAGE, null, ops, ops[0]);

		if (sel != null && JOptionPane.showConfirmDialog(this, "¿Eliminar \"" + sel + "\"?", "Confirmar",
				JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
			data.eliminarColumna(sel);
			refreshTableModel();
		}
	}

	private void resetear(boolean preservarConquista) {
		String msg = preservarConquista ? "¿Resetear todos los checks excepto Conquista?"
				: "¿Resetear TODOS los checks de todos los jugadores?";
		int confirm = JOptionPane.showConfirmDialog(this, msg, "Confirmar reset", JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);
		if (confirm != JOptionPane.YES_OPTION)
			return;

		for (Jugador j : data.getJugadores()) {
			for (String col : data.getColumnas()) {
				if (preservarConquista && col.equalsIgnoreCase("Conquista"))
					continue;
				j.setColumna(col, false);
			}
		}
		refreshTableModel();
	}

	private void showContextMenu(MouseEvent e, int row) {
		String nombre = (String) tableModel.getValueAt(row, 0);
		Jugador j = findJugador(nombre);

		if (j == null) {
			return;
		}

		JPopupMenu menu = new JPopupMenu();

		JMenuItem cambFac = new JMenuItem(
				"Cambiar facción → " + ("Alianza".equals(j.getFaccion()) ? "Horda" : "Alianza"));

		cambFac.addActionListener(ev -> {
			j.setFaccion("Alianza".equals(j.getFaccion()) ? "Horda" : "Alianza");
			refreshTableModel();
		});

		JMenuItem del = new JMenuItem("Eliminar a " + j.getNombre());
		del.setForeground(HORDA_RED);

		del.addActionListener(ev -> {
			if (JOptionPane.showConfirmDialog(this, "¿Eliminar a \"" + j.getNombre() + "\"?", "Confirmar",
					JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				data.eliminarJugador(j);
				refreshTableModel();
			}
		});

		menu.add(cambFac);
		menu.addSeparator();
		menu.add(del);

		menu.show(table, e.getX(), e.getY());
	}

	private JButton appButton(String text, boolean primary) {
		Color bgN = primary ? ACCENT : new Color(238, 238, 242);
		Color bgH = primary ? new Color(45, 95, 190) : new Color(218, 218, 224);

		JButton btn = new JButton(text);

		btn.setBackground(bgN);
		btn.setForeground(primary ? Color.WHITE : TEXT_MAIN);
		btn.setFont(new Font("SansSerif", primary ? Font.BOLD : Font.PLAIN, 12));
		btn.setBorder(new CompoundBorder(new LineBorder(primary ? ACCENT : BORDER_LIGHT, 1, true),
				BorderFactory.createEmptyBorder(5, 12, 5, 12)));
		btn.setFocusPainted(false);
		btn.setOpaque(true);
		btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		btn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				btn.setBackground(bgH);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				btn.setBackground(bgN);
			}
		});

		return btn;
	}

	private JPanel buildDialogPanel(String[] labels, JComponent[] fields) {
		JPanel p = new JPanel(new GridLayout(labels.length, 2, 8, 8));
		p.setBackground(Color.WHITE);
		p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		for (int i = 0; i < labels.length; i++) {
			JLabel lbl = new JLabel(labels[i]);
			lbl.setForeground(TEXT_MAIN);

			p.add(lbl);
			p.add(fields[i]);
		}

		return p;
	}

	private class ProgressAwareRenderer extends DefaultTableCellRenderer {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int row,
				int col) {
			Component c = super.getTableCellRendererComponent(t, val, sel, focus, row, col);

			colorearFila(c, row, col);

			int progCol = tableModel.getColumnCount() - 2;

			if (col == progCol && val instanceof String) {
				String[] parts = ((String) val).split("/");

				if (parts.length == 2) {
					try {
						int done = Integer.parseInt(parts[0].trim());
						int total = Integer.parseInt(parts[1].trim());

						if (total > 0) {
							float pct = (float) done / total;
							Color fg = pct >= 0.8f ? PROG_HIGH : pct >= 0.4f ? PROG_MID : PROG_LOW;

							c.setForeground(fg);

							if (c instanceof JLabel) {
								((JLabel) c).setFont(new Font("SansSerif", Font.BOLD, 12));
								((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
							}
						}
					} catch (NumberFormatException ignored) {
					}
				}
			}

			return c;
		}
	}

	// ── PILL CHECKBOX ─────────────────────────────────────────
	private class PillRenderer implements TableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int row,
				int col) {
			boolean checked = (val instanceof Boolean) && (Boolean) val;
			PillWidget w = new PillWidget(checked);
			colorearFila(w, row, col);
			return w;
		}
	}

	private static class PillWidget extends JComponent {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private final boolean on;

		PillWidget(boolean on) {
			this.on = on;
			setOpaque(true);
		}

		@Override
		protected void paintComponent(Graphics g) {
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());

			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			int pw = 38;
			int ph = 20;
			int x = (getWidth() - pw) / 2;
			int y = (getHeight() - ph) / 2;

			g2.setColor(on ? CHECK_ON_BG : CHECK_OFF_BG);
			g2.fill(new RoundRectangle2D.Float(x, y, pw, ph, ph, ph));

			int dd = ph - 4;
			int dx = on ? x + pw - dd - 2 : x + 2;

			g2.setColor(Color.WHITE);
			g2.fillOval(dx, y + 2, dd, dd);

			g2.dispose();
		}
	}

	private static class PillEditor extends AbstractCellEditor implements TableCellEditor {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private boolean value;

		@Override
		public Component getTableCellEditorComponent(JTable t, Object val, boolean sel, int row, int col) {
			value = !((val instanceof Boolean) && (Boolean) val);

			SwingUtilities.invokeLater(this::fireEditingStopped);

			return new PillWidget(value);
		}

		@Override
		public Object getCellEditorValue() {
			return value;
		}

		@Override
		public boolean isCellEditable(EventObject e) {
			return true;
		}

		@Override
		public boolean shouldSelectCell(EventObject e) {
			return true;
		}
	}
}