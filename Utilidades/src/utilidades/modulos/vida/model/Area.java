package utilidades.modulos.vida.model;

import java.awt.Color;

public class Area {
	private String id;
	private String nombre;
	private String emoji;
	private String colorHex;
	private long xpTotal;

	public Area(String id, String nombre, String emoji, String colorHex) {
		this.id = id;
		this.nombre = nombre;
		this.emoji = emoji;
		this.colorHex = colorHex;
		this.xpTotal = 0;
	}

	// ── XP / Nivel ────────────────────────────────────────────
	/** XP necesario para pasar del nivel n al n+1 */
	public static long xpParaNivel(int n) {
		return (n + 1) * 100L;
	}

	/** Nivel actual calculado desde xpTotal */
	public int getNivel() {
		long resto = xpTotal;
		int nivel = 0;
		while (resto >= xpParaNivel(nivel)) {
			resto -= xpParaNivel(nivel);
			nivel++;
		}
		return nivel;
	}

	/** XP acumulado dentro del nivel actual */
	public long getXpEnNivel() {
		long resto = xpTotal;
		int nivel = 0;
		while (resto >= xpParaNivel(nivel)) {
			resto -= xpParaNivel(nivel);
			nivel++;
		}
		return resto;
	}

	/** XP necesario para completar el nivel actual */
	public long getXpParaSiguiente() {
		return xpParaNivel(getNivel());
	}

	/** Porcentaje de progreso al siguiente nivel (0.0 - 1.0) */
	public float getPorcentaje() {
		long sig = getXpParaSiguiente();
		return sig > 0 ? (float) getXpEnNivel() / sig : 0f;
	}

	public void agregarXp(long xp) {
		this.xpTotal += xp;
	}

	// ── Getters / Setters ─────────────────────────────────────
	public String getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public String getEmoji() {
		return emoji;
	}

	public String getColorHex() {
		return colorHex;
	}

	public long getXpTotal() {
		return xpTotal;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setXpTotal(long xp) {
		this.xpTotal = xp;
	}

	public void setColorHex(String c) {
		this.colorHex = c;
	}

	public Color getColor() {
		try {
			return Color.decode(colorHex);
		} catch (Exception e) {
			return Color.GRAY;
		}
	}

	@Override
	public String toString() {
		return emoji + " " + nombre;
	}
}