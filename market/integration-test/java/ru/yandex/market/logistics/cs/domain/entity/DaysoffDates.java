package ru.yandex.market.logistics.cs.domain.entity;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DaysoffDates {
    @JsonProperty("dates")
    private List<LocalDate> daysOff;
    @JsonProperty("days")
    private List<Day> days;
}
