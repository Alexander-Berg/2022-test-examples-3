package dto.responses.logplatform.admin.station_tag_list;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ReportMeta {

    @JsonProperty("tag_table_fields")
    private List<TagTableFieldsItem> tagTableFields;
}
