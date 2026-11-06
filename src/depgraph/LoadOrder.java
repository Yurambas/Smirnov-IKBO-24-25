package depgraph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Порядок загрузки: каждая зависимость идёт раньше пакета, который от неё зависит.
 * Реализован итеративным обходом в глубину (обратный порядок завершения вершин).
 */
public final class LoadOrder {

    private LoadOrder() {
    }

    /**
     * Вычисляет порядок загрузки пакетов графа, достижимых из корня.
     * При циклических зависимостях обратные рёбра игнорируются.
     *
     * @param graph граф зависимостей
     * @return список имён пакетов в порядке загрузки
     */
    public static List<String> compute(Graph graph) {
        List<String> order = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Deque<String> path = new ArrayDeque<>();
        Deque<Iterator<String>> cursors = new ArrayDeque<>();
        visited.add(graph.root());
        path.push(graph.root());
        cursors.push(graph.dependenciesOf(graph.root()).iterator());
        while (!path.isEmpty()) {
            Iterator<String> cursor = cursors.peek();
            if (cursor.hasNext()) {
                String next = cursor.next();
                if (visited.add(next)) {
                    path.push(next);
                    cursors.push(graph.dependenciesOf(next).iterator());
                }
            } else {
                order.add(path.pop());
                cursors.pop();
            }
        }
        return order;
    }
}
