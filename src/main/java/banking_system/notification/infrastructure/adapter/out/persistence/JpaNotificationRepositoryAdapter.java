package banking_system.notification.infrastructure.adapter.out.persistence;

import banking_system.notification.application.port.out.NotificationRepositoryPort;
import banking_system.notification.domain.model.Notification;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class JpaNotificationRepositoryAdapter implements NotificationRepositoryPort {

    private final SpringDataNotificationRepository repository;

    public JpaNotificationRepositoryAdapter(SpringDataNotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Notification save(Notification notification) {
        NotificationEntity entity = NotificationMapper.toEntity(notification);
        NotificationEntity saved = repository.save(entity);
        return NotificationMapper.toDomain(saved);
    }

    @Override
    public List<Notification> findByUserId(String userId) {
        return repository.findByUserId(userId).stream()
                .map(NotificationMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Notification> findById(String id) {
        return repository.findById(id)
                .map(NotificationMapper::toDomain);
    }
}
