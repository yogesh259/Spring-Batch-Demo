package com.example.reconciliation.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** The outcome for one bank transaction: MATCHED, AMOUNT_MISMATCH or MISSING_IN_LEDGER. */
@Entity
@Table(name = "reconciliation_results")
public class ReconciliationResult {

    public static final String MATCHED = "MATCHED";
    public static final String AMOUNT_MISMATCH = "AMOUNT_MISMATCH";
    public static final String MISSING_IN_LEDGER = "MISSING_IN_LEDGER";

    @Id
    private String reference;
    private BigDecimal bankAmount;
    private BigDecimal ledgerAmount;
    private String status;

    protected ReconciliationResult() {
        // for JPA
    }

    public ReconciliationResult(String reference, BigDecimal bankAmount,
                                BigDecimal ledgerAmount, String status) {
        this.reference = reference;
        this.bankAmount = bankAmount;
        this.ledgerAmount = ledgerAmount;
        this.status = status;
    }

    public String getReference() {
        return reference;
    }

    public BigDecimal getBankAmount() {
        return bankAmount;
    }

    public BigDecimal getLedgerAmount() {
        return ledgerAmount;
    }

    public String getStatus() {
        return status;
    }
}
