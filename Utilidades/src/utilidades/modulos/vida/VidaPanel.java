package utilidades.modulos.vida;

import javax.swing.*;
import javax.swing.border.*;

import utilidades.modulos.vida.model.Area;
import utilidades.modulos.vida.model.Habito;
import utilidades.modulos.vida.model.Mision;
import utilidades.modulos.vida.model.Personaje;
import utilidades.modulos.vida.util.VidaManager;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class VidaPanel extends JPanel {

	// ── Paleta RPG oscura ─────────────────────────────────────
	static final Color BG_DARK = new Color(13, 15, 30);
	static final Color BG_CARD = new Color(20, 23, 45);
	static final Color BG_CARD2 = new Color(28, 32, 58);
	static final Color BG_HOVER = new Color(35, 40, 72);
	static final Color GOLD = new Color(255, 190, 50);
	static final Color GOLD_DIM = new Color(140, 100, 15);
	static final Color TEXT_MAIN = new Color(225, 218, 200);
	static final Color TEXT_DIM = new Color(130, 122, 105);
	static final Color HP_RED = new Color(220, 55, 55);
	static final Color XP_BLUE = new Color(80, 140, 255);
	static final Color BORDER_D = new Color(45, 50, 85);
	static final Color GREEN_OK = new Color(60, 200, 100);

	// ── Datos ─────────────────────────────────────────────────
	private Personaje personaje;
	private List<Area> areas;
	private List<Mision> misiones;
	private List<Habito> habitos;

	// ── UI ────────────────────────────────────────────────────
	private JPanel sidebarPanel;
	private JTabbedPane tabs;

	@SuppressWarnings("unchecked")
	public VidaPanel() {
		setLayout(new BorderLayout());
		setBackground(BG_DARK);

		Object[] data = VidaManager.cargar();
		personaje = (Personaje) data[0];
		areas = (List<Area>) data[1];
		misiones = (List<Mision>) data[2];
		habitos = (List<Habito>) data[3];

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildSidebar(), buildMain());
		split.setDividerLocation(230);
		split.setDividerSize(1);
		split.setBorder(null);
		split.setBackground(BG_DARK);

		add(split, BorderLayout.CENTER);
	}

	// ── SIDEBAR ───────────────────────────────────────────────
	private JPanel buildSidebar() {
		sidebarPanel = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				g.setColor(BG_CARD);
				g.fillRect(0, 0, getWidth(), getHeight());
			}
		};
		sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
		sidebarPanel.setOpaque(false);
		sidebarPanel.setBorder(new MatteBorder(0, 0, 0, 1, BORDER_D));
		sidebarPanel.setPreferredSize(new Dimension(230, 0));

		actualizarSidebar();

		JScrollPane scroll = new JScrollPane(sidebarPanel);
		scroll.setBorder(null);
		scroll.getViewport().setBackground(BG_CARD);
		scroll.getVerticalScrollBar().setUnitIncrement(12);

		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(BG_CARD);
		wrapper.add(scroll, BorderLayout.CENTER);
		return wrapper;
	}

	private void actualizarSidebar() {
		sidebarPanel.removeAll();
		sidebarPanel.add(Box.createVerticalStrut(18));

		// Nombre + Rango
		JLabel lblNombre = new JLabel(personaje.getNombre());
		lblNombre.setFont(new Font("SansSerif", Font.BOLD, 18));
		lblNombre.setForeground(GOLD);
		lblNombre.setAlignmentX(Component.CENTER_ALIGNMENT);

		int nivelGlobal = nivelGlobal();
		JPanel rangoBadge = rangoBadge(nivelGlobal);
		rangoBadge.setAlignmentX(Component.CENTER_ALIGNMENT);

		JLabel lblNivel = new JLabel("Nivel  " + nivelGlobal);
		lblNivel.setFont(new Font("SansSerif", Font.PLAIN, 13));
		lblNivel.setForeground(TEXT_DIM);
		lblNivel.setAlignmentX(Component.CENTER_ALIGNMENT);

		sidebarPanel.add(lblNombre);
		sidebarPanel.add(Box.createVerticalStrut(6));
		sidebarPanel.add(rangoBadge);
		sidebarPanel.add(Box.createVerticalStrut(4));
		sidebarPanel.add(lblNivel);
		sidebarPanel.add(Box.createVerticalStrut(14));

		// HP bar
		sidebarPanel
				.add(barraLabel("❤ HP", HP_RED, personaje.getHpPct(), personaje.getHp() + "/" + personaje.getHpMax()));
		sidebarPanel.add(Box.createVerticalStrut(8));

		// Separador
		sidebarPanel.add(separador());
		sidebarPanel.add(Box.createVerticalStrut(10));

		// Áreas
		JLabel lblAreas = label("ESTADÍSTICAS", 10, TEXT_DIM);
		lblAreas.setAlignmentX(Component.CENTER_ALIGNMENT);
		sidebarPanel.add(lblAreas);
		sidebarPanel.add(Box.createVerticalStrut(8));

		for (Area area : areas) {
			sidebarPanel.add(areaCard(area));
			sidebarPanel.add(Box.createVerticalStrut(4));
		}

		sidebarPanel.add(Box.createVerticalStrut(14));
		sidebarPanel.add(separador());
		sidebarPanel.add(Box.createVerticalStrut(10));

		// Botón editar nombre
		JButton btnEditar = rpgBtn("✏ Cambiar nombre");
		btnEditar.setAlignmentX(Component.CENTER_ALIGNMENT);
		btnEditar.setMaximumSize(new Dimension(190, 30));
		btnEditar.addActionListener(e -> {
			String nuevo = JOptionPane.showInputDialog(this, "Tu nombre de héroe:", personaje.getNombre());
			if (nuevo != null && !nuevo.isBlank()) {
				personaje.setNombre(nuevo.trim());
				guardar();
				actualizarSidebar();
				sidebarPanel.revalidate();
				sidebarPanel.repaint();
			}
		});
		sidebarPanel.add(btnEditar);
		sidebarPanel.add(Box.createVerticalStrut(16));
		sidebarPanel.revalidate();
		sidebarPanel.repaint();
	}

	private JPanel areaCard(Area area) {
		JPanel card = new JPanel(new BorderLayout(6, 2));
		card.setBackground(BG_CARD);
		card.setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 14));
		card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
		card.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel emojiNivel = new JLabel(area.getEmoji() + "  " + area.getNombre());
		emojiNivel.setFont(new Font("SansSerif", Font.PLAIN, 12));
		emojiNivel.setForeground(TEXT_MAIN);

		JLabel lvl = new JLabel("Lv." + area.getNivel());
		lvl.setFont(new Font("SansSerif", Font.BOLD, 11));
		lvl.setForeground(area.getColor());

		JPanel top = new JPanel(new BorderLayout());
		top.setBackground(BG_CARD);
		top.add(emojiNivel, BorderLayout.WEST);
		top.add(lvl, BorderLayout.EAST);

		final float pct = area.getPorcentaje();
		final Color col = area.getColor();
		JPanel barra = new JPanel(null) {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				int w = getWidth(), h = getHeight();
				g2.setColor(new Color(40, 44, 75));
				g2.fillRoundRect(0, 0, w, h, h, h);
				g2.setColor(col);
				g2.fillRoundRect(0, 0, (int) (w * pct), h, h, h);
				g2.dispose();
			}
		};
		barra.setPreferredSize(new Dimension(0, 5));
		barra.setOpaque(false);

		card.add(top, BorderLayout.NORTH);
		card.add(barra, BorderLayout.CENTER);
		return card;
	}

	private JPanel barraLabel(String etiqueta, Color color, float pct, String texto) {
		JPanel p = new JPanel(new BorderLayout(0, 3));
		p.setBackground(BG_CARD);
		p.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
		p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
		p.setAlignmentX(Component.LEFT_ALIGNMENT);

		JPanel top = new JPanel(new BorderLayout());
		top.setBackground(BG_CARD);
		JLabel lbl = label(etiqueta, 11, TEXT_DIM);
		JLabel val = label(texto, 11, color);
		top.add(lbl, BorderLayout.WEST);
		top.add(val, BorderLayout.EAST);

		final float f = pct;
		final Color c = color;
		JPanel barra = new JPanel(null) {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				int w = getWidth(), h = getHeight();
				g2.setColor(new Color(40, 44, 75));
				g2.fillRoundRect(0, 0, w, h, h, h);
				g2.setColor(c);
				g2.fillRoundRect(0, 0, (int) (w * Math.min(1f, f)), h, h, h);
				g2.dispose();
			}
		};
		barra.setPreferredSize(new Dimension(0, 8));
		barra.setOpaque(false);

		p.add(top, BorderLayout.NORTH);
		p.add(barra, BorderLayout.CENTER);
		return p;
	}

	private JPanel rangoBadge(int nivel) {
		String rango = getRango(nivel);
		Color color = getColorRango(nivel);
		JPanel badge = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 40));
				g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
				g2.setColor(color);
				g2.setStroke(new BasicStroke(1.2f));
				g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
				g2.dispose();
			}
		};
		badge.setOpaque(false);
		badge.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 3));
		JLabel lbl = new JLabel(rango);
		lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
		lbl.setForeground(color);
		badge.add(lbl);
		badge.setPreferredSize(new Dimension(160, 24));
		return badge;
	}

	// ── MAIN PANEL ────────────────────────────────────────────
	private JPanel buildMain() {
		JPanel main = new JPanel(new BorderLayout());
		main.setBackground(BG_DARK);

		tabs = new JTabbedPane();
		tabs.setBackground(BG_CARD);
		tabs.setForeground(TEXT_MAIN);
		tabs.setFont(new Font("SansSerif", Font.BOLD, 12));

		// Tab activo → texto negro, inactivo → TEXT_MAIN
		tabs.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
			@Override
			protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics, int tabIndex,
					String title, Rectangle textRect, boolean isSelected) {
				g.setFont(font);
				g.setColor(isSelected ? Color.BLACK : TEXT_MAIN);
				g.drawString(title, textRect.x, textRect.y + metrics.getAscent());
			}
		});

		tabs.addTab("⚔  Misiones", buildMisionesTab());
		tabs.addTab("📅  Hábitos", buildHabitosTab());
		tabs.addTab("🏆  Logros", buildLogrosTab());

		main.add(tabs, BorderLayout.CENTER);
		return main;
	}

	// ── TAB MISIONES ──────────────────────────────────────────
	private JPanel buildMisionesTab() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(BG_DARK);

		// Top bar
		JPanel topBar = new JPanel(new BorderLayout());
		topBar.setBackground(BG_CARD);
		topBar.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, BORDER_D),
				BorderFactory.createEmptyBorder(10, 14, 10, 14)));

		JLabel titulo = label("Misiones Activas", 15, GOLD);
		titulo.setFont(new Font("SansSerif", Font.BOLD, 15));

		JButton btnNueva = rpgBtnAccent("+ Nueva misión");
		btnNueva.addActionListener(e -> dlgNuevaMision());

		topBar.add(titulo, BorderLayout.WEST);
		topBar.add(btnNueva, BorderLayout.EAST);

		// Lista de misiones
		JPanel lista = buildListaMisiones(false);
		JScrollPane scroll = new JScrollPane(lista);
		scroll.setBorder(null);
		scroll.getViewport().setBackground(BG_DARK);
		scroll.getVerticalScrollBar().setUnitIncrement(16);

		panel.add(topBar, BorderLayout.NORTH);
		panel.add(scroll, BorderLayout.CENTER);
		return panel;
	}

	private JPanel buildListaMisiones(boolean soloCompletadas) {
		JPanel lista = new JPanel();
		lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
		lista.setBackground(BG_DARK);
		lista.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

		List<Mision> filtradas = misiones.stream().filter(m -> m.isCompletada() == soloCompletadas)
				.collect(Collectors.toList());

		if (filtradas.isEmpty()) {
			JLabel vacio = label(soloCompletadas ? "Aún no has completado ninguna misión. ¡A por ello!"
					: "No hay misiones activas. ¡Crea tu primera misión!", 13, TEXT_DIM);
			vacio.setFont(new Font("SansSerif", Font.ITALIC, 13));
			vacio.setAlignmentX(Component.LEFT_ALIGNMENT);
			lista.add(vacio);
		} else {
			for (Mision m : filtradas) {
				lista.add(buildMisionCard(m));
				lista.add(Box.createVerticalStrut(10));
			}
		}
		return lista;
	}

	private JPanel buildMisionCard(Mision m) {
		JPanel card = new JPanel(new BorderLayout(0, 8));
		card.setBackground(BG_CARD);
		card.setBorder(new CompoundBorder(
				new LineBorder(m.getTipo() == Mision.Tipo.EPICA ? new Color(140, 80, 200, 120) : BORDER_D, 1, true),
				BorderFactory.createEmptyBorder(12, 14, 12, 14)));
		card.setAlignmentX(Component.LEFT_ALIGNMENT);
		card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

		// Header: nombre + tipo badge
		JPanel header = new JPanel(new BorderLayout(8, 0));
		header.setBackground(BG_CARD);

		String tipoTxt = m.getTipo() == Mision.Tipo.EPICA ? "⚡ ÉPICA" : "📋 NORMAL";
		Color tipoCol = m.getTipo() == Mision.Tipo.EPICA ? new Color(180, 100, 255) : new Color(100, 160, 255);

		JLabel lblNombre = label(m.getNombre(), 14, TEXT_MAIN);
		lblNombre.setFont(new Font("SansSerif", Font.BOLD, 14));

		JLabel lblTipo = label(tipoTxt, 10, tipoCol);
		lblTipo.setFont(new Font("SansSerif", Font.BOLD, 10));
		lblTipo.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

		header.add(lblNombre, BorderLayout.WEST);
		header.add(lblTipo, BorderLayout.EAST);

		// Info: área + dificultad + XP
		JPanel info = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		info.setBackground(BG_CARD);

		Area area = findArea(m.getAreaId());
		if (area != null) {
			JLabel lblArea = label(area.getEmoji() + " " + area.getNombre(), 11, area.getColor());
			info.add(lblArea);
		}
		info.add(label(estrellas(m.getDificultad()), 11, GOLD));
		info.add(label("+" + m.getXpGanado() + " XP", 11, XP_BLUE));

		// Progreso (modo tiempo)
		JPanel progreso = new JPanel(new BorderLayout(0, 4));
		progreso.setBackground(BG_CARD);

		if (m.isModoTiempo()) {
			JLabel lblHoras = label(
					String.format("%.1fh / %.1fh registradas", m.getHorasRegistradas(), m.getHorasObjetivo()), 11,
					TEXT_DIM);
			final float pct = m.getPorcentajeTiempo();
			JPanel barra = new JPanel(null) {
				@Override
				protected void paintComponent(Graphics g) {
					Graphics2D g2 = (Graphics2D) g.create();
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g2.setColor(new Color(40, 44, 75));
					g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
					g2.setColor(XP_BLUE);
					g2.fillRoundRect(0, 0, (int) (getWidth() * pct), getHeight(), 6, 6);
					g2.dispose();
				}
			};
			barra.setPreferredSize(new Dimension(0, 7));
			barra.setOpaque(false);
			progreso.add(lblHoras, BorderLayout.NORTH);
			progreso.add(barra, BorderLayout.CENTER);
		}

		// Recompensa
		JPanel recomp = new JPanel(new BorderLayout());
		recomp.setBackground(BG_CARD);
		if (!m.getRecompensa().isBlank()) {
			JLabel lblR = label("🎁  " + m.getRecompensa(), 12, new Color(255, 170, 80));
			lblR.setFont(new Font("SansSerif", Font.ITALIC, 12));
			recomp.add(lblR, BorderLayout.WEST);
		}

		// Botones
		JPanel bots = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
		bots.setBackground(BG_CARD);

		if (m.isModoTiempo()) {
			JButton btnTiempo = rpgBtn("▶ Registrar tiempo");
			btnTiempo.addActionListener(e -> dlgRegistrarTiempo(m));
			bots.add(btnTiempo);
		}

		JButton btnCompletar = rpgBtnAccent("✔ Completar");
		btnCompletar.addActionListener(e -> completarMision(m));

		JButton btnEliminar = rpgBtnDanger("✕");
		btnEliminar.addActionListener(e -> {
			if (JOptionPane.showConfirmDialog(this, "¿Eliminar \"" + m.getNombre() + "\"?", "Confirmar",
					JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				misiones.remove(m);
				guardar();
				refrescarTabs();
			}
		});

		bots.add(btnCompletar);
		bots.add(btnEliminar);

		card.add(header, BorderLayout.NORTH);
		JPanel mid = new JPanel(new BorderLayout(0, 6));
		mid.setBackground(BG_CARD);
		mid.add(info, BorderLayout.NORTH);
		if (m.isModoTiempo())
			mid.add(progreso, BorderLayout.CENTER);
		mid.add(recomp, BorderLayout.SOUTH);
		card.add(mid, BorderLayout.CENTER);
		card.add(bots, BorderLayout.SOUTH);

		card.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				card.setBackground(BG_HOVER);
				mid.setBackground(BG_HOVER);
				header.setBackground(BG_HOVER);
				info.setBackground(BG_HOVER);
				recomp.setBackground(BG_HOVER);
				bots.setBackground(BG_HOVER);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				card.setBackground(BG_CARD);
				mid.setBackground(BG_CARD);
				header.setBackground(BG_CARD);
				info.setBackground(BG_CARD);
				recomp.setBackground(BG_CARD);
				bots.setBackground(BG_CARD);
			}
		});

		return card;
	}

	// ── TAB HÁBITOS ───────────────────────────────────────────
	private JPanel buildHabitosTab() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(BG_DARK);

		JPanel topBar = new JPanel(new BorderLayout());
		topBar.setBackground(BG_CARD);
		topBar.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, BORDER_D),
				BorderFactory.createEmptyBorder(10, 14, 10, 14)));

		JLabel titulo = label("Hábitos Diarios", 15, GOLD);
		titulo.setFont(new Font("SansSerif", Font.BOLD, 15));

		JButton btnNuevo = rpgBtnAccent("+ Nuevo hábito");
		btnNuevo.addActionListener(e -> dlgNuevoHabito());

		topBar.add(titulo, BorderLayout.WEST);
		topBar.add(btnNuevo, BorderLayout.EAST);

		JPanel lista = new JPanel();
		lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
		lista.setBackground(BG_DARK);
		lista.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

		if (habitos.isEmpty()) {
			lista.add(label("No hay hábitos. Los hábitos diarios dan XP cada día y construyen streaks.", 13, TEXT_DIM));
		} else {
			for (Habito h : habitos) {
				lista.add(buildHabitoCard(h));
				lista.add(Box.createVerticalStrut(8));
			}
		}

		JScrollPane scroll = new JScrollPane(lista);
		scroll.setBorder(null);
		scroll.getViewport().setBackground(BG_DARK);

		panel.add(topBar, BorderLayout.NORTH);
		panel.add(scroll, BorderLayout.CENTER);
		return panel;
	}

	private JPanel buildHabitoCard(Habito h) {
		boolean hoy = h.isCompletadoHoy();
		JPanel card = new JPanel(new BorderLayout(8, 0));
		card.setBackground(hoy ? new Color(20, 40, 28) : BG_CARD);
		card.setBorder(new CompoundBorder(new LineBorder(hoy ? new Color(60, 180, 90, 100) : BORDER_D, 1, true),
				BorderFactory.createEmptyBorder(10, 14, 10, 14)));
		card.setAlignmentX(Component.LEFT_ALIGNMENT);
		card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));

		JPanel left = new JPanel(new BorderLayout(0, 3));
		left.setBackground(card.getBackground());

		JLabel lblNombre = label(h.getNombre(), 13, hoy ? GREEN_OK : TEXT_MAIN);
		lblNombre.setFont(new Font("SansSerif", Font.BOLD, 13));

		Area area = findArea(h.getAreaId());
		String areaStr = area != null ? area.getEmoji() + " " + area.getNombre() : "";
		String streakStr = h.getStreakActual() > 0 ? h.getStreakEmoji() + " " + h.getStreakActual() + " días"
				: "Sin racha";
		JLabel sub = label(areaStr + "   " + streakStr + "   +" + h.getXpConBonus() + " XP", 11, TEXT_DIM);

		left.add(lblNombre, BorderLayout.NORTH);
		left.add(sub, BorderLayout.SOUTH);

		JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
		right.setBackground(card.getBackground());

		if (!hoy) {
			JButton btnHoy = rpgBtnAccent("✓ Completar hoy");
			btnHoy.addActionListener(e -> completarHabito(h));
			right.add(btnHoy);
		} else {
			right.add(label("✓ ¡Completado!", 12, GREEN_OK));
		}

		JButton btnDel = rpgBtnDanger("✕");
		btnDel.addActionListener(e -> {
			if (JOptionPane.showConfirmDialog(this, "¿Eliminar hábito \"" + h.getNombre() + "\"?", "Confirmar",
					JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				habitos.remove(h);
				guardar();
				refrescarTabs();
			}
		});
		right.add(btnDel);

		card.add(left, BorderLayout.CENTER);
		card.add(right, BorderLayout.EAST);
		return card;
	}

	// ── TAB LOGROS ────────────────────────────────────────────
	private JPanel buildLogrosTab() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(BG_DARK);

		JPanel topBar = new JPanel(new BorderLayout());
		topBar.setBackground(BG_CARD);
		topBar.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, BORDER_D),
				BorderFactory.createEmptyBorder(10, 14, 10, 14)));
		topBar.add(label("🏆  Misiones Completadas & Recompensas", 15, GOLD), BorderLayout.WEST);

		JPanel lista = new JPanel();
		lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
		lista.setBackground(BG_DARK);
		lista.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

		List<Mision> completadas = misiones.stream().filter(Mision::isCompletada)
				.sorted((a, b) -> b.getFechaCompletada().compareTo(a.getFechaCompletada()))
				.collect(Collectors.toList());

		if (completadas.isEmpty()) {
			lista.add(label("Completa misiones para verlas aquí. ¡Tu historia comienza ahora!", 13, TEXT_DIM));
		} else {
			for (Mision m : completadas) {
				lista.add(buildLogroCard(m));
				lista.add(Box.createVerticalStrut(8));
			}
		}

		JScrollPane scroll = new JScrollPane(lista);
		scroll.setBorder(null);
		scroll.getViewport().setBackground(BG_DARK);

		panel.add(topBar, BorderLayout.NORTH);
		panel.add(scroll, BorderLayout.CENTER);
		return panel;
	}

	private JPanel buildLogroCard(Mision m) {
		JPanel card = new JPanel(new BorderLayout(10, 0));
		card.setBackground(new Color(20, 28, 20));
		card.setBorder(new CompoundBorder(new LineBorder(new Color(60, 160, 80, 100), 1, true),
				BorderFactory.createEmptyBorder(10, 14, 10, 14)));
		card.setAlignmentX(Component.LEFT_ALIGNMENT);
		card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

		JLabel icono = label("🏅", 24, TEXT_MAIN);
		icono.setFont(new Font("SansSerif", Font.PLAIN, 24));

		JPanel content = new JPanel(new GridLayout(3, 1, 0, 2));
		content.setBackground(card.getBackground());

		Area area = findArea(m.getAreaId());
		JLabel nombre = label(m.getNombre(), 13, GREEN_OK);
		nombre.setFont(new Font("SansSerif", Font.BOLD, 13));
		JLabel sub = label((area != null ? area.getEmoji() + " " + area.getNombre() + "  " : "") + "+" + m.getXpGanado()
				+ " XP  •  " + m.getFechaCompletada(), 11, TEXT_DIM);

		JLabel recomp = m.getRecompensa().isBlank() ? label("", 11, TEXT_DIM)
				: label("🎁  " + m.getRecompensa(), 12, new Color(255, 170, 80));

		content.add(nombre);
		content.add(sub);
		content.add(recomp);

		card.add(icono, BorderLayout.WEST);
		card.add(content, BorderLayout.CENTER);
		return card;
	}

	// ── COMPLETAR MISIÓN ──────────────────────────────────────
	private void completarMision(Mision m) {
		int xp = m.getXpGanado();
		Area area = findArea(m.getAreaId());
		int nivelAntes = area != null ? area.getNivel() : 0;

		if (area != null)
			area.agregarXp(xp);
		personaje.curar(10);
		m.setCompletada(true);
		m.setFechaCompletada(LocalDate.now().toString());

		int nivelDespues = area != null ? area.getNivel() : 0;
		boolean subioNivel = nivelDespues > nivelAntes;

		guardar();
		refrescarTodo();
		mostrarCelebracion(m, xp, area, subioNivel ? nivelDespues : -1);
	}

	private void completarHabito(Habito h) {
		int xp = h.completarHoy();
		Area area = findArea(h.getAreaId());
		if (area != null)
			area.agregarXp(xp);
		personaje.curar(5);
		guardar();
		refrescarTodo();
	}

	// ── CELEBRACIÓN ───────────────────────────────────────────
	private void mostrarCelebracion(Mision m, int xp, Area area, int nuevoNivel) {
		JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this) instanceof Frame f ? f : null,
				"¡Misión Completada!", true);
		dlg.setSize(420, nuevoNivel > 0 ? 360 : 300);
		dlg.setLocationRelativeTo(this);
		dlg.setResizable(false);

		JPanel panel = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				g.setColor(BG_CARD);
				g.fillRect(0, 0, getWidth(), getHeight());
			}
		};
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(24, 30, 24, 30));

		JLabel icono = label("✨", 36, GOLD);
		icono.setFont(new Font("SansSerif", Font.PLAIN, 36));
		icono.setAlignmentX(Component.CENTER_ALIGNMENT);

		JLabel titulo = label("¡MISIÓN COMPLETADA!", 17, GOLD);
		titulo.setFont(new Font("SansSerif", Font.BOLD, 17));
		titulo.setAlignmentX(Component.CENTER_ALIGNMENT);

		JLabel nombre = label(m.getNombre(), 14, TEXT_MAIN);
		nombre.setAlignmentX(Component.CENTER_ALIGNMENT);

		JLabel xpLabel = label("+" + xp + " XP", 22, XP_BLUE);
		xpLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
		xpLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		if (area != null) {
			xpLabel.setText("+" + xp + " XP  →  " + area.getEmoji() + " Lv." + area.getNivel());
		}

		panel.add(icono);
		panel.add(Box.createVerticalStrut(8));
		panel.add(titulo);
		panel.add(Box.createVerticalStrut(12));
		panel.add(nombre);
		panel.add(Box.createVerticalStrut(16));
		panel.add(xpLabel);

		if (nuevoNivel > 0) {
			panel.add(Box.createVerticalStrut(10));
			JLabel lvlUp = label("🎉  ¡SUBISTE AL NIVEL " + nuevoNivel + "!", 14, new Color(180, 100, 255));
			lvlUp.setFont(new Font("SansSerif", Font.BOLD, 14));
			lvlUp.setAlignmentX(Component.CENTER_ALIGNMENT);
			panel.add(lvlUp);
		}

		if (!m.getRecompensa().isBlank()) {
			panel.add(Box.createVerticalStrut(14));
			JSeparator sep = new JSeparator();
			sep.setForeground(BORDER_D);
			sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
			panel.add(sep);
			panel.add(Box.createVerticalStrut(10));
			JLabel recomp1 = label("🎁  RECOMPENSA DESBLOQUEADA", 11, TEXT_DIM);
			recomp1.setAlignmentX(Component.CENTER_ALIGNMENT);
			JLabel recomp2 = label(m.getRecompensa(), 15, new Color(255, 170, 80));
			recomp2.setFont(new Font("SansSerif", Font.BOLD, 15));
			recomp2.setAlignmentX(Component.CENTER_ALIGNMENT);
			panel.add(recomp1);
			panel.add(Box.createVerticalStrut(4));
			panel.add(recomp2);
		}

		panel.add(Box.createVerticalStrut(20));
		JButton btnOk = rpgBtnAccent("  ¡A seguir adelante!  ");
		btnOk.setAlignmentX(Component.CENTER_ALIGNMENT);
		btnOk.addActionListener(e -> dlg.dispose());
		panel.add(btnOk);

		dlg.add(panel);
		dlg.setVisible(true);
	}

	// ── DIÁLOGOS ─────────────────────────────────────────────
	private void dlgNuevaMision() {
		JTextField tfNombre = dlgField("");
		JTextField tfDesc = dlgField("");
		JTextField tfRecomp = dlgField("");
		JComboBox<String> cbArea = areaCombo();
		JComboBox<Integer> cbDif = new JComboBox<>(new Integer[] { 1, 2, 3, 4, 5 });
		JComboBox<String> cbTipo = new JComboBox<>(new String[] { "Normal", "Épica" });
		JCheckBox chkTiempo = new JCheckBox("Por tiempo (XP por hora)");
		chkTiempo.setForeground(TEXT_MAIN);
		chkTiempo.setBackground(BG_CARD);
		JTextField tfXp = dlgField("200");
		JTextField tfXpHora = dlgField("30");
		JTextField tfHorasObj = dlgField("10");

		JPanel form = dlgPanel(
				new String[] { "Nombre:", "Descripción:", "Área:", "Dificultad (1-5):", "Tipo:", "XP recompensa:", "",
						"XP por hora:", "Horas objetivo:", "🎁 Recompensa:" },
				new JComponent[] { tfNombre, tfDesc, cbArea, cbDif, cbTipo, tfXp, chkTiempo, tfXpHora, tfHorasObj,
						tfRecomp });

		int ok = JOptionPane.showConfirmDialog(this, form, "Nueva Misión", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);
		if (ok != JOptionPane.OK_OPTION || tfNombre.getText().isBlank())
			return;

		String areaId = areas.isEmpty() ? "" : areas.get(cbArea.getSelectedIndex()).getId();
		Mision.Tipo tipo = cbTipo.getSelectedIndex() == 1 ? Mision.Tipo.EPICA : Mision.Tipo.NORMAL;
		int xp = parseIntSafe(tfXp.getText(), 200);

		Mision m = new Mision(tfNombre.getText().trim(), areaId, (int) cbDif.getSelectedItem(), tipo, xp,
				tfRecomp.getText().trim());
		m.setDescripcion(tfDesc.getText().trim());
		if (chkTiempo.isSelected()) {
			m.setModoTiempo(true);
			m.setXpPorHora(parseIntSafe(tfXpHora.getText(), 30));
			m.setHorasObjetivo(parseDoubleSafe(tfHorasObj.getText(), 10));
		}
		misiones.add(m);
		guardar();
		refrescarTabs();
	}

	private void dlgNuevoHabito() {
		JTextField tfNombre = dlgField("");
		JComboBox<String> cbArea = areaCombo();
		JTextField tfXp = dlgField("20");

		JPanel form = dlgPanel(new String[] { "Nombre:", "Área:", "XP por día:" },
				new JComponent[] { tfNombre, cbArea, tfXp });

		int ok = JOptionPane.showConfirmDialog(this, form, "Nuevo Hábito", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);
		if (ok != JOptionPane.OK_OPTION || tfNombre.getText().isBlank())
			return;

		String areaId = areas.isEmpty() ? "" : areas.get(cbArea.getSelectedIndex()).getId();
		habitos.add(new Habito(tfNombre.getText().trim(), areaId, parseIntSafe(tfXp.getText(), 20)));
		guardar();
		refrescarTabs();
	}

	private void dlgRegistrarTiempo(Mision m) {
		String input = JOptionPane.showInputDialog(this, "¿Cuántas horas trabajaste hoy en esta misión?",
				"Registrar tiempo", JOptionPane.PLAIN_MESSAGE);
		if (input == null || input.isBlank())
			return;
		double horas = parseDoubleSafe(input, 0);
		if (horas <= 0)
			return;
		m.registrarHoras(horas);
		Area area = findArea(m.getAreaId());
		int xpGanado = (int) (horas * m.getXpPorHora());
		if (area != null)
			area.agregarXp(xpGanado);
		guardar();
		refrescarTodo();

		JOptionPane.showMessageDialog(this,
				"+" + xpGanado + " XP registrados\n"
						+ String.format("Total: %.1fh / %.1fh", m.getHorasRegistradas(), m.getHorasObjetivo()),
				"Sesión registrada", JOptionPane.INFORMATION_MESSAGE);
	}

	// ── HELPERS ───────────────────────────────────────────────
	private void refrescarTodos() {
		actualizarSidebar();
		refrescarTabs();
	}

	private void refrescarTodo() {
		actualizarSidebar();
		sidebarPanel.revalidate();
		sidebarPanel.repaint();
		refrescarTabs();
	}

	private void refrescarTabs() {
		int sel = tabs.getSelectedIndex();
		tabs.setComponentAt(0,
				buildMisionesTab().getComponent(1) instanceof JScrollPane ? buildMisionesTab() : buildMisionesTab());
		tabs.removeAll();
		tabs.addTab("⚔  Misiones", buildMisionesTab());
		tabs.addTab("📅  Hábitos", buildHabitosTab());
		tabs.addTab("🏆  Logros", buildLogrosTab());
		if (sel >= 0 && sel < tabs.getTabCount())
			tabs.setSelectedIndex(sel);
	}

	private int nivelGlobal() {
		if (areas.isEmpty())
			return 1;
		return (int) areas.stream().mapToInt(Area::getNivel).average().orElse(1);
	}

	private Area findArea(String id) {
		return areas.stream().filter(a -> a.getId().equals(id)).findFirst().orElse(null);
	}

	private String estrellas(int n) {
		return "⭐".repeat(Math.max(1, Math.min(5, n)));
	}

	private void guardar() {
		VidaManager.guardar(personaje, areas, misiones, habitos);
	}

	// ── RANGOS ───────────────────────────────────────────────
	public static String getRango(int nivel) {
		if (nivel < 5)
			return "F  Novato";
		if (nivel < 10)
			return "E  Aprendiz";
		if (nivel < 20)
			return "D  Explorador";
		if (nivel < 35)
			return "C  Guerrero";
		if (nivel < 50)
			return "B  Héroe";
		if (nivel < 70)
			return "A  Campeón";
		if (nivel < 90)
			return "S  Maestro";
		return "SS  Leyenda";
	}

	public static Color getColorRango(int nivel) {
		if (nivel < 5)
			return new Color(150, 150, 150);
		if (nivel < 10)
			return new Color(100, 180, 100);
		if (nivel < 20)
			return new Color(100, 160, 255);
		if (nivel < 35)
			return new Color(80, 200, 220);
		if (nivel < 50)
			return new Color(255, 180, 50);
		if (nivel < 70)
			return new Color(255, 120, 50);
		if (nivel < 90)
			return new Color(200, 80, 255);
		return new Color(255, 60, 60);
	}

	// ── UI HELPERS ────────────────────────────────────────────
	private JLabel label(String txt, int size, Color color) {
		JLabel l = new JLabel(txt);
		l.setFont(new Font("SansSerif", Font.PLAIN, size));
		l.setForeground(color);
		return l;
	}

	private JButton rpgBtn(String txt) {
		JButton b = new JButton(txt);
		b.setFont(new Font("SansSerif", Font.PLAIN, 11));
		b.setForeground(TEXT_MAIN);
		b.setBackground(BG_CARD2);
		b.setOpaque(true);
		b.setFocusPainted(false);
		b.setBorder(
				new CompoundBorder(new LineBorder(BORDER_D, 1, true), BorderFactory.createEmptyBorder(4, 10, 4, 10)));
		b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		b.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				b.setBackground(BG_HOVER);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				b.setBackground(BG_CARD2);
			}
		});
		return b;
	}

	private JButton rpgBtnAccent(String txt) {
		JButton b = new JButton(txt);
		b.setFont(new Font("SansSerif", Font.BOLD, 11));
		b.setForeground(BG_DARK);
		b.setBackground(GOLD);
		b.setOpaque(true);
		b.setFocusPainted(false);
		b.setBorder(
				new CompoundBorder(new LineBorder(GOLD_DIM, 1, true), BorderFactory.createEmptyBorder(5, 12, 5, 12)));
		b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		b.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				b.setBackground(new Color(220, 160, 30));
			}

			@Override
			public void mouseExited(MouseEvent e) {
				b.setBackground(GOLD);
			}
		});
		return b;
	}

	private JButton rpgBtnDanger(String txt) {
		JButton b = rpgBtn(txt);
		b.setForeground(HP_RED);
		b.setBorder(new CompoundBorder(new LineBorder(new Color(100, 30, 30), 1, true),
				BorderFactory.createEmptyBorder(4, 8, 4, 8)));
		return b;
	}

	private JSeparator separador() {
		JSeparator s = new JSeparator();
		s.setForeground(BORDER_D);
		s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
		return s;
	}

	private JTextField dlgField(String val) {
		JTextField tf = new JTextField(val);
		tf.setFont(new Font("SansSerif", Font.PLAIN, 13));
		tf.setForeground(TEXT_MAIN);
		tf.setBackground(BG_CARD2);
		tf.setCaretColor(TEXT_MAIN);
		tf.setBorder(
				new CompoundBorder(new LineBorder(BORDER_D, 1, true), BorderFactory.createEmptyBorder(4, 8, 4, 8)));
		return tf;
	}

	private JComboBox<String> areaCombo() {
		String[] names = areas.stream().map(a -> a.getEmoji() + " " + a.getNombre()).toArray(String[]::new);
		JComboBox<String> cb = new JComboBox<>(names.length > 0 ? names : new String[] { "Sin área" });
		cb.setFont(new Font("SansSerif", Font.PLAIN, 12));
		return cb;
	}

	private JPanel dlgPanel(String[] labels, JComponent[] fields) {
		JPanel p = new JPanel(new GridBagLayout());
		p.setBackground(BG_CARD);
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		GridBagConstraints g = new GridBagConstraints();
		g.insets = new Insets(4, 5, 4, 5);
		g.fill = GridBagConstraints.HORIZONTAL;
		for (int i = 0; i < labels.length; i++) {
			g.gridy = i;
			g.gridx = 0;
			g.weightx = 0;
			JLabel l = new JLabel(labels[i]);
			l.setForeground(TEXT_MAIN);
			l.setFont(new Font("SansSerif", Font.PLAIN, 12));
			p.add(l, g);
			g.gridx = 1;
			g.weightx = 1;
			p.add(fields[i], g);
		}
		return p;
	}

	private int parseIntSafe(String s, int def) {
		try {
			return Integer.parseInt(s.trim());
		} catch (Exception e) {
			return def;
		}
	}

	private double parseDoubleSafe(String s, double d) {
		try {
			return Double.parseDouble(s.trim());
		} catch (Exception e) {
			return d;
		}
	}
}