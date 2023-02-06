package dto.responses.lms;

import java.util.List;

import lombok.Data;

@Data
public class LogisticSegmentDto {
    private long id;
    private List<LogisticSegmentServiceDto> services;
}
