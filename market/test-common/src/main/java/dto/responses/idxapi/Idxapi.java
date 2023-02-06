package dto.responses.idxapi;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Idxapi {

    @JsonProperty("feed")
    private String feed;

    @JsonProperty("smart_offer")
    private String smartOffer;

    @JsonProperty("published_offer")
    private String publishedOffer;

    @JsonProperty("dukalis")
    private String dukalis;

    @JsonProperty("check_supplier")
    private String checkSupplier;

    @JsonProperty("stocks")
    private String stocks;

    @JsonProperty("dimensions")
    private String dimensions;
}
