package utilidades.modulos.vida.model;

public class Personaje {
	private String nombre;
	private int hp;
	private int hpMax;

	public Personaje(String nombre) {
		this.nombre = nombre;
		this.hp = 100;
		this.hpMax = 100;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String n) {
		this.nombre = n;
	}

	public int getHp() {
		return hp;
	}

	public void setHp(int hp) {
		this.hp = Math.max(0, Math.min(hpMax, hp));
	}

	public int getHpMax() {
		return hpMax;
	}

	public void setHpMax(int hpMax) {
		this.hpMax = hpMax;
	}

	public float getHpPct() {
		return hpMax > 0 ? (float) hp / hpMax : 0;
	}

	public void dañar(int cantidad) {
		hp = Math.max(0, hp - cantidad);
		if (hp == 0)
			hp = 1; // nunca llegar a 0
	}

	public void curar(int cantidad) {
		hp = Math.min(hpMax, hp + cantidad);
	}
}