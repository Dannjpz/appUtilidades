package utilidades.modulos.wow.guias.modelo;

import java.time.LocalDate;
import java.util.UUID;

public class Entrada {
	public enum Tipo {
		TEXTO, IMAGEN, ARCHIVO
	}

	private String id;
	private Tipo tipo;
	private String nombre;
	private String contenido; // texto plano para TEXTO; nombre de archivo para IMAGEN/ARCHIVO
	private String fecha;

	public Entrada(Tipo tipo, String nombre, String contenido) {
		this.id = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
		this.tipo = tipo;
		this.nombre = nombre;
		this.contenido = contenido;
		this.fecha = LocalDate.now().toString();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Tipo getTipo() {
		return tipo;
	}

	public void setTipo(Tipo tipo) {
		this.tipo = tipo;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getContenido() {
		return contenido;
	}

	public void setContenido(String c) {
		this.contenido = c;
	}

	public String getFecha() {
		return fecha;
	}

	public void setFecha(String fecha) {
		this.fecha = fecha;
	}
}