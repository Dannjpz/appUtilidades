package utilidades.modulos.wow;

import javax.swing.*;

import utilidades.modulos.wow.cds.CdsPanel;
import utilidades.modulos.wow.cds.modelo.TrackerData;
import utilidades.modulos.wow.cds.utiles.DataManager;
import utilidades.modulos.wow.guias.GuiasPanel;

import java.awt.*;
import java.io.IOException;

public class WowPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final CdsPanel cdsPanel;
	private final GuiasPanel guiasPanel;

	public WowPanel() {
		setLayout(new BorderLayout());
		setBackground(new Color(245, 245, 245));

		TrackerData data;
		try {
			data = DataManager.cargar();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "No se pudo cargar datos: " + e.getMessage());
			data = new TrackerData();
		}

		cdsPanel = new CdsPanel(data);
		guiasPanel = new GuiasPanel();

		JTabbedPane subTabs = new JTabbedPane();
		subTabs.setFont(new Font("SansSerif", Font.PLAIN, 12));
		subTabs.setBackground(new Color(245, 245, 245));

		subTabs.addTab("CD's", cdsPanel);
		subTabs.addTab("📖 Guías", guiasPanel);
		// Futuros submódulos:
		// subTabs.addTab("Talentos", new TalentosPanel());
		// subTabs.addTab("Loot", new LootPanel());

		add(subTabs, BorderLayout.CENTER);
	}

	public void guardar() {
		cdsPanel.guardar();
		guiasPanel.guardar();
	}
}