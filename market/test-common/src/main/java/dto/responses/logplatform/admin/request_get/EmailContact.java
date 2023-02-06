package dto.responses.logplatform.admin.request_get;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class EmailContact {

    @JsonProperty("email_address")
    private String emailAddress;
}
