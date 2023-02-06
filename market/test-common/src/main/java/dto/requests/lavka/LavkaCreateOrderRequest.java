package dto.requests.lavka;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class LavkaCreateOrderRequest {
    @JsonProperty("yandex_uid")
    private String yandexUid;
    @JsonProperty("personal_phone_id")
    private String personalPhoneId;
    private Position position;
    private String locale;
    private List<LavkaItem> items;

    @Data
    @AllArgsConstructor
    public static class Position {
        @JsonProperty("place_id")
        private String placeId;
        private List<Double> location;
    }

    @Data
    @AllArgsConstructor
    public static class LavkaItem {
        private String id;
        private String quantity;
    }
}
