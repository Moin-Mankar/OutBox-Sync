package com.moinmankar.outboxsync.service;

import com.moinmankar.outboxsync.entity.ActivityLog;
import com.moinmankar.outboxsync.repository.ActivityLogRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    public ActivityLogService(
            ActivityLogRepository activityLogRepository
    ) {
        this.activityLogRepository = activityLogRepository;
    }

    public void log(
            UUID userId,
            String action,
            String entityType,
            UUID entityId
    ) {

        ActivityLog activityLog = new ActivityLog();

        activityLog.setUserId(userId);
        activityLog.setAction(action);
        activityLog.setEntityType(entityType);
        activityLog.setEntityId(entityId);

        activityLogRepository.save(activityLog);
    }
}