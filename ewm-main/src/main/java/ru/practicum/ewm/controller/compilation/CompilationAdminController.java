package ru.practicum.ewm.controller.compilation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.compilation.CompilationDto;
import ru.practicum.ewm.dto.compilation.NewCompilationDto;
import ru.practicum.ewm.dto.compilation.UpdateCompilationRequestDto;
import ru.practicum.ewm.service.interfaces.CompilationService;

@Slf4j
@RestController
@RequestMapping("/admin/compilations")
@RequiredArgsConstructor
public class CompilationAdminController {
    private final CompilationService compilationService;

    @PostMapping
    public ResponseEntity<CompilationDto> addCompilation(@RequestBody @Valid NewCompilationDto newCompilationDto) {
        log.info("POST запрос на создание подборки: title={}, pinned={}, eventsCount={}",
                newCompilationDto.getTitle(),
                newCompilationDto.getPinned(),
                newCompilationDto.getEvents() != null ? newCompilationDto.getEvents().size() : 0);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(compilationService.addCompilation(newCompilationDto));
    }

    @PatchMapping("/{compilationId}")
    public ResponseEntity<CompilationDto> updateCompilation(
            @PathVariable Long compilationId,
            @RequestBody @Valid UpdateCompilationRequestDto updateCompilation
    ) {
        log.info("PATCH запрос на обновление подборки: compilationId={}, title={}, pinned={}",
                compilationId,
                updateCompilation.getTitle(),
                updateCompilation.getPinned());

        return ResponseEntity.status(HttpStatus.OK)
                .body(compilationService.updateCompilation(compilationId, updateCompilation));
    }

    @DeleteMapping("/{compilationId}")
    public ResponseEntity<Void> deleteCompilation(@PathVariable long compilationId) {
        log.info("DELETE запрос на удаление подборки: compilationId={}", compilationId);

        compilationService.deleteCompilation(compilationId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
