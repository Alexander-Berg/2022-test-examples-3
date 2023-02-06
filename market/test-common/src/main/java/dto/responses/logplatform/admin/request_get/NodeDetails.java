package dto.responses.logplatform.admin.request_get;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class NodeDetails {

    @JsonProperty("code")
    private String code;

    @JsonProperty("implementation")
    private Implementation implementation;

    @JsonProperty("branches_limit_for_place")
    private int branchesLimitForPlace;

    @JsonProperty("public_output_carriage_id")
    private String publicOutputCarriageId;

    @JsonProperty("override_processing_interval")
    private String overrideProcessingInterval;

    @JsonProperty("need_output_electronic_certificate")
    private boolean needOutputElectronicCertificate;

    @JsonProperty("visits_limit_for_place")
    private int visitsLimitForPlace;

    @JsonProperty("node_id")
    private String nodeId;

    @JsonProperty("need_input_electronic_certificate")
    private boolean needInputElectronicCertificate;
}
