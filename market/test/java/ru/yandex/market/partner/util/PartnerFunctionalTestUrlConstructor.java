package ru.yandex.market.partner.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.apache.commons.lang.StringUtils;

public class PartnerFunctionalTestUrlConstructor {
    private final String baseUrl;
    private String path;
    private final List<String> params = new ArrayList<>();

    private PartnerFunctionalTestUrlConstructor(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public static PartnerFunctionalTestUrlConstructor setBaseUrl(String baseUrl) {
        if (!baseUrl.startsWith("http")) {
            throw new IllegalArgumentException("Base url must start with protocol prefix (http/https)");
        }
        return new PartnerFunctionalTestUrlConstructor(StringUtils.stripEnd(baseUrl, "/"));
    }

    public PartnerFunctionalTestUrlConstructor withPath(String path) {
        this.path = StringUtils.strip(path, "/");
        return this;
    }

    public PartnerFunctionalTestUrlConstructor withAuthorizationParams(int uid, int id) {
        params.add("_user_id=" + uid + "&id=" + id);
        return this;
    }

    public PartnerFunctionalTestUrlConstructor withPagingTokenParams(int limit, String pageToken) {
        params.add("limit=" + limit + (pageToken == null ? "" : "&page_token=" + pageToken));
        return this;
    }

    public <TValue> PartnerFunctionalTestUrlConstructor withCustomParam(String paramName, TValue paramValue) {
        var strValue = String.valueOf(paramValue);
        params.add(paramName + "=" + strValue);
        return this;
    }

    public String getUrl() {
        StringBuilder url = new StringBuilder();
        url.append(baseUrl);
        if (path != null) {
            url.append("/").append(path);
        }
        if (!params.isEmpty()) {
            url.append("?");
        }
        var paramsString = new StringJoiner("&");
        params.forEach(paramsString::add);
        url.append(paramsString);
        return url.toString();
    }
}
