package src;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Логика эмулятора командной строки, парсинг аргументов и выполнение команд/скриптов.
 */
public class ShellEmulator {
    private static final int MIN_QUOTED_LENGTH = 2;
    private static final String NOT_SET = "не задан";
    private final String vfsPath;
    private final String scriptPath;
    private final Consumer<String> outputCallback;
    private final Runnable exitCallback;
    private final VirtualFileSystem vfs;
    private boolean isRunning;

    /**
     * Создаёт эмулятор.
     *
     * @param vfsPath путь к XML-файлу VFS (или null)
     * @param scriptPath путь к стартовому скрипту (или null)
     * @param outputCallback приёмник вывода (null — вывод в консоль)
     * @param exitCallback действие при команде exit (null — ничего не делать)
     */
    public ShellEmulator(String vfsPath, String scriptPath,
                         Consumer<String> outputCallback, Runnable exitCallback) {
        this.vfsPath = vfsPath;
        this.scriptPath = scriptPath;
        this.outputCallback = outputCallback != null ? outputCallback : System.out::print;
        this.exitCallback = exitCallback != null ? exitCallback : () -> {};
        this.vfs = new VirtualFileSystem();
        this.isRunning = true;
    }

    /**
     * Выводит параметры, монтирует VFS и выполняет стартовый скрипт.
     */
    public void init() {
        outputCallback.accept("=== ОТЛАДОЧНЫЙ ВЫВОД ПАРАМЕТРОВ ===\n");
        outputCallback.accept("Путь к VFS: " + (vfsPath != null ? vfsPath : NOT_SET) + "\n");
        outputCallback.accept("Путь к стартовому скрипту: " + (scriptPath != null ? scriptPath : NOT_SET) + "\n");
        outputCallback.accept("=====================================\n\n");

        if (vfsPath != null && !vfsPath.isEmpty()) {
            try {
                vfs.loadFromXml(vfsPath);
                outputCallback.accept("[VFS] Файловая система успешно смонтирована из " + vfsPath + "\n");
            } catch (Exception e) {
                outputCallback.accept("[VFS ОШИБКА]: " + e.getMessage() + "\n");
            }
        }

        if (scriptPath != null && !scriptPath.isEmpty()) {
            runScript(scriptPath);
        }
    }

    /**
     * Формирует заголовок окна вида «Эмулятор [пользователь@хост]».
     *
     * @return заголовок окна
     */
    public static String getWindowTitle() {
        String username = System.getProperty("user.name");
        String hostname = "localhost";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
        }
        return "Эмулятор [" + username + "@" + hostname + "]";
    }

    /**
     * Подставляет переменные окружения вида $NAME и ${NAME}.
     *
     * @param input строка с переменными
     * @return строка с подставленными значениями (неизвестные заменяются пустотой)
     */
    public String expandEnvVars(String input) {
        Pattern pattern = Pattern.compile("\\$(?:([A-Za-z0-9_]+)|\\{([A-Za-z0-9_]+)\\})");
        Matcher matcher = pattern.matcher(input);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String varName = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            String val = System.getenv(varName);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(val != null ? val : ""));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Выполняет одну команду эмулятора.
     *
     * @param line строка с командой
     * @return true, если команда выполнена успешно, false — если команда неизвестна
     */
    public boolean executeCommand(String line) {
        String clean = line.trim();
        if (clean.isEmpty()) return true;

        String expanded = expandEnvVars(clean);
        List<String> tokens = parseArgs(expanded);
        if (tokens.isEmpty()) return true;

        String cmd = tokens.get(0);
        List<String> args = tokens.subList(1, tokens.size());

        switch (cmd) {
            case "exit":
                outputCallback.accept("Выход из эмулятора...\n");
                isRunning = false;
                exitCallback.run();
                return true;
            case "ls":
            case "cd":
                outputCallback.accept("[STUB] Команда: " + cmd + " " + args + "\n");
                return true;
            default:
                outputCallback.accept("Ошибка: неизвестная команда '" + cmd + "'\n");
                return false;
        }
    }

    /**
     * Выполняет стартовый скрипт построчно; пустые строки и строки с # пропускаются.
     * Выполнение прекращается на первой ошибке (требование Варианта 22).
     *
     * @param path путь к файлу скрипта
     */
    private void runScript(String path) {
        File f = new File(path);
        if (!f.exists()) {
            outputCallback.accept("[СКРИПТ ОШИБКА]: Файл не найден: " + path + "\n");
            return;
        }

        outputCallback.accept("--- Выполнение стартового скрипта: " + path + " ---\n");
        try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                outputCallback.accept(vfs.getCurrentPath() + "$ " + trimmed + "\n");
                boolean success = executeCommand(trimmed);

                if (!success) {
                    outputCallback.accept("[СКРИПТ ПРЕРВАН]: Ошибка на строке " + lineNum + ". Остановка.\n");
                    break;
                }
                if (!isRunning) break;
            }
        } catch (Exception e) {
            outputCallback.accept("[СКРИПТ ОШИБКА]: " + e.getMessage() + "\n");
        }
        outputCallback.accept("--- Завершение работы скрипта ---\n\n");
    }

    /**
     * Разбивает строку на аргументы; текст в двойных кавычках считается одним аргументом.
     *
     * @param line строка команды
     * @return список аргументов без внешних кавычек
     */
    private List<String> parseArgs(String line) {
        List<String> list = new ArrayList<>();
        Matcher m = Pattern.compile("([^\\s\"]+|\"[^\"]*\")").matcher(line);
        while (m.find()) {
            String match = m.group();
            if (match.startsWith("\"") && match.endsWith("\"") && match.length() >= MIN_QUOTED_LENGTH) {
                match = match.substring(1, match.length() - 1);
            }
            list.add(match);
        }
        return list;
    }

    /**
     * Возвращает виртуальную файловую систему.
     *
     * @return VFS эмулятора
     */
    public VirtualFileSystem getVfs() {
        return vfs;
    }

    /**
     * Показывает, работает ли эмулятор (false после команды exit).
     *
     * @return true, если эмулятор не завершён
     */
    public boolean isRunning() {
        return isRunning;
    }
}
