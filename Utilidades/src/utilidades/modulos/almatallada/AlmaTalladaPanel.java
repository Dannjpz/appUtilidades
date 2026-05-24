package utilidades.modulos.almatallada;

import javax.swing.*;

import utilidades.modulos.almatallada.calculadora.CalculadoraPanel;
import utilidades.modulos.almatallada.disenios.DiseniosPanel;
import utilidades.modulos.almatallada.velocidades.VelocidadesPanel;

import java.awt.*;

/**
 * Módulo Alma Tallada — contenedor de submódulos.
 */
public class AlmaTalladaPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public AlmaTalladaPanel() {
		setLayout(new BorderLayout());
		setBackground(new Color(245, 245, 247));

		JTabbedPane subTabs = new JTabbedPane();
		subTabs.setFont(new Font("SansSerif", Font.PLAIN, 12));
		subTabs.setBackground(new Color(245, 245, 247));

		subTabs.addTab("Calculadora de Costos", new CalculadoraPanel());
		subTabs.addTab("Tabla de Velocidades", new VelocidadesPanel());
		subTabs.addTab("Mis Diseños", new DiseniosPanel());

		add(subTabs, BorderLayout.CENTER);
	}
}