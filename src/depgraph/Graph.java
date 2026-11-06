package depgraph;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Ориентированный граф зависимостей: ребро A -> B означает «A зависит от B».
 */
public final class Graph {
    private final String root;
    private final Map<String, Set<String>> edges = new LinkedHashMap<>();

    /**
     * Создаёт граф с одной вершиной.
     *
     * @param root имя корневого пакета
     */
    public Graph(String root) {
        this.root = root;
        addNode(root);
    }

    /**
     * Добавляет вершину, если её ещё нет.
     *
     * @param name имя пакета
     */
    public void addNode(String name) {
        edges.computeIfAbsent(name, key -> new LinkedHashSet<>());
    }

    /**
     * Добавляет ребро «from зависит от to».
     *
     * @param from зависящий пакет
     * @param to пакет-зависимость
     */
    public void addEdge(String from, String to) {
        addNode(from);
        addNode(to);
        edges.get(from).add(to);
    }

    /**
     * Возвращает корневой пакет.
     *
     * @return имя корня
     */
    public String root() {
        return root;
    }

    /**
     * Возвращает все вершины в порядке добавления.
     *
     * @return множество имён
     */
    public Set<String> nodes() {
        return Collections.unmodifiableSet(edges.keySet());
    }

    /**
     * Возвращает прямые зависимости вершины.
     *
     * @param name имя пакета
     * @return множество имён зависимостей
     */
    public Set<String> dependenciesOf(String name) {
        return Collections.unmodifiableSet(edges.getOrDefault(name, Set.of()));
    }
}
