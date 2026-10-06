package banking_system.notification.application.service;

import banking_system.notification.domain.model.Notification;
import banking_system.notification.domain.port.NotificationRepositoryPort;
import banking_system.timemachine.application.service.TimeMachineService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

    @Mock
    private NotificationRepositoryPort notificationRepository;

    @Mock
    private TimeMachineService timeMachineService;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("sendNotification() stamps the virtual Time Machine time and saves an unread notification")
    void sendNotificationStampsVirtualTimeAndSaves() {
        when(timeMachineService.getCurrentTime()).thenReturn(NOW);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification sent = notificationService.sendNotification("user-1", "Hello", "World");

        assertThat(sent.getUserId()).isEqualTo("user-1");
        assertThat(sent.getTitle()).isEqualTo("Hello");
        assertThat(sent.getMessage()).isEqualTo("World");
        assertThat(sent.getCreatedAt()).isEqualTo(NOW);
        assertThat(sent.isRead()).isFalse();
        verify(notificationRepository).save(sent);
    }

    @Test
    @DisplayName("getUserNotifications() returns what the repository holds for that user")
    void getUserNotificationsDelegatesToRepository() {
        Notification notification = new Notification("user-1", "Hello", "World", NOW);
        when(notificationRepository.findByUserId("user-1")).thenReturn(List.of(notification));

        List<Notification> result = notificationService.getUserNotifications("user-1");

        assertThat(result).containsExactly(notification);
    }
}