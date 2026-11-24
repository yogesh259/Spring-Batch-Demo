package com.example.reconciliation.batch;

/** Thrown for a bad record. The step is configured to skip it, not fail. */
public class InvalidTransactionException extends RuntimeException {

    public InvalidTransactionException(String message) {
        super(message);
    }
}
