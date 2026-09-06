package banking_system.notification.infrastructure.adapter.out.persistance;

import banking_system.notification.application.port.out.NotificationRepositoryPort;
import banking_system.notification.domain.model.Notification;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class InMemoryNotificationRepository implements NotificationRepositoryPort {

    private final Map<String, Notification> storage = new HashMap<>();

    @Override
    public Notification save(Notification notification) {
        storage.put(notification.getId(), notification);
        return notification;
    }

    @Override
    public List<Notification> findByUserId(String userId) {
        return storage.values().stream()
                .filter(n -> n.getUserId().equals(userId))
                .toList();
    }

    @Override
    public Optional<Notification> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }
}