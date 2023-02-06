package ru.yandex.autotests.direct.cmd.data.commons.banner;

import com.google.gson.annotations.SerializedName;

public class HrefModel {

    @SerializedName("url_protocol")
    private String urlProtocol;

    @SerializedName("href")
    private String href;

    @SerializedName("domain")
    private String domain;

    @SerializedName("domain_sign")
    private String domainSign;

    @SerializedName("domain_redir")
    private String domainRedir;

    @SerializedName("domain_redir_sign")
    private String domainRedirSign;

    @SerializedName("market_rating")
    private Double marketRating;

    public String getUrlProtocol() {
        return urlProtocol;
    }

    public HrefModel withUrlProtocol(String urlProtocol) {
        this.urlProtocol = urlProtocol;
        return this;
    }

    public String getHref() {
        return href;
    }

    public HrefModel withHref(String href) {
        this.href = href;
        return this;
    }

    public String getDomain() {
        return domain;
    }

    public HrefModel withDomain(String domain) {
        this.domain = domain;
        return this;
    }

    public String getDomainSign() {
        return domainSign;
    }

    public HrefModel withDomainSign(String domainSign) {
        this.domainSign = domainSign;
        return this;
    }

    public String getDomainRedir() {
        return domainRedir;
    }

    public HrefModel withDomainRedir(String domainRedir) {
        this.domainRedir = domainRedir;
        return this;
    }

    public String getDomainRedirSign() {
        return domainRedirSign;
    }

    public HrefModel withDomainRedirSign(String domainRedirSign) {
        this.domainRedirSign = domainRedirSign;
        return this;
    }

    public Double getMarketRating() {
        return marketRating;
    }

    public HrefModel withMarketRating(Double marketRating) {
        this.marketRating = marketRating;
        return this;
    }
}
