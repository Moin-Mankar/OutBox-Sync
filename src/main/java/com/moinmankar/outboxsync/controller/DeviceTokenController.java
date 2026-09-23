package com.moinmankar.outboxsync.controller;

import com.moinmankar.outboxsync.dto.request.RegisterDeviceTokenRequest;
import com.moinmankar.outboxsync.entity.DeviceToken;
import com.moinmankar.outboxsync.entity.User;
import com.moinmankar.outboxsync.exception.ResourceNotFoundException;
import com.moinmankar.outboxsync.repository.DeviceTokenRepository;
import com.moinmankar.outboxsync.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/device-tokens")
public class DeviceTokenController {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    public DeviceTokenController(
            DeviceTokenRepository deviceTokenRepository,
            UserRepository userRepository
    ) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<Void> registerToken(
            @Valid @RequestBody RegisterDeviceTokenRequest request,
            Authentication authentication
    ) {

        User user = userRepository.findByEmail(
                authentication.getName()
        ).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!deviceTokenRepository.existsByToken(request.token())) {

            DeviceToken deviceToken = new DeviceToken();

            deviceToken.setUser(user);
            deviceToken.setToken(request.token());

            deviceTokenRepository.save(deviceToken);
        }

        return ResponseEntity.ok().build();
    }
}
