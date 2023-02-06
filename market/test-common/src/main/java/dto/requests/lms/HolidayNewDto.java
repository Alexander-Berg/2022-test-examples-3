package dto.requests.lms;

import java.time.LocalDate;

import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class HolidayNewDto {
    @Getter
    @NotNull
    private final LocalDate day;
}
