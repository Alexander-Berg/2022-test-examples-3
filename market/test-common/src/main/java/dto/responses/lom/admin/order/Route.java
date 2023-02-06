package dto.responses.lom.admin.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import toolkit.Mapper;

import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute;

@Data
public class Route {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("text")
    private String text;

    public CombinatorRoute getText() {
        return Mapper.mapResponse(text, CombinatorRoute.class);
    }

    public String getRawText() {
        return text;
    }
}
