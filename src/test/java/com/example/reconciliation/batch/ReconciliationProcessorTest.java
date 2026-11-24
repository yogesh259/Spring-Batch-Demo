package com.example.reconciliation.batch;

import com.example.reconciliation.domain.BankTransaction;
import com.example.reconciliation.domain.LedgerEntry;
import com.example.reconciliation.domain.ReconciliationResult;
import com.example.reconciliation.repository.LedgerEntryRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** The processor is plain Java: no Spring, no database. */
class ReconciliationProcessorTest {

    private final LedgerEntryRepository ledger = mock(LedgerEntryRepository.class);
    private final ReconciliationProcessor processor = new ReconciliationProcessor(ledger);

    @Test
    void matchedWhenAmountsAreEqualIgnoringScale() {
        when(ledger.findById("TXN-001")).thenReturn(Optional.of(new LedgerEntry("TXN-001", new BigDecimal("250.0"))));

        ReconciliationResult result = processor.process(txn("TXN-001", "250.00"));

        assertThat(result.getStatus()).isEqualTo(ReconciliationResult.MATCHED);
    }

    @Test
    void amountMismatch() {
        when(ledger.findById("TXN-002")).thenReturn(Optional.of(new LedgerEntry("TXN-002", new BigDecimal("1100.00"))));

        assertThat(processor.process(txn("TXN-002", "1200.00")).getStatus())
                .isEqualTo(ReconciliationResult.AMOUNT_MISMATCH);
    }

    @Test
    void missingInLedger() {
        when(ledger.findById("TXN-003")).thenReturn(Optional.empty());

        assertThat(processor.process(txn("TXN-003", "75.50")).getStatus())
                .isEqualTo(ReconciliationResult.MISSING_IN_LEDGER);
    }

    @Test
    void transactionWithoutAmountIsInvalid() {
        assertThatThrownBy(() -> processor.process(txn("TXN-007", null)))
                .isInstanceOf(InvalidTransactionException.class);
    }

    private static BankTransaction txn(String reference, String amount) {
        return new BankTransaction(reference, "NORTH", LocalDate.of(2026, 11, 24),
                amount == null ? null : new BigDecimal(amount));
    }
}
