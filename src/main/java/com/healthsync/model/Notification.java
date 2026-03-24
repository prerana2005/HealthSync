package com.healthsync.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notif_id")
    private Integer notifId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "notif_type", nullable = false)
    private NotifType notifType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    public enum NotifType { LAB_RESULT, APPOINTMENT, BILLING, GENERAL }

    @PrePersist
    protected void onCreate() { if (sentAt == null) sentAt = LocalDateTime.now(); }

    public Integer getNotifId() { return notifId; }
    public void setNotifId(Integer notifId) { this.notifId = notifId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public NotifType getNotifType() { return notifType; }
    public void setNotifType(NotifType notifType) { this.notifType = notifType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
