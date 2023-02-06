package dto.responses.logplatform.admin.station_tag_list;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class StationTagListResponse {

    @JsonProperty("objects")
    private List<ObjectsItem> objects;

    @JsonProperty("report_meta")
    private ReportMeta reportMeta;
}
