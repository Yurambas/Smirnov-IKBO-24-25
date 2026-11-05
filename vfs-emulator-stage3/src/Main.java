package src;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Графический интерфейс эмулятора на базе Swing (Этап 1-3).
 */
public class Main {
    private JFrame frame;
    private JTextArea textArea;
    private JTextField inputField;
    private JLabel promptLabel;
    private ShellEmulator emulator;

    public Main(String vfsPath, String scriptPath) {
        initUI(vfsPath, scriptPath);
    }

    private void initUI(String vfsPath, String scriptPath) {
        frame = new JFrame(ShellEmulator.getWindowTitle());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 520);
        frame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBackground(new Color(30, 30, 30));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        textArea = new JTextArea();
        textArea.setBackground(new Color(25, 25, 25));
        textArea.setForeground(new Color(210, 210, 210));
        textArea.setCaretColor(Color.WHITE);
        textArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60)));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 0));
        bottomPanel.setBackground(new Color(30, 30, 30));

        promptLabel = new JLabel("/$ ");
        promptLabel.setForeground(new Color(76, 175, 80));
        promptLabel.setFont(new Font("Consolas", Font.BOLD, 14));
        bottomPanel.add(promptLabel, BorderLayout.WEST);

        inputField = new JTextField();
        inputField.setBackground(new Color(45, 45, 45));
        inputField.setForeground(Color.WHITE);
        inputField.setCaretColor(Color.WHITE);
        inputField.setFont(new Font("Consolas", Font.PLAIN, 14));
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 70, 70)),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));

        bottomPanel.add(inputField, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        frame.setContentPane(mainPanel);

        emulator = new ShellEmulator(
                vfsPath,
                scriptPath,
                this::appendOutput,
                () -> SwingUtilities.invokeLater(() -> frame.dispose())
        );

        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String cmd = inputField.getText();
                    inputField.setText("");
                    appendOutput(promptLabel.getText() + cmd + "\n");
                    emulator.executeCommand(cmd);
                    if (emulator.isRunning()) {
                        updatePrompt();
                    }
                }
            }
        });

        frame.setVisible(true);
        inputField.requestFocusInWindow();

        emulator.init();
        updatePrompt();
    }

    private void appendOutput(String text) {
        SwingUtilities.invokeLater(() -> {
            textArea.append(text);
            textArea.setCaretPosition(textArea.getDocument().getLength());
        });
    }

    private void updatePrompt() {
        promptLabel.setText(emulator.getVfs().getCurrentPath() + "$ ");
    }

    public static void main(String[] args) {
        String vfsPath = null;
        String scriptPath = null;

        for (int i = 0; i < args.length; i++) {
            if ("--vfs".equals(args[i]) && i + 1 < args.length) {
                vfsPath = args[++i];
            } else if ("--script".equals(args[i]) && i + 1 < args.length) {
                scriptPath = args[++i];
            }
        }

        final String finalVfs = vfsPath;
        final String finalScript = scriptPath;
        SwingUtilities.invokeLater(() -> new Main(finalVfs, finalScript));
    }
}
