package ru.mirea.emulator.gui;

import ru.mirea.emulator.parser.CommandParser;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Окно графического интерфейса эмулятора терминала.
 */
public class EmulatorGui extends JFrame {

    private static final Color BG_MAIN = new Color(24, 24, 24);
    private static final Color BG_INPUT = new Color(36, 36, 36);
    private static final Font MONO = new Font("Monospaced", Font.PLAIN, 14);

    private final JTextArea terminalArea;
    private final JTextField commandInput;
    private final CommandParser parser;
    private final String promptString;

    /**
     * Конструктор графического интерфейса.
     */
    public EmulatorGui() {
        this.parser = new CommandParser();
        this.promptString = "vfs$ ";

        initFrameWindow();

        this.terminalArea = new JTextArea();
        this.commandInput = new JTextField();

        setupComponents();
        appendTerminalText("Эмулятор оболочки запущен (Этап 1: REPL).\n");
        appendTerminalText("Доступные команды: ls, cd, exit\n\n");
    }

    /**
     * Инициализирует параметры окна и формирует заголовок на основе данных ОС.
     */
    private void initFrameWindow() {
        setTitle(resolveTitleString());
        setSize(1125, 720);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    /**
     * Формирует заголовок окна: Эмулятор [username@hostname].
     *
     * @return Текст заголовка
     */
    private String resolveTitleString() {
        String username = System.getProperty("user.name", "unknown");
        String hostname = "localhost";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ignored) {
            hostname = "localhost";
        }
        return String.format("Эмулятор [%s@%s]", username, hostname);
    }

    /**
     * Настраивает элементы интерфейса терминала.
     */
    private void setupComponents() {
        terminalArea.setEditable(false);
        terminalArea.setBackground(BG_MAIN);
        terminalArea.setForeground(new Color(220, 220, 220));
        terminalArea.setFont(MONO);
        terminalArea.setMargin(new Insets(6, 6, 6, 6));

        JScrollPane scrollPane = new JScrollPane(terminalArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        setupInputField();

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        getContentPane().add(createBottomPanel(), BorderLayout.SOUTH);
    }

    /**
     * Настраивает поле ввода команд.
     */
    private void setupInputField() {
        commandInput.setBackground(BG_INPUT);
        commandInput.setForeground(Color.WHITE);
        commandInput.setCaretColor(Color.WHITE);
        commandInput.setFont(MONO);
        commandInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 60)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        commandInput.addActionListener(e -> handleCommandSubmit());
    }

    /**
     * Создает нижнюю панель с приглашением и полем ввода.
     *
     * @return Панель ввода
     */
    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JLabel promptLabel = new JLabel(" " + promptString);
        promptLabel.setForeground(new Color(100, 220, 100));
        promptLabel.setFont(MONO.deriveFont(Font.BOLD));
        promptLabel.setBackground(BG_INPUT);
        promptLabel.setOpaque(true);

        bottomPanel.add(promptLabel, BorderLayout.WEST);
        bottomPanel.add(commandInput, BorderLayout.CENTER);
        return bottomPanel;
    }

    /**
     * Обрабатывает ввод команды из текстового поля.
     */
    private void handleCommandSubmit() {
        String rawInput = commandInput.getText();
        commandInput.setText("");
        if (rawInput == null || rawInput.trim().isEmpty()) {
            return;
        }

        appendTerminalText(promptString + rawInput + "\n");
        List<String> tokens = parser.parse(rawInput);
        if (tokens.isEmpty()) {
            return;
        }

        executeCommand(tokens);
    }

    /**
     * Выполняет введенную команду или команду-заглушку.
     *
     * @param tokens Список токенов (команда + аргументы)
     */
    private void executeCommand(List<String> tokens) {
        String cmd = tokens.get(0);
        List<String> args = tokens.subList(1, tokens.size());

        switch (cmd) {
            case "exit":
                System.exit(0);
                break;
            case "ls":
                appendTerminalText(String.format("[stub] ls args: %s\n", args));
                break;
            case "cd":
                appendTerminalText(String.format("[stub] cd args: %s\n", args));
                break;
            default:
                appendTerminalText(String.format("Команда не найдена: %s\n", cmd));
                break;
        }
    }

    /**
     * Добавляет текст в терминал и прокручивает его вниз.
     *
     * @param text Выводимый текст
     */
    private void appendTerminalText(String text) {
        terminalArea.append(text);
        terminalArea.setCaretPosition(terminalArea.getDocument().getLength());
    }
}
