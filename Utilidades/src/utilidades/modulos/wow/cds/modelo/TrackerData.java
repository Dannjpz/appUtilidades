package utilidades.modulos.wow.cds.modelo;

import java.util.ArrayList;
import java.util.List;

public class TrackerData {
	private List<String> columnas;
	private List<Jugador> jugadores;

	public TrackerData() {
		columnas = new ArrayList<>();
		jugadores = new ArrayList<>();
		columnas.add("Conquista");
		columnas.add("ICC 10N");
		columnas.add("ICC 10H");
		columnas.add("SR 25N");
		columnas.add("ICC 25N");
		columnas.add("SR 25H");
		columnas.add("ICC 25H");
		columnas.add("ARCHA 25N");
	}

	public List<String> getColumnas() {
		return columnas;
	}

	public List<Jugador> getJugadores() {
		return jugadores;
	}

	public void agregarColumna(String nombre) {
		if (!columnas.contains(nombre)) {
			columnas.add(nombre);
			for (Jugador j : jugadores)
				j.agregarColumna(nombre);
		}
	}

	public void eliminarColumna(String nombre) {
		columnas.remove(nombre);
		for (Jugador j : jugadores)
			j.eliminarColumna(nombre);
	}

	public void agregarJugador(Jugador j) {
		for (String col : columnas)
			j.agregarColumna(col);
		jugadores.add(j);
	}

	public void eliminarJugador(Jugador j) {
		jugadores.remove(j);
	}
}