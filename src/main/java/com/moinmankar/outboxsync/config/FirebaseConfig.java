package com.moinmankar.outboxsync.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    private volatile boolean initialized;

    @PostConstruct
    public void initializeFirebase() {

        if (!FirebaseApp.getApps().isEmpty()) {
            initialized = true;
            return;
        }

        String credentialsPath = resolveCredentialsPath();

        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.warn("Firebase credentials not configured. Notifications are disabled.");
            return;
        }

        try (FileInputStream serviceAccount = new FileInputStream(credentialsPath)) {

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
            initialized = true;
            log.info("Firebase initialized successfully");

        } catch (Exception e) {
            initialized = false;
            log.warn("Failed to initialize Firebase. Notifications are disabled.", e);
        }
    }

    protected String resolveCredentialsPath() {
        return System.getenv("FIREBASE_CREDENTIALS_PATH");
    }

    public boolean isInitialized() {
        return initialized;
    }
}
