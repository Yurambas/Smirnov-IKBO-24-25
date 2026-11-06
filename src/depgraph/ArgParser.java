package depgraph;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Разбор и проверка параметров командной строки.
 */
public final class ArgParser {
    private static final String OPT_PACKAGE = "--package";
    private static final String OPT_REPO = "--repo";
    private static final String OPT_MODE = "--mode";
    private static final String OPT_OUTPUT = "--output";
    private static final String OPT_DEPTH = "--max-depth";
    private static final String OPT_FILTER = "--filter";
    private static final Set<String> VALUE_OPTIONS = Set.of(
            OPT_PACKAGE, OPT_REPO, OPT_MODE, OPT_OUTPUT, OPT_DEPTH, OPT_FILTER);
    private static final Map<String, Config.Action> FLAG_OPTIONS = Map.of(
            "--show-params", Config.Action.SHOW_PARAMS,
            "--direct", Config.Action.DIRECT,
            "--load-order", Config.Action.LOAD_ORDER);
    private static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z0-9_-]+");
    private static final String DEFAULT_MODE = "real";
    private static final String DEFAULT_OUTPUT = "graph.svg";
    private static final String DEFAULT_DEPTH = "3";
    private static final String SVG_SUFFIX = ".svg";
    private static final int MIN_DEPTH = 0;

    private ArgParser() {
    }

    /**
     * Разбирает аргументы командной строки.
     *
     * @param args аргументы программы
     * @return проверенные параметры
     * @throws AppException если параметры заданы неверно
     */
    public static Config parse(String[] args) throws AppException {
        Map<String, String> values = new HashMap<>();
        Set<Config.Action> actions = EnumSet.noneOf(Config.Action.class);
        Iterator<String> it = List.of(args).iterator();
        while (it.hasNext()) {
            String arg = it.next();
            if (FLAG_OPTIONS.containsKey(arg)) {
                actions.add(FLAG_OPTIONS.get(arg));
            } else if (VALUE_OPTIONS.contains(arg)) {
                if (!it.hasNext()) {
                    throw new AppException("у опции " + arg + " нет значения");
                }
                values.put(arg, it.next());
            } else {
                throw new AppException("неизвестная опция: " + arg);
            }
        }
        return build(values, actions);
    }

    /**
     * Собирает и проверяет итоговые параметры.
     *
     * @param values значения опций
     * @param actions включённые режимы
     * @return параметры программы
     * @throws AppException если значения некорректны
     */
    private static Config build(Map<String, String> values, Set<Config.Action> actions)
            throws AppException {
        String name = requireValue(values, OPT_PACKAGE);
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new AppException("недопустимое имя пакета: " + name);
        }
        Config.Mode mode = parseMode(values.getOrDefault(OPT_MODE, DEFAULT_MODE));
        String repo = requireValue(values, OPT_REPO);
        checkRepo(repo, mode);
        String output = values.getOrDefault(OPT_OUTPUT, DEFAULT_OUTPUT);
        checkOutput(output);
        int depth = parseDepth(values.getOrDefault(OPT_DEPTH, DEFAULT_DEPTH));
        String filter = values.getOrDefault(OPT_FILTER, "");
        if (!filter.isEmpty() && name.contains(filter)) {
            throw new AppException("подстрока фильтра содержится в имени самого пакета");
        }
        if (actions.contains(Config.Action.DIRECT) && actions.contains(Config.Action.LOAD_ORDER)) {
            throw new AppException("--direct и --load-order нельзя использовать вместе");
        }
        return new Config(name, repo, mode, output, depth, filter, actions);
    }

    /**
     * Возвращает обязательное непустое значение опции.
     *
     * @param values значения опций
     * @param key имя опции
     * @return значение
     * @throws AppException если опция не задана
     */
    private static String requireValue(Map<String, String> values, String key)
            throws AppException {
        String value = values.get(key);
        if (value == null || value.isBlank()) {
            throw new AppException("не задан обязательный параметр " + key);
        }
        return value;
    }

    /**
     * Преобразует текст в режим работы.
     *
     * @param text значение опции --mode
     * @return режим
     * @throws AppException если режим неизвестен
     */
    private static Config.Mode parseMode(String text) throws AppException {
        return switch (text.toLowerCase(Locale.ROOT)) {
            case "real" -> Config.Mode.REAL;
            case "test" -> Config.Mode.TEST;
            default -> throw new AppException("режим должен быть real или test: " + text);
        };
    }

    /**
     * Проверяет адрес репозитория или путь к тестовому файлу.
     *
     * @param repo URL или путь
     * @param mode режим работы
     * @throws AppException если значение не подходит режиму
     */
    private static void checkRepo(String repo, Config.Mode mode) throws AppException {
        if (mode == Config.Mode.REAL) {
            checkUrl(repo);
            return;
        }
        try {
            if (!Files.isRegularFile(Path.of(repo))) {
                throw new AppException("файл тестового репозитория не найден: " + repo);
            }
        } catch (InvalidPathException e) {
            throw new AppException("некорректный путь к файлу: " + repo);
        }
    }

    /**
     * Проверяет, что строка является http(s)-адресом.
     *
     * @param repo проверяемый адрес
     * @throws AppException если адрес некорректен
     */
    private static void checkUrl(String repo) throws AppException {
        try {
            URI uri = URI.create(repo);
            boolean http = "http".equals(uri.getScheme()) || "https".equals(uri.getScheme());
            if (!http || uri.getHost() == null) {
                throw new AppException("ожидается URL вида https://host: " + repo);
            }
        } catch (IllegalArgumentException e) {
            throw new AppException("некорректный URL: " + repo);
        }
    }

    /**
     * Проверяет имя выходного файла.
     *
     * @param output имя файла изображения
     * @throws AppException если расширение не .svg или нет каталога
     */
    private static void checkOutput(String output) throws AppException {
        if (!output.endsWith(SVG_SUFFIX)) {
            throw new AppException("файл изображения должен иметь расширение .svg");
        }
        Path parent = Path.of(output).toAbsolutePath().getParent();
        if (parent == null || !Files.isDirectory(parent)) {
            throw new AppException("каталог для файла не существует: " + output);
        }
    }

    /**
     * Разбирает максимальную глубину анализа.
     *
     * @param text значение опции --max-depth
     * @return неотрицательная глубина
     * @throws AppException если это не неотрицательное целое
     */
    private static int parseDepth(String text) throws AppException {
        try {
            int depth = Integer.parseInt(text.trim());
            if (depth < MIN_DEPTH) {
                throw new AppException("глубина не может быть отрицательной: " + text);
            }
            return depth;
        } catch (NumberFormatException e) {
            throw new AppException("глубина должна быть целым числом: " + text);
        }
    }
}
