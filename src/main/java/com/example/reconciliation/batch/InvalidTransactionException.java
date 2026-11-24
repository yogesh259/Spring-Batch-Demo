package com.example.reconciliation.batch;

/** A bank transaction that cannot be reconciled. Skipped by the reconcile step. */
public class InvalidTransactionException extends RuntimeException {

    public InvalidTransactionException(String message) {
        super(message);
    }
}
