package utilidades.modulos.wow.cds.utiles;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import utilidades.modulos.wow.cds.modelo.Jugador;
import utilidades.modulos.wow.cds.modelo.TrackerData;

public class DataManager {

	private static final String FILE = "utilidades_mazmorras.json";

	public static void guardar(TrackerData data) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("{\n");

		sb.append("  \"columnas\": [");
		List<String> cols = data.getColumnas();
		for (int i = 0; i < cols.size(); i++) {
			sb.append("\"").append(escape(cols.get(i))).append("\"");
			if (i < cols.size() - 1)
				sb.append(", ");
		}
		sb.append("],\n");

		sb.append("  \"jugadores\": [\n");
		List<Jugador> jugadores = data.getJugadores();
		for (int i = 0; i < jugadores.size(); i++) {
			Jugador j = jugadores.get(i);
			sb.append("    {\n");
			sb.append("      \"nombre\": \"").append(escape(j.getNombre())).append("\",\n");
			sb.append("      \"faccion\": \"").append(escape(j.getFaccion())).append("\",\n");
			sb.append("      \"nota\": \"").append(escape(j.getNota())).append("\",\n");
			sb.append("      \"progreso\": {");
			Map<String, Boolean> prog = j.getProgreso();
			List<String> keys = new ArrayList<>(prog.keySet());
			for (int k = 0; k < keys.size(); k++) {
				sb.append("\"").append(escape(keys.get(k))).append("\": ").append(prog.get(keys.get(k)));
				if (k < keys.size() - 1)
					sb.append(", ");
			}
			sb.append("}\n");
			sb.append("    }");
			if (i < jugadores.size() - 1)
				sb.append(",");
			sb.append("\n");
		}
		sb.append("  ]\n");
		sb.append("}\n");

		Files.writeString(Path.of(FILE), sb.toString());
	}

	public static TrackerData cargar() throws IOException {
		Path p = Path.of(FILE);
		if (!Files.exists(p))
			return cargarDatosEjemplo();

		String raw = Files.readString(p);
		TrackerData data = new TrackerData();
		data.getColumnas().clear();

		String colsSection = extractBetween(raw, "\"columnas\": [", "]");
		if (colsSection != null) {
			for (String col : splitJsonArray(colsSection))
				data.getColumnas().add(col);
		}

		String jugSection = extractBetween(raw, "\"jugadores\": [", "\n  ]");
		if (jugSection != null) {
			for (String bloque : splitJsonObjects(jugSection)) {
				String nombre = extractStringField(bloque, "nombre");
				String faccion = extractStringField(bloque, "faccion");
				String nota = extractStringField(bloque, "nota");
				Jugador j = new Jugador(nombre != null ? nombre : "", faccion != null ? faccion : "Alianza");
				if (nota != null)
					j.setNota(nota);
				String progSection = extractBetween(bloque, "\"progreso\": {", "}");
				if (progSection != null) {
					for (Map.Entry<String, Boolean> e : parseProgresoMap(progSection).entrySet())
						j.setColumna(e.getKey(), e.getValue());
				}
				data.getJugadores().add(j);
			}
		}

		for (Jugador j : data.getJugadores())
			for (String col : data.getColumnas())
				j.agregarColumna(col);

		return data;
	}

	private static String escape(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
	}

	private static String unescape(String s) {
		return s.replace("\\\"", "\"").replace("\\n", "\n").replace("\\\\", "\\");
	}

	private static String extractBetween(String src, String start, String end) {
		int s = src.indexOf(start);
		if (s < 0)
			return null;
		s += start.length();
		int e = src.indexOf(end, s);
		if (e < 0)
			return null;
		return src.substring(s, e).trim();
	}

	private static List<String> splitJsonArray(String arr) {
		List<String> result = new ArrayList<>();
		for (String part : arr.split(",")) {
			String t = part.trim().replaceAll("^\"|\"$", "");
			if (!t.isEmpty())
				result.add(unescape(t));
		}
		return result;
	}

	private static List<String> splitJsonObjects(String src) {
		List<String> result = new ArrayList<>();
		int depth = 0, start = -1;
		for (int i = 0; i < src.length(); i++) {
			char c = src.charAt(i);
			if (c == '{') {
				if (depth == 0)
					start = i;
				depth++;
			} else if (c == '}') {
				depth--;
				if (depth == 0 && start >= 0) {
					result.add(src.substring(start, i + 1));
					start = -1;
				}
			}
		}
		return result;
	}

	private static String extractStringField(String obj, String field) {
		String key = "\"" + field + "\": \"";
		int s = obj.indexOf(key);
		if (s < 0)
			return null;
		s += key.length();
		int e = obj.indexOf("\"", s);
		if (e < 0)
			return null;
		return unescape(obj.substring(s, e));
	}

	private static Map<String, Boolean> parseProgresoMap(String prog) {
		Map<String, Boolean> map = new LinkedHashMap<>();
		for (String part : prog.split(",")) {
			part = part.trim();
			int colon = part.indexOf(':');
			if (colon < 0)
				continue;
			String key = part.substring(0, colon).trim().replaceAll("^\"|\"$", "");
			String val = part.substring(colon + 1).trim();
			map.put(unescape(key), "true".equals(val));
		}
		return map;
	}

	private static TrackerData cargarDatosEjemplo() {
		TrackerData data = new TrackerData();

		String[][] alianza = { { "tzss", "" }, { "alinde", "" }, { "baly", "" }, { "hibernate", "" },
				{ "Drummerh", "" }, { "Lvesham", "" }, { "Bubblegumqt", "" }, { "lvdk", "" }, { "imlove", "" },
				{ "cashmoney", "" }, { "Uncrowneds", "archa" }, { "Abuntu", "archa — CAPA/ESCUDO/CUELLO ICC" }, };
		for (String[] row : alianza) {
			Jugador j = new Jugador(row[0], "Alianza");
			j.setNota(row[1]);
			data.agregarJugador(j);
		}

		String[][] horda = { { "howtytwo", "" }, { "softpaws", "" }, { "bigsmile", "" }, { "Taffypuffqt", "" },
				{ "xhtml", "" }, };
		for (String[] row : horda) {
			Jugador j = new Jugador(row[0], "Horda");
			j.setNota(row[1]);
			data.agregarJugador(j);
		}

		return data;
	}
}