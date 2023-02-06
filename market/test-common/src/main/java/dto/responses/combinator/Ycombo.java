package dto.responses.combinator;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class Ycombo {

    @JsonProperty("paths")
    private List<String> paths;

    @JsonProperty("sorted_paths")
    private List<String> sortedPaths;

    @JsonProperty("skipped_nodes")
    private List<String> skippedNodes;

    @JsonProperty("skipped_routes")
    private List<String> skippedRoutes;

    @JsonProperty("routes")
    private List<JsonNode> routes;
}
