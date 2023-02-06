package ru.yandex.autotests.direct.httpclient.data.banners.getadgroup;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectRequestParameters;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 10.03.15
 */
public class GetAdGroupRequestParameters extends BasicDirectRequestParameters {

    @JsonPath(requestPath = "adgroup_id")
    private String adGroupId;

    @JsonPath(requestPath = "count")
    private String count;

    @JsonPath(requestPath = "page")
    private String page;

    @JsonPath(requestPath = "tab")
    private GetAdGroupTab tab;

    @JsonPath(requestPath = "device")
    private String device;

    @JsonPath(requestPath = "arch_banners")
    private String archBanners;

    public String getAdGroupId() {
        return adGroupId;
    }

    public void setAdGroupId(String adGroupId) {
        this.adGroupId = adGroupId;
    }

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public GetAdGroupTab getTab() {
        return tab;
    }

    public void setTab(GetAdGroupTab tab) {
        this.tab = tab;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getArchBanners() {
        return archBanners;
    }

    public void setArchBanners(String archBanners) {
        this.archBanners = archBanners;
    }

    public GetAdGroupRequestParameters() {
    }

    public GetAdGroupRequestParameters(String adGroupId, String count, String page, GetAdGroupTab tab, String device, String archBanners) {
        this.adGroupId = adGroupId;
        this.count = count;
        this.page = page;
        this.tab = tab;
        this.device = device;
        this.archBanners = archBanners;
    }
}
