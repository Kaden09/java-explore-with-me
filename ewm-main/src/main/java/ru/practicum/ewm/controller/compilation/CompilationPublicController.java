package ru.practicum.ewm.controller.compilation;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.compilation.CompilationDto;
import ru.practicum.ewm.service.interfaces.CompilationService;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/compilations")
@RequiredArgsConstructor
public class CompilationPublicController {
    private final CompilationService compilationService;

    @GetMapping
    public ResponseEntity<List<CompilationDto>> getCompilations(
            @RequestParam(required = false) Boolean pinned,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size
    ) {
        log.info("GET запрос на получение списка подборок: pinned={}, from={}, size={}", pinned, from, size);

        return ResponseEntity.status(HttpStatus.OK).body(compilationService.getCompilations(pinned, from, size));
    }

    @GetMapping("/{compilationId}")
    public ResponseEntity<CompilationDto> getCompilationById(@PathVariable Long compilationId) {
        log.info("GET запрос на получение подборки по id: compilationId={}", compilationId);

        return ResponseEntity.status(HttpStatus.OK).body(compilationService.getCompilationById(compilationId));
    }
}
