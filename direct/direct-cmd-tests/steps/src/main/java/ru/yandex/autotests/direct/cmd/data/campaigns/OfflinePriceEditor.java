package ru.yandex.autotests.direct.cmd.data.campaigns;
//Task: TESTIRT-9409.

import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.irt.testutils.json.JsonUtils;

public class OfflinePriceEditor {
    @SerializeKey("price_context")
    private Integer priceContext;

    @SerializeKey("price_search")
    private Integer priceSearch;

    @SerializeKey("platform")
    private String platform;

    @SerializeKey("simple")
    private Integer isSimple;

    @SerializeKey("price_base_search")
    private String priceBaseSearch;

    @SerializeKey("context_scope")
    private String contextScope;

    @SerializeKey("proc_search")
    private Integer procSearcg;

    @SerializeKey("proc_base_search")
    private String procBaseSearch;

    public String getProcBaseSearch() {
        return procBaseSearch;
    }

    public OfflinePriceEditor withProcBaseSearch(String procBaseSearch) {
        this.procBaseSearch = procBaseSearch;
        return this;
    }

    public Integer getPriceContext() {
        return priceContext;
    }

    public OfflinePriceEditor withPriceContext(Integer priceContext) {
        this.priceContext = priceContext;
        return this;
    }

    public Integer getPriceSearch() {
        return priceSearch;
    }

    public OfflinePriceEditor withPriceSearch(Integer priceSearch) {
        this.priceSearch = priceSearch;
        return this;
    }

    public String getPlatform() {
        return platform;
    }

    public OfflinePriceEditor withPlatform(String platform) {
        this.platform = platform;
        return this;
    }

    public Integer getIsSimple() {
        return isSimple;
    }

    public OfflinePriceEditor withIsSimple(Integer isSimple) {
        this.isSimple = isSimple;
        return this;
    }

    public String getPriceBaseSearch() {
        return priceBaseSearch;
    }

    public OfflinePriceEditor withPriceBaseSearch(String priceBaseSearch) {
        this.priceBaseSearch = priceBaseSearch;
        return this;
    }

    public String getContextScope() {
        return contextScope;
    }

    public OfflinePriceEditor withContextScope(String contextScope) {
        this.contextScope = contextScope;
        return this;
    }

    public Integer getProcSearcg() {
        return procSearcg;
    }

    public OfflinePriceEditor withProcSearch(Integer procSearcg) {
        this.procSearcg = procSearcg;
        return this;
    }
    @Override
    public String toString() {
        return JsonUtils.toString(this);
    }
}
