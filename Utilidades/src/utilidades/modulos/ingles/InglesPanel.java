package utilidades.modulos.ingles;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public class InglesPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	// ── Paleta ────────────────────────────────────────────────
	private static final Color BG_APP = new Color(242, 242, 247);
	private static final Color BG_SIDEBAR = new Color(248, 248, 250);
	private static final Color BG_PANEL = new Color(255, 255, 255);
	private static final Color ACCENT = new Color(60, 120, 220);
	private static final Color BORDER_LIGHT = new Color(210, 210, 215);
	private static final Color TEXT_MAIN = new Color(20, 20, 20);
	private static final Color TEXT_DIM = new Color(130, 130, 140);
	private static final Color SEL_BG = new Color(210, 225, 255);
	private static final Color DEL_RED = new Color(180, 45, 45);

	private static final String DATA_DIR = "ingles";

	// ── Estado ────────────────────────────────────────────────
	private final List<Nota> notas = new ArrayList<>();
	private Nota notaActual = null;

	// ── UI ────────────────────────────────────────────────────
	private final DefaultListModel<Nota> listModel = new DefaultListModel<>();
	private JList<Nota> listaNota;
	private JTextPane editorPane;
	private JLabel lblTitulo;
	private boolean guardando = false;
	private JPanel cardPanel;
	private CardLayout cardLayout;

	public InglesPanel() {
		setLayout(new BorderLayout());
		setBackground(BG_APP);

		cargarNotas();

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildSidebar(), buildEditor());
		split.setDividerLocation(200);
		split.setDividerSize(1);
		split.setBorder(null);
		split.setResizeWeight(0);

		add(split, BorderLayout.CENTER);

		if (!notas.isEmpty())
			listaNota.setSelectedIndex(0);
	}

	// ── SIDEBAR ───────────────────────────────────────────────
	private JPanel buildSidebar() {
		JPanel sidebar = new JPanel(new BorderLayout());
		sidebar.setBackground(BG_SIDEBAR);
		sidebar.setBorder(new MatteBorder(0, 0, 0, 1, BORDER_LIGHT));
		sidebar.setPreferredSize(new Dimension(200, 0));

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

		listaNota = new JList<>(listModel);
		listaNota.setBackground(BG_SIDEBAR);
		listaNota.setFont(new Font("SansSerif", Font.PLAIN, 13));
		listaNota.setFixedCellHeight(40);
		listaNota.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
		listaNota.setCellRenderer(new InglesNotaRenderer());
		listaNota.setSelectionBackground(SEL_BG);
		listaNota.setSelectionForeground(TEXT_MAIN);

		listaNota.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				Nota sel = listaNota.getSelectedValue();
				if (sel != null)
					abrirNota(sel);
			}
		});

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

		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(BG_PANEL);
		header.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, BORDER_LIGHT),
				BorderFactory.createEmptyBorder(10, 16, 10, 16)));

		lblTitulo = new JLabel("Selecciona o crea una nota");
		lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 15));
		lblTitulo.setForeground(TEXT_MAIN);
		header.add(lblTitulo, BorderLayout.CENTER);

		// JTextPane con soporte HTML (necesario para imágenes inline)
		editorPane = new JTextPane();
		editorPane.setContentType("text/html");
		editorPane.setFont(new Font("SansSerif", Font.PLAIN, 14));
		editorPane.setForeground(TEXT_MAIN);
		editorPane.setBackground(BG_PANEL);
		editorPane.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
		editorPane.setEnabled(false);
		editorPane.setCaretColor(TEXT_MAIN);

		// CSS base para que el texto se vea limpio
		HTMLEditorKit kit = new HTMLEditorKit();
		kit.getStyleSheet()
				.addRule("body { font-family: SansSerif; font-size: 14pt; color: #141414; margin: 0; padding: 0; }"
						+ "img  { max-width: 100%; margin: 4px 0; }");
		editorPane.setEditorKit(kit);
		editorPane.setDocument(kit.createDefaultDocument());

		// ── Atajos de teclado ─────────────────────────────────
		InputMap im = editorPane.getInputMap(JComponent.WHEN_FOCUSED);
		ActionMap am = editorPane.getActionMap();

		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK), "copy");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK), "cut");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK), "select-all");
		am.put("copy", TransferHandler.getCopyAction());
		am.put("cut", TransferHandler.getCutAction());
		am.put("select-all", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				editorPane.selectAll();
			}
		});

		// Ctrl+V: pegar texto O imagen
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK), "paste-smart");
		am.put("paste-smart", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pegarDesdePortapapeles();
			}
		});

		// Auto-guardar al escribir
		editorPane.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			public void insertUpdate(javax.swing.event.DocumentEvent e) {
				autoGuardar();
			}

			public void removeUpdate(javax.swing.event.DocumentEvent e) {
				autoGuardar();
			}

			public void changedUpdate(javax.swing.event.DocumentEvent e) {
			}
		});

		JScrollPane scroll = new JScrollPane(editorPane);
		scroll.setBorder(null);
		scroll.getViewport().setBackground(BG_PANEL);

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

	// ── PEGAR (texto o imagen) ────────────────────────────────
	private void pegarDesdePortapapeles() {
		Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable t = cb.getContents(null);
		if (t == null)
			return;

		// ¿Hay imagen en el portapapeles?
		if (t.isDataFlavorSupported(DataFlavor.imageFlavor)) {
			try {
				Image img = (Image) t.getTransferData(DataFlavor.imageFlavor);
				insertarImagen(img);
				return;
			} catch (Exception ex) {
				System.err.println("Error pegando imagen: " + ex.getMessage());
			}
		}

		// Si no, pegar como texto normal
		editorPane.paste();
	}

	private void insertarImagen(Image img) throws IOException {
		// Convertir a BufferedImage
		BufferedImage bi;
		if (img instanceof BufferedImage) {
			bi = (BufferedImage) img;
		} else {
			bi = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2 = bi.createGraphics();
			g2.drawImage(img, 0, 0, null);
			g2.dispose();
		}

		// Codificar como Base64 directamente — sin archivos externos
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(bi, "png", baos);
		String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
		String src = "data:image/png;base64," + base64;

		// Insertar <img> con data URI en el documento
		HTMLEditorKit kit = (HTMLEditorKit) editorPane.getEditorKit();
		HTMLDocument doc = (HTMLDocument) editorPane.getDocument();
		try {
			kit.insertHTML(doc, editorPane.getCaretPosition(), "<img src=\"" + src + "\">", 0, 0, HTML.Tag.IMG);
		} catch (BadLocationException ex) {
			System.err.println("Error insertando imagen: " + ex.getMessage());
		}
		autoGuardar();
	}

	// ── ABRIR NOTA ────────────────────────────────────────────
	private void abrirNota(Nota nota) {
		notaActual = nota;
		lblTitulo.setText("📝  " + nota.getNombre());

		guardando = true;
		try {
			HTMLEditorKit kit = new HTMLEditorKit();
			kit.getStyleSheet()
					.addRule("body { font-family: SansSerif; font-size: 14pt; color: #141414; margin: 0; padding: 0; }"
							+ "img  { max-width: 100%; margin: 4px 0; }");
			editorPane.setEditorKit(kit);
			HTMLDocument doc = (HTMLDocument) kit.createDefaultDocument();
			editorPane.setDocument(doc);

			String html = nota.getContenido();
			if (html == null || html.isBlank())
				html = "<html><body></body></html>";
			editorPane.setText(html);
			editorPane.setCaretPosition(0);
		} finally {
			guardando = false;
		}

		editorPane.setEnabled(true);
		cardLayout.show(cardPanel, "editor");
		editorPane.requestFocusInWindow();
	}

	// ── AUTO-GUARDAR ──────────────────────────────────────────
	private void autoGuardar() {
		if (guardando || notaActual == null)
			return;
		try {
			StringWriter sw = new StringWriter();
			editorPane.getEditorKit().write(sw, editorPane.getDocument(), 0, editorPane.getDocument().getLength());
			notaActual.setContenido(sw.toString());
			guardarNota(notaActual);
		} catch (Exception ex) {
			System.err.println("Error al auto-guardar: " + ex.getMessage());
		}
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
				new File(DATA_DIR, sanitize(nota.getNombre()) + ".html").delete();
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
				new File(DATA_DIR, sanitize(nota.getNombre()) + ".html").delete();
				new File(DATA_DIR, sanitize(nota.getNombre()) + ".txt").delete();
				notas.remove(nota);
				listModel.removeElement(nota);
				if (notaActual == nota) {
					notaActual = null;
					editorPane.setText("");
					editorPane.setEnabled(false);
					lblTitulo.setText("Selecciona o crea una nota");
					cardLayout.show(cardPanel, "hint");
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

			// Cargar .html (formato nuevo)
			File[] htmlFiles = dir.listFiles((d, n) -> n.endsWith(".html"));
			// Cargar .txt (formato viejo — migrar)
			File[] txtFiles = dir.listFiles((d, n) -> n.endsWith(".txt"));

			List<File> todos = new ArrayList<>();
			if (htmlFiles != null)
				todos.addAll(Arrays.asList(htmlFiles));
			if (txtFiles != null)
				todos.addAll(Arrays.asList(txtFiles));
			todos.sort(Comparator.comparing(File::getName));

			Set<String> nombresVistos = new HashSet<>();
			for (File f : todos) {
				String nombre = f.getName().replaceAll("\\.(html|txt)$", "").replace("_", " ");
				if (nombresVistos.contains(nombre))
					continue; // evitar duplicados
				nombresVistos.add(nombre);

				String contenido = Files.readString(f.toPath(), StandardCharsets.UTF_8);

				// Migrar .txt a .html
				if (f.getName().endsWith(".txt")) {
					contenido = "<html><body>"
							+ contenido.replace("&", "&amp;").replace("<", "&lt;").replace("\n", "<br>")
							+ "</body></html>";
				}

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
			String contenido = nota.getContenido();
			if (contenido == null)
				contenido = "";
			Files.writeString(Path.of(DATA_DIR, sanitize(nota.getNombre()) + ".html"), contenido,
					StandardCharsets.UTF_8);
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
	private static class InglesNotaRenderer extends DefaultListCellRenderer {
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