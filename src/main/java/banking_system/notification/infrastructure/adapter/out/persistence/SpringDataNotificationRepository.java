package banking_system.notification.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataNotificationRepository extends JpaRepository<NotificationEntity, String> {
    List<NotificationEntity> findByUserId(String userId);
}
