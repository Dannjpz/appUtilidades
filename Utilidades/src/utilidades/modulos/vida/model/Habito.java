package utilidades.modulos.vida.model;

import java.time.LocalDate;
import java.util.UUID;

public class Habito {

	private String id;
	private String nombre;
	private String areaId;
	private int xpPorDia;
	private int streakActual;
	private int mejorStreak;
	private String ultimaFecha; // yyyy-MM-dd de la última vez completado

	public Habito(String nombre, String areaId, int xpPorDia) {
		this.id = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
		this.nombre = nombre;
		this.areaId = areaId;
		this.xpPorDia = xpPorDia;
		this.streakActual = 0;
		this.mejorStreak = 0;
		this.ultimaFecha = "";
	}

	/** ¿Ya fue completado hoy? */
	public boolean isCompletadoHoy() {
		return LocalDate.now().toString().equals(ultimaFecha);
	}

	/** ¿Se rompió el streak? (no completado ayer ni hoy) */
	public boolean streakRoto() {
		if (ultimaFecha == null || ultimaFecha.isBlank())
			return false;
		try {
			LocalDate ultima = LocalDate.parse(ultimaFecha);
			LocalDate ayer = LocalDate.now().minusDays(1);
			LocalDate hoy = LocalDate.now();
			return ultima.isBefore(ayer) && !ultima.equals(hoy);
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Completa el hábito hoy. Devuelve el XP ganado (con streak bonus).
	 */
	public int completarHoy() {
		if (isCompletadoHoy())
			return 0;

		// Verificar si el streak se rompió
		if (streakRoto())
			streakActual = 0;

		streakActual++;
		if (streakActual > mejorStreak)
			mejorStreak = streakActual;
		ultimaFecha = LocalDate.now().toString();

		return getXpConBonus();
	}

	/** XP con bonus de streak */
	public int getXpConBonus() {
		float bonus = 1.0f;
		if (streakActual >= 30)
			bonus = 1.5f;
		else if (streakActual >= 7)
			bonus = 1.2f;
		return (int) (xpPorDia * bonus);
	}

	/** Emoji de fuego si el streak está activo */
	public String getStreakEmoji() {
		if (streakActual == 0)
			return "";
		if (streakActual >= 30)
			return "🔥🔥🔥";
		if (streakActual >= 7)
			return "🔥🔥";
		return "🔥";
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

	public String getAreaId() {
		return areaId;
	}

	public void setAreaId(String a) {
		this.areaId = a;
	}

	public int getXpPorDia() {
		return xpPorDia;
	}

	public void setXpPorDia(int x) {
		this.xpPorDia = x;
	}

	public int getStreakActual() {
		return streakActual;
	}

	public void setStreakActual(int s) {
		this.streakActual = s;
	}

	public int getMejorStreak() {
		return mejorStreak;
	}

	public void setMejorStreak(int s) {
		this.mejorStreak = s;
	}

	public String getUltimaFecha() {
		return ultimaFecha;
	}

	public void setUltimaFecha(String f) {
		this.ultimaFecha = f;
	}
}