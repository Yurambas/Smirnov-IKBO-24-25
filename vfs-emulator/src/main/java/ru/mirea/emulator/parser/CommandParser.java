package ru.mirea.emulator.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Класс для разбора командной строки и раскрытия переменных окружения.
 */
public class CommandParser {

    private static final Pattern ENV_PATTERN = Pattern.compile("\\$([A-Za-z0-9_]+)");

    /**
     * Выполняет подстановку переменных окружения вида $VAR.
     *
     * @param input Исходная строка ввода
     * @return Строка с раскрытыми значениями переменных окружения
     */
    public String expandEnv(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        Matcher matcher = ENV_PATTERN.matcher(input);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String envValue = System.getenv(varName);
            String replacement = (envValue != null) ? envValue : "";
            matcher.appendReplacement(builder, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

    /**
     * Разделяет входную команду на лексемы с учетом раскрытия переменных окружения.
     *
     * @param input Исходная строка команды
     * @return Список токенов (первый элемент - имя команды, остальные - аргументы)
     */
    public List<String> parse(String input) {
        if (input == null) {
            return Collections.emptyList();
        }
        String expanded = expandEnv(input.trim());
        if (expanded.isEmpty()) {
            return Collections.emptyList();
        }

        String[] rawTokens = expanded.split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (String token : rawTokens) {
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens;
    }
}
