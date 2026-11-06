package depgraph;

import java.util.List;

/**
 * Источник сведений о прямых зависимостях пакетов.
 */
public interface DependencySource {

    /**
     * Возвращает прямые зависимости пакета.
     *
     * @param name имя пакета
     * @return список имён зависимостей
     * @throws AppException если пакет не найден или данные недоступны
     */
    public List<String> directDependencies(String name) throws AppException;
}
