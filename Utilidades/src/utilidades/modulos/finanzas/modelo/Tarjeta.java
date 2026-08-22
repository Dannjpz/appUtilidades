package utilidades.modulos.finanzas.modelo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class Tarjeta {

	private String id;
	private String nombre;
	private int diaCorte; // ej. 9
	private int diaLimitePago; // ej. 29
	private double limiteCredito; // 0 = sin límite
	private String color; // hex

	public Tarjeta(String nombre, int diaCorte, int diaLimitePago, double limiteCredito, String color) {
		this.id = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
		this.nombre = nombre;
		this.diaCorte = diaCorte;
		this.diaLimitePago = diaLimitePago;
		this.limiteCredito = limiteCredito;
		this.color = color != null ? color : "#60A0FF";
	}

	// ── Período actual de la tarjeta ──────────────────────────
	/**
	 * Devuelve [inicio, fin] del período de corte vigente. Ejemplo diaCorte=9,
	 * hoy=5 junio → período: 10 mayo - 9 junio Ejemplo diaCorte=9, hoy=15 junio →
	 * período: 10 junio - 9 julio
	 */
	public LocalDate[] getPeriodoActual() {
		LocalDate hoy = LocalDate.now();
		LocalDate inicio, fin;

		if (hoy.getDayOfMonth() <= diaCorte) {
			// Estamos antes o en el corte → período cubre mes anterior → este mes
			LocalDate mesPrev = hoy.minusMonths(1);
			int diaIni = Math.min(diaCorte + 1, mesPrev.lengthOfMonth());
			inicio = LocalDate.of(mesPrev.getYear(), mesPrev.getMonth(), diaIni);
			fin = LocalDate.of(hoy.getYear(), hoy.getMonth(), Math.min(diaCorte, hoy.lengthOfMonth()));
		} else {
			// Ya pasó el corte → nuevo período: este mes → siguiente
			int diaIni = Math.min(diaCorte + 1, hoy.lengthOfMonth());
			inicio = LocalDate.of(hoy.getYear(), hoy.getMonth(), diaIni);
			LocalDate mesSig = hoy.plusMonths(1);
			fin = LocalDate.of(mesSig.getYear(), mesSig.getMonth(), Math.min(diaCorte, mesSig.lengthOfMonth()));
		}
		return new LocalDate[] { inicio, fin };
	}

	/** Fecha exacta del próximo corte */
	public LocalDate getFechaCorte() {
		return getPeriodoActual()[1];
	}

	/** Fecha exacta del límite de pago */
	public LocalDate getFechaLimitePago() {
		LocalDate corte = getFechaCorte();
		if (diaLimitePago >= diaCorte) {
			// Pago en el mismo mes del corte
			return LocalDate.of(corte.getYear(), corte.getMonth(), Math.min(diaLimitePago, corte.lengthOfMonth()));
		} else {
			// Pago en el mes siguiente al corte
			LocalDate mesSig = corte.plusMonths(1);
			return LocalDate.of(mesSig.getYear(), mesSig.getMonth(), Math.min(diaLimitePago, mesSig.lengthOfMonth()));
		}
	}

	/** Días restantes para el corte (negativo = ya pasó) */
	public long getDiasParaCorte() {
		return ChronoUnit.DAYS.between(LocalDate.now(), getFechaCorte());
	}

	/** Días restantes para el pago (negativo = vencido) */
	public long getDiasParaPago() {
		return ChronoUnit.DAYS.between(LocalDate.now(), getFechaLimitePago());
	}

	/** Etiqueta del período: "10/May – 09/Jun" */
	public String getPeriodoLabel() {
		DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MMM", new java.util.Locale("es", "MX"));
		LocalDate[] p = getPeriodoActual();
		return p[0].format(fmt) + " – " + p[1].format(fmt);
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

	public int getDiaCorte() {
		return diaCorte;
	}

	public void setDiaCorte(int d) {
		this.diaCorte = d;
	}

	public int getDiaLimitePago() {
		return diaLimitePago;
	}

	public void setDiaLimitePago(int d) {
		this.diaLimitePago = d;
	}

	public double getLimiteCredito() {
		return limiteCredito;
	}

	public void setLimiteCredito(double l) {
		this.limiteCredito = l;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String c) {
		this.color = c;
	}

	@Override
	public String toString() {
		return "💳 " + nombre;
	}
}