package depgraph;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Формирование описания графа на языке Graphviz и отрисовка SVG.
 */
public final class DotWriter {
    private static final String SVG_SUFFIX = ".svg";
    private static final String DOT_SUFFIX = ".dot";
    private static final int SUCCESS = 0;

    private DotWriter() {
    }

    /**
     * Строит текстовое представление графа на языке DOT.
     *
     * @param graph граф зависимостей
     * @return текст диаграммы
     */
    public static String toDot(Graph graph) {
        StringBuilder text = new StringBuilder();
        text.append("digraph \"").append(graph.root()).append("\" {\n");
        text.append("  node [shape=box];\n");
        for (String node : graph.nodes()) {
            text.append("  \"").append(node).append("\";\n");
            for (String dep : graph.dependenciesOf(node)) {
                text.append("  \"").append(node).append("\" -> \"").append(dep).append("\";\n");
            }
        }
        return text.append("}\n").toString();
    }

    /**
     * Возвращает имя .dot-файла, соответствующее имени .svg-файла.
     *
     * @param svgName имя файла изображения
     * @return имя файла с текстом диаграммы
     */
    public static String dotName(String svgName) {
        return svgName.substring(0, svgName.length() - SVG_SUFFIX.length()) + DOT_SUFFIX;
    }

    /**
     * Сохраняет .dot-файл и вызывает Graphviz для получения SVG.
     *
     * @param dotText текст диаграммы
     * @param svgName имя выходного SVG-файла
     * @throws AppException если не удалось записать файл или запустить dot
     */
    public static void save(String dotText, String svgName) throws AppException {
        Path dotFile = Path.of(dotName(svgName));
        try {
            Files.writeString(dotFile, dotText, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AppException("не удалось записать " + dotFile + ": " + e.getMessage());
        }
        renderSvg(dotFile, Path.of(svgName));
    }

    /**
     * Запускает команду dot для преобразования диаграммы в SVG.
     *
     * @param dotFile входной файл
     * @param svgFile выходной файл
     * @throws AppException если Graphviz не установлен или завершился с ошибкой
     */
    private static void renderSvg(Path dotFile, Path svgFile) throws AppException {
        ProcessBuilder builder = new ProcessBuilder(
                "dot", "-Tsvg", dotFile.toString(), "-o", svgFile.toString());
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            String output = new String(process.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
            if (process.waitFor() != SUCCESS) {
                throw new AppException("Graphviz завершился с ошибкой: " + output);
            }
        } catch (IOException e) {
            throw new AppException("не удалось запустить Graphviz (команда dot); файл "
                    + dotFile + " сохранён, установите Graphviz");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException("отрисовка прервана");
        }
    }
}
