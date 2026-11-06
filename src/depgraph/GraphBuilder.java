package depgraph;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Построение графа зависимостей обходом в ширину (BFS) без рекурсии.
 */
public final class GraphBuilder {
    private static final int ROOT_DEPTH = 0;

    private GraphBuilder() {
    }

    /**
     * Строит граф зависимостей с учётом глубины и фильтра.
     * Циклы не приводят к повторному обходу: посещённые пакеты не добавляются в очередь,
     * но ребро к ним сохраняется.
     *
     * @param source источник данных о зависимостях
     * @param root имя корневого пакета
     * @param maxDepth максимальная глубина (корень имеет глубину 0)
     * @param filter подстрока; пакеты с ней в имени исключаются
     * @return граф зависимостей
     * @throws AppException если не удалось получить зависимости пакета
     */
    public static Graph build(DependencySource source, String root, int maxDepth,
                              String filter) throws AppException {
        Graph graph = new Graph(root);
        Map<String, Integer> depth = new HashMap<>();
        Deque<String> queue = new ArrayDeque<>();
        depth.put(root, ROOT_DEPTH);
        queue.add(root);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            int level = depth.get(current);
            if (level >= maxDepth) {
                continue;
            }
            for (String dep : source.directDependencies(current)) {
                if (isExcluded(dep, filter)) {
                    continue;
                }
                graph.addEdge(current, dep);
                if (depth.putIfAbsent(dep, level + 1) == null) {
                    queue.add(dep);
                }
            }
        }
        return graph;
    }

    /**
     * Проверяет, нужно ли исключить пакет из анализа.
     *
     * @param name имя пакета
     * @param filter подстрока фильтра (пустая — фильтр выключен)
     * @return true, если пакет нужно пропустить
     */
    static boolean isExcluded(String name, String filter) {
        return !filter.isEmpty() && name.contains(filter);
    }
}
