package dto.responses.logplatform.admin.request_get;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FeaturesItem {

    @JsonProperty("resource_personal_info_feature")
    private ResourcePersonalInfoFeature resourcePersonalInfoFeature;

    @JsonProperty("class_name")
    private String className;

    @JsonProperty("resource_billing_feature")
    private ResourceBillingFeature resourceBillingFeature;

    @JsonProperty("resource_physical_feature")
    private ResourcePhysicalFeature resourcePhysicalFeature;
}
