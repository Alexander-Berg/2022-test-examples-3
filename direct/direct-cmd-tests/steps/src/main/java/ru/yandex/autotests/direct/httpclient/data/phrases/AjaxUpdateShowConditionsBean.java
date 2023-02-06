package ru.yandex.autotests.direct.httpclient.data.phrases;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * Created by shmykov on 28.05.15.
 */
public class AjaxUpdateShowConditionsBean {

    @JsonPath(requestPath = "price", responsePath = "price")
    private String price;

    @JsonPath(requestPath = "autobudgetPriority", responsePath = "autobudgetPriority")
    private String autobudgetPriority;

    @JsonPath(requestPath = "phrase", responsePath = "phrase")
    private String phrase;

    @JsonPath(responsePath = "norm_phrase")
    private String normalizedPhrase;

    @JsonPath(requestPath = "is_suspended", responsePath = "is_suspended")
    private String isSuspended;

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getAutobudgetPriority() {
        return autobudgetPriority;
    }

    public void setAutobudgetPriority(String autobudgetPriority) {
        this.autobudgetPriority = autobudgetPriority;
    }

    public void setPhrase(String phrase) {
        this.phrase = phrase;
    }

    public String getIsSuspended() {
        return isSuspended;
    }

    public void setIsSuspended(String isSuspended) {
        this.isSuspended = isSuspended;
    }

    public String getNormalizedPhrase() {
        return normalizedPhrase;
    }

    public void setNormalizedPhrase(String normalizedPhrase) {
        this.normalizedPhrase = normalizedPhrase;
    }

    public String getPhrase() {
        return phrase;
    }

    public AjaxUpdateShowConditionsBean withPrice(String price) {
        this.price = price;
        return this;
    }

    public AjaxUpdateShowConditionsBean withAutobudgetPriority(String autobudgetPriority) {
        this.autobudgetPriority = autobudgetPriority;
        return this;
    }

    public AjaxUpdateShowConditionsBean withPhrase(String phrase) {
        this.phrase = phrase;
        return this;
    }

    public AjaxUpdateShowConditionsBean withNormalizedPhrase(String normalizedPhrase) {
        this.normalizedPhrase = normalizedPhrase;
        return this;
    }

    public AjaxUpdateShowConditionsBean withIsSuspended(String isSuspended) {
        this.isSuspended = isSuspended;
        return this;
    }
}
