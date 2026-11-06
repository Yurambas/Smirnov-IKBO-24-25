package src;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Графический интерфейс эмулятора на базе Swing (Этап 1-3).
 */
public class Main {
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 520;
    private static final int GAP = 5;
    private static final int PANEL_PADDING = 8;
    private static final int FONT_SIZE = 14;
    private static final String FONT_NAME = "Consolas";
    private static final String VFS_OPTION = "--vfs";
    private static final String SCRIPT_OPTION = "--script";
    private static final Color PANEL_COLOR = new Color(30, 30, 30);
    private static final Color OUTPUT_COLOR = new Color(25, 25, 25);
    private static final Color OUTPUT_TEXT_COLOR = new Color(210, 210, 210);
    private static final Color INPUT_COLOR = new Color(45, 45, 45);
    private static final Color PROMPT_COLOR = new Color(76, 175, 80);
    private static final Color OUTPUT_BORDER_COLOR = new Color(60, 60, 60);
    private static final Color INPUT_BORDER_COLOR = new Color(70, 70, 70);

    private JFrame frame;
    private JTextArea textArea;
    private JTextField inputField;
    private JLabel promptLabel;
    private ShellEmulator emulator;

    /**
     * Создаёт окно эмулятора и запускает эмулятор.
     *
     * @param vfsPath путь к XML-файлу виртуальной файловой системы (или null)
     * @param scriptPath путь к стартовому скрипту (или null)
     */
    public Main(String vfsPath, String scriptPath) {
        initUI(vfsPath, scriptPath);
    }

    /**
     * Собирает окно, подключает эмулятор и выполняет его инициализацию.
     *
     * @param vfsPath путь к XML-файлу виртуальной файловой системы (или null)
     * @param scriptPath путь к стартовому скрипту (или null)
     */
    private void initUI(String vfsPath, String scriptPath) {
        frame = new JFrame(ShellEmulator.getWindowTitle());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        frame.setLocationRelativeTo(null);
        frame.setContentPane(createMainPanel());

        emulator = new ShellEmulator(
                vfsPath,
                scriptPath,
                this::appendOutput,
                () -> SwingUtilities.invokeLater(() -> frame.dispose())
        );
        inputField.addKeyListener(createEnterListener());

        frame.setVisible(true);
        inputField.requestFocusInWindow();
        emulator.init();
        updatePrompt();
    }

    /**
     * Создаёт главную панель: область вывода сверху и строка ввода снизу.
     *
     * @return готовая панель
     */
    private JPanel createMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(GAP, GAP));
        mainPanel.setBackground(PANEL_COLOR);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(
                PANEL_PADDING, PANEL_PADDING, PANEL_PADDING, PANEL_PADDING));
        mainPanel.add(createOutputPane(), BorderLayout.CENTER);
        mainPanel.add(createBottomPanel(), BorderLayout.SOUTH);
        return mainPanel;
    }

    /**
     * Создаёт область вывода с прокруткой.
     *
     * @return панель прокрутки с текстовой областью
     */
    private JScrollPane createOutputPane() {
        textArea = new JTextArea();
        textArea.setBackground(OUTPUT_COLOR);
        textArea.setForeground(OUTPUT_TEXT_COLOR);
        textArea.setCaretColor(Color.WHITE);
        textArea.setFont(new Font(FONT_NAME, Font.PLAIN, FONT_SIZE));
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(OUTPUT_BORDER_COLOR));
        return scrollPane;
    }

    /**
     * Создаёт нижнюю панель с приглашением и полем ввода.
     *
     * @return нижняя панель
     */
    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout(GAP, 0));
        bottomPanel.setBackground(PANEL_COLOR);

        promptLabel = new JLabel("/$ ");
        promptLabel.setForeground(PROMPT_COLOR);
        promptLabel.setFont(new Font(FONT_NAME, Font.BOLD, FONT_SIZE));
        bottomPanel.add(promptLabel, BorderLayout.WEST);

        inputField = createInputField();
        bottomPanel.add(inputField, BorderLayout.CENTER);
        return bottomPanel;
    }

    /**
     * Создаёт оформленное поле ввода команд.
     *
     * @return поле ввода
     */
    private JTextField createInputField() {
        JTextField field = new JTextField();
        field.setBackground(INPUT_COLOR);
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        field.setFont(new Font(FONT_NAME, Font.PLAIN, FONT_SIZE));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INPUT_BORDER_COLOR),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));
        return field;
    }

    /**
     * Создаёт обработчик клавиши Enter для поля ввода.
     *
     * @return слушатель клавиатуры
     */
    private KeyAdapter createEnterListener() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    submitCommand();
                }
            }
        };
    }

    /**
     * Берёт команду из поля ввода, выводит её и передаёт эмулятору.
     */
    private void submitCommand() {
        String cmd = inputField.getText();
        inputField.setText("");
        appendOutput(promptLabel.getText() + cmd + "\n");
        emulator.executeCommand(cmd);
        if (emulator.isRunning()) {
            updatePrompt();
        }
    }

    /**
     * Добавляет текст в область вывода и прокручивает её вниз.
     *
     * @param text добавляемый текст
     */
    private void appendOutput(String text) {
        SwingUtilities.invokeLater(() -> {
            textArea.append(text);
            textArea.setCaretPosition(textArea.getDocument().getLength());
        });
    }

    /**
     * Обновляет приглашение командной строки текущим путём в VFS.
     */
    private void updatePrompt() {
        promptLabel.setText(emulator.getVfs().getCurrentPath() + "$ ");
    }

    /**
     * Точка входа: разбирает аргументы и запускает окно.
     *
     * @param args аргументы командной строки: --vfs путь и --script путь
     */
    public static void main(String[] args) {
        String vfsPath = null;
        String scriptPath = null;

        Iterator<String> it = Arrays.asList(args).iterator();
        while (it.hasNext()) {
            String arg = it.next();
            if (VFS_OPTION.equals(arg) && it.hasNext()) {
                vfsPath = it.next();
            } else if (SCRIPT_OPTION.equals(arg) && it.hasNext()) {
                scriptPath = it.next();
            }
        }

        final String finalVfs = vfsPath;
        final String finalScript = scriptPath;
        SwingUtilities.invokeLater(() -> new Main(finalVfs, finalScript));
    }
}
