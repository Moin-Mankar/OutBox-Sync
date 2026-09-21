package com.moinmankar.outboxsync.controller;

import com.moinmankar.outboxsync.dto.auth.LoginRequest;
import com.moinmankar.outboxsync.dto.auth.LoginResponse;
import com.moinmankar.outboxsync.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(
                authService.login(request)
        );
    }
}
