package dto.responses.logplatform.admin.request_get;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Registry {

    @JsonProperty("config_template")
    private String configTemplate;

    @JsonProperty("skip_header")
    private boolean skipHeader;

    @JsonProperty("separator")
    private String separator;
}
