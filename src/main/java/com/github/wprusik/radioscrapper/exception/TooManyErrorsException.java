package com.github.wprusik.radioscrapper.exception;

public class TooManyErrorsException extends RuntimeException {

    public TooManyErrorsException(Throwable cause) {
        super("The error limit has been exceeded", cause);
    }
}
