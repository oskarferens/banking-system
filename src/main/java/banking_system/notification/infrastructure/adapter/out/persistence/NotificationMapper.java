package banking_system.notification.infrastructure.adapter.out.persistence;

import banking_system.notification.domain.model.Notification;

public class NotificationMapper {

    public static NotificationEntity toEntity(Notification notification) {
        return new NotificationEntity(
                notification.getId(),
                notification.getUserId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getCreatedAt(),
                notification.isRead()
        );
    }

    public static Notification toDomain(NotificationEntity entity) {
        Notification notification = new Notification(
                entity.getUserId(),
                entity.getTitle(),
                entity.getMessage(),
                entity.getCreatedAt()
        );
        if (entity.isRead()) {
            notification.markAsRead();
        }
        return notification;
    }
}
