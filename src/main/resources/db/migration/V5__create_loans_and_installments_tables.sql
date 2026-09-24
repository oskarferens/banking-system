CREATE TABLE loans (
id VARCHAR(36) NOT NULL PRIMARY KEY,
borrower_account_id VARCHAR(36) NOT NULL,
principal_amount DECIMAL(19, 2) NOT NULL,
currency VARCHAR(3) NOT NULL,
annual_interest_rate DECIMAL(6, 4) NOT NULL,
term_in_months INT NOT NULL,
status VARCHAR(20) NOT NULL,
created_at TIMESTAMP NOT NULL,
CONSTRAINT fk_loans_borrower_account FOREIGN KEY (borrower_account_id) REFERENCES accounts(id)
);

CREATE TABLE installments (
id VARCHAR(36) NOT NULL PRIMARY KEY,
loan_id VARCHAR(36) NOT NULL,
installment_number INT NOT NULL,
amount DECIMAL(19, 2) NOT NULL,
due_date TIMESTAMP NOT NULL,
status VARCHAR(20) NOT NULL,
CONSTRAINT fk_installments_loan FOREIGN KEY (loan_id) REFERENCES loans(id),
CONSTRAINT uq_installments_loan_number UNIQUE (loan_id, installment_number)
);