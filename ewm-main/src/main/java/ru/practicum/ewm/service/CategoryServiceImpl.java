package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.category.CategoryDto;
import ru.practicum.ewm.dto.category.NewCategoryDto;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CategoryMapper;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.service.interfaces.CategoryService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryDto addCategory(NewCategoryDto newCategoryDto) {
        log.debug("Добавление категории: name={}", newCategoryDto.getName());

        return CategoryMapper
                .toCategoryDto(categoryRepository.save(CategoryMapper.toCategory(newCategoryDto)));
    }

    @Transactional
    public CategoryDto updateCategory(Long categoryId, CategoryDto categoryDto) {
        log.debug("Обновление категории: id={}, name={}", categoryId, categoryDto.getName());

        Category category = getCategory(categoryId);
        category.setName(categoryDto.getName());
        return CategoryMapper.toCategoryDto(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getCategories(Integer from, Integer size) {
        log.debug("Запрос категорий: from={}, size={}", from, size);

        return categoryRepository.findAll(PageRequest.of(from / size, size)).stream()
                .map(CategoryMapper::toCategoryDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryDto getCategoryById(Long categoryId) {
        log.debug("Запрос категории по id={}", categoryId);

        return CategoryMapper.toCategoryDto(getCategory(categoryId));
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        log.debug("Удаление категории: id={}", categoryId);

        categoryRepository.delete(getCategory(categoryId));
    }

    private Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId).orElseThrow(() ->
                new NotFoundException("Category with id=" + categoryId + " was not found"));
    }
}
