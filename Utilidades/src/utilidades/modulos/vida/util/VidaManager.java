package utilidades.modulos.vida.util;

import utilidades.modulos.vida.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class VidaManager {

	private static final String DIR = "vida";
	private static final String FILE = DIR + File.separator + "data.json";

	// ── GUARDAR ───────────────────────────────────────────────
	public static void guardar(Personaje p, List<Area> areas, List<Mision> misiones, List<Habito> habitos) {
		try {
			Files.createDirectories(Path.of(DIR));
			StringBuilder sb = new StringBuilder("{\n");

			// Personaje
			sb.append("  \"personaje\": {\"nombre\":\"").append(esc(p.getNombre())).append("\",\"hp\":")
					.append(p.getHp()).append(",\"hpMax\":").append(p.getHpMax()).append("},\n");

			// Areas
			sb.append("  \"areas\": [\n");
			for (int i = 0; i < areas.size(); i++) {
				Area a = areas.get(i);
				sb.append("    {\"id\":\"").append(esc(a.getId())).append("\",\"nombre\":\"").append(esc(a.getNombre()))
						.append("\",\"emoji\":\"").append(esc(a.getEmoji())).append("\",\"colorHex\":\"")
						.append(esc(a.getColorHex())).append("\",\"xpTotal\":").append(a.getXpTotal()).append("}");
				if (i < areas.size() - 1)
					sb.append(",");
				sb.append("\n");
			}
			sb.append("  ],\n");

			// Misiones
			sb.append("  \"misiones\": [\n");
			for (int i = 0; i < misiones.size(); i++) {
				Mision m = misiones.get(i);
				sb.append("    {\"id\":\"").append(esc(m.getId())).append("\",\"nombre\":\"").append(esc(m.getNombre()))
						.append("\",\"descripcion\":\"").append(esc(m.getDescripcion())).append("\",\"areaId\":\"")
						.append(esc(m.getAreaId())).append("\",\"dificultad\":").append(m.getDificultad())
						.append(",\"tipo\":\"").append(m.getTipo().name()).append("\",\"xpRecompensa\":")
						.append(m.getXpRecompensa()).append(",\"modoTiempo\":").append(m.isModoTiempo())
						.append(",\"xpPorHora\":").append(m.getXpPorHora()).append(",\"horasObjetivo\":")
						.append(m.getHorasObjetivo()).append(",\"horasRegistradas\":").append(m.getHorasRegistradas())
						.append(",\"recompensa\":\"").append(esc(m.getRecompensa())).append("\",\"completada\":")
						.append(m.isCompletada()).append(",\"fechaCompletada\":\"").append(esc(m.getFechaCompletada()))
						.append("\"}");
				if (i < misiones.size() - 1)
					sb.append(",");
				sb.append("\n");
			}
			sb.append("  ],\n");

			// Habitos
			sb.append("  \"habitos\": [\n");
			for (int i = 0; i < habitos.size(); i++) {
				Habito h = habitos.get(i);
				sb.append("    {\"id\":\"").append(esc(h.getId())).append("\",\"nombre\":\"").append(esc(h.getNombre()))
						.append("\",\"areaId\":\"").append(esc(h.getAreaId())).append("\",\"xpPorDia\":")
						.append(h.getXpPorDia()).append(",\"streakActual\":").append(h.getStreakActual())
						.append(",\"mejorStreak\":").append(h.getMejorStreak()).append(",\"ultimaFecha\":\"")
						.append(esc(h.getUltimaFecha())).append("\"}");
				if (i < habitos.size() - 1)
					sb.append(",");
				sb.append("\n");
			}
			sb.append("  ]\n}\n");

			Files.writeString(Path.of(FILE), sb.toString(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			System.err.println("Error guardando vida: " + e.getMessage());
		}
	}

	// ── CARGAR ────────────────────────────────────────────────
	public static Object[] cargar() {
		// Returns [Personaje, List<Area>, List<Mision>, List<Habito>]
		if (!Files.exists(Path.of(FILE)))
			return defaults();
		try {
			String raw = Files.readString(Path.of(FILE), StandardCharsets.UTF_8);

			// Personaje
			String pSec = between(raw, "\"personaje\": {", "}");
			Personaje per = new Personaje(sf(pSec, "nombre"));
			per.setHp((int) nf(pSec, "hp"));
			per.setHpMax((int) nf(pSec, "hpMax"));
			if (per.getHpMax() <= 0)
				per.setHpMax(100);

			// Areas
			List<Area> areas = new ArrayList<>();
			String aSec = between(raw, "\"areas\": [", "\n  ]");
			if (aSec != null) {
				for (String obj : objects(aSec)) {
					Area a = new Area(sf(obj, "id"), sf(obj, "nombre"), sf(obj, "emoji"), sf(obj, "colorHex"));
					a.setXpTotal((long) nf(obj, "xpTotal"));
					areas.add(a);
				}
			}

			// Misiones
			List<Mision> misiones = new ArrayList<>();
			String mSec = between(raw, "\"misiones\": [", "\n  ]");
			if (mSec != null) {
				for (String obj : objects(mSec)) {
					String tipo = sf(obj, "tipo");
					Mision m = new Mision(sf(obj, "nombre"), sf(obj, "areaId"), (int) nf(obj, "dificultad"),
							"EPICA".equals(tipo) ? Mision.Tipo.EPICA : Mision.Tipo.NORMAL,
							(int) nf(obj, "xpRecompensa"), sf(obj, "recompensa"));
					m.setId(sf(obj, "id"));
					m.setDescripcion(sf(obj, "descripcion"));
					m.setModoTiempo("true".equals(sf2(obj, "modoTiempo")));
					m.setXpPorHora((int) nf(obj, "xpPorHora"));
					m.setHorasObjetivo(nf(obj, "horasObjetivo"));
					m.setHorasRegistradas(nf(obj, "horasRegistradas"));
					m.setCompletada("true".equals(sf2(obj, "completada")));
					m.setFechaCompletada(sf(obj, "fechaCompletada"));
					misiones.add(m);
				}
			}

			// Habitos
			List<Habito> habitos = new ArrayList<>();
			String hSec = between(raw, "\"habitos\": [", "\n  ]");
			if (hSec != null) {
				for (String obj : objects(hSec)) {
					Habito h = new Habito(sf(obj, "nombre"), sf(obj, "areaId"), (int) nf(obj, "xpPorDia"));
					h.setId(sf(obj, "id"));
					h.setStreakActual((int) nf(obj, "streakActual"));
					h.setMejorStreak((int) nf(obj, "mejorStreak"));
					h.setUltimaFecha(sf(obj, "ultimaFecha"));
					habitos.add(h);
				}
			}

			if (areas.isEmpty())
				areas = areasDefault();
			return new Object[] { per, areas, misiones, habitos };
		} catch (Exception e) {
			System.err.println("Error cargando vida: " + e.getMessage());
			return defaults();
		}
	}

	private static Object[] defaults() {
		return new Object[] { new Personaje("Héroe"), areasDefault(), new ArrayList<Mision>(),
				new ArrayList<Habito>() };
	}

	public static List<Area> areasDefault() {
		return new ArrayList<>(List.of(new Area("mental", "Mental", "🧠", "#9664FF"),
				new Area("espiritual", "Espiritual", "💫", "#00D4CC"),
				new Area("economico", "Económico", "💰", "#FFB800"), new Area("fisico", "Físico", "💪", "#00CC66"),
				new Area("social", "Social", "❤️", "#FF4D6D")));
	}

	// ── JSON helpers ──────────────────────────────────────────
	private static String esc(String s) {
		if (s == null)
			return "";
		return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
	}

	private static String unesc(String s) {
		return s.replace("\\\"", "\"").replace("\\n", "\n").replace("\\\\", "\\");
	}

	private static String between(String src, String a, String b) {
		int s = src.indexOf(a);
		if (s < 0)
			return null;
		s += a.length();
		int e = src.indexOf(b, s);
		if (e < 0)
			return null;
		return src.substring(s, e);
	}

	private static String sf(String obj, String key) {
		String k = "\"" + key + "\":\"";
		int s = obj.indexOf(k);
		if (s < 0)
			return "";
		s += k.length();
		StringBuilder v = new StringBuilder();
		for (int i = s; i < obj.length(); i++) {
			char c = obj.charAt(i);
			if (c == '\\' && i + 1 < obj.length()) {
				v.append(obj.charAt(++i));
				continue;
			}
			if (c == '"')
				break;
			v.append(c);
		}
		return unesc(v.toString());
	}

	private static String sf2(String obj, String key) {
		// Para booleanos sin comillas
		String k = "\"" + key + "\":";
		int s = obj.indexOf(k);
		if (s < 0)
			return "";
		s += k.length();
		int e = s;
		while (e < obj.length() && obj.charAt(e) != ',' && obj.charAt(e) != '}')
			e++;
		return obj.substring(s, e).trim();
	}

	private static double nf(String obj, String key) {
		String k = "\"" + key + "\":";
		int s = obj.indexOf(k);
		if (s < 0)
			return 0;
		s += k.length();
		while (s < obj.length() && (obj.charAt(s) == ' ' || obj.charAt(s) == '"'))
			s++;
		int e = s;
		while (e < obj.length() && (Character.isDigit(obj.charAt(e)) || obj.charAt(e) == '.' || obj.charAt(e) == '-'))
			e++;
		try {
			return Double.parseDouble(obj.substring(s, e));
		} catch (Exception ex) {
			return 0;
		}
	}

	private static List<String> objects(String src) {
		List<String> res = new ArrayList<>();
		int depth = 0, start = -1;
		boolean inStr = false;
		for (int i = 0; i < src.length(); i++) {
			char c = src.charAt(i);
			if (c == '"' && (i == 0 || src.charAt(i - 1) != '\\')) {
				inStr = !inStr;
				continue;
			}
			if (inStr)
				continue;
			if (c == '{') {
				if (depth == 0)
					start = i;
				depth++;
			} else if (c == '}') {
				depth--;
				if (depth == 0 && start >= 0) {
					res.add(src.substring(start, i + 1));
					start = -1;
				}
			}
		}
		return res;
	}
}