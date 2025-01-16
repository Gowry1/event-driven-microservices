package com.example.notificationservice.service;

import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification saveNotification(String userId, String orderId, String type, String message) {
        Notification notification = Notification.builder()
                .notificationId(UUID.randomUUID().toString())
                .userId(userId)
                .orderId(orderId)
                .type(type)
                .message(message)
                .status("SENT")
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification stored in database: ID = {}, Msg = '{}'", saved.getNotificationId(), message);
        return saved;
    }

    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    public List<Notification> getNotificationsByUser(String userId) {
        return notificationRepository.findByUserId(userId);
    }
}
