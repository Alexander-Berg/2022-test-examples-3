package ru.yandex.autotests.direct.cmd.data.creatives;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeBy;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.impl.ListToCommaSeparatedStringSerializer;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.impl.ValueToJsonSerializer;

import java.util.List;

public class SearchCreativesRequest extends BasicDirectRequest {

    @SerializeKey("page")
    private Integer page;

    @SerializeKey("per_page")
    private Integer perPage;

    @SerializeKey("count")
    private Integer count;

    @SerializeKey("search")
    private String search;

    @SerializeKey("creative_id")
    @SerializeBy(ListToCommaSeparatedStringSerializer.class)
    private List<Long> creativeIds;

    @SerializeKey("json_filter")
    @SerializeBy(ValueToJsonSerializer.class)
    private List<SearchFilter> jsonFilter;

    @SerializeKey("business_type")
    private String businessType;

    @SerializeKey("feed_type")
    private String feedType;

    @SerializeKey("sort")
    private String sort;

    @SerializeKey("order")
    private String order;

    @SerializeKey("group")
    private Integer group;

    @SerializeKey("group_id")
    private Long groupId;

    public Integer getPage() {
        return page;
    }

    public SearchCreativesRequest withPage(Integer page) {
        this.page = page;
        return this;
    }

    public Integer getPerPage() {
        return perPage;
    }

    public SearchCreativesRequest withPerPage(Integer perPage) {
        this.perPage = perPage;
        return this;
    }

    public Integer getCount() {
        return count;
    }

    public SearchCreativesRequest withCount(Integer count) {
        this.count = count;
        return this;
    }

    public String getSearch() {
        return search;
    }

    public SearchCreativesRequest withSearch(String search) {
        this.search = search;
        return this;
    }

    public List<Long> getCreativeIds() {
        return creativeIds;
    }

    public SearchCreativesRequest withCreative(List<Long> creative) {
        this.creativeIds = creative;
        return this;
    }

    public List<SearchFilter> getJsonFilter() {
        return jsonFilter;
    }

    public SearchCreativesRequest withJsonFilter(List<SearchFilter> jsonFilter) {
        this.jsonFilter = jsonFilter;
        return this;
    }

    public String getBusinessType() {
        return businessType;
    }

    public SearchCreativesRequest withBusinessType(String businessType) {
        this.businessType = businessType;
        return this;
    }

    public String getFeedType() {
        return feedType;
    }

    public SearchCreativesRequest withFeedType(String feedType) {
        this.feedType = feedType;
        return this;
    }

    public String getSort() {
        return sort;
    }

    public SearchCreativesRequest withSort(String sort) {
        this.sort = sort;
        return this;
    }

    public String getOrder() {
        return order;
    }

    public SearchCreativesRequest withOrder(String order) {
        this.order = order;
        return this;
    }

    public Integer getGroup() {
        return group;
    }

    public SearchCreativesRequest withGroup(Integer group) {
        this.group = group;
        return this;
    }

    public Long getGroupId() {
        return groupId;
    }

    public SearchCreativesRequest withGroupId(Long groupId) {
        this.groupId = groupId;
        return this;
    }
}
