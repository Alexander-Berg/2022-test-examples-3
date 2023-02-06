package dto.responses.idxapi;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Report {

    @JsonProperty("prime")
    private String prime;

    @JsonProperty("delivery")
    private String delivery;

    @JsonProperty("delivery with fake offer")
    private String deliveryWithFakeOffer;

    @JsonProperty("offer_info")
    private String offerInfo;

    @JsonProperty("print_doc")
    private String printDoc;
}
