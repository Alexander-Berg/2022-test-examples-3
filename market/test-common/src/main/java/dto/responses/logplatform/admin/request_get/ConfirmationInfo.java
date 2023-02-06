package dto.responses.logplatform.admin.request_get;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ConfirmationInfo {

    @JsonProperty("external_confirmation_code")
    private String externalConfirmationCode;

    @JsonProperty("need_electronic_certificate")
    private boolean needElectronicCertificate;
}
