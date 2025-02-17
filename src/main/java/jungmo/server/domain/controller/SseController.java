package jungmo.server.domain.controller;

import io.swagger.v3.oas.annotations.Operation;
import jungmo.server.domain.dto.request.NotificationReadRequest;
import jungmo.server.domain.dto.response.NotificationResponse;
import jungmo.server.domain.service.NotificationService;
import jungmo.server.domain.service.SseEmitterService;
import jungmo.server.global.result.ResultCode;
import jungmo.server.global.result.ResultDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/sse")
@RequiredArgsConstructor
public class SseController implements SseSwaggerController{

    private final SseEmitterService sseEmitterService;
    private final NotificationService notificationService;

    @Override
    @GetMapping(value = "/subscribe", produces = "text/event-stream")
    public SseEmitter subscribe() {
        return sseEmitterService.subscribe();
    }

    @Override
    @GetMapping("/notifications")
    public List<NotificationResponse> getNotifications() {
        return notificationService.getNotifications();
    }

    @Override
    @PatchMapping("/notifications")
    public ResultDetailResponse<Void> markNotificationsAsRead(@RequestBody NotificationReadRequest request) {
        notificationService.markAsRead(request.getNotificationIds());
        return new ResultDetailResponse<>(ResultCode.PROCESSED_IS_READ, null);
    }
}