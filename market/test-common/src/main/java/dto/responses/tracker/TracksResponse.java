package dto.responses.tracker;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TracksResponse {

    @JsonProperty("tracks")
    private List<TracksItem> tracks;
}
