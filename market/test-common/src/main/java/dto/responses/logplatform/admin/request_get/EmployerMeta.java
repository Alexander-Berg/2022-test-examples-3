package dto.responses.logplatform.admin.request_get;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class EmployerMeta {

    @JsonProperty("registry")
    private Registry registry;

    @JsonProperty("default_inn")
    private String defaultInn;

    @JsonProperty("default_store_id")
    private String defaultStoreId;

    @JsonProperty("default_nds")
    private int defaultNds;

    @JsonProperty("ndd_route_policy")
    private String nddRoutePolicy;

    @JsonProperty("brand_name")
    private String brandName;

    @JsonProperty("corp_client_id")
    private String corpClientId;
}
