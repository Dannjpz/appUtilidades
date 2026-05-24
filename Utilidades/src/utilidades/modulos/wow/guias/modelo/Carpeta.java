package utilidades.modulos.wow.guias.modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Carpeta {
	private String id;
	private String nombre;
	private List<Entrada> entradas;

	public Carpeta(String nombre) {
		this.id = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
		this.nombre = nombre;
		this.entradas = new ArrayList<>();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public List<Entrada> getEntradas() {
		return entradas;
	}

	@Override
	public String toString() {
		return nombre;
	}
}