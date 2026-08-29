package ru.practicum.ewm.dto.user;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewUserRequestDto {
    @NotBlank
    @Size(min = 2, max = 250)
    private String name;

    @NotEmpty
    @Email
    @Size(min = 6, max = 254)
    private String email;
}
