package banking_system.account.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "account_number", nullable = false, unique = true, length = 24)
    private String accountNumber;

    @Column(name = "owner_id", nullable = false, length = 255)
    private String ownerId;

    @Column(name = "balance_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "overdraft_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal overdraftLimit;
}