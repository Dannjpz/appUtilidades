package utilidades;

import javax.swing.*;

public class Main {
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (Exception ignored) {
		}

		UIManager.put("OptionPane.background", new java.awt.Color(255, 255, 255));
		UIManager.put("Panel.background", new java.awt.Color(255, 255, 255));
		UIManager.put("OptionPane.messageForeground", new java.awt.Color(30, 30, 30));
		UIManager.put("Button.background", new java.awt.Color(240, 240, 240));
		UIManager.put("Button.foreground", new java.awt.Color(30, 30, 30));
		UIManager.put("TextField.background", new java.awt.Color(255, 255, 255));
		UIManager.put("TextField.foreground", new java.awt.Color(30, 30, 30));
		UIManager.put("ComboBox.background", new java.awt.Color(255, 255, 255));
		UIManager.put("ComboBox.foreground", new java.awt.Color(30, 30, 30));
		UIManager.put("Label.foreground", new java.awt.Color(30, 30, 30));

		SwingUtilities.invokeLater(() -> {
			MainFrame frame = new MainFrame();
			frame.setVisible(true);
		});
	}
}