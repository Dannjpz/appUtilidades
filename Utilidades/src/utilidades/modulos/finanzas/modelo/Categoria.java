package utilidades.modulos.finanzas.modelo;

import java.util.UUID;

public class Categoria {
	private String id;
	private String nombre;
	private String color; // hex ej. "#FF6B6B"
	private double presupuesto; // 0 = sin límite

	public Categoria(String nombre, String color, double presupuesto) {
		this.id = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
		this.nombre = nombre;
		this.color = color;
		this.presupuesto = presupuesto;
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

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public double getPresupuesto() {
		return presupuesto;
	}

	public void setPresupuesto(double p) {
		this.presupuesto = p;
	}

	@Override
	public String toString() {
		return nombre;
	}
}