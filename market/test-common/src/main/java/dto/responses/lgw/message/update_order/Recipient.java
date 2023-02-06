package dto.responses.lgw.message.update_order;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Recipient {

    @JsonProperty("phones")
    private List<PhonesItem> phones;

    @JsonProperty("fio")
    private Fio fio;

    @JsonProperty("email")
    private String email;
}
