CREATE TABLE transfers (
id VARCHAR(36) NOT NULL PRIMARY KEY,
source_account_id VARCHAR(36) NOT NULL,
target_account_id VARCHAR(36) NOT NULL,
amount DECIMAL(19, 2) NOT NULL,
currency VARCHAR(3) NOT NULL,
status VARCHAR(20) NOT NULL,
timestamp TIMESTAMP NOT NULL,
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT fk_transfers_source FOREIGN KEY (source_account_id) REFERENCES accounts(id),
CONSTRAINT fk_transfers_target FOREIGN KEY (target_account_id) REFERENCES accounts(id)
);