package dto.responses.lgw.message.get_courier;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ElectronicAcceptanceCertificate {

    @JsonProperty("code")
    private String code;
}
