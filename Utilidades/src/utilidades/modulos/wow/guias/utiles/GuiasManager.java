package utilidades.modulos.wow.guias.utiles;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import utilidades.modulos.wow.guias.modelo.Carpeta;
import utilidades.modulos.wow.guias.modelo.Entrada;

public class GuiasManager {

	public static final String BASE_DIR = "guias";
	public static final String MEDIA_DIR = BASE_DIR + File.separator + "media";
	private static final String DATA_FILE = BASE_DIR + File.separator + "data.json";

	// ── GUARDAR ───────────────────────────────────────────────
	public static void guardar(List<Carpeta> carpetas) {
		try {
			Files.createDirectories(Path.of(BASE_DIR));
			StringBuilder sb = new StringBuilder();
			sb.append("{\n  \"carpetas\": [\n");
			for (int i = 0; i < carpetas.size(); i++) {
				Carpeta c = carpetas.get(i);
				sb.append("    {\n");
				sb.append("      \"id\": \"").append(esc(c.getId())).append("\",\n");
				sb.append("      \"nombre\": \"").append(esc(c.getNombre())).append("\",\n");
				sb.append("      \"entradas\": [\n");
				List<Entrada> entradas = c.getEntradas();
				for (int j = 0; j < entradas.size(); j++) {
					Entrada e = entradas.get(j);
					sb.append("        {\n");
					sb.append("          \"id\": \"").append(esc(e.getId())).append("\",\n");
					sb.append("          \"tipo\": \"").append(e.getTipo().name()).append("\",\n");
					sb.append("          \"nombre\": \"").append(esc(e.getNombre())).append("\",\n");
					sb.append("          \"contenido\": \"").append(esc(e.getContenido())).append("\",\n");
					sb.append("          \"fecha\": \"").append(esc(e.getFecha())).append("\"\n");
					sb.append("        }");
					if (j < entradas.size() - 1)
						sb.append(",");
					sb.append("\n");
				}
				sb.append("      ]\n    }");
				if (i < carpetas.size() - 1)
					sb.append(",");
				sb.append("\n");
			}
			sb.append("  ]\n}\n");
			Files.writeString(Path.of(DATA_FILE), sb.toString());
		} catch (IOException e) {
			System.err.println("Error guardando guías: " + e.getMessage());
		}
	}

	// ── CARGAR ────────────────────────────────────────────────
	public static List<Carpeta> cargar() {
		List<Carpeta> result = new ArrayList<>();
		if (!Files.exists(Path.of(DATA_FILE)))
			return result;
		try {
			String raw = Files.readString(Path.of(DATA_FILE));
			String sec = extractBetween(raw, "\"carpetas\": [", "\n  ]");
			if (sec == null)
				return result;
			for (String bc : splitObjects(sec)) {
				String id = field(bc, "id");
				String nombre = field(bc, "nombre");
				if (nombre == null)
					continue;
				Carpeta c = new Carpeta(nombre);
				if (id != null)
					c.setId(id);
				String es = extractBetween(bc, "\"entradas\": [", "\n      ]");
				if (es != null) {
					for (String be : splitObjects(es)) {
						String eid = field(be, "id");
						String tipo = field(be, "tipo");
						String en = field(be, "nombre");
						String cont = field(be, "contenido");
						String fec = field(be, "fecha");
						if (tipo == null || en == null || cont == null)
							continue;
						try {
							Entrada.Tipo t = Entrada.Tipo.valueOf(tipo);
							Entrada e = new Entrada(t, en, cont);
							if (eid != null)
								e.setId(eid);
							if (fec != null)
								e.setFecha(fec);
							c.getEntradas().add(e);
						} catch (IllegalArgumentException ignored) {
						}
					}
				}
				result.add(c);
			}
		} catch (IOException e) {
			System.err.println("Error cargando guías: " + e.getMessage());
		}
		return result;
	}

	// ── COPIAR MEDIA ──────────────────────────────────────────
	/**
	 * Copia el archivo al directorio de media. Devuelve el nombre del archivo
	 * destino.
	 */
	public static String copiarMedia(File src) throws IOException {
		Files.createDirectories(Path.of(MEDIA_DIR));
		String ext = "";
		int dot = src.getName().lastIndexOf('.');
		if (dot >= 0)
			ext = src.getName().substring(dot).toLowerCase();
		String nombre = UUID.randomUUID().toString().replace("-", "").substring(0, 12) + ext;
		Files.copy(src.toPath(), Path.of(MEDIA_DIR, nombre), StandardCopyOption.REPLACE_EXISTING);
		return nombre;
	}

	// ── JSON helpers ──────────────────────────────────────────
	private static String esc(String s) {
		if (s == null)
			return "";
		return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
	}

	private static String unesc(String s) {
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
		return src.substring(s, e);
	}

	private static String field(String obj, String key) {
		String k = "\"" + key + "\": \"";
		int s = obj.indexOf(k);
		if (s < 0)
			return null;
		s += k.length();
		StringBuilder val = new StringBuilder();
		for (int i = s; i < obj.length(); i++) {
			char c = obj.charAt(i);
			if (c == '\\' && i + 1 < obj.length()) {
				val.append(obj.charAt(++i));
				continue;
			}
			if (c == '"')
				break;
			val.append(c);
		}
		return unesc(val.toString());
	}

	private static List<String> splitObjects(String src) {
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