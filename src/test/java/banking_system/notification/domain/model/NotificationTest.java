package banking_system.notification.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    @DisplayName("a new notification is unread and carries the given content")
    void newNotificationIsUnread() {
        Notification notification = new Notification("user-1", "Title", "Body", CREATED_AT);

        assertThat(notification.getUserId()).isEqualTo("user-1");
        assertThat(notification.getTitle()).isEqualTo("Title");
        assertThat(notification.getMessage()).isEqualTo("Body");
        assertThat(notification.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(notification.isRead()).isFalse();
        assertThat(notification.getId()).isNotBlank();
    }

    @Test
    @DisplayName("markAsRead() flips the read flag")
    void markAsReadMarksNotification() {
        Notification notification = new Notification("user-1", "Title", "Body", CREATED_AT);

        notification.markAsRead();

        assertThat(notification.isRead()).isTrue();
    }

    @Test
    @DisplayName("every notification gets its own id")
    void eachNotificationGetsUniqueId() {
        Notification first = new Notification("user-1", "Title", "Body", CREATED_AT);
        Notification second = new Notification("user-1", "Title", "Body", CREATED_AT);

        assertThat(first.getId()).isNotEqualTo(second.getId());
    }
}