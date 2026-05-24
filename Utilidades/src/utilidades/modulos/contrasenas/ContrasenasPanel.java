package utilidades.modulos.contrasenas;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public class ContrasenasPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// ── Paleta ────────────────────────────────────────────────
	private static final Color BG_APP = new Color(242, 242, 247);
	private static final Color BG_SIDEBAR = new Color(248, 248, 250);
	private static final Color BG_PANEL = new Color(255, 255, 255);
	private static final Color ACCENT = new Color(60, 120, 220);
	@SuppressWarnings("unused")
	private static final Color ACCENT_HOVER = new Color(45, 95, 190);
	private static final Color BORDER_LIGHT = new Color(210, 210, 215);
	private static final Color TEXT_MAIN = new Color(20, 20, 20);
	private static final Color TEXT_DIM = new Color(130, 130, 140);
	private static final Color SEL_BG = new Color(210, 225, 255);
	private static final Color DEL_RED = new Color(180, 45, 45);

	private static final String DATA_DIR = "contrasenas";

	// ── Estado ────────────────────────────────────────────────
	private final List<Nota> notas = new ArrayList<>();
	private Nota notaActual = null;

	// ── UI ────────────────────────────────────────────────────
	private final DefaultListModel<Nota> listModel = new DefaultListModel<>();
	private JList<Nota> listaNota;
	private JTextArea areaTexto;
	private JLabel lblTitulo;
	private boolean guardando = false;
	private JPanel cardPanel; // referencia directa al CardLayout panel
	private CardLayout cardLayout;

	public ContrasenasPanel() {
		setLayout(new BorderLayout());
		setBackground(BG_APP);

		cargarNotas();

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildSidebar(), buildEditor());
		split.setDividerLocation(200);
		split.setDividerSize(1);
		split.setBorder(null);
		split.setResizeWeight(0);

		add(split, BorderLayout.CENTER);

		if (!notas.isEmpty()) {
			listaNota.setSelectedIndex(0);
		}
	}

	// ── SIDEBAR ───────────────────────────────────────────────
	private JPanel buildSidebar() {
		JPanel sidebar = new JPanel(new BorderLayout());
		sidebar.setBackground(BG_SIDEBAR);
		sidebar.setBorder(new MatteBorder(0, 0, 0, 1, BORDER_LIGHT));
		sidebar.setPreferredSize(new Dimension(200, 0));

		// Header
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(BG_SIDEBAR);
		header.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, BORDER_LIGHT),
				BorderFactory.createEmptyBorder(10, 14, 10, 10)));

		JLabel lbl = new JLabel("Notas");
		lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
		lbl.setForeground(TEXT_MAIN);

		JButton btnNueva = new JButton("+");
		btnNueva.setFont(new Font("SansSerif", Font.BOLD, 18));
		btnNueva.setForeground(ACCENT);
		btnNueva.setBorder(BorderFactory.createEmptyBorder(0, 4, 2, 2));
		btnNueva.setContentAreaFilled(false);
		btnNueva.setBorderPainted(false);
		btnNueva.setFocusPainted(false);
		btnNueva.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btnNueva.setToolTipText("Nueva nota");
		btnNueva.addActionListener(e -> nuevaNota());

		header.add(lbl, BorderLayout.WEST);
		header.add(btnNueva, BorderLayout.EAST);

		// Lista
		listaNota = new JList<>(listModel);
		listaNota.setBackground(BG_SIDEBAR);
		listaNota.setFont(new Font("SansSerif", Font.PLAIN, 13));
		listaNota.setFixedCellHeight(40);
		listaNota.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
		listaNota.setCellRenderer(new NotaRenderer());
		listaNota.setSelectionBackground(SEL_BG);
		listaNota.setSelectionForeground(TEXT_MAIN);

		listaNota.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				Nota sel = listaNota.getSelectedValue();
				if (sel != null)
					abrirNota(sel);
			}
		});

		// Click derecho → renombrar / eliminar
		listaNota.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					int idx = listaNota.locationToIndex(e.getPoint());
					if (idx >= 0) {
						listaNota.setSelectedIndex(idx);
						showContextMenu(e, listModel.get(idx));
					}
				}
			}
		});

		JScrollPane scroll = new JScrollPane(listaNota);
		scroll.setBorder(null);
		scroll.setBackground(BG_SIDEBAR);

		sidebar.add(header, BorderLayout.NORTH);
		sidebar.add(scroll, BorderLayout.CENTER);
		return sidebar;
	}

	// ── EDITOR ────────────────────────────────────────────────
	private JPanel buildEditor() {
		JPanel editor = new JPanel(new BorderLayout());
		editor.setBackground(BG_PANEL);

		// Header del editor
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(BG_PANEL);
		header.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, BORDER_LIGHT),
				BorderFactory.createEmptyBorder(10, 16, 10, 16)));

		lblTitulo = new JLabel("Selecciona o crea una nota");
		lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 15));
		lblTitulo.setForeground(TEXT_MAIN);
		header.add(lblTitulo, BorderLayout.CENTER);

		// Área de texto
		areaTexto = new JTextArea();
		areaTexto.setFont(new Font("SansSerif", Font.PLAIN, 14));
		areaTexto.setForeground(TEXT_MAIN);
		areaTexto.setBackground(BG_PANEL);
		areaTexto.setLineWrap(true);
		areaTexto.setWrapStyleWord(true);
		areaTexto.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
		areaTexto.setEnabled(false);
		areaTexto.setCaretColor(TEXT_MAIN);

		// Auto-guardar mientras escribe
		areaTexto.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			public void insertUpdate(javax.swing.event.DocumentEvent e) {
				autoGuardar();
			}

			public void removeUpdate(javax.swing.event.DocumentEvent e) {
				autoGuardar();
			}

			public void changedUpdate(javax.swing.event.DocumentEvent e) {
			}
		});

		JScrollPane scroll = new JScrollPane(areaTexto);
		scroll.setBorder(null);
		scroll.getViewport().setBackground(BG_PANEL);

		// Hint cuando no hay nota seleccionada
		JLabel hint = new JLabel("← Selecciona una nota o crea una nueva con  +");
		hint.setFont(new Font("SansSerif", Font.ITALIC, 13));
		hint.setForeground(TEXT_DIM);
		hint.setHorizontalAlignment(SwingConstants.CENTER);

		cardLayout = new CardLayout();
		cardPanel = new JPanel(cardLayout);
		cardPanel.add(hint, "hint");
		cardPanel.add(scroll, "editor");
		cardLayout.show(cardPanel, "hint");

		editor.add(header, BorderLayout.NORTH);
		editor.add(cardPanel, BorderLayout.CENTER);

		return editor;
	}

	// ── ABRIR NOTA ────────────────────────────────────────────
	private void abrirNota(Nota nota) {
		notaActual = nota;
		lblTitulo.setText("📝  " + nota.getNombre());

		guardando = true;
		areaTexto.setText(nota.getContenido());
		areaTexto.setEnabled(true);
		areaTexto.setCaretPosition(0);
		guardando = false;

		cardLayout.show(cardPanel, "editor");
		areaTexto.requestFocusInWindow();
	}

	// ── AUTO-GUARDAR ──────────────────────────────────────────
	private void autoGuardar() {
		if (guardando || notaActual == null)
			return;
		notaActual.setContenido(areaTexto.getText());
		guardarNota(notaActual);
	}

	// ── NUEVA NOTA ────────────────────────────────────────────
	private void nuevaNota() {
		String nombre = JOptionPane.showInputDialog(this, "Nombre de la nota:");
		if (nombre == null || nombre.isBlank())
			return;
		nombre = nombre.trim();

		Nota n = new Nota(nombre, "");
		notas.add(n);
		listModel.addElement(n);
		guardarNota(n);
		listaNota.setSelectedValue(n, true);
	}

	// ── CONTEXT MENU ──────────────────────────────────────────
	private void showContextMenu(MouseEvent e, Nota nota) {
		JPopupMenu menu = new JPopupMenu();

		JMenuItem rename = new JMenuItem("Renombrar");
		rename.addActionListener(ev -> {
			String nuevo = JOptionPane.showInputDialog(this, "Nuevo nombre:", nota.getNombre());
			if (nuevo != null && !nuevo.isBlank()) {
				// Borrar archivo viejo
				new File(DATA_DIR, sanitize(nota.getNombre()) + ".txt").delete();
				nota.setNombre(nuevo.trim());
				listaNota.repaint();
				if (notaActual == nota)
					lblTitulo.setText("📝  " + nuevo.trim());
				guardarNota(nota);
			}
		});

		JMenuItem del = new JMenuItem("Eliminar");
		del.setForeground(DEL_RED);
		del.addActionListener(ev -> {
			int c = JOptionPane.showConfirmDialog(this, "¿Eliminar \"" + nota.getNombre() + "\"?", "Confirmar",
					JOptionPane.YES_NO_OPTION);
			if (c == JOptionPane.YES_OPTION) {
				new File(DATA_DIR, sanitize(nota.getNombre()) + ".txt").delete();
				notas.remove(nota);
				listModel.removeElement(nota);
				if (notaActual == nota) {
					notaActual = null;
					areaTexto.setText("");
					areaTexto.setEnabled(false);
					lblTitulo.setText("Selecciona o crea una nota");
				}
			}
		});

		menu.add(rename);
		menu.addSeparator();
		menu.add(del);
		menu.show(listaNota, e.getX(), e.getY());
	}

	// ── PERSISTENCIA ─────────────────────────────────────────
	private void cargarNotas() {
		try {
			Files.createDirectories(Path.of(DATA_DIR));
			File dir = new File(DATA_DIR);
			File[] archivos = dir.listFiles((d, n) -> n.endsWith(".txt"));
			if (archivos == null)
				return;
			Arrays.sort(archivos, Comparator.comparing(File::getName));
			for (File f : archivos) {
				String nombre = f.getName().replace(".txt", "").replace("_", " ");
				String contenido = Files.readString(f.toPath());
				Nota n = new Nota(nombre, contenido);
				notas.add(n);
				listModel.addElement(n);
			}
		} catch (IOException e) {
			System.err.println("Error cargando notas: " + e.getMessage());
		}
	}

	private void guardarNota(Nota nota) {
		try {
			Files.createDirectories(Path.of(DATA_DIR));
			Files.writeString(Path.of(DATA_DIR, sanitize(nota.getNombre()) + ".txt"), nota.getContenido());
		} catch (IOException e) {
			System.err.println("Error guardando nota: " + e.getMessage());
		}
	}

	private String sanitize(String nombre) {
		return nombre.replaceAll("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ_\\-]", "_");
	}

	// ── NOTA MODEL ────────────────────────────────────────────
	static class Nota {
		private String nombre;
		private String contenido;

		Nota(String nombre, String contenido) {
			this.nombre = nombre;
			this.contenido = contenido;
		}

		String getNombre() {
			return nombre;
		}

		void setNombre(String n) {
			this.nombre = n;
		}

		String getContenido() {
			return contenido;
		}

		void setContenido(String c) {
			this.contenido = c;
		}

		@Override
		public String toString() {
			return nombre;
		}
	}

	// ── NOTA RENDERER ─────────────────────────────────────────
	private static class NotaRenderer extends DefaultListCellRenderer {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value instanceof Nota n) {
				setText("📝  " + n.getNombre());
				setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 8));
				setFont(new Font("SansSerif", Font.PLAIN, 13));
			}
			return this;
		}
	}
}