package jungmo.server.domain.service;

import jungmo.server.domain.dto.request.GatheringDto;
import jungmo.server.domain.dto.response.GatheringListResponseDto;
import jungmo.server.domain.dto.response.GatheringResponseDto;
import jungmo.server.domain.entity.*;
import jungmo.server.domain.repository.GatheringRepository;
import jungmo.server.domain.repository.GatheringUserRepository;
import jungmo.server.domain.repository.UserRepository;
import jungmo.server.global.auth.dto.response.SecurityUserDto;
import jungmo.server.global.auth.service.PrincipalDetails;
import jungmo.server.global.error.ErrorCode;
import jungmo.server.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GatheringService {

    private final GatheringRepository gatheringRepository;
    private final UserRepository userRepository;
    private final GatheringUserRepository gatheringUserRepository;

    @Transactional
    public Gathering saveGathering(GatheringDto dto) {
        Gathering gathering = Gathering.builder()
                .title(dto.getTitle())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .startTime(dto.getStartTime())
                .memo(dto.getMemo())
                .allExpense(0L)
                .isConnected(false)
                .isDeleted(false)
                .build();
        Gathering savedGathering = gatheringRepository.save(gathering);
        return savedGathering;
    }

    @Transactional
    public void updateGathering(Long gatheringId, GatheringDto gatheringDto) {
        User user = getUser();
        Gathering gathering = gatheringRepository.findById(gatheringId).orElseThrow(() -> new BusinessException(ErrorCode.GATHERING_NOT_EXISTS));
        Optional<GatheringUser> gatheringUser = gatheringUserRepository.findByAuthority(user, gathering, Authority.WRITE);
        if (gatheringUser.isPresent()) {
            if (!gathering.getIsDeleted()) {
                gathering.update(gatheringDto);
            } else {
                throw new BusinessException(ErrorCode.GATHERING_ALREADY_DELETED);
            }
        } else {
            throw new BusinessException(ErrorCode.NOT_HAVE_WRITE_AUTHORITY);
        }
    }

    @Transactional
    public void deleteGathering(Long gatheringId) {
        Gathering gathering = gatheringRepository.findById(gatheringId).orElseThrow(() -> new BusinessException(ErrorCode.GATHERING_NOT_EXISTS));
        User user = getUser();
        Optional<GatheringUser> gatheringUser = gatheringUserRepository.findByAuthority(user, gathering, Authority.WRITE);
        if (gatheringUser.isPresent()) {
            if (!gathering.getIsDeleted()) {
                gathering.setDeleted(true);
            } else {
                throw new BusinessException(ErrorCode.GATHERING_ALREADY_DELETED);
            }
        } else {
            throw new BusinessException(ErrorCode.NOT_HAVE_WRITE_AUTHORITY);
        }
    }

    public Gathering findGathering(Long gatheringId) {
        Gathering gathering = gatheringRepository.findById(gatheringId).orElseThrow(() -> new BusinessException(ErrorCode.GATHERING_NOT_EXISTS));
        if (gathering.getIsDeleted()) {
            throw new BusinessException(ErrorCode.GATHERING_ALREADY_DELETED);
        }
        return gathering;
    }

    public List<GatheringListResponseDto> findMyGatherings() {
        User user = getUser();
        List<GatheringListResponseDto> allGatherings = gatheringRepository.findAllByUserId(user.getId(), GatheringStatus.ACCEPT);
        return allGatherings;
    }

    private User getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        SecurityUserDto securityUser = SecurityUserDto.from(principalDetails);
        Long userId = securityUser.getUserId();

        // 데이터베이스에서 사용자 조회
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
    }


    public GatheringResponseDto toDto(Gathering gathering) {
        return gathering.toDto();
    }
}
