package ru.mirea.emulator;

import ru.mirea.emulator.parser.CommandParser;
import java.util.List;

/**
 * Тестирование парсера и раскрытия переменных окружения.
 */
public class CommandParserTest {

    /**
     * Запускает все тесты.
     *
     * @param args Аргументы командной строки
     */
    public static void main(String[] args) {
        testVariableExpansion();
        testParseTokens();
        System.out.println("Все тесты пройдены успешно!");
    }

    /**
     * Проверяет раскрытие переменной окружения.
     */
    private static void testVariableExpansion() {
        CommandParser parser = new CommandParser();
        String result = parser.expandEnv("echo $USER is here");
        if (result.contains("$USER")) {
            throw new AssertionError("Переменная $USER не была раскрыта!");
        }
    }

    /**
     * Проверяет разбиение строки на токены.
     */
    private static void testParseTokens() {
        CommandParser parser = new CommandParser();
        List<String> tokens = parser.parse("ls -l /home");
        if (tokens.size() != 3 || !tokens.get(0).equals("ls")) {
            throw new AssertionError("Ошибка разбора токенов для 'ls -l /home'");
        }
    }
}
