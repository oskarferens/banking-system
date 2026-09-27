ALTER TABLE loans
ADD COLUMN accrued_penalty DECIMAL(19, 2) NOT NULL DEFAULT 0.00 AFTER principal_amount;