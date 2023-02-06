package ru.yandex.autotests.httpclient.metabeanprocessor.beans;

public class BannerTagBeanBuilder {
    private String value;
    private String tagId;
    private String usesCount;

    public BannerTagBeanBuilder setValue(String value) {
        this.value = value;
        return this;
    }

    public BannerTagBeanBuilder setTagId(String tagId) {
        this.tagId = tagId;
        return this;
    }

    public BannerTagBeanBuilder setUsesCount(String usesCount) {
        this.usesCount = usesCount;
        return this;
    }

    public BannerTagBean createBannerTagBean() {
        return new BannerTagBean(value, tagId, usesCount);
    }
}