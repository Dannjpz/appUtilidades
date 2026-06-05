package utilidades.modulos.vida.model;

import java.util.UUID;

public class Mision {

	public enum Tipo {
		NORMAL, EPICA
	}

	private String id;
	private String nombre;
	private String descripcion;
	private String areaId;
	private int dificultad; // 1-5 estrellas
	private Tipo tipo;
	private int xpRecompensa; // XP fijo al completar
	private boolean modoTiempo; // si usa XP por hora
	private int xpPorHora;
	private double horasObjetivo;
	private double horasRegistradas;
	private String recompensa; // texto de la recompensa personal
	private boolean completada;
	private String fechaCompletada; // yyyy-MM-dd

	public Mision(String nombre, String areaId, int dificultad, Tipo tipo, int xpRecompensa, String recompensa) {
		this.id = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
		this.nombre = nombre;
		this.descripcion = "";
		this.areaId = areaId;
		this.dificultad = dificultad;
		this.tipo = tipo;
		this.xpRecompensa = xpRecompensa;
		this.modoTiempo = false;
		this.xpPorHora = 0;
		this.horasObjetivo = 0;
		this.horasRegistradas = 0;
		this.recompensa = recompensa != null ? recompensa : "";
		this.completada = false;
		this.fechaCompletada = "";
	}

	/** XP ganado hasta ahora (modo tiempo) o total (modo fijo) */
	public int getXpGanado() {
		if (modoTiempo)
			return (int) (horasRegistradas * xpPorHora);
		return xpRecompensa;
	}

	/** Porcentaje completado (modo tiempo) */
	public float getPorcentajeTiempo() {
		if (!modoTiempo || horasObjetivo <= 0)
			return completada ? 1f : 0f;
		return (float) Math.min(1.0, horasRegistradas / horasObjetivo);
	}

	public void registrarHoras(double horas) {
		horasRegistradas += horas;
	}

	// ── Getters / Setters ─────────────────────────────────────
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String n) {
		this.nombre = n;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String d) {
		this.descripcion = d;
	}

	public String getAreaId() {
		return areaId;
	}

	public void setAreaId(String a) {
		this.areaId = a;
	}

	public int getDificultad() {
		return dificultad;
	}

	public void setDificultad(int d) {
		this.dificultad = d;
	}

	public Tipo getTipo() {
		return tipo;
	}

	public void setTipo(Tipo t) {
		this.tipo = t;
	}

	public int getXpRecompensa() {
		return xpRecompensa;
	}

	public void setXpRecompensa(int x) {
		this.xpRecompensa = x;
	}

	public boolean isModoTiempo() {
		return modoTiempo;
	}

	public void setModoTiempo(boolean m) {
		this.modoTiempo = m;
	}

	public int getXpPorHora() {
		return xpPorHora;
	}

	public void setXpPorHora(int x) {
		this.xpPorHora = x;
	}

	public double getHorasObjetivo() {
		return horasObjetivo;
	}

	public void setHorasObjetivo(double h) {
		this.horasObjetivo = h;
	}

	public double getHorasRegistradas() {
		return horasRegistradas;
	}

	public void setHorasRegistradas(double h) {
		this.horasRegistradas = h;
	}

	public String getRecompensa() {
		return recompensa;
	}

	public void setRecompensa(String r) {
		this.recompensa = r;
	}

	public boolean isCompletada() {
		return completada;
	}

	public void setCompletada(boolean c) {
		this.completada = c;
	}

	public String getFechaCompletada() {
		return fechaCompletada;
	}

	public void setFechaCompletada(String f) {
		this.fechaCompletada = f;
	}
}