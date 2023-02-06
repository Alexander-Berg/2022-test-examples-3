package dto.responses.logplatform.admin.request_get;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PhoneContact {

    @JsonProperty("phone_number")
    private String phoneNumber;
}
