package ru.yandex.mail.common.api;

public class RequestTraits {
    private String xRequestId = "";
    private String url = "";
    private String serviceTicket;

    public String getXRequestId() {
        return xRequestId;
    }

    public RequestTraits withXRequestId(String xRequestId) {
        this.xRequestId = xRequestId;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public RequestTraits withUrl(String url) {
        this.url = url;
        return this;
    }

    public String getServiceTicket() {
        return serviceTicket;
    }

    public RequestTraits withServiceTicket(String serviceTicket) {
        this.serviceTicket = serviceTicket;
        return this;
    }
}
