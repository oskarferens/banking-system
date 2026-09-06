package banking_system.notification.application.port.out;

import banking_system.notification.domain.model.Notification;

import java.util.List;
import java.util.Optional;

public interface NotificationRepositoryPort {
    Notification save(Notification notification);
    List<Notification> findByUserId(String userId);
    Optional<Notification> findById(String id);
}