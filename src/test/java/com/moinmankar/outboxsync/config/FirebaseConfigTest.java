package com.moinmankar.outboxsync.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class FirebaseConfigTest {

    private static class TestableFirebaseConfig extends FirebaseConfig {
        private final String path;

        TestableFirebaseConfig(String path) {
            this.path = path;
        }

        @Override
        protected String resolveCredentialsPath() {
            return path;
        }
    }

    @Test
    void skipsInitializationWithoutThrowingWhenCredentialsMissing() {

        FirebaseConfig config = new TestableFirebaseConfig(null);

        try (MockedStatic<FirebaseApp> firebaseApp = mockStatic(FirebaseApp.class)) {
            firebaseApp.when(FirebaseApp::getApps).thenReturn(List.of());

            assertThatCode(config::initializeFirebase).doesNotThrowAnyException();
        }

        assertThat(config.isInitialized()).isFalse();
    }

    @Test
    void skipsInitializationWhenCredentialsFileIsMissing() {

        FirebaseConfig config = new TestableFirebaseConfig(
                Path.of("does-not-exist.json").toString());

        try (MockedStatic<FirebaseApp> firebaseApp = mockStatic(FirebaseApp.class)) {
            firebaseApp.when(FirebaseApp::getApps).thenReturn(List.of());

            assertThatCode(config::initializeFirebase).doesNotThrowAnyException();
        }

        assertThat(config.isInitialized()).isFalse();
    }

    @Test
    void initializesWhenValidCredentialsAreConfigured(@TempDir Path tempDir) throws Exception {

        Path credentialsFile = tempDir.resolve("service-account.json");
        Files.writeString(credentialsFile, "{}");

        FirebaseApp mockApp = mock(FirebaseApp.class);

        try (MockedStatic<FirebaseApp> firebaseApp = mockStatic(FirebaseApp.class);
             MockedStatic<GoogleCredentials> credentials = mockStatic(GoogleCredentials.class)) {

            credentials.when(() -> GoogleCredentials.fromStream(any())).thenReturn(mock(GoogleCredentials.class));
            firebaseApp.when(FirebaseApp::getApps).thenReturn(List.of());
            firebaseApp.when(() -> FirebaseApp.initializeApp(any(FirebaseOptions.class))).thenReturn(mockApp);

            FirebaseConfig config = new TestableFirebaseConfig(credentialsFile.toString());
            config.initializeFirebase();

            assertThat(config.isInitialized()).isTrue();
            firebaseApp.verify(() -> FirebaseApp.initializeApp(any(FirebaseOptions.class)));
        }
    }
}
