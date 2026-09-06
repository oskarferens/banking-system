package banking_system.notification.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Notification {

    private final String id;
    private final String userId;
    private final String title;
    private final String message;
    private final Instant createdAt;
    private boolean read;

    public Notification(String userId, String title, String message, Instant createdAt) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.createdAt = createdAt;
        this.read = false;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isRead() { return read; }

    public void markAsRead() {
        this.read = true;
    }
}
