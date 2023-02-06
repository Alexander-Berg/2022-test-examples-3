package dto.responses.tm.register;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import ru.yandex.market.delivery.transport_manager.model.dto.RegisterUnitDto;

@Data
public class TmRegisterSearchResponse {

    @JsonProperty("pageNumber")
    private Integer pageNumber;

    @JsonProperty("data")
    private List<RegisterUnitDto> data;

    @JsonProperty("size")
    private Integer size;

    @JsonProperty("totalPages")
    private Integer totalPages;

    @JsonProperty("totalElements")
    private Integer totalElements;
}
