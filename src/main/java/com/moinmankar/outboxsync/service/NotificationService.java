package com.moinmankar.outboxsync.service;


import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.moinmankar.outboxsync.config.FirebaseConfig;
import com.moinmankar.outboxsync.entity.DeviceToken;
import com.moinmankar.outboxsync.repository.DeviceTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationService.class);

    private final DeviceTokenRepository deviceTokenRepository;
    private final FirebaseConfig firebaseConfig;

    public NotificationService(
            DeviceTokenRepository deviceTokenRepository,
            FirebaseConfig firebaseConfig
    ) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.firebaseConfig = firebaseConfig;
    }

    public void sendNotification(
            UUID userId,
            String title,
            String body
    ) {

        if (!firebaseConfig.isInitialized()) {
            log.warn("[FCM] Firebase not initialized. Skipping notification for userId={}", userId);
            return;
        }

        List<DeviceToken> tokens =
                deviceTokenRepository.findByUserId(userId);

        for (DeviceToken deviceToken : tokens) {

            Message message = Message.builder()
                    .setToken(deviceToken.getToken())
                    .setNotification(
                            Notification.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .build()
                    )
                    .build();

            try {

                String messageId =
                        FirebaseMessaging
                                .getInstance()
                                .send(message);

                log.info(
                        "[FCM] Notification sent successfully. userId={}, messageId={}",
                        userId,
                        messageId
                );

            } catch (FirebaseMessagingException e) {

                if (e.getMessagingErrorCode() != null &&
                        e.getMessagingErrorCode().name().equals("UNREGISTERED")) {

                    log.warn(
                            "[FCM] Removing invalid device token. userId={}",
                            userId
                    );

                    deviceTokenRepository.delete(deviceToken);

                } else {

                    log.error(
                            "[FCM] Failed to send notification. userId={}, token={}",
                            userId,
                            deviceToken.getToken(),
                            e
                    );
                }
            }
        }
    }
}