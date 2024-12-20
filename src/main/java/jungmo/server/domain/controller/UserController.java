package jungmo.server.domain.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jungmo.server.domain.dto.request.UserCodeDto;
import jungmo.server.domain.dto.response.UserDto;
import jungmo.server.domain.service.UserService;
import jungmo.server.global.result.ResultCode;
import jungmo.server.global.result.ResultDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/gathering")
@Tag(name = "User", description = "사용자 관련 API")
public class UserController {

    private final UserService userService;

    @GetMapping("/user/search")
    public ResultDetailResponse<UserDto> searchUser(@RequestBody @Valid UserCodeDto userCodeDto) {
        UserDto user = userService.getUser(userCodeDto);
        return new ResultDetailResponse<>(ResultCode.GET_USER_SUCCESS, user);
    }



}
