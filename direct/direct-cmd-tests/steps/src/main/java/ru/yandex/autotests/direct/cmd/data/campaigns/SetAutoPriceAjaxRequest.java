package ru.yandex.autotests.direct.cmd.data.campaigns;
//Task: TESTIRT-9409.

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class SetAutoPriceAjaxRequest extends BasicDirectRequest {
    @SerializeKey("cid")
    private Long cid;

    @SerializeKey("tab_simple")
    private Integer tabSimple;

    @SerializeKey("simple_platform")
    private String simplePlatform;

    @SerializeKey("simple_price")
    private String simplePrice;

    @SerializeKey("single_price_ctx")
    private String singlePriceCtx;

    @SerializeKey("wizard_search")
    private Integer wizardSearch;

    @SerializeKey("wizard_ctx")
    private Integer wizardCtx;

    @SerializeKey("ctx_max_price")
    private String ctxMaxPrice;

    @SerializeKey("search_proc_ctx")
    private Integer searchProcCtx;

    @SerializeKey("wizard_ctx_phrases")
    private Integer wizardCtxPhrases;

    @SerializeKey("ctx_scope")
    private Integer ctxScope;

    @SerializeKey("ctx_proc")
    private Integer ctxProc;

    @SerializeKey("wizard_search_price_base") //вход в гарантию, спецразмещение...
    private String wizardSearchPriceBase;

    @SerializeKey("wizard_search_position_ctr_correction")
    private Integer wizardSearchPositionCtrCorrection;

    @SerializeKey("wizard_search_proc") //цена + __ %
    private Integer wizardSearchProc;

    @SerializeKey("wizard_search_max_price") //но не более
    private Integer wizardSearchMaxPrice;

    @SerializeKey("wizard_platform")
    private String wizardPlatform;

    @SerializeKey("wizard_network_max_price")
    private Integer wizardNetworkMaxPrice;

    @SerializeKey("wizard_context_phrases")
    private Integer wizardContextPhrases;

    @SerializeKey("wizard_context_retargetings")
    private Integer wizardContextRetargetings;

    @SerializeKey("wizard_network_scope")
    private Integer wizardNetworkScope;

    @SerializeKey("wizard_search_phrases")
    private Integer wizardSearchPhrases;

    @SerializeKey("search_proc_base") //% от цены или от разницы до 1-го места
    private String searchProcBase;

    public String getSearchProcBase() {
        return searchProcBase;
    }

    public SetAutoPriceAjaxRequest withSearchProcBase(String searchProcBase) {
        this.searchProcBase = searchProcBase;
        return this;
    }

    public Long getCid() {
        return cid;
    }

    public SetAutoPriceAjaxRequest withCid(Long cid) {
        this.cid = cid;
        return this;
    }

    public Integer getTabSimple() {
        return tabSimple;
    }

    public SetAutoPriceAjaxRequest withTabSimple(Integer tabSimple) {
        this.tabSimple = tabSimple;
        return this;
    }

    public String getSimplePlatform() {
        return simplePlatform;
    }

    public SetAutoPriceAjaxRequest withSimplePlatform(String simplePlatform) {
        this.simplePlatform = simplePlatform;
        return this;
    }

    public String getSimplePrice() {
        return simplePrice;
    }

    public SetAutoPriceAjaxRequest withSimplePrice(String simplePrice) {
        this.simplePrice = simplePrice;
        return this;
    }

    public String getSinglePriceCtx() {
        return singlePriceCtx;
    }

    public SetAutoPriceAjaxRequest withSinglePriceCtx(String singlePriceCtx) {
        this.singlePriceCtx = singlePriceCtx;
        return this;
    }

    public Integer getWizardSearch() {
        return wizardSearch;
    }

    public SetAutoPriceAjaxRequest withWizardSearch(Integer wizardSearch) {
        this.wizardSearch = wizardSearch;
        return this;
    }

    public Integer getWizardCtx() {
        return wizardCtx;
    }

    public SetAutoPriceAjaxRequest withWizardCtx(Integer wizardCtx) {
        this.wizardCtx = wizardCtx;
        return this;
    }

    public String getCtxMaxPrice() {
        return ctxMaxPrice;
    }

    public SetAutoPriceAjaxRequest withCtxMaxPrice(String ctxMaxPrice) {
        this.ctxMaxPrice = ctxMaxPrice;
        return this;
    }

    public Integer getSearchProcCtx() {
        return searchProcCtx;
    }

    public SetAutoPriceAjaxRequest withSearchProcCtx(Integer searchProcCtx) {
        this.searchProcCtx = searchProcCtx;
        return this;
    }

    public Integer getWizardCtxPhrases() {
        return wizardCtxPhrases;
    }

    public SetAutoPriceAjaxRequest withWizardCtxPhrases(Integer wizardCtxPhrases) {
        this.wizardCtxPhrases = wizardCtxPhrases;
        return this;
    }

    public Integer getCtxScope() {
        return ctxScope;
    }

    public SetAutoPriceAjaxRequest withCtxScope(Integer ctxScope) {
        this.ctxScope = ctxScope;
        return this;
    }

    public Integer getCtxProc() {
        return ctxProc;
    }

    public SetAutoPriceAjaxRequest withCtxProc(Integer ctxProc) {
        this.ctxProc = ctxProc;
        return this;
    }

    public String getWizardSearchPriceBase() {
        return wizardSearchPriceBase;
    }

    public SetAutoPriceAjaxRequest withWizardSearchPriceBase(String wizardSearchPriceBase) {
        this.wizardSearchPriceBase = wizardSearchPriceBase;
        return this;
    }

    public Integer getWizardSearchPositionCtrCorrection() {
        return wizardSearchPositionCtrCorrection;
    }

    public SetAutoPriceAjaxRequest withWizardSearchPositionCtrCorrection(Integer wizardSearchPositionCtrCorrection) {
        this.wizardSearchPositionCtrCorrection = wizardSearchPositionCtrCorrection;
        return this;
    }

    public Integer getWizardSearchProc() {
        return wizardSearchProc;
    }

    public SetAutoPriceAjaxRequest withWizardSearchProc(Integer wizardSearchProc) {
        this.wizardSearchProc = wizardSearchProc;
        return this;
    }

    public Integer getWizardSearchMaxPrice() {
        return wizardSearchMaxPrice;
    }

    public SetAutoPriceAjaxRequest withWizardSearchMaxPrice(Integer wizardSearchMaxPrice) {
        this.wizardSearchMaxPrice = wizardSearchMaxPrice;
        return this;
    }

    public String getWizardPlatform() {
        return wizardPlatform;
    }

    public SetAutoPriceAjaxRequest withWizardPlatform(String wizardPlatform) {
        this.wizardPlatform = wizardPlatform;
        return this;
    }

    public Integer getWizardNetworkMaxPrice() {
        return wizardNetworkMaxPrice;
    }

    public SetAutoPriceAjaxRequest withWizardNetworkMaxPrice(Integer wizardNetworkMaxPrice) {
        this.wizardNetworkMaxPrice = wizardNetworkMaxPrice;
        return this;
    }

    public Integer getWizardContextPhrases() {
        return wizardContextPhrases;
    }

    public SetAutoPriceAjaxRequest withWizardContextPhrases(Integer wizardContextPhrases) {
        this.wizardContextPhrases = wizardContextPhrases;
        return this;
    }

    public Integer getWizardContextRetargetings() {
        return wizardContextRetargetings;
    }

    public SetAutoPriceAjaxRequest withWizardContextRetargetings(Integer wizardContextRetargetings) {
        this.wizardContextRetargetings = wizardContextRetargetings;
        return this;
    }

    public Integer getWizardNetworkScope() {
        return wizardNetworkScope;
    }

    public SetAutoPriceAjaxRequest withWizardNetworkScope(Integer wizardNetworkScope) {
        this.wizardNetworkScope = wizardNetworkScope;
        return this;
    }

    public Integer getWizardSearchPhrases() {
        return wizardSearchPhrases;
    }

    public SetAutoPriceAjaxRequest withWizardSearchPhrases(Integer wizardSearchPhrases) {
        this.wizardSearchPhrases = wizardSearchPhrases;
        return this;
    }
}
