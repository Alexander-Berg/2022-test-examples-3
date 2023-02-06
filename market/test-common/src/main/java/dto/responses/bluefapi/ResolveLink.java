package dto.responses.bluefapi;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.apache.commons.lang.StringUtils;

@Data
public class ResolveLink {

    private final List<Object> results;
    private final Collection collections;

    @JsonCreator
    public ResolveLink(
        @JsonProperty(value = "results", required = true) List<Object> results,
        @JsonProperty(value = "collections", required = true) Collection collections
    ) {
        this.results = results;
        this.collections = collections;
    }

    @Data
    public static class Collection {
        private final List<OnDemandUrl> onDemandUrl;

        @JsonCreator
        public Collection(
            @JsonProperty(value = "onDemandUrl", required = true) List<OnDemandUrl> onDemandUrl) {
            this.onDemandUrl = onDemandUrl;
        }

        @Data
        public static class OnDemandUrl {
            private final Long id;
            private final String url;

            @JsonCreator
            public OnDemandUrl(
                @JsonProperty(value = "id", required = true) Long id,
                @JsonProperty(value = "url", required = true) String url) {
                this.id = id;
                this.url = url;
            }

            public String getTransferId() {
                return StringUtils.substringAfterLast(url, "/");
            }
        }
    }
}
