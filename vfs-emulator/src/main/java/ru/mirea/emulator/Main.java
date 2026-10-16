package ru.mirea.emulator;

import ru.mirea.emulator.gui.EmulatorGui;
import javax.swing.SwingUtilities;

/**
 * Главный класс приложения.
 */
public class Main {

    /**
     * Главный метод программы.
     *
     * @param args Аргументы командной строки
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EmulatorGui gui = new EmulatorGui();
            gui.setVisible(true);
        });
    }
}
