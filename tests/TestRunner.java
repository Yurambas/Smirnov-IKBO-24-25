package depgraph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Простой запуск проверок без внешних библиотек.
 */
public final class TestRunner {
    private static final String CYCLE_REPO =
            "A: B C\nB: D\nC: D E\nD:\nE: A\n";
    private static boolean allPassed = true;

    private TestRunner() {
    }

    /**
     * Запускает все проверки и завершает процесс с кодом ошибки при неудаче.
     *
     * @param args не используются
     * @throws Exception при непредвиденной ошибке
     */
    public static void main(String[] args) throws Exception {
        Path repo = Files.createTempFile("repo", ".txt");
        Files.writeString(repo, CYCLE_REPO);
        testParser(repo);
        testBuilder(repo);
        testLoadOrder(repo);
        testDot(repo);
        testIndexParsing();
        Files.deleteIfExists(repo);
        System.out.println(allPassed ? "ВСЕ ТЕСТЫ ПРОЙДЕНЫ" : "ЕСТЬ ОШИБКИ");
        if (!allPassed) {
            System.exit(1);
        }
    }

    /**
     * Фиксирует результат одной проверки.
     *
     * @param name название проверки
     * @param condition результат
     */
    private static void check(String name, boolean condition) {
        System.out.println((condition ? "ok   " : "FAIL ") + name);
        allPassed &= condition;
    }

    /**
     * Проверяет, что разбор аргументов отклоняет заданные аргументы.
     *
     * @param args аргументы командной строки
     * @return true, если выброшена AppException
     */
    private static boolean rejects(String... args) {
        try {
            ArgParser.parse(args);
            return false;
        } catch (AppException e) {
            return true;
        }
    }

    /**
     * Проверки разбора параметров командной строки.
     *
     * @param repo путь к тестовому репозиторию
     * @throws AppException при ошибке разбора корректных аргументов
     */
    private static void testParser(Path repo) throws AppException {
        String file = repo.toString();
        Config ok = ArgParser.parse(new String[] {"--package", "A", "--repo", file,
            "--mode", "test", "--max-depth", "2", "--filter", "Z"});
        check("параметры разобраны", "A".equals(ok.packageName()) && ok.maxDepth() == 2);
        check("нет --package", rejects("--repo", file, "--mode", "test"));
        check("нет --repo", rejects("--package", "A", "--mode", "test"));
        check("неизвестная опция", rejects("--bogus"));
        check("опция без значения", rejects("--package"));
        check("плохой режим", rejects("--package", "A", "--repo", file, "--mode", "x"));
        check("глубина не число", rejects("--package", "A", "--repo", file,
                "--mode", "test", "--max-depth", "abc"));
        check("глубина отрицательная", rejects("--package", "A", "--repo", file,
                "--mode", "test", "--max-depth", "-1"));
        check("нет тестового файла", rejects("--package", "A", "--repo", "/no/file",
                "--mode", "test"));
        check("плохой URL", rejects("--package", "serde", "--repo", "not a url"));
        check("не .svg", rejects("--package", "A", "--repo", file, "--mode", "test",
                "--output", "g.png"));
        check("фильтр совпадает с пакетом", rejects("--package", "A", "--repo", file,
                "--mode", "test", "--filter", "A"));
    }

    /**
     * Читает граф из тестового файла.
     *
     * @param repo путь к файлу
     * @param depth глубина
     * @param filter фильтр
     * @return построенный граф
     * @throws AppException при ошибке чтения
     */
    private static Graph graphOf(Path repo, int depth, String filter) throws AppException {
        return GraphBuilder.build(TestRepository.load(repo), "A", depth, filter);
    }

    /**
     * Проверки построения графа: глубина, фильтр, цикл.
     *
     * @param repo путь к тестовому репозиторию
     * @throws AppException при ошибке построения
     */
    private static void testBuilder(Path repo) throws AppException {
        Graph full = graphOf(repo, 10, "");
        check("все вершины", full.nodes().equals(Set.of("A", "B", "C", "D", "E")));
        check("цикл: ребро E -> A", full.dependenciesOf("E").contains("A"));
        Graph shallow = graphOf(repo, 1, "");
        check("глубина 1", shallow.nodes().equals(Set.of("A", "B", "C")));
        check("глубина 0", graphOf(repo, 0, "").nodes().equals(Set.of("A")));
        Graph filtered = graphOf(repo, 10, "D");
        check("фильтр D", filtered.nodes().equals(Set.of("A", "B", "C", "E")));
        boolean missing = false;
        try {
            GraphBuilder.build(TestRepository.load(repo), "Q", 1, "");
        } catch (AppException e) {
            missing = true;
        }
        check("пакет не найден", missing);
    }

    /**
     * Проверка порядка загрузки.
     *
     * @param repo путь к тестовому репозиторию
     * @throws AppException при ошибке построения
     */
    private static void testLoadOrder(Path repo) throws AppException {
        List<String> order = LoadOrder.compute(graphOf(repo, 10, ""));
        check("порядок загрузки", order.equals(List.of("D", "B", "E", "C", "A")));
    }

    /**
     * Проверка текстового представления Graphviz.
     *
     * @param repo путь к тестовому репозиторию
     * @throws AppException при ошибке построения
     */
    private static void testDot(Path repo) throws AppException {
        String dot = DotWriter.toDot(graphOf(repo, 10, ""));
        check("DOT: ребро", dot.contains("\"A\" -> \"B\";"));
        check("DOT: заголовок", dot.startsWith("digraph \"A\" {"));
        check("имя .dot", "g.dot".equals(DotWriter.dotName("g.svg")));
    }

    /**
     * Строит JSON-описание одной зависимости индекса.
     *
     * @param name имя
     * @param kind вид зависимости
     * @param optional признак опциональности
     * @return JSON-объект
     */
    private static String dep(String name, String kind, boolean optional) {
        return "{\"name\":\"" + name + "\",\"req\":\"^1\",\"features\":[\"std\"],"
                + "\"optional\":" + optional + ",\"default_features\":true,"
                + "\"target\":null,\"kind\":\"" + kind + "\"}";
    }

    /**
     * Проверки разбора строки индекса Cargo.
     *
     * @throws AppException при ошибке разбора
     */
    private static void testIndexParsing() throws AppException {
        String line = "{\"name\":\"foo\",\"vers\":\"1.0.0\",\"deps\":["
                + dep("bar", "normal", false) + "," + dep("baz", "dev", false) + ","
                + dep("opt", "normal", true) + "],\"cksum\":\"x\",\"features\":{},"
                + "\"yanked\":false}";
        check("индекс: только обычные",
                CratesIndexRepository.parseDependencies(line).equals(List.of("bar")));
        check("путь: длинное имя", "se/rd/serde".equals(CratesIndexRepository.indexPath("serde")));
        check("путь: 1 буква", "1/a".equals(CratesIndexRepository.indexPath("a")));
        check("путь: 2 буквы", "2/ab".equals(CratesIndexRepository.indexPath("ab")));
        check("путь: 3 буквы", "3/a/abc".equals(CratesIndexRepository.indexPath("abc")));
        String body = line.replace("1.0.0", "0.9.0") + "\n" + line + "\n";
        check("последняя версия", CratesIndexRepository.latestRelease(body).contains("1.0.0"));
    }
}
