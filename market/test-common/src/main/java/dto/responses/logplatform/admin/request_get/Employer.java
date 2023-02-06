package dto.responses.logplatform.admin.request_get;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Employer {

    @JsonProperty("employer_code")
    private String employerCode;

    @JsonProperty("employer_type")
    private String employerType;

    @JsonProperty("employer_meta")
    private EmployerMeta employerMeta;

    @JsonProperty("employer_id")
    private String employerId;
}
