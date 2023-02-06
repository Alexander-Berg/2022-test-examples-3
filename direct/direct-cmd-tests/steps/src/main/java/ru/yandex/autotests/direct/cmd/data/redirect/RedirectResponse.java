package ru.yandex.autotests.direct.cmd.data.redirect;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import ru.yandex.autotests.httpclientlite.core.ResponseContent;

import java.util.Map;

public class RedirectResponse {

    private String location;
    private Map<String, String> locationParams;
    private ResponseContent content;

    public RedirectResponse(String location,
                            Map<String, String> locationParams,
                            ResponseContent content) {
        this.location = location;
        this.locationParams = locationParams;
        this.content = content;
    }

    public String getLocation() {
        return location;
    }

    public Map<String, String> getLocationParams() {
        return locationParams;
    }

    public String getLocationParam(LocationParam locationParam) {
        return locationParams.get(locationParam.toString());
    }

    public Long getLocationParamAsLong(LocationParam locationParam) {
        String value = getLocationParam(locationParam);
        return value != null ? Long.parseLong(value) : null;
    }

    public Integer getLocationParamAsInteger(LocationParam locationParam) {
        String value = getLocationParam(locationParam);
        return value != null ? Integer.parseInt(value) : null;
    }

    public ResponseContent getContent() {
        return content;
    }

    public Document getDocumentContent() {
        return Jsoup.parse(content.asString());
    }
}
