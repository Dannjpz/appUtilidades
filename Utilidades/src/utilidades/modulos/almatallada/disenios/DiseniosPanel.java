package utilidades.modulos.almatallada.disenios;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import javax.imageio.ImageIO;
import javax.swing.TransferHandler;
import java.awt.datatransfer.*;

public class DiseniosPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// ── Colores ────────────────────────────────────────────────────────────
	static final Color BG = new Color(245, 245, 247);
	static final Color BG_CARD = new Color(255, 255, 255);
	static final Color BG_INPUT = new Color(250, 250, 252);
	static final Color BG_ROW = new Color(252, 252, 255);
	static final Color BG_HOVER = new Color(235, 240, 255);
	static final Color TEXT = new Color(20, 20, 20); // TEXT
	static final Color TEXT_SEC = new Color(130, 130, 140);
	static final Color TEXT_HINT = new Color(180, 180, 195);
	static final Color BORDER = new Color(210, 210, 215);
	static final Color BORDER_LT = new Color(190, 190, 195);
	static final Color VERDE = new Color(100, 200, 120);
	static final Color ROJO = new Color(220, 80, 80);

	// ── Extensiones ────────────────────────────────────────────────────────
	static final Set<String> EXT_IMAGEN = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "bmp", "gif", "webp"));
	static final Set<String> EXT_DISENO = new HashSet<>(Arrays.asList("cdr", "ai", "svg", "eps", "dxf"));
	static final Set<String> EXT_MAQUINA = new HashSet<>(Arrays.asList("lbrn", "lbrn2", "lbm2", "gc", "gcode", "nc"));
	static final Set<String> EXT_OTRAS = new HashSet<>(Arrays.asList("rar", "zip", "7z", "pdf"));
	static final Pattern VERSION_PAT = Pattern.compile("_v\\d+$", Pattern.CASE_INSENSITIVE);

	static final File CONFIG = new File(System.getProperty("user.home"), "almatallada_disenios.properties");
	static final String ETIQUETAS_DEFAULT = "Navidad|Bodas|XV Anos|Letreros|Cajas|Centros de mesa|Lamparas|Animales|Flores|Marcos|Religioso|Grabado|Corte|Otros";

	// ── Estado ─────────────────────────────────────────────────────────────
	File carpetaActual = null;
	Map<String, Grupo> grupos = new LinkedHashMap<>();
	List<Grupo> filtrados = new ArrayList<>();
	Properties meta = new Properties();
	List<String> etiquetas = new ArrayList<>();

	// ── UI ─────────────────────────────────────────────────────────────────
	JTextField tfBuscar;
	JComboBox<String> cbFiltroEtiqueta;
	JLabel lCarpeta, lConteo;
	JPanel panelLista, panelDetalle;
	JScrollPane scrollDetalle;

	public DiseniosPanel() {
		setLayout(new BorderLayout(0, 8));
		setBackground(BG);
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		inicializarEtiquetas();
		add(crearTopBar(), BorderLayout.NORTH);
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, crearLista(), crearPanelDetalle());
		split.setDividerLocation(500);
		split.setBackground(BG);
		split.setBorder(null);
		add(split, BorderLayout.CENTER);
		cargarConfig();
	}

	void inicializarEtiquetas() {
		etiquetas.clear();
		for (String e : ETIQUETAS_DEFAULT.split("\\|"))
			etiquetas.add(e.trim());
	}

	// ── Top bar ────────────────────────────────────────────────────────────
	JPanel crearTopBar() {
		JPanel p = new JPanel(new BorderLayout(0, 8));
		p.setBackground(BG);

		JPanel fCarpeta = new JPanel(new BorderLayout(10, 0));
		fCarpeta.setBackground(BG_CARD);
		fCarpeta.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_LT),
				BorderFactory.createEmptyBorder(8, 12, 8, 12)));

		JLabel lblBtn = crearLabelBtn("Seleccionar carpeta");
		lblBtn.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				seleccionarCarpeta();
			}
		});

		lCarpeta = new JLabel("Ninguna carpeta seleccionada");
		lCarpeta.setFont(new Font("SansSerif", Font.PLAIN, 11));
		lCarpeta.setForeground(TEXT_HINT);
		lCarpeta.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));

		lConteo = new JLabel("");
		lConteo.setFont(new Font("SansSerif", Font.BOLD, 11));
		lConteo.setForeground(TEXT_SEC);

		fCarpeta.add(lblBtn, BorderLayout.WEST);
		fCarpeta.add(lCarpeta, BorderLayout.CENTER);
		fCarpeta.add(lConteo, BorderLayout.EAST);

		JPanel fBuscar = new JPanel(new BorderLayout(10, 0));
		fBuscar.setBackground(BG);

		JPanel pBuscar = new JPanel(new BorderLayout(8, 0));
		pBuscar.setOpaque(false);
		JLabel lB = lbl("Buscar:");
		tfBuscar = inputField("");
		tfBuscar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
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
		pBuscar.add(lB, BorderLayout.WEST);
		pBuscar.add(tfBuscar, BorderLayout.CENTER);

		JPanel pFiltro = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		pFiltro.setOpaque(false);
		cbFiltroEtiqueta = combo();
		actualizarCbFiltro();
		cbFiltroEtiqueta.setPreferredSize(new Dimension(200, 28));
		cbFiltroEtiqueta.addActionListener(e -> filtrar());

		JLabel lblRecargar = crearLabelBtn("Recargar");
		lblRecargar.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				cargarGrupos(true);
			}
		});

		pFiltro.add(lbl("Etiqueta:"));
		pFiltro.add(cbFiltroEtiqueta);
		pFiltro.add(lblRecargar);

		fBuscar.add(pBuscar, BorderLayout.CENTER);
		fBuscar.add(pFiltro, BorderLayout.EAST);

		p.add(fCarpeta, BorderLayout.NORTH);
		p.add(fBuscar, BorderLayout.SOUTH);
		return p;
	}

	// ── Lista ──────────────────────────────────────────────────────────────
	JPanel crearLista() {
		panelLista = new JPanel();
		panelLista.setLayout(new BoxLayout(panelLista, BoxLayout.Y_AXIS));
		panelLista.setBackground(BG);

		JScrollPane sp = new JScrollPane(panelLista);
		sp.setBorder(BorderFactory.createLineBorder(BORDER_LT));
		sp.getViewport().setBackground(BG);
		sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		JPanel c = new JPanel(new BorderLayout());
		c.setBackground(BG);
		c.add(sp, BorderLayout.CENTER);
		return c;
	}

	// ── Panel detalle ──────────────────────────────────────────────────────
	JPanel crearPanelDetalle() {
		panelDetalle = new JPanel();
		panelDetalle.setLayout(new BoxLayout(panelDetalle, BoxLayout.Y_AXIS));
		panelDetalle.setBackground(BG_CARD);
		panelDetalle.setOpaque(true);
		panelDetalle.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
		mostrarHint();

		scrollDetalle = new JScrollPane(panelDetalle);
		scrollDetalle.setBorder(BorderFactory.createLineBorder(BORDER_LT));
		scrollDetalle.setBackground(BG_CARD);
		scrollDetalle.setOpaque(true);
		scrollDetalle.getViewport().setBackground(BG_CARD);
		scrollDetalle.getViewport().setOpaque(true);
		scrollDetalle.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		JPanel wrap = new JPanel(new BorderLayout());
		wrap.setBackground(BG_CARD);
		wrap.setOpaque(true);
		wrap.add(scrollDetalle, BorderLayout.CENTER);
		return wrap;
	}

	void mostrarHint() {
		panelDetalle.removeAll();
		panelDetalle.setBackground(BG_CARD);
		JLabel h = new JLabel("Selecciona un diseno");
		h.setFont(new Font("SansSerif", Font.ITALIC, 11));
		h.setForeground(TEXT_HINT);
		h.setAlignmentX(CENTER_ALIGNMENT);
		panelDetalle.add(Box.createVerticalGlue());
		panelDetalle.add(h);
		panelDetalle.add(Box.createVerticalGlue());
		panelDetalle.revalidate();
		panelDetalle.repaint();
	}

	// ── Cargar grupos ──────────────────────────────────────────────────────
	void cargarGrupos(boolean detectarNuevos) {
		if (carpetaActual == null || !carpetaActual.exists())
			return;

		Set<String> anteriores = new HashSet<>(grupos.keySet());
		grupos.clear();

		File[] archivos = carpetaActual.listFiles();
		if (archivos == null)
			return;
		Arrays.sort(archivos, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));

		for (File f : archivos) {
			if (!f.isFile())
				continue;
			String ex = ext(f.getName());
			if (!esReconocida(ex))
				continue;
			String base = nombreBase(f.getName());
			grupos.computeIfAbsent(base, k -> new Grupo(k, leerEtiquetas(k))).agregar(f, ex);
		}

		if (detectarNuevos) {
			List<String> nuevos = new ArrayList<>();
			for (String n : grupos.keySet())
				if (!anteriores.contains(n))
					nuevos.add(n);
			if (!nuevos.isEmpty())
				dialogNuevos(nuevos);
		}

		filtrar();
	}

	// ── Leer / guardar etiquetas de un grupo ───────────────────────────────
	Set<String> leerEtiquetas(String nombre) {
		String raw = meta.getProperty("etq|" + nombre, "");
		Set<String> result = new LinkedHashSet<>();
		if (!raw.isBlank())
			for (String e : raw.split("\\$"))
				if (!e.isBlank())
					result.add(e.trim());
		return result;
	}

	void escribirEtiquetas(Grupo g) {
		meta.setProperty("etq|" + g.nombre, String.join("$", g.etiquetas));
	}

	// ── Diálogo nuevos diseños ─────────────────────────────────────────────
	void dialogNuevos(List<String> nuevos) {
		JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
				nuevos.size() + " diseno(s) nuevo(s) detectado(s)", true);
		dlg.setLayout(new BorderLayout(10, 10));
		dlg.setSize(540, 420);
		dlg.setLocationRelativeTo(this);

		JLabel lInst = new JLabel("<html><b>" + nuevos.size() + " diseno(s) nuevo(s).</b> "
				+ "Selecciona cuales quieres etiquetar y marca las etiquetas.</html>");
		lInst.setBorder(BorderFactory.createEmptyBorder(10, 14, 0, 14));
		dlg.add(lInst, BorderLayout.NORTH);

		// Lista de nuevos
		DefaultListModel<String> lm = new DefaultListModel<>();
		for (String n : nuevos)
			lm.addElement(n);
		JList<String> lista = new JList<>(lm);
		lista.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		if (!lm.isEmpty())
			lista.setSelectionInterval(0, lm.getSize() - 1);
		lista.setFont(new Font("SansSerif", Font.PLAIN, 11));
		JScrollPane spLista = new JScrollPane(lista);
		spLista.setPreferredSize(new Dimension(0, 130));

		// Checkboxes de etiquetas
		JPanel panelChecks = new JPanel(new WrapLayout(FlowLayout.LEFT, 6, 4));
		panelChecks.setBackground(Color.WHITE);
		List<JCheckBox> checks = new ArrayList<>();
		for (String etq : etiquetas) {
			JCheckBox cb = new JCheckBox(etq);
			cb.setFont(new Font("SansSerif", Font.PLAIN, 11));
			cb.setBackground(Color.WHITE);
			checks.add(cb);
			panelChecks.add(cb);
		}
		JScrollPane spChecks = new JScrollPane(panelChecks);
		spChecks.setPreferredSize(new Dimension(0, 120));
		JLabel lEtqTit = new JLabel("Selecciona etiquetas:");
		lEtqTit.setFont(new Font("SansSerif", Font.BOLD, 11));
		lEtqTit.setBorder(BorderFactory.createEmptyBorder(6, 0, 2, 0));

		// Nueva etiqueta
		JPanel pNueva = new JPanel(new BorderLayout(6, 0));
		pNueva.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
		JTextField tfNueva = new JTextField();
		tfNueva.setFont(new Font("SansSerif", Font.PLAIN, 11));
		JButton btnCrear = new JButton("+ Crear etiqueta");
		btnCrear.setFont(new Font("SansSerif", Font.PLAIN, 11));
		btnCrear.addActionListener(e -> {
			String nueva = tfNueva.getText().trim();
			if (!nueva.isBlank() && !etiquetas.contains(nueva)) {
				etiquetas.add(nueva);
				JCheckBox cb = new JCheckBox(nueva);
				cb.setFont(new Font("SansSerif", Font.PLAIN, 11));
				cb.setBackground(Color.WHITE);
				cb.setSelected(true);
				checks.add(cb);
				panelChecks.add(cb);
				panelChecks.revalidate();
				panelChecks.repaint();
				actualizarCbFiltro();
				guardarConfig();
				tfNueva.setText("");
			}
		});
		pNueva.add(new JLabel("Nueva: "), BorderLayout.WEST);
		pNueva.add(tfNueva, BorderLayout.CENTER);
		pNueva.add(btnCrear, BorderLayout.EAST);

		JPanel centro = new JPanel(new BorderLayout(0, 4));
		centro.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
		centro.add(spLista, BorderLayout.NORTH);
		centro.add(lEtqTit, BorderLayout.CENTER);

		JPanel abajo = new JPanel(new BorderLayout(0, 4));
		abajo.add(spChecks, BorderLayout.CENTER);
		abajo.add(pNueva, BorderLayout.SOUTH);
		centro.add(abajo, BorderLayout.SOUTH);
		dlg.add(centro, BorderLayout.CENTER);

		// Botones
		JButton btnAsignar = new JButton("Asignar etiquetas seleccionadas");
		btnAsignar.setFont(new Font("SansSerif", Font.BOLD, 11));
		btnAsignar.addActionListener(e -> {
			Set<String> sel = new LinkedHashSet<>();
			for (JCheckBox cb : checks)
				if (cb.isSelected())
					sel.add(cb.getText());
			if (sel.isEmpty()) {
				JOptionPane.showMessageDialog(dlg, "Selecciona al menos una etiqueta.");
				return;
			}
			for (String nombre : lista.getSelectedValuesList()) {
				Grupo g = grupos.get(nombre);
				if (g != null) {
					g.etiquetas.addAll(sel);
					escribirEtiquetas(g);
				}
			}
			guardarConfig();
			renderizarLista();
			JOptionPane.showMessageDialog(dlg, "Etiquetas asignadas correctamente.");
		});

		JButton btnListo = new JButton("Cerrar");
		btnListo.setFont(new Font("SansSerif", Font.PLAIN, 11));
		btnListo.addActionListener(e -> dlg.dispose());

		JPanel bots = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
		bots.add(btnAsignar);
		bots.add(btnListo);
		dlg.add(bots, BorderLayout.SOUTH);
		dlg.setVisible(true);
	}

	void actualizarCbFiltro() {
		String actual = cbFiltroEtiqueta != null && cbFiltroEtiqueta.getSelectedItem() != null
				? cbFiltroEtiqueta.getSelectedItem().toString()
				: "Todas";
		if (cbFiltroEtiqueta != null) {
			cbFiltroEtiqueta.removeAllItems();
			cbFiltroEtiqueta.addItem("Todas las etiquetas");
			cbFiltroEtiqueta.addItem("Sin etiqueta");
			for (String e : etiquetas)
				cbFiltroEtiqueta.addItem(e);
			cbFiltroEtiqueta.setSelectedItem(actual);
		}
	}

	// ── Filtrar ────────────────────────────────────────────────────────────
	void filtrar() {
		String buscar = tfBuscar.getText().trim().toLowerCase();
		String filtroEtq = cbFiltroEtiqueta.getSelectedItem() != null ? cbFiltroEtiqueta.getSelectedItem().toString()
				: "Todas las etiquetas";

		filtrados.clear();
		for (Grupo g : grupos.values()) {
			boolean matchNombre = buscar.isEmpty() || g.nombre.toLowerCase().contains(buscar);
			boolean matchEtq;
			if (filtroEtq.equals("Todas las etiquetas"))
				matchEtq = true;
			else if (filtroEtq.equals("Sin etiqueta"))
				matchEtq = g.etiquetas.isEmpty();
			else
				matchEtq = g.etiquetas.contains(filtroEtq);
			if (matchNombre && matchEtq)
				filtrados.add(g);
		}
		renderizarLista();
		int total = grupos.size(), shown = filtrados.size();
		lConteo.setText(shown == total ? total + " disenos" : shown + " de " + total);
	}

	// ── Renderizar lista ───────────────────────────────────────────────────
	void renderizarLista() {
		panelLista.removeAll();
		if (filtrados.isEmpty()) {
			JLabel lv = new JLabel("No hay disenos");
			lv.setFont(new Font("SansSerif", Font.ITALIC, 12));
			lv.setForeground(TEXT_HINT);
			lv.setAlignmentX(CENTER_ALIGNMENT);
			panelLista.add(Box.createVerticalStrut(30));
			panelLista.add(lv);
		}
		for (Grupo g : filtrados) {
			panelLista.add(crearFilaGrupo(g));
			panelLista.add(sep());
		}
		panelLista.add(Box.createVerticalGlue());
		panelLista.revalidate();
		panelLista.repaint();
	}

	JPanel crearFilaGrupo(Grupo g) {
		JPanel fila = new JPanel(new BorderLayout(10, 0));
		fila.setBackground(BG);
		fila.setOpaque(true);
		fila.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
		fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));
		fila.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		// Miniatura
		JLabel lImg = new JLabel();
		lImg.setPreferredSize(new Dimension(56, 56));
		lImg.setMinimumSize(new Dimension(56, 56));
		lImg.setMaximumSize(new Dimension(56, 56));
		lImg.setOpaque(true);
		lImg.setBackground(BG_CARD);
		lImg.setBorder(BorderFactory.createLineBorder(BORDER));
		lImg.setHorizontalAlignment(SwingConstants.CENTER);
		lImg.setVerticalAlignment(SwingConstants.CENTER);

		if (g.archivoImagen != null) {
			try {
				BufferedImage img = ImageIO.read(g.archivoImagen);
				if (img != null) {
					Image sc = img.getScaledInstance(54, 54, Image.SCALE_SMOOTH);
					lImg.setIcon(new ImageIcon(sc));
				} else {
					setLabelPlaceholder(lImg, "IMG");
				}
			} catch (IOException ex) {
				setLabelPlaceholder(lImg, "?");
			}
		} else {
			setLabelPlaceholder(lImg, "—");
		}

		// Info
		JPanel info = new JPanel();
		info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
		info.setOpaque(false);

		JLabel lNom = new JLabel(g.nombre);
		lNom.setFont(new Font("SansSerif", Font.BOLD, 12));
		lNom.setForeground(TEXT);

		// Etiquetas
		JPanel pEtqs = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
		pEtqs.setOpaque(false);
		if (g.etiquetas.isEmpty()) {
			JLabel lSin = new JLabel("sin etiqueta");
			lSin.setFont(new Font("SansSerif", Font.ITALIC, 10));
			lSin.setForeground(TEXT_HINT);
			pEtqs.add(lSin);
		} else {
			for (String etq : g.etiquetas) {
				JLabel lEtq = new JLabel(etq);
				lEtq.setFont(new Font("SansSerif", Font.PLAIN, 10));
				lEtq.setForeground(TEXT);
				lEtq.setOpaque(true);
				lEtq.setBackground(new Color(220, 230, 250));
				lEtq.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_LT),
						BorderFactory.createEmptyBorder(2, 6, 2, 6)));
				pEtqs.add(lEtq);
			}
		}

		// Badges extensiones
		JPanel pExts = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
		pExts.setOpaque(false);
		for (File f : g.archivos)
			pExts.add(badgeExt(ext(f.getName())));

		info.add(lNom);
		info.add(Box.createVerticalStrut(2));
		info.add(pEtqs);
		info.add(Box.createVerticalStrut(2));
		info.add(pExts);

		fila.add(lImg, BorderLayout.WEST);
		fila.add(info, BorderLayout.CENTER);

		MouseAdapter ma = new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				mostrarDetalle(g);
			}

			public void mouseEntered(MouseEvent e) {
				fila.setBackground(BG_HOVER);
			}

			public void mouseExited(MouseEvent e) {
				fila.setBackground(BG);
			}
		};
		fila.addMouseListener(ma);
		lImg.addMouseListener(ma);
		info.addMouseListener(ma);
		return fila;
	}

	void setLabelPlaceholder(JLabel l, String txt) {
		l.setText(txt);
		l.setForeground(TEXT_HINT);
		l.setFont(new Font("SansSerif", Font.PLAIN, 14));
	}

	// ── Panel global de gestión de etiquetas ─────────────────────────────────
	void mostrarGestorEtiquetas() {
		JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Gestionar etiquetas", true);
		dlg.setLayout(new BorderLayout(10, 10));
		dlg.setSize(420, 460);
		dlg.setLocationRelativeTo(this);

		JLabel lInst = new JLabel("<html><b>Arrastra</b> para reordenar. Doble clic para renombrar.</html>");
		lInst.setFont(new Font("SansSerif", Font.PLAIN, 11));
		lInst.setBorder(BorderFactory.createEmptyBorder(10, 14, 4, 14));
		dlg.add(lInst, BorderLayout.NORTH);

		DefaultListModel<String> model = new DefaultListModel<>();
		for (String e : etiquetas)
			model.addElement(e);

		JList<String> lista = new JList<>(model);
		lista.setFont(new Font("SansSerif", Font.PLAIN, 12));
		lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		lista.setDragEnabled(true);
		lista.setDropMode(DropMode.INSERT);
		lista.setTransferHandler(new TransferHandler() {
			int fromIdx = -1;

			public int getSourceActions(JComponent c) {
				return MOVE;
			}

			protected Transferable createTransferable(JComponent c) {
				fromIdx = lista.getSelectedIndex();
				return new StringSelection(lista.getSelectedValue());
			}

			public boolean canImport(TransferSupport ts) {
				return ts.isDrop() && ts.isDataFlavorSupported(DataFlavor.stringFlavor);
			}

			public boolean importData(TransferSupport ts) {
				try {
					JList.DropLocation dl = (JList.DropLocation) ts.getDropLocation();
					int toIdx = dl.getIndex();
					String val = (String) ts.getTransferable().getTransferData(DataFlavor.stringFlavor);
					if (fromIdx < 0)
						return false;
					if (toIdx > fromIdx)
						toIdx--;
					model.remove(fromIdx);
					model.add(toIdx, val);
					lista.setSelectedIndex(toIdx);
					return true;
				} catch (Exception e) {
					return false;
				}
			}
		});

		// Doble clic para renombrar
		lista.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					int idx = lista.locationToIndex(e.getPoint());
					if (idx < 0)
						return;
					String actual = model.get(idx);
					String nuevo = JOptionPane.showInputDialog(dlg, "Nuevo nombre:", actual);
					if (nuevo != null && !nuevo.isBlank() && !nuevo.equals(actual)) {
						// Renombrar en todos los grupos
						for (Grupo g : grupos.values()) {
							if (g.etiquetas.remove(actual)) {
								g.etiquetas.add(nuevo);
								escribirEtiquetas(g);
							}
						}
						model.set(idx, nuevo.trim());
					}
				}
			}
		});

		JScrollPane sp = new JScrollPane(lista);
		sp.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
		dlg.add(sp, BorderLayout.CENTER);

		// Botones
		JPanel bots = new JPanel(new BorderLayout(8, 0));
		bots.setBorder(BorderFactory.createEmptyBorder(4, 14, 10, 14));

		JPanel izq = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		JButton btnEliminar = new JButton("Eliminar");
		btnEliminar.setFont(new Font("SansSerif", Font.PLAIN, 11));
		btnEliminar.addActionListener(e -> {
			int idx = lista.getSelectedIndex();
			if (idx < 0)
				return;
			String etq = model.get(idx);
			int conf = JOptionPane.showConfirmDialog(dlg, "Eliminar etiqueta [" + etq + "] de todos los disenos?",
					"Confirmar", JOptionPane.YES_NO_OPTION);
			if (conf == JOptionPane.YES_OPTION) {
				for (Grupo g : grupos.values()) {
					g.etiquetas.remove(etq);
					escribirEtiquetas(g);
				}
				model.remove(idx);
			}
		});
		izq.add(btnEliminar);
		bots.add(izq, BorderLayout.WEST);

		JButton btnOk = new JButton("Guardar orden");
		btnOk.setFont(new Font("SansSerif", Font.BOLD, 11));
		btnOk.addActionListener(e -> {
			etiquetas.clear();
			for (int i = 0; i < model.getSize(); i++)
				etiquetas.add(model.get(i));
			actualizarCbFiltro();
			guardarConfig();
			renderizarLista();
			dlg.dispose();
		});
		bots.add(btnOk, BorderLayout.EAST);
		dlg.add(bots, BorderLayout.SOUTH);
		dlg.setVisible(true);
	}

	// ── Detalle ────────────────────────────────────────────────────────────
	void mostrarDetalle(Grupo g) {
		panelDetalle.removeAll();
		panelDetalle.setBackground(BG_CARD);
		panelDetalle.setOpaque(true);

		// Nombre
		JLabel lNom = new JLabel(g.nombre);
		lNom.setFont(new Font("SansSerif", Font.BOLD, 14));
		lNom.setForeground(TEXT);
		lNom.setAlignmentX(LEFT_ALIGNMENT);
		panelDetalle.add(lNom);
		panelDetalle.add(Box.createVerticalStrut(12));

		// ── Sección ETIQUETAS ──────────────────────────────────────────────
		panelDetalle.add(secLabel("ETIQUETAS  (marca / desmarca para editar)"));
		panelDetalle.add(Box.createVerticalStrut(8));

		// Panel de checkboxes con TODAS las etiquetas
		JPanel pChecks = new JPanel(new WrapLayout(FlowLayout.LEFT, 6, 4));
		pChecks.setBackground(BG_CARD);
		pChecks.setOpaque(true);
		pChecks.setAlignmentX(LEFT_ALIGNMENT);
		pChecks.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

		List<JCheckBox> checks = new ArrayList<>();
		for (String etq : etiquetas) {
			JCheckBox cb = new JCheckBox(etq);
			cb.setFont(new Font("SansSerif", Font.PLAIN, 11));
			cb.setForeground(TEXT);
			cb.setBackground(BG_CARD);
			cb.setOpaque(true);
			cb.setSelected(g.etiquetas.contains(etq));
			cb.addActionListener(e -> {
				if (cb.isSelected())
					g.etiquetas.add(etq);
				else
					g.etiquetas.remove(etq);
				escribirEtiquetas(g);
				guardarConfig();
				renderizarLista();
			});
			checks.add(cb);
			pChecks.add(cb);
		}
		panelDetalle.add(pChecks);
		panelDetalle.add(Box.createVerticalStrut(8));

		// Scroll para checkboxes si hay muchas etiquetas
		JScrollPane spChecks = new JScrollPane(pChecks);
		spChecks.setBackground(BG_CARD);
		spChecks.setOpaque(true);
		spChecks.getViewport().setBackground(BG_CARD);
		spChecks.getViewport().setOpaque(true);
		spChecks.setBorder(BorderFactory.createLineBorder(BORDER));
		spChecks.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
		spChecks.setPreferredSize(new Dimension(Integer.MAX_VALUE, 140));
		spChecks.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		spChecks.setAlignmentX(LEFT_ALIGNMENT);
		panelDetalle.remove(pChecks);
		panelDetalle.add(spChecks);
		panelDetalle.add(Box.createVerticalStrut(6));

		// Boton gestionar etiquetas
		JLabel lblGestionar = crearLabelBtn("Gestionar etiquetas");
		lblGestionar.setAlignmentX(LEFT_ALIGNMENT);
		lblGestionar.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				mostrarGestorEtiquetas();
				mostrarDetalle(g);
			}
		});
		panelDetalle.add(lblGestionar);
		panelDetalle.add(Box.createVerticalStrut(10));

		// Campo para crear nueva etiqueta
		JPanel pNueva = new JPanel(new BorderLayout(6, 0));
		pNueva.setBackground(BG_CARD);
		pNueva.setOpaque(true);
		pNueva.setAlignmentX(LEFT_ALIGNMENT);
		pNueva.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
		JTextField tfNueva = inputField("");
		tfNueva.setToolTipText("Escribe una nueva etiqueta y presiona Enter");
		JLabel lblCrear = crearLabelBtn("+ Crear");
		Runnable crearEtiqueta = () -> {
			String nueva = tfNueva.getText().trim();
			if (nueva.isBlank())
				return;
			if (!etiquetas.contains(nueva)) {
				etiquetas.add(nueva);
				actualizarCbFiltro();
				guardarConfig();
			}
			if (!g.etiquetas.contains(nueva)) {
				g.etiquetas.add(nueva);
				escribirEtiquetas(g);
				guardarConfig();
				renderizarLista();
			}
			tfNueva.setText("");
			mostrarDetalle(g); // refrescar checkboxes
		};
		tfNueva.addActionListener(e -> crearEtiqueta.run());
		lblCrear.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				crearEtiqueta.run();
			}
		});
		JLabel lNueva = new JLabel("Nueva etiqueta:");
		lNueva.setFont(new Font("SansSerif", Font.PLAIN, 11));
		lNueva.setForeground(TEXT_SEC);
		pNueva.add(lNueva, BorderLayout.WEST);
		pNueva.add(tfNueva, BorderLayout.CENTER);
		pNueva.add(lblCrear, BorderLayout.EAST);
		panelDetalle.add(pNueva);
		panelDetalle.add(Box.createVerticalStrut(16));

		// ── Miniatura grande ───────────────────────────────────────────────
		if (g.archivoImagen != null) {
			try {
				BufferedImage img = ImageIO.read(g.archivoImagen);
				if (img != null) {
					int maxW = 300, maxH = 200;
					double sc = Math.min((double) maxW / img.getWidth(), (double) maxH / img.getHeight());
					Image scaled = img.getScaledInstance((int) (img.getWidth() * sc), (int) (img.getHeight() * sc),
							Image.SCALE_SMOOTH);
					JLabel lImg = new JLabel(new ImageIcon(scaled));
					lImg.setAlignmentX(LEFT_ALIGNMENT);
					lImg.setBorder(BorderFactory.createLineBorder(BORDER_LT));
					panelDetalle.add(lImg);
					panelDetalle.add(Box.createVerticalStrut(14));
				}
			} catch (IOException ignored) {
			}
		}

		// ── Archivos por tipo ──────────────────────────────────────────────
		Map<String, List<File>> porTipo = new LinkedHashMap<>();
		porTipo.put("IMAGEN", new ArrayList<>());
		porTipo.put("DISENO", new ArrayList<>());
		porTipo.put("MAQUINA", new ArrayList<>());
		porTipo.put("OTRO", new ArrayList<>());
		for (File f : g.archivos) {
			String ex = ext(f.getName());
			if (EXT_IMAGEN.contains(ex))
				porTipo.get("IMAGEN").add(f);
			else if (EXT_DISENO.contains(ex))
				porTipo.get("DISENO").add(f);
			else if (EXT_MAQUINA.contains(ex))
				porTipo.get("MAQUINA").add(f);
			else
				porTipo.get("OTRO").add(f);
		}
		for (Map.Entry<String, List<File>> entry : porTipo.entrySet()) {
			if (entry.getValue().isEmpty())
				continue;
			panelDetalle.add(secLabel(entry.getKey()));
			panelDetalle.add(Box.createVerticalStrut(4));
			for (File f : entry.getValue()) {
				panelDetalle.add(filaArchivo(f));
				panelDetalle.add(Box.createVerticalStrut(4));
			}
			panelDetalle.add(Box.createVerticalStrut(8));
		}

		panelDetalle.add(Box.createVerticalGlue());
		panelDetalle.revalidate();
		panelDetalle.repaint();
		scrollDetalle.getVerticalScrollBar().setValue(0);
	}

	// ── Fila de archivo ────────────────────────────────────────────────────
	JPanel filaArchivo(File f) {
		JPanel row = new JPanel(new BorderLayout(8, 0));
		row.setBackground(new Color(250, 250, 252));
		row.setOpaque(true);
		row.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_LT),
				BorderFactory.createEmptyBorder(7, 10, 7, 10)));
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
		row.setAlignmentX(LEFT_ALIGNMENT);
		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		// Icono del programa
		JLabel lIcono = iconoPrograma(ext(f.getName()));

		JLabel lNom = new JLabel(f.getName());
		lNom.setFont(new Font("SansSerif", Font.PLAIN, 11));
		lNom.setForeground(TEXT);

		JPanel izq = new JPanel(new BorderLayout(6, 0));
		izq.setOpaque(false);
		izq.add(lIcono, BorderLayout.WEST);
		izq.add(lNom, BorderLayout.CENTER);

		JLabel lblAbrir = new JLabel("[ Abrir ]");
		lblAbrir.setFont(new Font("SansSerif", Font.BOLD, 11));
		lblAbrir.setForeground(Color.WHITE);
		lblAbrir.setOpaque(true);
		lblAbrir.setBackground(new Color(60, 120, 220));
		lblAbrir.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(45, 95, 190)),
				BorderFactory.createEmptyBorder(4, 10, 4, 10)));
		lblAbrir.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		lblAbrir.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				abrirArchivo(f);
			}

			public void mouseEntered(MouseEvent e) {
				lblAbrir.setBackground(new Color(45, 95, 190));
			}

			public void mouseExited(MouseEvent e) {
				lblAbrir.setBackground(new Color(60, 120, 220));
			}
		});

		row.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2)
					abrirArchivo(f);
			}

			public void mouseEntered(MouseEvent e) {
				row.setBackground(new Color(220, 230, 250));
			}

			public void mouseExited(MouseEvent e) {
				row.setBackground(new Color(250, 250, 252));
			}
		});

		row.add(izq, BorderLayout.CENTER);
		row.add(lblAbrir, BorderLayout.EAST);
		return row;
	}

	// ── Icono por programa ─────────────────────────────────────────────────
	JLabel iconoPrograma(String extension) {
		String texto;
		Color color;
		String tooltip;
		switch (extension.toLowerCase()) {
		case "cdr":
		case "ai":
			texto = "CDR";
			color = new Color(0, 100, 200);
			tooltip = "CorelDraw";
			break;
		case "svg":
		case "dxf":
		case "eps":
			texto = "WEB";
			color = new Color(230, 80, 0);
			tooltip = "Chrome / Edge";
			break;
		case "lbrn":
		case "lbrn2":
		case "lbm2":
			texto = "LB";
			color = new Color(180, 0, 0);
			tooltip = "LightBurn";
			break;
		case "png":
		case "jpg":
		case "jpeg":
		case "bmp":
		case "gif":
			texto = "IMG";
			color = new Color(0, 140, 60);
			tooltip = "Visor de imagenes";
			break;
		case "rar":
		case "zip":
		case "7z":
			texto = "ZIP";
			color = new Color(120, 80, 0);
			tooltip = "WinRAR";
			break;
		case "pdf":
			texto = "PDF";
			color = new Color(180, 0, 0);
			tooltip = "PDF";
			break;
		default:
			texto = extension.toUpperCase();
			color = new Color(80, 80, 80);
			tooltip = extension;
		}
		JLabel l = new JLabel(texto) {
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(color);
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
				g2.setColor(Color.WHITE);
				g2.setFont(getFont());
				FontMetrics fm = g2.getFontMetrics();
				int x = (getWidth() - fm.stringWidth(getText())) / 2;
				int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
				g2.drawString(getText(), x, y);
				g2.dispose();
			}
		};
		l.setFont(new Font("SansSerif", Font.BOLD, 9));
		l.setOpaque(false);
		l.setPreferredSize(new Dimension(32, 18));
		l.setMinimumSize(new Dimension(32, 18));
		l.setMaximumSize(new Dimension(32, 18));
		l.setToolTipText(tooltip);
		return l;
	}

	void abrirArchivo(File f) {
		try {
			Desktop.getDesktop().open(f);
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(this,
					"No se pudo abrir el archivo.\nVerifica que tengas un programa asociado para ." + ext(f.getName())
							+ "\n\n" + ex.getMessage(),
					"Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	// ── Guardar / Cargar ───────────────────────────────────────────────────
	void seleccionarCarpeta() {
		JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (carpetaActual != null)
			fc.setCurrentDirectory(carpetaActual);
		if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
			return;
		carpetaActual = fc.getSelectedFile();
		lCarpeta.setText(carpetaActual.getAbsolutePath());
		lCarpeta.setForeground(TEXT_SEC);
		cargarGrupos(true);
		guardarConfig();
	}

	void guardarConfig() {
		meta.setProperty("carpeta", carpetaActual != null ? carpetaActual.getAbsolutePath() : "");
		meta.setProperty("etiquetas", String.join("|", etiquetas));
		// Guardar etiquetas de todos los grupos cargados
		for (Grupo g : grupos.values())
			escribirEtiquetas(g);
		try (FileOutputStream o = new FileOutputStream(CONFIG)) {
			meta.store(o, "Alma Tallada Disenios");
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(this, "Error guardando configuracion: " + ex.getMessage());
		}
	}

	void cargarConfig() {
		if (!CONFIG.exists())
			return;
		try (FileInputStream in = new FileInputStream(CONFIG)) {
			meta.load(in);
			// Cargar etiquetas globales
			String etqStr = meta.getProperty("etiquetas", "");
			if (!etqStr.isBlank()) {
				etiquetas.clear();
				for (String e : etqStr.split("\\|"))
					if (!e.isBlank())
						etiquetas.add(e.trim());
				actualizarCbFiltro();
			}
			// Cargar carpeta
			String carpetaStr = meta.getProperty("carpeta", "");
			if (!carpetaStr.isBlank()) {
				carpetaActual = new File(carpetaStr);
				lCarpeta.setText(carpetaStr);
				lCarpeta.setForeground(TEXT_SEC);
				if (carpetaActual.exists())
					cargarGrupos(false);
			}
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "Error cargando configuracion: " + ex.getMessage());
		}
	}

	// ── Helpers ────────────────────────────────────────────────────────────
	String ext(String nombre) {
		int dot = nombre.lastIndexOf('.');
		return dot >= 0 ? nombre.substring(dot + 1).toLowerCase() : "";
	}

	String nombreBase(String nombre) {
		int dot = nombre.lastIndexOf('.');
		String sin = dot >= 0 ? nombre.substring(0, dot) : nombre;
		return VERSION_PAT.matcher(sin).replaceAll("").toLowerCase().trim();
	}

	boolean esReconocida(String ext) {
		return EXT_IMAGEN.contains(ext) || EXT_DISENO.contains(ext) || EXT_MAQUINA.contains(ext)
				|| EXT_OTRAS.contains(ext);
	}

	JLabel secLabel(String txt) {
		JLabel l = new JLabel(txt);
		l.setFont(new Font("SansSerif", Font.BOLD, 9));
		l.setForeground(TEXT_HINT);
		l.setAlignmentX(LEFT_ALIGNMENT);
		return l;
	}

	JLabel badgeExt(String ext) {
		JLabel l = new JLabel(ext.toUpperCase());
		l.setFont(new Font("SansSerif", Font.BOLD, 9));
		l.setForeground(TEXT_HINT);
		l.setOpaque(true);
		l.setBackground(new Color(248, 248, 250));
		l.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
				BorderFactory.createEmptyBorder(1, 4, 1, 4)));
		return l;
	}

	JSeparator sep() {
		JSeparator s = new JSeparator();
		s.setForeground(BORDER);
		s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
		return s;
	}

	JTextField inputField(String v) {
		JTextField tf = new JTextField(v);
		tf.setFont(new Font("SansSerif", Font.PLAIN, 12));
		tf.setForeground(TEXT);
		tf.setBackground(BG_INPUT);
		tf.setCaretColor(TEXT);
		tf.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_LT),
				BorderFactory.createEmptyBorder(4, 8, 4, 8)));
		return tf;
	}

	JComboBox<String> combo() {
		JComboBox<String> cb = new JComboBox<>();
		cb.setFont(new Font("SansSerif", Font.PLAIN, 12));
		cb.setBackground(BG_INPUT);
		cb.setForeground(TEXT);
		return cb;
	}

	JLabel lbl(String t) {
		JLabel l = new JLabel(t);
		l.setFont(new Font("SansSerif", Font.PLAIN, 12));
		l.setForeground(TEXT_SEC);
		return l;
	}

	// Label que actúa como botón (más confiable que JButton con Metal L&F)
	JLabel crearLabelBtn(String txt) {
		JLabel l = new JLabel(txt);
		l.setFont(new Font("SansSerif", Font.BOLD, 11));
		l.setForeground(Color.WHITE);
		l.setOpaque(true);
		l.setBackground(new Color(60, 120, 220));
		l.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(45, 95, 190)),
				BorderFactory.createEmptyBorder(6, 14, 6, 14)));
		l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		l.addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent e) {
				l.setBackground(new Color(45, 95, 190));
			}

			public void mouseExited(MouseEvent e) {
				l.setBackground(new Color(60, 120, 220));
			}
		});
		return l;
	}

	// ── Clase Grupo ────────────────────────────────────────────────────────
	static class Grupo {
		String nombre;
		Set<String> etiquetas;
		File archivoImagen = null;
		List<File> archivos = new ArrayList<>();

		Grupo(String nombre, Set<String> etiquetas) {
			this.nombre = nombre;
			this.etiquetas = etiquetas;
		}

		void agregar(File f, String ext) {
			archivos.add(f);
			if (archivoImagen == null && esImagen(ext))
				archivoImagen = f;
		}

		static boolean esImagen(String ext) {
			return ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("bmp")
					|| ext.equals("gif");
		}
	}

	// ── WrapLayout ─────────────────────────────────────────────────────────
	static class WrapLayout extends FlowLayout {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		WrapLayout(int align, int hgap, int vgap) {
			super(align, hgap, vgap);
		}

		public Dimension preferredLayoutSize(Container target) {
			return layoutSize(target, true);
		}

		public Dimension minimumLayoutSize(Container target) {
			Dimension min = layoutSize(target, false);
			min.width -= (getHgap() + 1);
			return min;
		}

		private Dimension layoutSize(Container target, boolean preferred) {
			synchronized (target.getTreeLock()) {
				int width = target.getWidth();
				if (width == 0)
					width = Integer.MAX_VALUE;
				int hgap = getHgap(), vgap = getVgap();
				Insets ins = target.getInsets();
				int maxW = width - ins.left - ins.right;
				int rowW = 0, rowH = 0, totalH = ins.top + ins.bottom;
				boolean started = false;
				for (int i = 0; i < target.getComponentCount(); i++) {
					Component c = target.getComponent(i);
					if (!c.isVisible())
						continue;
					Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
					if (started && rowW + hgap + d.width > maxW) {
						totalH += rowH + vgap;
						rowW = 0;
						rowH = 0;
						started = false;
					}
					rowW += (started ? hgap : 0) + d.width;
					rowH = Math.max(rowH, d.height);
					started = true;
				}
				totalH += rowH;
				return new Dimension(maxW, totalH);
			}
		}
	}
}