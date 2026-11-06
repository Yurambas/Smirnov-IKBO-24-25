package depgraph;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Реальный репозиторий Cargo: разреженный индекс (например, https://index.crates.io).
 * Для пакета берётся последняя не отозванная версия и её обычные зависимости.
 */
public final class CratesIndexRepository implements DependencySource {
    private static final String DEPS_KEY = "\"deps\":[";
    private static final String YANKED_FALSE = "\"yanked\":false";
    private static final String OPTIONAL_TRUE = "\"optional\":true";
    private static final String TARGET_SET = "\"target\":\"";
    private static final String KIND_NORMAL = "normal";
    private static final Pattern OBJECT = Pattern.compile("\\{[^{}]*}");
    private static final Pattern NAME = Pattern.compile("\"name\":\"([^\"]+)\"");
    private static final Pattern RENAMED = Pattern.compile("\"package\":\"([^\"]+)\"");
    private static final Pattern KIND = Pattern.compile("\"kind\":\"([^\"]+)\"");
    private static final int TIMEOUT_SECONDS = 15;
    private static final int NO_LEVEL = 0;
    private static final int SHORT_NAME_MAX = 2;
    private static final int THREE_LETTERS = 3;
    private static final int PREFIX_LENGTH = 2;

    private final String baseUrl;
    private final HttpClient client;

    /**
     * Создаёт клиент индекса.
     *
     * @param baseUrl адрес индекса, например https://index.crates.io
     */
    public CratesIndexRepository(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
    }

    @Override
    public List<String> directDependencies(String name) throws AppException {
        String body = download(baseUrl + "/" + indexPath(name), name);
        return parseDependencies(latestRelease(body));
    }

    /**
     * Строит путь к файлу пакета в индексе по правилам Cargo.
     *
     * @param name имя пакета
     * @return относительный путь, например se/rd/serde
     */
    static String indexPath(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        int length = lower.length();
        if (length <= SHORT_NAME_MAX) {
            return length + "/" + lower;
        }
        if (length == THREE_LETTERS) {
            return THREE_LETTERS + "/" + lower.charAt(0) + "/" + lower;
        }
        String first = lower.substring(0, PREFIX_LENGTH);
        String second = lower.substring(PREFIX_LENGTH, PREFIX_LENGTH + PREFIX_LENGTH);
        return first + "/" + second + "/" + lower;
    }

    /**
     * Скачивает файл индекса.
     *
     * @param url адрес файла
     * @param name имя пакета для сообщений об ошибках
     * @return содержимое файла
     * @throws AppException при сетевой ошибке или если пакета нет
     */
    private String download(String url, String name) throws AppException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .header("User-Agent", "depgraph-study-tool")
                .build();
        try {
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                throw new AppException("пакет не найден в репозитории: " + name);
            }
            if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                throw new AppException("репозиторий ответил кодом " + response.statusCode());
            }
            return response.body();
        } catch (IOException e) {
            throw new AppException("ошибка сети при обращении к " + url + " ("
                    + e.getClass().getSimpleName() + ")");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException("запрос прерван");
        }
    }

    /**
     * Выбирает последнюю не отозванную версию из файла индекса.
     *
     * @param body содержимое файла индекса (по версии в строке)
     * @return JSON-строка версии
     * @throws AppException если подходящих версий нет
     */
    static String latestRelease(String body) throws AppException {
        List<String> lines = body.lines().filter(l -> l.contains(YANKED_FALSE)).toList();
        if (lines.isEmpty()) {
            throw new AppException("в индексе нет доступных версий пакета");
        }
        return lines.get(lines.size() - 1);
    }

    /**
     * Извлекает имена обычных зависимостей из JSON-строки версии.
     *
     * @param line JSON-строка версии
     * @return список имён без повторов
     * @throws AppException если в строке нет списка зависимостей
     */
    static List<String> parseDependencies(String line) throws AppException {
        String array = extractDepsArray(line);
        Set<String> result = new LinkedHashSet<>();
        Matcher matcher = OBJECT.matcher(array);
        while (matcher.find()) {
            String object = matcher.group();
            if (isRequired(object)) {
                result.add(crateName(object));
            }
        }
        return new ArrayList<>(result);
    }

    /**
     * Вырезает содержимое массива deps с учётом вложенных скобок.
     *
     * @param line JSON-строка версии
     * @return текст внутри квадратных скобок массива
     * @throws AppException если массив не найден
     */
    private static String extractDepsArray(String line) throws AppException {
        int index = line.indexOf(DEPS_KEY);
        if (index < 0) {
            throw new AppException("в строке индекса нет списка зависимостей");
        }
        int start = index + DEPS_KEY.length();
        int level = 1;
        int pos = start;
        while (pos < line.length() && level > NO_LEVEL) {
            char symbol = line.charAt(pos);
            if (symbol == '[') {
                level++;
            } else if (symbol == ']') {
                level--;
            }
            pos++;
        }
        return line.substring(start, pos);
    }

    /**
     * Проверяет, что зависимость обычная, не опциональная и не платформенная.
     *
     * @param object JSON-объект зависимости
     * @return true, если зависимость нужно учитывать
     */
    private static boolean isRequired(String object) {
        String kind = firstGroup(KIND, object);
        boolean normal = kind == null || KIND_NORMAL.equals(kind);
        return normal && !object.contains(OPTIONAL_TRUE) && !object.contains(TARGET_SET);
    }

    /**
     * Возвращает настоящее имя пакета (с учётом переименования).
     *
     * @param object JSON-объект зависимости
     * @return имя пакета
     */
    private static String crateName(String object) {
        String renamed = firstGroup(RENAMED, object);
        return renamed != null ? renamed : firstGroup(NAME, object);
    }

    /**
     * Возвращает первую группу совпадения или null.
     *
     * @param pattern шаблон с одной группой
     * @param text исследуемый текст
     * @return текст группы либо null
     */
    private static String firstGroup(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }
}
