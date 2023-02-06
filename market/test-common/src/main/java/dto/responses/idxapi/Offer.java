package dto.responses.idxapi;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Offer {

    @JsonProperty("hits_count")
    private Integer hitsCount;

    @JsonProperty("ware_md5")
    private String wareMd5;

    @JsonProperty("hits_count_ann")
    private Integer hitsCountAnn;

    @JsonProperty("type")
    private String type;

    @JsonProperty("promos")
    private List<PromosItem> promos;

    @JsonProperty("offer_id")
    private String offerId;

    @JsonProperty("feed_id")
    private String feedId;

    @JsonProperty("warehouse_id")
    private Integer warehouseId;
}
