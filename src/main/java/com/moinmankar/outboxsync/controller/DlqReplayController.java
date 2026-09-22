package com.moinmankar.outboxsync.controller;

import com.moinmankar.outboxsync.service.DlqReplayService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/dlq")
public class DlqReplayController {

    private final DlqReplayService dlqReplayService;

    public DlqReplayController(DlqReplayService dlqReplayService) {
        this.dlqReplayService = dlqReplayService;
    }

    @PostMapping("/replay")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> replay(
            @RequestParam String key,
            @RequestBody String payload
    ) {

        dlqReplayService.replay(key, payload);

        return ResponseEntity.accepted().build();
    }
}