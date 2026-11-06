package depgraph;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Тестовый репозиторий из текстового файла. Формат строки: {@code A: B C}.
 * Имена пакетов — большие латинские буквы, {@code #} начинает комментарий.
 */
public final class TestRepository implements DependencySource {
    private static final Pattern LINE = Pattern.compile("([A-Z]+)\\s*:\\s*([A-Z\\s]*)");
    private static final Pattern COMMENT = Pattern.compile("#.*$");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private final Map<String, Set<String>> packages;

    /**
     * Создаёт репозиторий из готовой таблицы зависимостей.
     *
     * @param packages имя пакета -> его зависимости
     */
    private TestRepository(Map<String, Set<String>> packages) {
        this.packages = packages;
    }

    /**
     * Загружает тестовый репозиторий из файла.
     *
     * @param file путь к файлу описания графа
     * @return репозиторий
     * @throws AppException если файл нельзя прочитать или он неверного формата
     */
    public static TestRepository load(Path file) throws AppException {
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            return new TestRepository(parse(lines));
        } catch (IOException e) {
            throw new AppException("не удалось прочитать файл: " + e.getMessage());
        }
    }

    /**
     * Разбирает строки описания графа.
     *
     * @param lines строки файла
     * @return имя пакета -> его зависимости
     * @throws AppException если строка не соответствует формату
     */
    static Map<String, Set<String>> parse(List<String> lines) throws AppException {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        int number = 0;
        for (String raw : lines) {
            number++;
            String line = COMMENT.matcher(raw).replaceFirst("").strip();
            if (line.isEmpty()) {
                continue;
            }
            Matcher matcher = LINE.matcher(line);
            if (!matcher.matches()) {
                throw new AppException("строка " + number + ": ожидается формат 'A: B C'");
            }
            addLine(result, matcher.group(1), matcher.group(2));
        }
        return result;
    }

    /**
     * Добавляет одну строку описания в таблицу.
     *
     * @param table таблица зависимостей
     * @param name имя пакета
     * @param deps зависимости через пробел
     */
    private static void addLine(Map<String, Set<String>> table, String name, String deps) {
        Set<String> set = table.computeIfAbsent(name, key -> new LinkedHashSet<>());
        for (String dep : SPACES.split(deps.strip())) {
            if (!dep.isEmpty()) {
                set.add(dep);
                table.computeIfAbsent(dep, key -> new LinkedHashSet<>());
            }
        }
    }

    @Override
    public List<String> directDependencies(String name) throws AppException {
        Set<String> deps = packages.get(name);
        if (deps == null) {
            throw new AppException("пакет не найден в тестовом репозитории: " + name);
        }
        return List.copyOf(deps);
    }
}
