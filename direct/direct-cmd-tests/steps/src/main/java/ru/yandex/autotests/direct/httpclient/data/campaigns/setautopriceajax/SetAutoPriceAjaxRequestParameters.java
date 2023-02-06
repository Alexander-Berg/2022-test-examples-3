package ru.yandex.autotests.direct.httpclient.data.campaigns.setautopriceajax;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectRequestParameters;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * Created by shmykov on 11.06.15.
 */
public class SetAutoPriceAjaxRequestParameters extends BasicDirectRequestParameters {

    @JsonPath(requestPath = "cid")
    private String cid;

    @JsonPath(requestPath = "tab_simple")
    private String tabSimple;

    @JsonPath(requestPath = "simple_platform")
    private String simplePlatform;

    @JsonPath(requestPath = "wizard_search_price_base") //вход в гарантию, спецразмещение...
    private String wizardSearchPriceBase;

    @JsonPath(requestPath = "wizard_search_position_ctr_correction")
    private String wizardSearchPositionCtrCorrection;

    @JsonPath(requestPath = "wizard_search_proc") //цена + __ %
    private String wizardSearchProc;

    @JsonPath(requestPath = "wizard_search_max_price") //но не более
    private String wizardSearchMaxPrice;

    @JsonPath(requestPath = "wizard_platform")
    private String wizardPlatform;

    @JsonPath(requestPath = "search_proc_base") //% от цены или от разницы до 1-го места
    private String searchProcBase;

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getTabSimple() {
        return tabSimple;
    }

    public void setTabSimple(String tabSimple) {
        this.tabSimple = tabSimple;
    }

    public String getSimplePlatform() {
        return simplePlatform;
    }

    public void setSimplePlatform(String simplePlatform) {
        this.simplePlatform = simplePlatform;
    }

    public String getSimplePrice() {
        return simplePrice;
    }

    public void setSimplePrice(String simplePrice) {
        this.simplePrice = simplePrice;
    }

    @JsonPath(requestPath = "simple_price")
    private String simplePrice;

    public String getWizardSearchPriceBase() {
        return wizardSearchPriceBase;
    }

    public void setWizardSearchPriceBase(String wizardSearchPriceBase) {
        this.wizardSearchPriceBase = wizardSearchPriceBase;
    }

    public String getWizardSearchPositionCtrCorrection() {
        return wizardSearchPositionCtrCorrection;
    }

    public void setWizardSearchPositionCtrCorrection(String wizardSearchPositionCtrCorrection) {
        this.wizardSearchPositionCtrCorrection = wizardSearchPositionCtrCorrection;
    }

    public String getWizardSearchProc() {
        return wizardSearchProc;
    }

    public void setWizardSearchProc(String wizardSearchProc) {
        this.wizardSearchProc = wizardSearchProc;
    }

    public String getWizardSearchMaxPrice() {
        return wizardSearchMaxPrice;
    }

    public void setWizardSearchMaxPrice(String wizardSearchMaxPrice) {
        this.wizardSearchMaxPrice = wizardSearchMaxPrice;
    }

    public String getSearchProcBase() {
        return searchProcBase;
    }

    public void setSearchProcBase(String searchProcBase) {
        this.searchProcBase = searchProcBase;
    }

    public String getWizardPlatform() {
        return wizardPlatform;
    }

    public void setWizardPlatform(String wizardPlatform) {
        this.wizardPlatform = wizardPlatform;
    }
}
