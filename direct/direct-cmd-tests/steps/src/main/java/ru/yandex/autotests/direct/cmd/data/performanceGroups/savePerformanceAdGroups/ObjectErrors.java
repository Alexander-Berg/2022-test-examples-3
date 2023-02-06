package ru.yandex.autotests.direct.cmd.data.performanceGroups.savePerformanceAdGroups;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorData;

import java.util.List;

public class ObjectErrors {

    @SerializedName("group_name")
    private List<ErrorData> adgroupNameErrors;

    @SerializedName("performance_filters")
    private Errors filtersErrors;

    @SerializedName("banners")
    private Errors bannersErrors;

    @SerializedName("dynamic_conditions")
    private Errors dinamicConditions;

    @SerializedName("main_domain")
    private List<ErrorData> mainDomain;

    @SerializedName("body")
    private List<ErrorData> body;

    @SerializedName("minus_words")
    private Object minusWords;

    @SerializedName("groups")
    private GroupErrors groupErrors;

    @SerializedName("href_params")
    private List<ErrorData> hrefParams;

    @SerializedName("callout")
    private List<ErrorData> callouts;

    @SerializedName("array_errors")
    private List<ArrayError> arrayErrors;

    @SerializedName("generic_errors")
    private List<ErrorData> genericErrors;

    @SerializedName("hierarchical_multipliers")
    private List<ErrorData> hierarchicalMultipliers;

    public List<ErrorData> getCallouts() {
        return callouts;
    }

    public void setCallouts(List<ErrorData> callouts) {
        this.callouts = callouts;
    }

    public List<ErrorData> getHrefParams() {
        return hrefParams;
    }

    public void setHrefParams(List<ErrorData> hrefParams) {
        this.hrefParams = hrefParams;
    }

    public List<ArrayError> getArrayErrors() {
        return arrayErrors;
    }

    public void setArrayErrors(List<ArrayError> arrayErrors) {
        this.arrayErrors = arrayErrors;
    }

    public List<ErrorData> getGenericErrors() {
        return genericErrors;
    }

    public List<ErrorData> getMainDomain() {
        return mainDomain;
    }

    public List<ErrorData> getBody() {
        return body;
    }

    public void setBody(List<ErrorData> body) {
        this.body = body;
    }

    public void setMainDomain(List<ErrorData> mainDomain) {
        this.mainDomain = mainDomain;
    }

    public void setGenericErrors(List<ErrorData> genericErrors) {
        this.genericErrors = genericErrors;
    }

    public GroupErrors getGroupErrors() {
        return groupErrors;
    }

    public void setGroupErrors(GroupErrors groupErrors) {
        this.groupErrors = groupErrors;
    }

    public Object getMinusWords() {
        return minusWords;
    }

    public void setMinusWords(Object minusWords) {
        this.minusWords = minusWords;
    }

    public List<ErrorData> getAdgroupNameErrors() {
        return adgroupNameErrors;
    }

    public void setAdgroupNameErrors(List<ErrorData> adgroupNameErrors) {
        this.adgroupNameErrors = adgroupNameErrors;
    }

    public Errors getFiltersErrors() {
        return filtersErrors;
    }

    public void setFiltersErrors(Errors filtersErrors) {
        this.filtersErrors = filtersErrors;
    }

    public Errors getBannersErrors() {
        return bannersErrors;
    }

    public void setBannersErrors(Errors bannersErrors) {
        this.bannersErrors = bannersErrors;
    }

    public Errors getDinamicConditions() {
        return dinamicConditions;
    }

    public void setDinamicConditions(Errors dinamicConditions) {
        this.dinamicConditions = dinamicConditions;
    }

    public List<ErrorData> getHierarchicalMultipliers() {
        return hierarchicalMultipliers;
    }

    public void setHierarchicalMultipliers(
            List<ErrorData> hierarchicalMultipliers)
    {
        this.hierarchicalMultipliers = hierarchicalMultipliers;
    }
}
