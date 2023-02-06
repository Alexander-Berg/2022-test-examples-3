package ru.yandex.market.api.listener.expectations;

import ru.yandex.market.api.listener.domain.HttpStatus;
import ru.yandex.market.api.util.ApiCollections;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpResponse {
    private final int httpStatusCode;
    private final byte[] content;
    private final HttpStatus httpStatus;
    private final Map<String, List<String>> httpHeaders;

    public HttpResponse(int status, List<Map.Entry<String, String>> httpHeaders, byte[] content) {
        this.httpStatusCode = status;
        this.content = content;
        this.httpStatus = HttpStatus.valueOf(status);
        this.httpHeaders = getHeader(httpHeaders);
    }

    public byte[] getBody() {
        return content;
    }

    public String getFirstHeader(String headerName) {
        return ApiCollections.firstOrNull(httpHeaders.get(headerName));
    }

    public List<String> getHeader(String headerName) {
        return new ArrayList<>(httpHeaders.get(headerName));
    }

    public Map<String, List<String>> getHeaders() {
        return httpHeaders;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    private Map<String, List<String>> getHeader(List<Map.Entry<String, String>> httpHeaders) {
        Map<String, List<String>> result = new HashMap<>();
        for (Map.Entry<String, String> entry : httpHeaders) {
            if (entry.getKey() == null) {
                continue;
            }
            List<String> httpHeaderValues = result.get(entry.getKey());
            if (httpHeaderValues == null) {
                httpHeaderValues = new ArrayList<>();
            }
            httpHeaderValues.add(entry.getValue());
            result.put(entry.getKey(), httpHeaderValues);
        }
        return result;
    }
}
