package jungmo.server.domain.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jungmo.server.domain.provider.UserDataProvider;
import jungmo.server.domain.repository.EmitterRepository;
import jungmo.server.global.error.ErrorCode;
import jungmo.server.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class SseEmitterService {

    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 30 ;
    private final EmitterRepository emitterRepository;
    private final UserDataProvider userDataProvider;

    /**
     * 클라이언트의 이벤트 구독을 허용하는 메서드
     */
    @Transactional
    public SseEmitter subscribe() {
        Long userId = userDataProvider.getUser().getId();
        // sse의 유효 시간 만료 시, 클라어언트에서 다시 서버로 이벤트 구독을 시도
        SseEmitter sseEmitter = emitterRepository.save(userId, new SseEmitter(DEFAULT_TIMEOUT));

        // 사용자에게 모든 데이터가 전송되었다면 emitter 삭제
        sseEmitter.onCompletion(() -> emitterRepository.deleteById(userId));
        // Emmiter의 유효 시간 만료 시, emitter 삭제. 유효 시간의 만료는 연결된 시간동안 아무런 이벤트가 발생하지 않았음을 의미함
        sseEmitter.onTimeout(() -> emitterRepository.deleteById(userId));
        return sseEmitter;
    }

    /**
     * 이벤트가 구독되어 있는 클라이언트에게 데이터를 전송
     */
    public void sendToClient(Long userId, Object data, String sse) {
        SseEmitter sseEmitter = emitterRepository.findById(userId);

        if (sseEmitter == null) {
            log.warn("SSE Emitter가 존재하지 않음. userId: {}", userId);
            return;
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonData = objectMapper.writeValueAsString(data);
            sseEmitter.send(
                    SseEmitter.event()
                            .id(userId.toString())
                            .name(sse)
                            .data(jsonData)
            );
            log.info("알림 전송 성공");
        } catch (JsonProcessingException e){
            log.warn("JSON 변환 오류 발생: ", e);
        } catch (IOException ex) {
            log.warn("클라이언트 연결 끊김(Broken Pipe): userId={}, error={}", userId, ex.getMessage());
            emitterRepository.deleteById(userId);  // 클라이언트 연결 끊김 시 Emitter 제거
        } catch (Exception e) {
            log.error("알림 전송 실패: " + e.getMessage());
            emitterRepository.deleteById(userId);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
