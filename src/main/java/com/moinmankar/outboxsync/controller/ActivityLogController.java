package com.moinmankar.outboxsync.controller;

import com.moinmankar.outboxsync.dto.response.ActivityLogResponse;
import com.moinmankar.outboxsync.entity.ActivityLog;
import com.moinmankar.outboxsync.entity.User;
import com.moinmankar.outboxsync.exception.ResourceNotFoundException;
import com.moinmankar.outboxsync.repository.ActivityLogRepository;
import com.moinmankar.outboxsync.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/activity")
public class ActivityLogController {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;

    public ActivityLogController(
            ActivityLogRepository activityLogRepository,
            UserRepository userRepository
    ) {
        this.activityLogRepository = activityLogRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<ActivityLogResponse> getMyActivity(
            Authentication authentication
    ) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UUID userId = user.getId();

        return activityLogRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ActivityLogResponse toResponse(ActivityLog activityLog) {

        return new ActivityLogResponse(
                activityLog.getId(),
                activityLog.getUserId(),
                activityLog.getAction(),
                activityLog.getEntityType(),
                activityLog.getEntityId(),
                activityLog.getCreatedAt()
        );
    }
}