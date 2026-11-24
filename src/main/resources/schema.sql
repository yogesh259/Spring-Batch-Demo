-- Runs on every startup; IF NOT EXISTS keeps it safe. Works on H2 and PostgreSQL.

CREATE TABLE IF NOT EXISTS bank_transactions (
    reference VARCHAR(20) PRIMARY KEY,
    branch    VARCHAR(10),
    txn_date  DATE,
    amount    DECIMAL(12, 2)
);

CREATE TABLE IF NOT EXISTS ledger_entries (
    reference VARCHAR(20) PRIMARY KEY,
    amount    DECIMAL(12, 2)
);

CREATE TABLE IF NOT EXISTS reconciliation_results (
    reference     VARCHAR(20) PRIMARY KEY,
    bank_amount   DECIMAL(12, 2),
    ledger_amount DECIMAL(12, 2),
    status        VARCHAR(20)
);
