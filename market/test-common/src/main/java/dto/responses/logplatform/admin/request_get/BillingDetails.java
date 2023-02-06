package dto.responses.logplatform.admin.request_get;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BillingDetails {

    @JsonProperty("i_n_n")
    private String iNN;

    @JsonProperty("assessed_unit_price")
    private int assessedUnitPrice;

    @JsonProperty("refundable")
    private boolean refundable;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("unit_price")
    private int unitPrice;

    @JsonProperty("n_d_s")
    private int nDS;

    @JsonProperty("tax_system_code")
    private int taxSystemCode;
}
