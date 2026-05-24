package utilidades.modulos.wow.cds.modelo;

import java.util.LinkedHashMap;
import java.util.Map;

public class Jugador {
	private String nombre;
	private String faccion;
	private String nota;
	private Map<String, Boolean> progreso;

	public Jugador(String nombre, String faccion) {
		this.nombre = nombre;
		this.faccion = faccion;
		this.nota = "";
		this.progreso = new LinkedHashMap<>();
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getFaccion() {
		return faccion;
	}

	public void setFaccion(String faccion) {
		this.faccion = faccion;
	}

	public String getNota() {
		return nota;
	}

	public void setNota(String nota) {
		this.nota = nota;
	}

	public Map<String, Boolean> getProgreso() {
		return progreso;
	}

	public boolean getColumna(String col) {
		return progreso.getOrDefault(col, false);
	}

	public void setColumna(String col, boolean val) {
		progreso.put(col, val);
	}

	public void agregarColumna(String col) {
		progreso.putIfAbsent(col, false);
	}

	public void eliminarColumna(String col) {
		progreso.remove(col);
	}
}