package dto.requests.lms;

import java.util.List;

import lombok.Data;

@Data
public class HolidayPageDto {
    private List<HolidayDto> content;
}
