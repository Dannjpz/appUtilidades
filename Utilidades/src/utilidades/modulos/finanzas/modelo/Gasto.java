package utilidades.modulos.finanzas.modelo;

import java.util.UUID;

public class Gasto {
	private String id;
	private double monto;
	private String categoriaId;
	private String descripcion;
	private String fecha; // yyyy-MM-dd

	public Gasto(double monto, String categoriaId, String descripcion, String fecha) {
		this.id = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
		this.monto = monto;
		this.categoriaId = categoriaId;
		this.descripcion = descripcion;
		this.fecha = fecha;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public double getMonto() {
		return monto;
	}

	public void setMonto(double monto) {
		this.monto = monto;
	}

	public String getCategoriaId() {
		return categoriaId;
	}

	public void setCategoriaId(String cid) {
		this.categoriaId = cid;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String desc) {
		this.descripcion = desc;
	}

	public String getFecha() {
		return fecha;
	}

	public void setFecha(String fecha) {
		this.fecha = fecha;
	}
}