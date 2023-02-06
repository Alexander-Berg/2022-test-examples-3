package dto.responses.lgw.message.update_order;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PlacesItem {

    @JsonProperty("resourceId")
    private ResourceId resourceId;

    @JsonProperty("partnerCodes")
    private List<PartnerCodesItem> partnerCodes;

    @JsonProperty("korobyte")
    private Korobyte korobyte;

    @JsonProperty("itemPlaces")
    private List<Object> itemPlaces;
}
