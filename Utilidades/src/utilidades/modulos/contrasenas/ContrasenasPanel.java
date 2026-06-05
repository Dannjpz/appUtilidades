package utilidades.modulos.contrasenas;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.undo.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public class ContrasenasPanel extends JPanel {

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

	private static final String DATA_DIR = "contrasenas";
	private static final String IMG_DIR = DATA_DIR + File.separator + "img";

	// ── Estado ────────────────────────────────────────────────
	private final List<Nota> notas = new ArrayList<>();
	private Nota notaActual = null;
	private Color colorTexto = Color.BLACK;
	private Color colorFondo = Color.WHITE;

	// ── UI ────────────────────────────────────────────────────
	private final DefaultListModel<Nota> listModel = new DefaultListModel<>();
	private JList<Nota> listaNota;
	private JTextPane editorPane;
	private JLabel lblTitulo;
	private boolean guardando = false;
	private JPanel cardPanel;
	private CardLayout cardLayout;

	// Botones de color (para actualizar preview)
	private JButton btnColorTexto;
	private JButton btnColorFondo;

	// ── Undo ──────────────────────────────────────────────────
	private UndoManager undoManager = new UndoManager();

	// ── Auto-save timer ───────────────────────────────────────
	private final javax.swing.Timer saveTimer = new javax.swing.Timer(400, e -> realizarGuardado());

	public ContrasenasPanel() {
		setLayout(new BorderLayout());
		setBackground(BG_APP);
		saveTimer.setRepeats(false);
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

		// Header con título
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(BG_PANEL);
		header.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, BORDER_LIGHT),
				BorderFactory.createEmptyBorder(10, 16, 10, 16)));

		lblTitulo = new JLabel("Selecciona o crea una nota");
		lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 15));
		lblTitulo.setForeground(TEXT_MAIN);
		header.add(lblTitulo, BorderLayout.CENTER);

		// ── Barra de formato ──────────────────────────────────
		JPanel toolbar = buildToolbar();

		// JTextPane HTML
		editorPane = new JTextPane();
		editorPane.setContentType("text/html");
		editorPane.setFont(new Font("SansSerif", Font.PLAIN, 14));
		editorPane.setForeground(TEXT_MAIN);
		editorPane.setBackground(BG_PANEL);
		editorPane.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
		editorPane.setEnabled(false);
		editorPane.setCaretColor(TEXT_MAIN);

		aplicarKit();

		// ── Atajos ────────────────────────────────────────────
		InputMap im = editorPane.getInputMap(JComponent.WHEN_FOCUSED);
		ActionMap am = editorPane.getActionMap();

		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK), "copy");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK), "cut");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK), "select-all");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK), "paste-smart");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undo");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK), "bold");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK), "italic");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_DOWN_MASK), "underline");

		am.put("copy", TransferHandler.getCopyAction());
		am.put("cut", TransferHandler.getCutAction());
		am.put("select-all", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				editorPane.selectAll();
			}
		});
		am.put("paste-smart", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pegarDesdePortapapeles();
			}
		});
		am.put("undo", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (undoManager.canUndo()) {
					undoManager.undo();
					realizarGuardado();
				}
			}
		});
		am.put("bold", new StyledEditorKit.BoldAction());
		am.put("italic", new StyledEditorKit.ItalicAction());
		am.put("underline", new StyledEditorKit.UnderlineAction());

		// Click derecho sobre imagen → menú de tamaño
		editorPane.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					Element img = imagenEnPunto(e.getPoint());
					if (img != null)
						mostrarMenuImagen(img, e);
				}
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

		JPanel editorWrapper = new JPanel(new BorderLayout());
		editorWrapper.setBackground(BG_PANEL);
		editorWrapper.add(toolbar, BorderLayout.NORTH);
		editorWrapper.add(scroll, BorderLayout.CENTER);

		cardPanel.add(hint, "hint");
		cardPanel.add(editorWrapper, "editor");
		cardLayout.show(cardPanel, "hint");

		editor.add(header, BorderLayout.NORTH);
		editor.add(cardPanel, BorderLayout.CENTER);
		return editor;
	}

	// ── TOOLBAR DE FORMATO ────────────────────────────────────
	private JPanel buildToolbar() {
		JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
		bar.setBackground(new Color(248, 248, 250));
		bar.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_LIGHT));

		// Bold / Italic / Underline
		bar.add(formatBtn("B", Font.BOLD, "Negrita (Ctrl+B)", new StyledEditorKit.BoldAction()));
		bar.add(formatBtn("I", Font.ITALIC, "Cursiva (Ctrl+I)", new StyledEditorKit.ItalicAction()));
		bar.add(formatBtn("U", Font.PLAIN, "Subrayado (Ctrl+U)", new StyledEditorKit.UnderlineAction()));

		bar.add(separador());

		// Color de texto
		btnColorTexto = colorBtn(colorTexto, false);
		btnColorTexto.setToolTipText("Color de texto");
		btnColorTexto.addActionListener(e -> {
			Color c = JColorChooser.showDialog(this, "Color de texto", colorTexto);
			if (c != null) {
				colorTexto = c;
				actualizarColorBtn(btnColorTexto, c, false);
				aplicarColor(c, false);
			}
		});

		// Color de fondo/resaltado
		btnColorFondo = colorBtn(colorFondo, true);
		btnColorFondo.setToolTipText("Color de resaltado");
		btnColorFondo.addActionListener(e -> {
			Color c = JColorChooser.showDialog(this, "Color de resaltado", colorFondo);
			if (c != null) {
				colorFondo = c;
				actualizarColorBtn(btnColorFondo, c, true);
				aplicarColor(c, true);
			}
		});

		JLabel lblTxt = new JLabel("A");
		lblTxt.setFont(new Font("SansSerif", Font.BOLD, 13));
		lblTxt.setForeground(TEXT_MAIN);

		JLabel lblBg = new JLabel("▌");
		lblBg.setFont(new Font("SansSerif", Font.PLAIN, 14));
		lblBg.setForeground(TEXT_DIM);

		JPanel txtColorPanel = new JPanel(new BorderLayout(3, 0));
		txtColorPanel.setBackground(new Color(248, 248, 250));
		txtColorPanel.add(lblTxt, BorderLayout.WEST);
		txtColorPanel.add(btnColorTexto, BorderLayout.CENTER);
		txtColorPanel.setToolTipText("Color de texto");

		JPanel bgColorPanel = new JPanel(new BorderLayout(3, 0));
		bgColorPanel.setBackground(new Color(248, 248, 250));
		bgColorPanel.add(lblBg, BorderLayout.WEST);
		bgColorPanel.add(btnColorFondo, BorderLayout.CENTER);
		bgColorPanel.setToolTipText("Color de resaltado");

		bar.add(txtColorPanel);
		bar.add(bgColorPanel);

		bar.add(separador());

		// Quitar formato
		JButton btnLimpiar = toolbarBtn("✕ Formato", "Quitar formato del texto seleccionado");
		btnLimpiar.addActionListener(e -> quitarFormato());
		bar.add(btnLimpiar);

		return bar;
	}

	private JButton formatBtn(String txt, int style, String tooltip, Action action) {
		JButton b = new JButton(txt);
		b.setFont(new Font("SansSerif", style, 13));
		b.setToolTipText(tooltip);
		b.setPreferredSize(new Dimension(28, 24));
		b.setFocusPainted(false);
		b.setBackground(new Color(238, 238, 242));
		b.setForeground(TEXT_MAIN);
		b.setBorder(new LineBorder(BORDER_LIGHT, 1, true));
		b.setOpaque(true);
		b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		b.addActionListener(e -> {
			action.actionPerformed(e);
			editorPane.requestFocusInWindow();
		});
		b.addMouseListener(hoverListener(b));
		return b;
	}

	private JButton toolbarBtn(String txt, String tooltip) {
		JButton b = new JButton(txt);
		b.setFont(new Font("SansSerif", Font.PLAIN, 11));
		b.setToolTipText(tooltip);
		b.setFocusPainted(false);
		b.setBackground(new Color(238, 238, 242));
		b.setForeground(TEXT_MAIN);
		b.setBorder(new LineBorder(BORDER_LIGHT, 1, true));
		b.setOpaque(true);
		b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		b.addMouseListener(hoverListener(b));
		return b;
	}

	private JButton colorBtn(Color c, boolean esFondo) {
		JButton b = new JButton() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				Graphics2D g2 = (Graphics2D) g;
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				Color col = (Color) getClientProperty("col");
				if (col == null)
					col = Color.BLACK;
				g2.setColor(col);
				g2.fillRoundRect(2, getHeight() - 6, getWidth() - 4, 4, 2, 2);
			}
		};
		b.putClientProperty("col", c);
		b.setPreferredSize(new Dimension(22, 24));
		b.setFocusPainted(false);
		b.setBackground(new Color(238, 238, 242));
		b.setBorder(new LineBorder(BORDER_LIGHT, 1, true));
		b.setOpaque(true);
		b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		b.addMouseListener(hoverListener(b));
		return b;
	}

	private void actualizarColorBtn(JButton btn, Color c, boolean esFondo) {
		btn.putClientProperty("col", c);
		btn.repaint();
	}

	private MouseAdapter hoverListener(JButton b) {
		Color normal = new Color(238, 238, 242);
		Color hover = new Color(218, 218, 224);
		return new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				b.setBackground(hover);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				b.setBackground(normal);
			}
		};
	}

	private JSeparator separador() {
		JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
		sep.setPreferredSize(new Dimension(1, 20));
		sep.setForeground(BORDER_LIGHT);
		return sep;
	}

	private void aplicarColor(Color color, boolean esFondo) {
		int start = editorPane.getSelectionStart();
		int end = editorPane.getSelectionEnd();
		if (start == end)
			return; // nada seleccionado
		MutableAttributeSet attr = new SimpleAttributeSet();
		if (esFondo)
			StyleConstants.setBackground(attr, color);
		else
			StyleConstants.setForeground(attr, color);
		editorPane.getStyledDocument().setCharacterAttributes(start, end - start, attr, false);
		saveTimer.restart();
	}

	private void quitarFormato() {
		int start = editorPane.getSelectionStart();
		int end = editorPane.getSelectionEnd();
		if (start == end)
			return;
		SimpleAttributeSet reset = new SimpleAttributeSet();
		StyleConstants.setBold(reset, false);
		StyleConstants.setItalic(reset, false);
		StyleConstants.setUnderline(reset, false);
		StyleConstants.setForeground(reset, Color.BLACK);
		StyleConstants.setBackground(reset, Color.WHITE);
		editorPane.getStyledDocument().setCharacterAttributes(start, end - start, reset, true);
		saveTimer.restart();
	}

	// ── KIT HTML ──────────────────────────────────────────────
	private void aplicarKit() {
		HTMLEditorKit kit = new HTMLEditorKit();
		kit.getStyleSheet()
				.addRule("body { font-family: SansSerif; font-size: 14pt; color: #141414; margin: 0; padding: 0; }"
						+ "img  { max-width: 520px; margin: 6px 0; display: block; }");
		editorPane.setEditorKit(kit);
		editorPane.setDocument(kit.createDefaultDocument());
	}

	// ── ABRIR NOTA ────────────────────────────────────────────
	private void abrirNota(Nota nota) {
		saveTimer.stop();
		notaActual = nota;
		lblTitulo.setText("📝  " + nota.getNombre());

		guardando = true;
		try {
			aplicarKit();
			undoManager = new UndoManager();

			String html = nota.getContenido();
			if (html == null || html.isBlank())
				html = "<html><body><p></p></body></html>";
			editorPane.setText(html);
			editorPane.setCaretPosition(0);
			editorPane.setEnabled(true);

			editorPane.getDocument().addUndoableEditListener(undoManager);
			editorPane.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
				public void insertUpdate(javax.swing.event.DocumentEvent e) {
					if (!guardando)
						saveTimer.restart();
				}

				public void removeUpdate(javax.swing.event.DocumentEvent e) {
					if (!guardando)
						saveTimer.restart();
				}

				public void changedUpdate(javax.swing.event.DocumentEvent e) {
				}
			});
		} finally {
			guardando = false;
		}

		cardLayout.show(cardPanel, "editor");
		editorPane.requestFocusInWindow();
	}

	// ── PEGAR ─────────────────────────────────────────────────
	private void pegarDesdePortapapeles() {
		Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable t = cb.getContents(null);
		if (t == null)
			return;

		// 1. Java imageFlavor (capturas de pantalla)
		if (t.isDataFlavorSupported(DataFlavor.imageFlavor)) {
			try {
				Image img = (Image) t.getTransferData(DataFlavor.imageFlavor);
				insertarImagen(img);
				return;
			} catch (Exception ex) {
				System.err.println("imageFlavor: " + ex.getMessage());
			}
		}

		// 2. image/* como InputStream
		for (DataFlavor f : t.getTransferDataFlavors()) {
			if (f.getMimeType().startsWith("image/")
					&& InputStream.class.isAssignableFrom(f.getRepresentationClass())) {
				try {
					InputStream is = (InputStream) t.getTransferData(f);
					BufferedImage img = ImageIO.read(is);
					is.close();
					if (img != null) {
						insertarImagen(img);
						return;
					}
				} catch (Exception ex) {
					System.err.println("image/stream: " + ex.getMessage());
				}
			}
		}

		// 3. Lista de archivos (explorador)
		if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			try {
				@SuppressWarnings("unchecked")
				List<File> archivos = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
				for (File f : archivos) {
					String nom = f.getName().toLowerCase();
					if (nom.matches(".*\\.(png|jpg|jpeg|gif|bmp|webp)")) {
						BufferedImage img = ImageIO.read(f);
						if (img != null) {
							insertarImagen(img);
							return;
						}
					}
				}
			} catch (Exception ex) {
				System.err.println("fileList: " + ex.getMessage());
			}
		}

		// 4. HTML — intentar pegar con formato (colores, negrita, etc.)
		boolean tieneHtml = false;
		try {
			DataFlavor htmlFlavor = null;
			for (DataFlavor f : t.getTransferDataFlavors()) {
				if (f.getMimeType().startsWith("text/html") && String.class.equals(f.getRepresentationClass())) {
					htmlFlavor = f;
					break;
				}
			}

			if (htmlFlavor != null) {
				tieneHtml = true;
				String html = (String) t.getTransferData(htmlFlavor);

				// a) Buscar <img src> estándar
				String src = extraerSrcDeHtml(html);
				if (src != null && !src.isBlank()) {
					BufferedImage img = cargarImagenDesdeUrl(src);
					if (img != null) {
						insertarImagen(img);
						return;
					}
				}

				// b) data:image base64 inline
				BufferedImage imgInline = extraerImagenBase64DeHtml(html);
				if (imgInline != null) {
					insertarImagen(imgInline);
					return;
				}

				// c) Pegar HTML limpio preservando colores y formato
				String fragmento = extraerFragmento(html);
				if (fragmento != null && !fragmento.isBlank()) {
					String htmlLimpio = limpiarHtml(fragmento);
					if (!htmlLimpio.isBlank()) {
						insertarHtmlFormateado(htmlLimpio);
						return;
					}
				}

				// d) Sin contenido útil
				JOptionPane.showMessageDialog(this,
						"<html><b>No se pudo pegar el contenido.</b><br><br>"
								+ "Para imágenes usa <b>Win+Shift+S</b> → Ctrl+V aquí.</html>",
						"Formato no compatible", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
		} catch (Exception ex) {
			System.err.println("html: " + ex.getMessage());
		}

		// 5. Texto plano
		if (!tieneHtml) {
			try {
				if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
					String texto = (String) t.getTransferData(DataFlavor.stringFlavor);
					editorPane.replaceSelection(texto);
				}
			} catch (Exception ex) {
				System.err.println("texto: " + ex.getMessage());
			}
		}
	}

	/**
	 * Extrae el fragmento entre StartFragment/EndFragment del HTML de portapapeles
	 */
	private String extraerFragmento(String html) {
		int start = html.indexOf("<!--StartFragment-->");
		int end = html.indexOf("<!--EndFragment-->");
		if (start >= 0 && end > start) {
			return html.substring(start + "<!--StartFragment-->".length(), end).trim();
		}
		// Sin marcadores → usar el body
		int bodyStart = html.indexOf("<body");
		int bodyEnd = html.lastIndexOf("</body>");
		if (bodyStart >= 0 && bodyEnd > bodyStart) {
			int gt = html.indexOf('>', bodyStart);
			return html.substring(gt + 1, bodyEnd).trim();
		}
		return html;
	}

	/** Limpia el HTML manteniendo colores, negrita, cursiva, subrayado */
	private String limpiarHtml(String html) {
		if (html == null || html.isBlank())
			return "";

		// Reemplazar bloques de imágenes propietarias por nada
		html = html.replaceAll("(?i)<img[^>]*data-canva[^>]*>", "");

		// Conservar solo los atributos style con
		// color/font-weight/font-style/text-decoration
		// Eliminar atributos data-*, class, id, etc.
		html = html.replaceAll("(?i)\\s+data-[a-z0-9_-]+\\s*=\\s*\"[^\"]*\"", "");
		html = html.replaceAll("(?i)\\s+data-[a-z0-9_-]+\\s*=\\s*'[^']*'", "");
		html = html.replaceAll("(?i)\\s+class\\s*=\\s*\"[^\"]*\"", "");
		html = html.replaceAll("(?i)\\s+class\\s*=\\s*'[^']*'", "");
		html = html.replaceAll("(?i)\\s+id\\s*=\\s*\"[^\"]*\"", "");

		// Simplificar style: conservar solo color, background-color, font-weight,
		// font-style, text-decoration
		html = simplificarStyle(html);

		// Limpiar tags vacíos
		html = html.replaceAll("<span\\s*>\\s*</span>", "");
		html = html.replaceAll("<div\\s*>\\s*</div>", "");

		return html.trim();
	}

	/** Simplifica atributos style conservando solo propiedades visuales básicas */
	private String simplificarStyle(String html) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		while (i < html.length()) {
			int styleIdx = html.indexOf("style=\"", i);
			if (styleIdx < 0) {
				sb.append(html.substring(i));
				break;
			}
			sb.append(html, i, styleIdx + 7); // incluir style="
			int endQuote = html.indexOf("\"", styleIdx + 7);
			if (endQuote < 0) {
				sb.append(html.substring(styleIdx + 7));
				break;
			}
			String styleValue = html.substring(styleIdx + 7, endQuote);
			sb.append(filtrarStyle(styleValue));
			sb.append("\"");
			i = endQuote + 1;
		}
		return sb.toString();
	}

	private String filtrarStyle(String style) {
		StringBuilder resultado = new StringBuilder();
		for (String prop : style.split(";")) {
			prop = prop.trim();
			String lower = prop.toLowerCase();
			if (lower.startsWith("color:") || lower.startsWith("background-color:") || lower.startsWith("font-weight:")
					|| lower.startsWith("font-style:") || lower.startsWith("text-decoration:")
					|| lower.startsWith("font-size:")) {
				if (resultado.length() > 0)
					resultado.append("; ");
				resultado.append(prop);
			}
		}
		return resultado.toString();
	}

	/** Inserta HTML formateado en la posición actual del caret */
	private void insertarHtmlFormateado(String html) {
		try {
			HTMLEditorKit kit = (HTMLEditorKit) editorPane.getEditorKit();
			HTMLDocument doc = (HTMLDocument) editorPane.getDocument();
			int pos = editorPane.getCaretPosition();
			kit.insertHTML(doc, pos, html, 0, 0, null);
			saveTimer.restart();
		} catch (Exception ex) {
			// Fallback: pegar como texto plano
			String textoPlano = stripHtml(html);
			if (!textoPlano.isBlank())
				editorPane.replaceSelection(textoPlano);
			System.err.println("insertarHtmlFormateado falló, texto plano: " + ex.getMessage());
		}
	}

	// ── INSERTAR IMAGEN ───────────────────────────────────────
	private void insertarImagen(Image img) throws IOException {
		BufferedImage bi;
		if (img instanceof BufferedImage) {
			bi = (BufferedImage) img;
		} else {
			bi = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = bi.createGraphics();
			g2.setColor(Color.WHITE);
			g2.fillRect(0, 0, bi.getWidth(), bi.getHeight());
			g2.drawImage(img, 0, 0, null);
			g2.dispose();
		}
		Files.createDirectories(Path.of(IMG_DIR));
		String nombre = UUID.randomUUID().toString().replace("-", "").substring(0, 12) + ".png";
		File imgFile = new File(IMG_DIR, nombre);
		ImageIO.write(bi, "png", imgFile);

		String src = imgFile.toURI().toASCIIString();
		HTMLEditorKit kit = (HTMLEditorKit) editorPane.getEditorKit();
		HTMLDocument doc = (HTMLDocument) editorPane.getDocument();
		try {
			kit.insertHTML(doc, editorPane.getCaretPosition(), "<img src=\"" + src + "\" width=\"350\">", 0, 0,
					HTML.Tag.IMG);
		} catch (BadLocationException ex) {
			System.err.println("Error insertando imagen: " + ex.getMessage());
		}
		saveTimer.restart();
	}

	// ── DETECCIÓN Y REDIMENSIÓN DE IMÁGENES ───────────────────
	private Element imagenEnPunto(Point p) {
		int pos = editorPane.viewToModel2D(p);
		if (pos < 0)
			return null;
		HTMLDocument doc = (HTMLDocument) editorPane.getDocument();
		for (int offset : new int[] { pos, pos - 1 }) {
			if (offset < 0)
				continue;
			Element elem = doc.getCharacterElement(offset);
			if (esImagen(elem))
				return elem;
		}
		return null;
	}

	private boolean esImagen(Element elem) {
		if (elem == null)
			return false;
		AttributeSet attrs = elem.getAttributes();
		return HTML.Tag.IMG.equals(attrs.getAttribute(StyleConstants.NameAttribute))
				|| attrs.isDefined(HTML.Attribute.SRC);
	}

	private void mostrarMenuImagen(Element imgElem, MouseEvent e) {
		JPopupMenu menu = new JPopupMenu();
		JLabel titulo = new JLabel("  Tamaño de imagen");
		titulo.setFont(new Font("SansSerif", Font.BOLD, 11));
		titulo.setForeground(TEXT_DIM);
		menu.add(titulo);
		menu.addSeparator();

		String[] etiquetas = { "Pequeña (150px)", "Mediana (350px)", "Grande (550px)", "Original" };
		int[] anchos = { 150, 350, 550, -1 };
		for (int i = 0; i < etiquetas.length; i++) {
			final int ancho = anchos[i];
			JMenuItem item = new JMenuItem(etiquetas[i]);
			item.addActionListener(ev -> redimensionarImagen(imgElem, ancho));
			menu.add(item);
		}
		menu.addSeparator();
		JMenuItem custom = new JMenuItem("✏ Personalizado...");
		custom.addActionListener(ev -> {
			String input = JOptionPane.showInputDialog(this, "Ancho en píxeles:");
			if (input != null && !input.isBlank()) {
				try {
					redimensionarImagen(imgElem, Integer.parseInt(input.trim()));
				} catch (NumberFormatException ignored) {
				}
			}
		});
		menu.add(custom);
		menu.addSeparator();
		JMenuItem eliminar = new JMenuItem("🗑 Eliminar imagen");
		eliminar.setForeground(DEL_RED);
		eliminar.addActionListener(ev -> eliminarImagen(imgElem));
		menu.add(eliminar);
		menu.show(editorPane, e.getX(), e.getY());
	}

	private void redimensionarImagen(Element imgElem, int nuevoAncho) {
		try {
			HTMLDocument doc = (HTMLDocument) editorPane.getDocument();
			String src = (String) imgElem.getAttributes().getAttribute(HTML.Attribute.SRC);
			if (src == null)
				return;
			int start = imgElem.getStartOffset();
			int end = imgElem.getEndOffset();
			guardando = true;
			doc.remove(start, end - start);
			String tag = nuevoAncho > 0 ? "<img src=\"" + src + "\" width=\"" + nuevoAncho + "\">"
					: "<img src=\"" + src + "\">";
			((HTMLEditorKit) editorPane.getEditorKit()).insertHTML(doc, start, tag, 0, 0, HTML.Tag.IMG);
			guardando = false;
			saveTimer.restart();
		} catch (Exception ex) {
			guardando = false;
		}
	}

	private void eliminarImagen(Element imgElem) {
		try {
			HTMLDocument doc = (HTMLDocument) editorPane.getDocument();
			guardando = true;
			doc.remove(imgElem.getStartOffset(), imgElem.getEndOffset() - imgElem.getStartOffset());
			guardando = false;
			saveTimer.restart();
		} catch (Exception ex) {
			guardando = false;
		}
	}

	// ── GUARDAR ───────────────────────────────────────────────
	private void realizarGuardado() {
		if (notaActual == null)
			return;
		try {
			StringWriter sw = new StringWriter();
			editorPane.getEditorKit().write(sw, editorPane.getDocument(), 0, editorPane.getDocument().getLength());
			notaActual.setContenido(sw.toString());
			guardarNota(notaActual);
		} catch (Exception ex) {
			System.err.println("Error al guardar: " + ex.getMessage());
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
				saveTimer.stop();
				new File(DATA_DIR, sanitize(nota.getNombre()) + ".html").delete();
				new File(DATA_DIR, sanitize(nota.getNombre()) + ".txt").delete();
				notas.remove(nota);
				listModel.removeElement(nota);
				if (notaActual == nota) {
					notaActual = null;
					guardando = true;
					editorPane.setText("");
					guardando = false;
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

	// ── HELPERS HTML ─────────────────────────────────────────
	private String extraerSrcDeHtml(String html) {
		if (html == null)
			return null;
		String lower = html.toLowerCase();
		int imgIdx = lower.indexOf("<img");
		if (imgIdx < 0)
			return null;
		String desde = html.substring(imgIdx);
		for (String patron : new String[] { "src=\"", "src='" }) {
			int s = desde.toLowerCase().indexOf(patron);
			if (s >= 0) {
				s += patron.length();
				char cierre = patron.endsWith("\"") ? '"' : '\'';
				int e = desde.indexOf(cierre, s);
				if (e > s)
					return desde.substring(s, e);
			}
		}
		return null;
	}

	private BufferedImage extraerImagenBase64DeHtml(String html) {
		int idx = html.indexOf("data:image/");
		if (idx < 0)
			return null;
		int commaIdx = html.indexOf(",", idx);
		if (commaIdx < 0)
			return null;
		int end = commaIdx + 1;
		while (end < html.length() && "\"' \n)".indexOf(html.charAt(end)) < 0)
			end++;
		try {
			byte[] bytes = Base64.getDecoder().decode(html.substring(commaIdx + 1, end).trim());
			return ImageIO.read(new ByteArrayInputStream(bytes));
		} catch (Exception ex) {
			return null;
		}
	}

	private BufferedImage cargarImagenDesdeUrl(String src) {
		try {
			java.net.URL url = new java.net.URL(src);
			java.net.URLConnection conn = url.openConnection();
			conn.setRequestProperty("User-Agent", "Mozilla/5.0");
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(8000);
			return ImageIO.read(conn.getInputStream());
		} catch (Exception ex) {
			return null;
		}
	}

	private String stripHtml(String html) {
		if (html == null)
			return "";
		int bodyIdx = html.indexOf("<body");
		if (bodyIdx >= 0)
			html = html.substring(bodyIdx);
		return html.replaceAll("(?i)<br\\s*/?>", "\n").replaceAll("(?i)</(p|div|tr|li)>", "\n")
				.replaceAll("(?i)<img[^>]*>", "").replaceAll("<[^>]+>", "").replace("&nbsp;", " ").replace("&amp;", "&")
				.replace("&lt;", "<").replace("&gt;", ">").replaceAll("\n{3,}", "\n\n").trim();
	}

	// ── PERSISTENCIA ─────────────────────────────────────────
	private void cargarNotas() {
		try {
			Files.createDirectories(Path.of(DATA_DIR));
			File dir = new File(DATA_DIR);
			List<File> archivos = new ArrayList<>();
			File[] html = dir.listFiles((d, n) -> n.endsWith(".html"));
			File[] txt = dir.listFiles((d, n) -> n.endsWith(".txt"));
			if (html != null)
				archivos.addAll(Arrays.asList(html));
			if (txt != null)
				archivos.addAll(Arrays.asList(txt));
			archivos.sort(Comparator.comparing(File::getName));
			Set<String> vistos = new HashSet<>();
			for (File f : archivos) {
				String nombre = f.getName().replaceAll("\\.(html|txt)$", "").replace("_", " ");
				if (vistos.contains(nombre))
					continue;
				vistos.add(nombre);
				String contenido = Files.readString(f.toPath(), StandardCharsets.UTF_8);
				if (f.getName().endsWith(".txt")) {
					contenido = "<html><body>"
							+ contenido.replace("&", "&amp;").replace("<", "&lt;").replace("\n", "<br>")
							+ "</body></html>";
				}
				notas.add(new Nota(nombre, contenido));
				listModel.addElement(notas.get(notas.size() - 1));
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
		private String nombre, contenido;

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

	private static class NotaRenderer extends DefaultListCellRenderer {
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