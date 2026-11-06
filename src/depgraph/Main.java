package depgraph;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Точка входа: инструмент визуализации графа зависимостей Cargo.
 */
public final class Main {
    private static final int EXIT_ERROR = 1;
    private static final String DEFAULT_HINT = "Подробнее об опциях см. README.md";

    private Main() {
    }

    /**
     * Запускает программу.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        try {
            run(args);
        } catch (AppException e) {
            System.err.println("Ошибка: " + e.getMessage());
            System.err.println(DEFAULT_HINT);
            System.exit(EXIT_ERROR);
        }
    }

    /**
     * Выполняет выбранный сценарий.
     *
     * @param args аргументы командной строки
     * @throws AppException при любой ошибке пользователя или данных
     */
    static void run(String[] args) throws AppException {
        Config config = ArgParser.parse(args);
        if (config.has(Config.Action.SHOW_PARAMS)) {
            printParams(config);
        }
        DependencySource source = createSource(config);
        if (config.has(Config.Action.DIRECT)) {
            printDirect(config, source);
            return;
        }
        Graph graph = GraphBuilder.build(source, config.packageName(),
                config.maxDepth(), config.filter());
        if (config.has(Config.Action.LOAD_ORDER)) {
            printOrder(LoadOrder.compute(graph));
            return;
        }
        visualize(graph, config.output());
    }

    /**
     * Создаёт источник данных в соответствии с режимом.
     *
     * @param config параметры программы
     * @return источник зависимостей
     * @throws AppException если тестовый файл не удалось прочитать
     */
    private static DependencySource createSource(Config config) throws AppException {
        if (config.mode() == Config.Mode.TEST) {
            return TestRepository.load(Path.of(config.repo()));
        }
        return new CratesIndexRepository(config.repo());
    }

    /**
     * Печатает параметры в формате ключ-значение.
     *
     * @param config параметры программы
     */
    private static void printParams(Config config) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("package", config.packageName());
        params.put("repo", config.repo());
        params.put("mode", config.mode().name().toLowerCase());
        params.put("output", config.output());
        params.put("max-depth", String.valueOf(config.maxDepth()));
        params.put("filter", config.filter());
        params.forEach((key, value) -> System.out.println(key + "=" + value));
    }

    /**
     * Печатает прямые зависимости пакета.
     *
     * @param config параметры программы
     * @param source источник зависимостей
     * @throws AppException если зависимости получить не удалось
     */
    private static void printDirect(Config config, DependencySource source)
            throws AppException {
        List<String> deps = source.directDependencies(config.packageName());
        System.out.println("Прямые зависимости пакета " + config.packageName() + ":");
        if (deps.isEmpty()) {
            System.out.println("  (нет)");
        }
        deps.forEach(dep -> System.out.println("  " + dep));
    }

    /**
     * Печатает порядок загрузки пакетов.
     *
     * @param order пакеты в порядке загрузки
     */
    private static void printOrder(List<String> order) {
        System.out.println("Порядок загрузки (зависимости раньше зависимых):");
        int number = 0;
        for (String name : order) {
            number++;
            System.out.println(number + ". " + name);
        }
    }

    /**
     * Печатает DOT-описание и сохраняет изображение.
     *
     * @param graph граф зависимостей
     * @param output имя SVG-файла
     * @throws AppException если файлы не удалось создать
     */
    private static void visualize(Graph graph, String output) throws AppException {
        String dot = DotWriter.toDot(graph);
        System.out.print(dot);
        DotWriter.save(dot, output);
        System.out.println("Изображение сохранено: " + output);
    }
}
