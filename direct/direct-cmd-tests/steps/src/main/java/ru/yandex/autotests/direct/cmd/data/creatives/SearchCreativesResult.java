package ru.yandex.autotests.direct.cmd.data.creatives;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.List;

public class SearchCreativesResult {

    @SerializedName("per_page")
    private Integer perPage;

    @SerializedName("current_page")
    private Integer currentPage;

    @SerializedName("total_pages")
    private Integer totalPages;

    @SerializedName("other_ids")
    private List<ShortCreative> otherIds;

    private List<Creative> creatives;

    @SerializedName("status")
    private HashMap<String, Integer> status; //Данне о количестве промодерированных и принятых креативов

    @SerializedName("filters")
    private HashMap<String, SearchFilterData> filters;

    @SerializedName("groups")
    private List<CreativeGroup> groups;

    public Integer getPerPage() {
        return perPage;
    }

    public SearchCreativesResult withPerPage(Integer perPage) {
        this.perPage = perPage;
        return this;
    }

    public Integer getCurrentPage() {
        return currentPage;
    }

    public SearchCreativesResult withCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
        return this;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public SearchCreativesResult withTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
        return this;
    }

    public List<ShortCreative> getOtherIds() {
        return otherIds;
    }

    public SearchCreativesResult withOtherIds(List<ShortCreative> otherIds) {
        this.otherIds = otherIds;
        return this;
    }

    public List<Creative> getCreatives() {
        return creatives;
    }

    public SearchCreativesResult withCreatives(List<Creative> creatives) {
        this.creatives = creatives;
        return this;
    }

    public HashMap<String, Integer> getStatus() {
        return status;
    }

    public SearchCreativesResult withStatus(HashMap<String, Integer> status) {
        this.status = status;
        return this;
    }

    public HashMap<String, SearchFilterData> getFilters() {
        return filters;
    }

    public SearchCreativesResult withFilters(HashMap<String, SearchFilterData> filters) {
        this.filters = filters;
        return this;
    }

    public List<CreativeGroup> getGroups() {
        return groups;
    }

    public SearchCreativesResult withGroups(List<CreativeGroup> groups) {
        this.groups = groups;
        return this;
    }
}