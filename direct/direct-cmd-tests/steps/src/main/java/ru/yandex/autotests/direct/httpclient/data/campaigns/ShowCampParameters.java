package ru.yandex.autotests.direct.httpclient.data.campaigns;


import ru.yandex.autotests.direct.httpclient.core.AbstractFormParameters;
import ru.yandex.autotests.httpclient.lite.core.FormParameter;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class ShowCampParameters extends AbstractFormParameters {
    @FormParameter("cid")
    private String cid;
    @FormParameter("ulogin")
    private String ulogin;
    @FormParameter("search_banner")
    private String searchBanner;
    @FormParameter("search_by")
    private String searchBy;
    @FormParameter("tab")
    private String tab;
    @FormParameter("interface")
    private String interfaceType;
    @FormParameter("optimizeCamp")
    private String optimizeCamp;
    @FormParameter("page")
    private String page;
    @FormParameter("ws_place")
    private String wsPlace;
    @FormParameter("sb")
    private String sb;
    @FormParameter("uid_url")
    private String uidUrl;

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getUlogin() {
        return ulogin;
    }

    public void setUlogin(String ulogin) {
        this.ulogin = ulogin;
    }

    public String getSearchBanner() {
        return searchBanner;
    }

    public void setSearchBanner(String searchBanner) {
        this.searchBanner = searchBanner;
    }

    public String getSearchBy() {
        return searchBy;
    }

    public void setSearchBy(String searchBy) {
        this.searchBy = searchBy;
    }

    public String getTab() {
        return tab;
    }

    public void setTab(String tab) {
        this.tab = tab;
    }

    public String getInterfaceType() {
        return interfaceType;
    }

    public void setInterfaceType(String interfaceType) {
        this.interfaceType = interfaceType;
    }

    public String getOptimizeCamp() {
        return optimizeCamp;
    }

    public void setOptimizeCamp(String optimizeCamp) {
        this.optimizeCamp = optimizeCamp;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public String getWsPlace() {
        return wsPlace;
    }

    public void setWsPlace(String wsPlace) {
        this.wsPlace = wsPlace;
    }

    public String getSb() {
        return sb;
    }

    public void setSb(String sb) {
        this.sb = sb;
    }

    public String getUidUrl() {
        return uidUrl;
    }

    public void setUidUrl(String uidUrl) {
        this.uidUrl = uidUrl;
    }
}
