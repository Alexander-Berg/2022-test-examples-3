package dto.requests.lms;

import java.time.LocalDate;

import lombok.Data;

@Data
public class HolidayDto {
    private Long id;
    private LocalDate day;
}
