package ru.practicum.ewm.controller.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.user.NewUserRequestDto;
import ru.practicum.ewm.dto.user.UserDto;
import ru.practicum.ewm.service.interfaces.UserService;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/users")
public class UserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserDto> addUser(@RequestBody @Valid NewUserRequestDto newUserRequest) {
        log.info("POST запрос на создание пользователя: name={}, email={}",
                newUserRequest.getName(), newUserRequest.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.addUser(newUserRequest));
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getUsers(
            @RequestParam(value = "ids", required = false) List<Long> ids,
            @RequestParam(value = "from", defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(value = "size", defaultValue = "10") @Positive Integer size
    ) {
        log.info("GET запрос на получение пользователей: ids={}, from={}, size={}", ids, from, size);

        return ResponseEntity.status(HttpStatus.OK)
                .body(userService.getUsers(ids, from, size));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        log.info("DELETE запрос на удаление пользователя: userId={}", userId);

        userService.deleteUser(userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}