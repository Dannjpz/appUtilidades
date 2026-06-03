package utilidades.modulos.finanzas.util;

import utilidades.modulos.finanzas.modelo.Categoria;
import utilidades.modulos.finanzas.modelo.Gasto;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class FinanzasManager {

	private static final String FILE = "finanzas" + File.separator + "data.json";

	// ── GUARDAR ───────────────────────────────────────────────
	public static void guardar(double ingresoMensual, List<Categoria> categorias, List<Gasto> gastos) {
		try {
			Files.createDirectories(Path.of("finanzas"));
			StringBuilder sb = new StringBuilder("{\n");
			sb.append("  \"ingresoMensual\": ").append(ingresoMensual).append(",\n");

			// Categorias
			sb.append("  \"categorias\": [\n");
			for (int i = 0; i < categorias.size(); i++) {
				Categoria c = categorias.get(i);
				sb.append("    {\"id\":\"").append(esc(c.getId())).append("\",\"nombre\":\"").append(esc(c.getNombre()))
						.append("\",\"color\":\"").append(esc(c.getColor())).append("\",\"presupuesto\":")
						.append(c.getPresupuesto()).append("}");
				if (i < categorias.size() - 1)
					sb.append(",");
				sb.append("\n");
			}
			sb.append("  ],\n");

			// Gastos
			sb.append("  \"gastos\": [\n");
			for (int i = 0; i < gastos.size(); i++) {
				Gasto g = gastos.get(i);
				sb.append("    {\"id\":\"").append(esc(g.getId())).append("\",\"monto\":").append(g.getMonto())
						.append(",\"categoriaId\":\"").append(esc(g.getCategoriaId())).append("\",\"descripcion\":\"")
						.append(esc(g.getDescripcion())).append("\",\"fecha\":\"").append(esc(g.getFecha()))
						.append("\"}");
				if (i < gastos.size() - 1)
					sb.append(",");
				sb.append("\n");
			}
			sb.append("  ]\n}\n");

			Files.writeString(Path.of(FILE), sb.toString(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			System.err.println("Error guardando finanzas: " + e.getMessage());
		}
	}

	// ── CARGAR ────────────────────────────────────────────────
	public static double[] cargarIngreso(String raw) {
		return new double[] { numField(raw, "ingresoMensual") };
	}

	public static List<Categoria> cargarCategorias(String raw) {
		List<Categoria> list = new ArrayList<>();
		String sec = between(raw, "\"categorias\": [", "\n  ]");
		if (sec == null)
			return list;
		for (String obj : objects(sec)) {
			String id = strField(obj, "id");
			String nom = strField(obj, "nombre");
			String col = strField(obj, "color");
			double pres = numField(obj, "presupuesto");
			if (nom == null)
				continue;
			Categoria c = new Categoria(nom, col != null ? col : "#888888", pres);
			if (id != null)
				c.setId(id);
			list.add(c);
		}
		return list;
	}

	public static List<Gasto> cargarGastos(String raw) {
		List<Gasto> list = new ArrayList<>();
		String sec = between(raw, "\"gastos\": [", "\n  ]");
		if (sec == null)
			return list;
		for (String obj : objects(sec)) {
			String id = strField(obj, "id");
			double monto = numField(obj, "monto");
			String catId = strField(obj, "categoriaId");
			String desc = strField(obj, "descripcion");
			String fecha = strField(obj, "fecha");
			if (catId == null || fecha == null)
				continue;
			Gasto g = new Gasto(monto, catId, desc != null ? desc : "", fecha);
			if (id != null)
				g.setId(id);
			list.add(g);
		}
		return list;
	}

	public static String leerArchivo() {
		try {
			if (!Files.exists(Path.of(FILE)))
				return null;
			return Files.readString(Path.of(FILE), StandardCharsets.UTF_8);
		} catch (IOException e) {
			return null;
		}
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

	private static String between(String src, String start, String end) {
		int s = src.indexOf(start);
		if (s < 0)
			return null;
		s += start.length();
		int e = src.indexOf(end, s);
		if (e < 0)
			return null;
		return src.substring(s, e);
	}

	private static String strField(String obj, String key) {
		String k = "\"" + key + "\":\"";
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

	private static double numField(String obj, String key) {
		String k = "\"" + key + "\": ";
		int s = obj.indexOf(k);
		if (s < 0) {
			k = "\"" + key + "\":";
			s = obj.indexOf(k);
		}
		if (s < 0)
			return 0;
		s += k.length();
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