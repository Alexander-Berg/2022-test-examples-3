package dto.requests.partner;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
@JacksonXmlRootElement(localName = "root")
public class GetReferencePickupPointsRequest {

    @JacksonXmlProperty(localName = "token")
    private Long token;
    @JacksonXmlProperty(localName = "hash")
    private Object hash;
    @JacksonXmlProperty(localName = "request")
    private Request request;

    @AllArgsConstructor
    @Data
    public static class Request {
        @JacksonXmlProperty(isAttribute = true)
        private String type;
        @JacksonXmlProperty(localName = "calendarInterval")
        private String calendarInterval;
    }
}
