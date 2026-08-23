package ru.practicum.ewm;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ViewStats {
    private String app;
    private String uri;
    private Long hits;
}
