package depgraph;

/**
 * Ошибка, о которой нужно сообщить пользователю понятным текстом.
 */
public class AppException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Создаёт ошибку с сообщением.
     *
     * @param message текст для пользователя
     */
    public AppException(String message) {
        super(message);
    }
}
