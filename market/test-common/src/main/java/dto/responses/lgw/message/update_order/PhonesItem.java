package dto.responses.lgw.message.update_order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PhonesItem {

    @JsonProperty("phoneNumber")
    private String phoneNumber;
}
