package com.example.reconciliation.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** One entry in our own ledger: what we think happened. */
@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    private String reference;
    private BigDecimal amount;

    protected LedgerEntry() {
        // for JPA
    }

    public LedgerEntry(String reference, BigDecimal amount) {
        this.reference = reference;
        this.amount = amount;
    }

    public String getReference() {
        return reference;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
