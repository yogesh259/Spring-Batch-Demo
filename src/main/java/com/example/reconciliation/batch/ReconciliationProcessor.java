package com.example.reconciliation.batch;

import com.example.reconciliation.domain.BankTransaction;
import com.example.reconciliation.domain.LedgerEntry;
import com.example.reconciliation.domain.ReconciliationResult;
import com.example.reconciliation.repository.LedgerEntryRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static com.example.reconciliation.domain.ReconciliationResult.AMOUNT_MISMATCH;
import static com.example.reconciliation.domain.ReconciliationResult.MATCHED;
import static com.example.reconciliation.domain.ReconciliationResult.MISSING_IN_LEDGER;

/**
 * The business logic: compare one bank transaction with our ledger.
 * Knows nothing about chunks, transactions or restarts, so it is easy to unit-test.
 * It must stay free of side effects: on a retry it can run more than once per item.
 */
@Component
public class ReconciliationProcessor implements ItemProcessor<BankTransaction, ReconciliationResult> {

    private final LedgerEntryRepository ledgerRepository;

    public ReconciliationProcessor(LedgerEntryRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    @Override
    public ReconciliationResult process(BankTransaction txn) {
        if (txn.getAmount() == null) {
            throw new InvalidTransactionException("No amount for " + txn.getReference());
        }

        BigDecimal ledgerAmount = ledgerRepository.findById(txn.getReference())
                .map(LedgerEntry::getAmount)
                .orElse(null);

        String status;
        if (ledgerAmount == null) {
            status = MISSING_IN_LEDGER;
        } else if (ledgerAmount.compareTo(txn.getAmount()) == 0) {   // compareTo, not equals: 250.0 == 250.00
            status = MATCHED;
        } else {
            status = AMOUNT_MISMATCH;
        }
        return new ReconciliationResult(txn.getReference(), txn.getAmount(), ledgerAmount, status);
    }
}
