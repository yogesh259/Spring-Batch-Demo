-- Sample data for 24 November 2026. MERGE makes it safe to run on every startup.
--   TXN-002: wrong amount in our ledger  -> AMOUNT_MISMATCH
--   TXN-003: missing from our ledger     -> MISSING_IN_LEDGER
--   TXN-007: no amount at all            -> skipped as a bad record

MERGE INTO bank_transactions KEY (reference) VALUES
    ('TXN-001', 'NORTH', '2026-11-24',  250.00),
    ('TXN-002', 'NORTH', '2026-11-24', 1200.00),
    ('TXN-003', 'SOUTH', '2026-11-24',   75.50),
    ('TXN-004', 'SOUTH', '2026-11-24',  300.00),
    ('TXN-005', 'EAST',  '2026-11-24',  980.00),
    ('TXN-006', 'EAST',  '2026-11-24',   45.00),
    ('TXN-007', 'EAST',  '2026-11-24',   NULL);

MERGE INTO ledger_entries KEY (reference) VALUES
    ('TXN-001',  250.00),
    ('TXN-002', 1100.00),
    ('TXN-004',  300.00),
    ('TXN-005',  980.00),
    ('TXN-006',   45.00);
