package ru.yandex.market.logistics.cs.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Day {
    @JsonProperty("dayOff")
    private LocalDate dayOff;
    @JsonProperty("created")
    private LocalDateTime created;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Day day = (Day) o;
        return Objects.equals(dayOff, day.dayOff) &&
            Objects.equals(created.toLocalDate(), day.created.toLocalDate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(dayOff, created.toLocalDate());
    }
}
