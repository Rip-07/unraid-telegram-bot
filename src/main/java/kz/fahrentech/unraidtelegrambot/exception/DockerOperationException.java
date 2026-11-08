package kz.fahrentech.unraidtelegrambot.exception;

public class DockerOperationException extends RuntimeException {

    public DockerOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
