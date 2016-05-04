package ru.slicer;

public class ExpectedException extends Exception {
    private final String description;
    private final String errorCode;

    public ExpectedException(final String description, final String errorCode) {
        this.description = description;
        this.errorCode = errorCode;
    }

    public String getDescription() {
        return description;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
