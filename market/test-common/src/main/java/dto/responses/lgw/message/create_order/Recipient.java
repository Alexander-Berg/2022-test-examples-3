package dto.responses.lgw.message.create_order;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Recipient {

    @JsonProperty("phones")
    private List<PhonesItem> phones;

    @JsonProperty("fio")
    private Fio fio;

    @JsonProperty("email")
    private String email;
}
