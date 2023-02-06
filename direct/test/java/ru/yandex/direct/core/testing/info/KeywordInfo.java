package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.keyword.model.Keyword;

public class KeywordInfo {
    private AdGroupInfo adGroupInfo = new AdGroupInfo();
    private Keyword keyword;

    public AdGroupInfo getAdGroupInfo() {
        return adGroupInfo;
    }

    public void setAdGroupInfo(AdGroupInfo adGroupInfo) {
        this.adGroupInfo = adGroupInfo;
    }

    public KeywordInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        this.adGroupInfo = adGroupInfo;
        return this;
    }

    public Keyword getKeyword() {
        return keyword;
    }

    public void setKeyword(Keyword keyword) {
        this.keyword = keyword;
    }

    public KeywordInfo withKeyword(Keyword keyword) {
        this.keyword = keyword;
        return this;
    }

    public Long getId() {
        return keyword != null ? keyword.getId() : null;
    }

    public Long getAdGroupId() {
        return adGroupInfo != null ? adGroupInfo.getAdGroupId() : null;
    }

    public Long getCampaignId() {
        return adGroupInfo != null ? adGroupInfo.getCampaignId() : null;
    }

    public Integer getShard() {
        return getAdGroupInfo().getShard();
    }


}
