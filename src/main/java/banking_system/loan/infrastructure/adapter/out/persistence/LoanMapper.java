package banking_system.loan.infrastructure.adapter.out.persistence;

import banking_system.account.domain.model.AccountId;
import banking_system.loan.domain.model.Installment;
import banking_system.loan.domain.model.InstallmentStatus;
import banking_system.loan.domain.model.Loan;
import banking_system.loan.domain.model.LoanId;
import banking_system.loan.domain.model.LoanStatus;
import banking_system.shared.domain.model.Money;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class LoanMapper {

    public LoanEntity toEntity(Loan domain) {
        if (domain == null) return null;

        LoanEntity loanEntity = LoanEntity.builder()
                .id(domain.getId().value())
                .borrowerAccountId(domain.getBorrowerAccountId().value())
                .principalAmount(domain.getPrincipal().amount())
                .accruedPenalty(domain.getAccruedPenalty().amount())
                .currency(domain.getPrincipal().currency().getCurrencyCode())
                .annualInterestRate(domain.getAnnualInterestRate())
                .termInMonths(domain.getTermInMonths())
                .status(domain.getStatus().name())
                .createdAt(domain.getCreatedAt())
                .build();

        List<InstallmentEntity> installmentEntities = domain.getInstallments().stream()
                .map(installment -> toInstallmentEntity(installment, loanEntity))
                .collect(Collectors.toList());

        loanEntity.setInstallments(installmentEntities);
        return loanEntity;
    }

    private InstallmentEntity toInstallmentEntity(Installment installment, LoanEntity loanEntity) {
        return InstallmentEntity.builder()
                .id(loanEntity.getId() + "-" + installment.getNumber())
                .loan(loanEntity)
                .installmentNumber(installment.getNumber())
                .amount(installment.getAmount().amount())
                .dueDate(installment.getDueDate())
                .status(installment.getStatus().name())
                .build();
    }

    public Loan toDomain(LoanEntity entity) {
        if (entity == null) return null;

        Currency currency = Currency.getInstance(entity.getCurrency());

        List<Installment> installments = entity.getInstallments().stream()
                .sorted(Comparator.comparingInt(InstallmentEntity::getInstallmentNumber))
                .map(installmentEntity -> new Installment(
                        installmentEntity.getInstallmentNumber(),
                        new Money(installmentEntity.getAmount(), currency),
                        installmentEntity.getDueDate(),
                        InstallmentStatus.valueOf(installmentEntity.getStatus())
                ))
                .collect(Collectors.toList());

        LoanStatus status = LoanStatus.valueOf(entity.getStatus());

        return new Loan(
                new LoanId(entity.getId()),
                new AccountId(entity.getBorrowerAccountId()),
                new Money(entity.getPrincipalAmount(), currency),
                entity.getAnnualInterestRate(),
                entity.getTermInMonths(),
                entity.getCreatedAt(),
                installments,
                new Money(entity.getAccruedPenalty(), currency),
                status.toState()
        );
    }
}