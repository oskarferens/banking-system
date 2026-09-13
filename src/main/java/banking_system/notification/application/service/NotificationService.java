package banking_system.notification.application.service;

import banking_system.notification.domain.port.NotificationRepositoryPort;
import banking_system.notification.domain.model.Notification;
import banking_system.timemachine.application.service.TimeMachineService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepositoryPort notificationRepositoryPort;
    private final TimeMachineService timeMachineService;

    public NotificationService(NotificationRepositoryPort notificationRepositoryPort, TimeMachineService timeMachineService) {
        this.notificationRepositoryPort = notificationRepositoryPort;
        this.timeMachineService = timeMachineService;
    }

    public Notification sendNotification(String userId, String title, String message) {
        Notification notification = new Notification(
                userId,
                title,
                message,
                timeMachineService.getCurrentTime()
        );
        return notificationRepositoryPort.save(notification);
    }

    public List<Notification> getUserNotifications(String userId) {
        return notificationRepositoryPort.findByUserId(userId);
    }
}
