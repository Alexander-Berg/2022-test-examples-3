package ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions;

import com.google.gson.annotations.SerializedName;

public class AjaxUpdateShowConditions {
    @SerializedName("price")
    private String price;

    @SerializedName("price_context")
    private String priceContext;

    @SerializedName("autobudgetPriority")
    private String autobudgetPriority;

    @SerializedName("phrase")
    private String phrase;

    @SerializedName("norm_phrase")
    private String normalizedPhrase;

    @SerializedName("is_suspended")
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

    public AjaxUpdateShowConditions withIsSuspended(String isSuspended) {
        this.isSuspended = isSuspended;
        return this;
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

    public AjaxUpdateShowConditions withPrice(String price) {
        this.price = price;
        return this;
    }

    public AjaxUpdateShowConditions withAutobudgetPriority(String autobudgetPriority) {
        this.autobudgetPriority = autobudgetPriority;
        return this;
    }

    public AjaxUpdateShowConditions withPhrase(String phrase) {
        this.phrase = phrase;
        return this;
    }

    public AjaxUpdateShowConditions withNormalizedPhrase(String normalizedPhrase) {
        this.normalizedPhrase = normalizedPhrase;
        return this;
    }

    public String getPriceContext() {
        return priceContext;
    }

    public AjaxUpdateShowConditions withPriceContext(String priceContext) {
        this.priceContext = priceContext;
        return this;
    }

    public void setPriceContext(String priceContext) {
        this.priceContext = priceContext;
    }
}
