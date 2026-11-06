package depgraph;

import java.util.List;

/**
 * Источник сведений о прямых зависимостях пакетов.
 * Метод {@code directDependencies(name)} принимает имя пакета и возвращает список имён
 * его прямых зависимостей. Если пакет не найден или данные недоступны,
 * выбрасывается {@link AppException}.
 */
public interface DependencySource {
    List<String> directDependencies(String name) throws AppException;
}
