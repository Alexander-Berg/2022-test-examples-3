package ru.yandex.autotests.direct.cmd.data.banners;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class SearchBannersRequest extends BasicDirectRequest {

    @SerializeKey("reverse")
    private Integer reverse;

    @SerializeKey("sort")
    private String sort;

    @SerializeKey("where")
    private String where;

    @SerializeKey("what")
    private String what;

    @SerializeKey("text_search")
    private String textSearch;

    @SerializeKey("group_camp")
    private String groupCamp;

    @SerializeKey("include_currency_archived_campaigns")
    private String includeCurrencyArchivedCampaigns;

    @SerializeKey("activeonly")
    private String activeOnly;

    @SerializeKey("strict_search")
    private String strictSearch;


    public String getGroupCamp() {
        return groupCamp;
    }

    public void setGroupCamp(String groupCamp) {
        this.groupCamp = groupCamp;
    }

    public String getIncludeCurrencyArchivedCampaigns() {
        return includeCurrencyArchivedCampaigns;
    }

    public void setIncludeCurrencyArchivedCampaigns(String includeCurrencyArchivedCampaigns) {
        this.includeCurrencyArchivedCampaigns = includeCurrencyArchivedCampaigns;
    }

    public String getActiveOnly() {
        return activeOnly;
    }

    public void setActiveOnly(String activeOnly) {
        this.activeOnly = activeOnly;
    }

    public Integer getReverse() {
        return reverse;
    }

    public void setReverse(Integer reverse) {
        this.reverse = reverse;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }


    public String getTextSearch() {
        return textSearch;
    }

    public void setTextSearch(String textSearch) {
        this.textSearch = textSearch;
    }

    public String getWhere() {
        return where;
    }

    public void setWhere(String where) {
        this.where = where;
    }

    public String getWhat() {
        return what;
    }

    public void setWhat(String what) {
        this.what = what;
    }

    public SearchBannersRequest withReverse(Integer reverse) {
        this.reverse = reverse;
        return this;
    }

    public SearchBannersRequest withSort(String sort) {
        this.sort = sort;
        return this;
    }

    public SearchBannersRequest withWhere(String where) {
        this.where = where;
        return this;
    }

    public SearchBannersRequest withWhat(String what) {
        this.what = what;
        return this;
    }

    public SearchBannersRequest withTextSearch(String textSearch) {
        this.textSearch = textSearch;
        return this;
    }

    public SearchBannersRequest withGroupCamp(String groupCamp) {
        this.groupCamp = groupCamp;
        return this;
    }

    public SearchBannersRequest withIncludeCurrencyArchivedCampaigns(String includeCurrencyArchivedCampaigns) {
        this.includeCurrencyArchivedCampaigns = includeCurrencyArchivedCampaigns;
        return this;
    }

    public SearchBannersRequest withActiveOnly(String activeOnly) {
        this.activeOnly = activeOnly;
        return this;
    }

    public SearchBannersRequest withStrictSearch(String strictSearch) {
        this.strictSearch = strictSearch;
        return this;
    }
}