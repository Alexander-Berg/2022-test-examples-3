package ru.yandex.autotests.direct.httpclient.data.banners.getadgroup;

public class GetAdGroupRequestParametersBuilder {
    private String adGroupId;
    private String count;
    private String page;
    private GetAdGroupTab tab;
    private String device;
    private String archBanners;

    public GetAdGroupRequestParametersBuilder setAdGroupId(String adGroupId) {
        this.adGroupId = adGroupId;
        return this;
    }

    public GetAdGroupRequestParametersBuilder setCount(String count) {
        this.count = count;
        return this;
    }

    public GetAdGroupRequestParametersBuilder setPage(String page) {
        this.page = page;
        return this;
    }

    public GetAdGroupRequestParametersBuilder setTab(GetAdGroupTab tab) {
        this.tab = tab;
        return this;
    }

    public GetAdGroupRequestParametersBuilder setDevice(String device) {
        this.device = device;
        return this;
    }

    public GetAdGroupRequestParametersBuilder setArchBanners(String archBanners) {
        this.archBanners = archBanners;
        return this;
    }

    public GetAdGroupRequestParameters createGetAdGroupRequestParameters() {
        return new GetAdGroupRequestParameters(adGroupId, count, page, tab, device, archBanners);
    }
}