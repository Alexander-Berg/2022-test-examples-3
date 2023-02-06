package ru.yandex.autotests.reporting.api.beans;

import ru.yandex.autotests.market.stat.requests.AbstractLightweightRequest;
import ru.yandex.autotests.market.stat.requests.LightweightRequest;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by kateleb on 15.11.16.
 */
public class ReportingApiRequest extends AbstractLightweightRequest<ReportingApiRequest> implements LightweightRequest {

    public static ReportingApiConfig config = new ReportingApiConfig();

    public ReportingApiRequest(ReportingApiRequest original) {
        super(original);
    }

    public ReportingApiRequest(String baseUrl) {
        super(baseUrl);
    }

    public static ReportingApiRequest instanceFor(ReportingHandle handle) {
        return new ReportingApiRequest(config.getApiBaseUrl() + handle.asString());
    }

    public static ReportingApiRequest instanceFor(ReportingHandle handle, Map<String, String> urlParams) {
        String appendix = urlParams.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining("&", "?", ""));
        return new ReportingApiRequest(config.getApiBaseUrl() + handle.asString() + appendix);
    }

    public static ReportingApiRequest instanceFor(ReportingHandle handle, String param, String value) {
        String appendix = "?" + param + "=" + value;
        return new ReportingApiRequest(config.getApiBaseUrl() + handle.asString() + appendix);
    }

    @Override
    protected ReportingApiRequest copy(ReportingApiRequest original) {
        return new ReportingApiRequest(original);
    }

    public ReportingApiRequest with(String key, String value) {
        return (ReportingApiRequest) withUrlParam(key, value);
    }
}
