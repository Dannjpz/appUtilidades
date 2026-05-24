package utilidades.modulos.wow.guias;

import utilidades.modulos.wow.guias.modelo.Carpeta;
import utilidades.modulos.wow.guias.modelo.Entrada;
import utilidades.modulos.wow.guias.utiles.GuiasManager;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.List;

public class GuiasPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// ── Paleta ────────────────────────────────────────────────
	private static final Color BG_APP = new Color(242, 242, 247);
	private static final Color BG_PANEL = new Color(255, 255, 255);
	private static final Color BG_SIDEBAR = new Color(248, 248, 250);
	private static final Color ACCENT = new Color(60, 120, 220);
	private static final Color BORDER_LIGHT = new Color(210, 210, 215);
	private static final Color TEXT_MAIN = new Color(20, 20, 20);
	private static final Color TEXT_DIM = new Color(130, 130, 140);
	private static final Color SEL_BG = new Color(210, 225, 255);
	private static final Color CARD_BG = new Color(255, 255, 255);
	private static final Color CARD_BORDER = new Color(225, 225, 230);
	private static final Color DEL_RED = new Color(180, 45, 45);
	private static final Color ZOOM_BG = new Color(30, 30, 30);

	private List<Carpeta> carpetas;
	private Carpeta carpetaActual = null;

	private DefaultListModel<Carpeta> listModel;
	private JList<Carpeta> listaCarpetas;
	private JPanel contentPanel;
	private JScrollPane contentScroll;
	private JLabel lblTitulo;

	public GuiasPanel() {
		setLayout(new BorderLayout());
		setBackground(BG_APP);

		carpetas = GuiasManager.cargar();

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildSidebar(), buildContentArea());
		split.setDividerLocation(210);
		split.setDividerSize(1);
		split.setBorder(null);

		add(split, BorderLayout.CENTER);

		if (!carpetas.isEmpty()) {
			listaCarpetas.setSelectedIndex(0);
		}
	}

	public void guardar() {
		GuiasManager.guardar(carpetas);
	}

	// ── SIDEBAR ───────────────────────────────────────────────
	private JPanel buildSidebar() {
		JPanel sidebar = new JPanel(new BorderLayout());
		sidebar.setBackground(BG_SIDEBAR);
		sidebar.setBorder(new MatteBorder(0, 0, 0, 1, BORDER_LIGHT));
		sidebar.setPreferredSize(new Dimension(210, 0));

		// Header
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(BG_SIDEBAR);
		header.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, BORDER_LIGHT),
				BorderFactory.createEmptyBorder(10, 14, 10, 10)));

		JLabel lbl = new JLabel("Carpetas");
		lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
		lbl.setForeground(TEXT_MAIN);

		JButton btnNueva = new JButton("+");
		btnNueva.setFont(new Font("SansSerif", Font.BOLD, 16));
		btnNueva.setForeground(ACCENT);
		btnNueva.setBorder(BorderFactory.createEmptyBorder(0, 6, 2, 2));
		btnNueva.setContentAreaFilled(false);
		btnNueva.setBorderPainted(false);
		btnNueva.setFocusPainted(false);
		btnNueva.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btnNueva.setToolTipText("Nueva carpeta");
		btnNueva.addActionListener(e -> nuevaCarpeta());

		header.add(lbl, BorderLayout.WEST);
		header.add(btnNueva, BorderLayout.EAST);

		// List
		listModel = new DefaultListModel<>();
		carpetas.forEach(listModel::addElement);

		listaCarpetas = new JList<>(listModel);
		listaCarpetas.setBackground(BG_SIDEBAR);
		listaCarpetas.setFont(new Font("SansSerif", Font.PLAIN, 13));
		listaCarpetas.setFixedCellHeight(38);
		listaCarpetas.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
		listaCarpetas.setCellRenderer(new CarpetaRenderer());
		listaCarpetas.setSelectionBackground(SEL_BG);
		listaCarpetas.setSelectionForeground(TEXT_MAIN);

		listaCarpetas.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				Carpeta sel = listaCarpetas.getSelectedValue();
				if (sel != null)
					mostrarCarpeta(sel);
			}
		});

		listaCarpetas.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					int idx = listaCarpetas.locationToIndex(e.getPoint());
					if (idx >= 0) {
						listaCarpetas.setSelectedIndex(idx);
						showCarpetaMenu(e, listModel.get(idx));
					}
				}
			}
		});

		JScrollPane scroll = new JScrollPane(listaCarpetas);
		scroll.setBorder(null);
		scroll.setBackground(BG_SIDEBAR);

		sidebar.add(header, BorderLayout.NORTH);
		sidebar.add(scroll, BorderLayout.CENTER);
		return sidebar;
	}

	// ── CONTENT AREA ──────────────────────────────────────────
	private JPanel buildContentArea() {
		JPanel area = new JPanel(new BorderLayout());
		area.setBackground(BG_APP);

		// Header
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(BG_PANEL);
		header.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, BORDER_LIGHT),
				BorderFactory.createEmptyBorder(10, 16, 10, 12)));

		lblTitulo = new JLabel("Selecciona una carpeta");
		lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 15));
		lblTitulo.setForeground(TEXT_MAIN);

		JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
		btns.setBackground(BG_PANEL);
		JButton bTexto = smallBtn("+ Texto");
		JButton bImagen = smallBtn("+ Imagen");
		JButton bArchivo = smallBtn("+ Archivo");
		JButton bRealmlist = smallBtnAccent("📄 realmlist.wtf");
		bTexto.addActionListener(e -> agregarTexto());
		bImagen.addActionListener(e -> agregarImagen());
		bArchivo.addActionListener(e -> agregarArchivo());
		bRealmlist.addActionListener(e -> abrirRealmlist());
		btns.add(bRealmlist);
		btns.add(bTexto);
		btns.add(bImagen);
		btns.add(bArchivo);

		header.add(lblTitulo, BorderLayout.WEST);
		header.add(btns, BorderLayout.EAST);

		// Content
		contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBackground(BG_APP);
		contentPanel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

		contentScroll = new JScrollPane(contentPanel);
		contentScroll.setBorder(null);
		contentScroll.getViewport().setBackground(BG_APP);
		contentScroll.getVerticalScrollBar().setUnitIncrement(16);

		area.add(header, BorderLayout.NORTH);
		area.add(contentScroll, BorderLayout.CENTER);
		return area;
	}

	// ── MOSTRAR CARPETA ───────────────────────────────────────
	private void mostrarCarpeta(Carpeta c) {
		carpetaActual = c;
		lblTitulo.setText("📁  " + c.getNombre());
		contentPanel.removeAll();

		if (c.getEntradas().isEmpty()) {
			JLabel vacio = new JLabel("Carpeta vacía — usa los botones para agregar contenido.");
			vacio.setForeground(TEXT_DIM);
			vacio.setFont(new Font("SansSerif", Font.ITALIC, 13));
			vacio.setAlignmentX(Component.LEFT_ALIGNMENT);
			contentPanel.add(vacio);
		} else {
			for (Entrada e : c.getEntradas()) {
				contentPanel.add(buildCard(e));
				contentPanel.add(Box.createVerticalStrut(10));
			}
		}

		contentPanel.revalidate();
		contentPanel.repaint();
		SwingUtilities.invokeLater(() -> contentScroll.getVerticalScrollBar().setValue(0));
	}

	// ── ENTRY CARD ────────────────────────────────────────────
	private JPanel buildCard(Entrada entrada) {
		JPanel card = new JPanel(new BorderLayout(0, 8));
		card.setBackground(CARD_BG);
		card.setBorder(new CompoundBorder(new LineBorder(CARD_BORDER, 1, true),
				BorderFactory.createEmptyBorder(10, 14, 12, 14)));
		card.setAlignmentX(Component.LEFT_ALIGNMENT);
		card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

		// Card header row
		JPanel cardHeader = new JPanel(new BorderLayout());
		cardHeader.setBackground(CARD_BG);

		String icon = switch (entrada.getTipo()) {
		case TEXTO -> "📝";
		case IMAGEN -> "🖼";
		case ARCHIVO -> "📎";
		};

		JLabel lblNombre = new JLabel(icon + "  " + entrada.getNombre());
		lblNombre.setFont(new Font("SansSerif", Font.BOLD, 12));
		lblNombre.setForeground(TEXT_MAIN);

		JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
		right.setBackground(CARD_BG);

		JLabel lblFecha = new JLabel(entrada.getFecha());
		lblFecha.setFont(new Font("SansSerif", Font.PLAIN, 11));
		lblFecha.setForeground(TEXT_DIM);

		JButton btnDel = new JButton("✕");
		btnDel.setFont(new Font("SansSerif", Font.PLAIN, 11));
		btnDel.setForeground(DEL_RED);
		btnDel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 2));
		btnDel.setContentAreaFilled(false);
		btnDel.setBorderPainted(false);
		btnDel.setFocusPainted(false);
		btnDel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btnDel.addActionListener(e -> eliminarEntrada(entrada));

		right.add(lblFecha);
		right.add(btnDel);
		cardHeader.add(lblNombre, BorderLayout.WEST);
		cardHeader.add(right, BorderLayout.EAST);

		// Separator
		JSeparator sep = new JSeparator();
		sep.setForeground(CARD_BORDER);

		// Card body
		Component body = switch (entrada.getTipo()) {
		case TEXTO -> buildTextoBody(entrada);
		case IMAGEN -> buildImagenBody(entrada);
		case ARCHIVO -> buildArchivoBody(entrada);
		};

		card.add(cardHeader, BorderLayout.NORTH);
		card.add(sep, BorderLayout.CENTER);
		card.add(body, BorderLayout.SOUTH);
		return card;
	}

	private Component buildTextoBody(Entrada entrada) {
		JTextArea ta = new JTextArea(entrada.getContenido());
		ta.setFont(new Font("SansSerif", Font.PLAIN, 13));
		ta.setForeground(TEXT_MAIN);
		ta.setBackground(CARD_BG);
		ta.setLineWrap(true);
		ta.setWrapStyleWord(true);
		ta.setBorder(null);
		ta.setRows(5);
		ta.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				String nuevo = ta.getText();
				if (!nuevo.equals(entrada.getContenido())) {
					entrada.setContenido(nuevo);
					guardar();
				}
			}
		});
		return ta;
	}

	private Component buildImagenBody(Entrada entrada) {
		String ruta = GuiasManager.MEDIA_DIR + File.separator + entrada.getContenido();
		File f = new File(ruta);

		if (!f.exists()) {
			JLabel err = new JLabel("⚠  Imagen no encontrada: " + entrada.getContenido());
			err.setForeground(DEL_RED);
			err.setFont(new Font("SansSerif", Font.ITALIC, 12));
			return err;
		}

		ImageIcon original = new ImageIcon(ruta);
		int maxW = 520;
		int ow = original.getIconWidth();
		int oh = original.getIconHeight();
		int w = Math.min(ow, maxW);
		int h = (ow > 0) ? (int) ((double) w / ow * oh) : oh;

		JLabel imgLbl = new JLabel(new ImageIcon(original.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH)));
		imgLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		imgLbl.setToolTipText("Click para ampliar — ESC para cerrar");
		imgLbl.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
		imgLbl.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				mostrarZoom(ruta, entrada.getNombre(), original);
			}
		});

		JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		wrap.setBackground(CARD_BG);
		wrap.add(imgLbl);
		return wrap;
	}

	private Component buildArchivoBody(Entrada entrada) {
		String ruta = GuiasManager.MEDIA_DIR + File.separator + entrada.getContenido();

		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
		p.setBackground(CARD_BG);

		JButton btnAbrir = smallBtn("Abrir archivo");
		btnAbrir.addActionListener(e -> {
			try {
				Desktop.getDesktop().open(new File(ruta));
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(this, "No se pudo abrir:\n" + ex.getMessage());
			}
		});

		JLabel lblFile = new JLabel(entrada.getContenido());
		lblFile.setFont(new Font("SansSerif", Font.PLAIN, 11));
		lblFile.setForeground(TEXT_DIM);

		p.add(btnAbrir);
		p.add(lblFile);
		return p;
	}

	// ── VISOR DE IMAGEN (ZOOM) ────────────────────────────────
	private void mostrarZoom(String ruta, String nombre, ImageIcon original) {
		Window owner = SwingUtilities.getWindowAncestor(this);
		JDialog dlg = new JDialog(owner, nombre, Dialog.ModalityType.APPLICATION_MODAL);
		dlg.getContentPane().setBackground(ZOOM_BG);

		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		int maxW = (int) (screen.width * 0.88);
		int maxH = (int) (screen.height * 0.88);

		int ow = original.getIconWidth();
		int oh = original.getIconHeight();
		double scale = Math.min(1.0, Math.min((double) maxW / ow, (double) maxH / oh));
		int w = (int) (ow * scale);
		int h = (int) (oh * scale);

		// Si la imagen es menor que la pantalla, mostrar nativa; si es mayor, escalar
		ImageIcon display = (scale < 1.0)
				? new ImageIcon(original.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH))
				: original;

		JLabel imgLbl = new JLabel(display);
		imgLbl.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		imgLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		imgLbl.setToolTipText("Click o ESC para cerrar");
		imgLbl.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				dlg.dispose();
			}
		});

		JScrollPane scroll = new JScrollPane(imgLbl);
		scroll.setBorder(null);
		scroll.getViewport().setBackground(ZOOM_BG);
		scroll.setBackground(ZOOM_BG);

		// Cerrar con ESC
		dlg.getRootPane().registerKeyboardAction(e -> dlg.dispose(), KeyStroke.getKeyStroke("ESCAPE"),
				JComponent.WHEN_IN_FOCUSED_WINDOW);

		dlg.add(scroll);
		dlg.setSize(Math.min(w + 32, maxW), Math.min(h + 32, maxH));
		dlg.setLocationRelativeTo(owner);
		dlg.setVisible(true);
	}

	// ── ACCIONES ──────────────────────────────────────────────
	private void nuevaCarpeta() {
		String nombre = JOptionPane.showInputDialog(this, "Nombre de la carpeta:");
		if (nombre != null && !nombre.isBlank()) {
			Carpeta c = new Carpeta(nombre.trim());
			carpetas.add(c);
			listModel.addElement(c);
			listaCarpetas.setSelectedValue(c, true);
			guardar();
		}
	}

	private void agregarTexto() {
		if (carpetaActual == null) {
			noFolder();
			return;
		}

		JTextField titulo = new JTextField("Sin título", 24);
		JTextArea cuerpo = new JTextArea(8, 36);
		cuerpo.setLineWrap(true);
		cuerpo.setWrapStyleWord(true);

		JPanel panel = new JPanel(new BorderLayout(0, 6));
		panel.add(label("Título:"), BorderLayout.NORTH);
		panel.add(titulo, BorderLayout.CENTER);
		JPanel bottom = new JPanel(new BorderLayout(0, 4));
		bottom.add(label("Contenido:"), BorderLayout.NORTH);
		bottom.add(new JScrollPane(cuerpo), BorderLayout.CENTER);
		panel.add(bottom, BorderLayout.SOUTH);

		int ok = JOptionPane.showConfirmDialog(this, panel, "Agregar texto", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);
		if (ok == JOptionPane.OK_OPTION) {
			carpetaActual.getEntradas().add(new Entrada(Entrada.Tipo.TEXTO, titulo.getText().trim(), cuerpo.getText()));
			mostrarCarpeta(carpetaActual);
			guardar();
		}
	}

	private void agregarImagen() {
		if (carpetaActual == null) {
			noFolder();
			return;
		}
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Imágenes", "png", "jpg", "jpeg", "gif",
				"bmp", "webp"));
		fc.setDialogTitle("Seleccionar imagen");
		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			try {
				File src = fc.getSelectedFile();
				String archivo = GuiasManager.copiarMedia(src);
				carpetaActual.getEntradas().add(new Entrada(Entrada.Tipo.IMAGEN, src.getName(), archivo));
				mostrarCarpeta(carpetaActual);
				guardar();
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(this, "Error al copiar imagen:\n" + ex.getMessage());
			}
		}
	}

	private void agregarArchivo() {
		if (carpetaActual == null) {
			noFolder();
			return;
		}
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Seleccionar archivo");
		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			try {
				File src = fc.getSelectedFile();
				String archivo = GuiasManager.copiarMedia(src);
				carpetaActual.getEntradas().add(new Entrada(Entrada.Tipo.ARCHIVO, src.getName(), archivo));
				mostrarCarpeta(carpetaActual);
				guardar();
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(this, "Error al copiar archivo:\n" + ex.getMessage());
			}
		}
	}

	private void eliminarEntrada(Entrada entrada) {
		int c = JOptionPane.showConfirmDialog(this, "¿Eliminar \"" + entrada.getNombre() + "\"?", "Confirmar",
				JOptionPane.YES_NO_OPTION);
		if (c == JOptionPane.YES_OPTION) {
			carpetaActual.getEntradas().remove(entrada);
			mostrarCarpeta(carpetaActual);
			guardar();
		}
	}

	private void showCarpetaMenu(MouseEvent e, Carpeta c) {
		JPopupMenu menu = new JPopupMenu();

		JMenuItem rename = new JMenuItem("Renombrar");
		rename.addActionListener(ev -> {
			String nuevo = JOptionPane.showInputDialog(this, "Nuevo nombre:", c.getNombre());
			if (nuevo != null && !nuevo.isBlank()) {
				c.setNombre(nuevo.trim());
				listaCarpetas.repaint();
				if (carpetaActual == c)
					lblTitulo.setText("📁  " + nuevo.trim());
				guardar();
			}
		});

		JMenuItem del = new JMenuItem("Eliminar carpeta");
		del.setForeground(DEL_RED);
		del.addActionListener(ev -> {
			int confirm = JOptionPane.showConfirmDialog(this,
					"¿Eliminar \"" + c.getNombre() + "\" y todo su contenido?", "Confirmar", JOptionPane.YES_NO_OPTION);
			if (confirm == JOptionPane.YES_OPTION) {
				carpetas.remove(c);
				listModel.removeElement(c);
				if (carpetaActual == c) {
					carpetaActual = null;
					contentPanel.removeAll();
					lblTitulo.setText("Selecciona una carpeta");
					contentPanel.revalidate();
					contentPanel.repaint();
				}
				guardar();
			}
		});

		menu.add(rename);
		menu.addSeparator();
		menu.add(del);
		menu.show(listaCarpetas, e.getX(), e.getY());
	}

	// ── HELPERS ───────────────────────────────────────────────
	private void abrirRealmlist() {
		File f = new File("C:\\Naerzone 3.3.5a\\Data\\esES\\realmlist.wtf");
		if (!f.exists()) {
			JOptionPane.showMessageDialog(this, "No se encontró el archivo:\n" + f.getAbsolutePath(),
					"Archivo no encontrado", JOptionPane.WARNING_MESSAGE);
			return;
		}
		try {
			Desktop.getDesktop().open(f);
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(this, "No se pudo abrir el archivo:\n" + ex.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private JButton smallBtnAccent(String text) {
		JButton btn = new JButton(text);
		btn.setFont(new Font("SansSerif", Font.BOLD, 12));
		btn.setForeground(Color.WHITE);
		btn.setBackground(ACCENT);
		btn.setOpaque(true);
		btn.setBorder(new CompoundBorder(new LineBorder(new Color(45, 95, 190), 1, true),
				BorderFactory.createEmptyBorder(4, 10, 4, 10)));
		btn.setFocusPainted(false);
		btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				btn.setBackground(new Color(45, 95, 190));
			}

			@Override
			public void mouseExited(MouseEvent e) {
				btn.setBackground(ACCENT);
			}
		});
		return btn;
	}

	private void noFolder() {
		JOptionPane.showMessageDialog(this, "Primero selecciona o crea una carpeta.");
	}

	private JButton smallBtn(String text) {
		JButton btn = new JButton(text);
		btn.setFont(new Font("SansSerif", Font.PLAIN, 12));
		btn.setForeground(TEXT_MAIN);
		btn.setBackground(new Color(238, 238, 242));
		btn.setBorder(new CompoundBorder(new LineBorder(BORDER_LIGHT, 1, true),
				BorderFactory.createEmptyBorder(4, 10, 4, 10)));
		btn.setFocusPainted(false);
		btn.setOpaque(true);
		btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		return btn;
	}

	private JLabel label(String text) {
		JLabel l = new JLabel(text);
		l.setForeground(TEXT_MAIN);
		return l;
	}

	// ── CARPETA LIST RENDERER ─────────────────────────────────
	private static class CarpetaRenderer extends DefaultListCellRenderer {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value instanceof Carpeta c) {
				setText("📁  " + c.getNombre());
				setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 8));
				setFont(new Font("SansSerif", Font.PLAIN, 13));
			}
			return this;
		}
	}
}