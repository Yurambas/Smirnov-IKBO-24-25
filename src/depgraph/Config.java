package depgraph;

import java.util.Set;

/**
 * Параметры, заданные пользователем в командной строке.
 *
 * @param packageName имя анализируемого пакета
 * @param repo URL репозитория или путь к файлу тестового репозитория
 * @param mode режим работы с репозиторием
 * @param output имя файла с изображением графа
 * @param maxDepth максимальная глубина анализа
 * @param filter подстрока для исключения пакетов
 * @param actions включённые дополнительные режимы
 */
public record Config(String packageName, String repo, Mode mode, String output,
                     int maxDepth, String filter, Set<Action> actions) {

    /**
     * Режим работы с репозиторием.
     */
    public enum Mode {
        /** Реальный репозиторий по URL. */
        REAL,
        /** Тестовый репозиторий из файла. */
        TEST
    }

    /**
     * Дополнительные режимы работы программы.
     */
    public enum Action {
        /** Вывести параметры в формате ключ-значение. */
        SHOW_PARAMS,
        /** Вывести только прямые зависимости. */
        DIRECT,
        /** Вывести порядок загрузки зависимостей. */
        LOAD_ORDER
    }

    /**
     * Проверяет, включён ли режим.
     *
     * @param action проверяемый режим
     * @return true, если режим включён
     */
    public boolean has(Action action) {
        return actions.contains(action);
    }
}
