-- Same sample data as data-h2.sql, in PostgreSQL syntax.

INSERT INTO bank_transactions (reference, branch, txn_date, amount) VALUES
    ('TXN-001', 'NORTH', '2026-11-24',  250.00),
    ('TXN-002', 'NORTH', '2026-11-24', 1200.00),
    ('TXN-003', 'SOUTH', '2026-11-24',   75.50),
    ('TXN-004', 'SOUTH', '2026-11-24',  300.00),
    ('TXN-005', 'EAST',  '2026-11-24',  980.00),
    ('TXN-006', 'EAST',  '2026-11-24',   45.00),
    ('TXN-007', 'EAST',  '2026-11-24',   NULL)
ON CONFLICT (reference) DO NOTHING;

INSERT INTO ledger_entries (reference, amount) VALUES
    ('TXN-001',  250.00),
    ('TXN-002', 1100.00),
    ('TXN-004',  300.00),
    ('TXN-005',  980.00),
    ('TXN-006',   45.00)
ON CONFLICT (reference) DO NOTHING;
