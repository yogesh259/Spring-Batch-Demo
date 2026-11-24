package com.example.reconciliation.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One line of the bank's statement: what actually went through. */
@Entity
@Table(name = "bank_transactions")
public class BankTransaction {

    @Id
    private String reference;
    private String branch;
    private LocalDate txnDate;
    private BigDecimal amount;

    protected BankTransaction() {
        // for JPA
    }

    public BankTransaction(String reference, String branch, LocalDate txnDate, BigDecimal amount) {
        this.reference = reference;
        this.branch = branch;
        this.txnDate = txnDate;
        this.amount = amount;
    }

    public String getReference() {
        return reference;
    }

    public String getBranch() {
        return branch;
    }

    public LocalDate getTxnDate() {
        return txnDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
