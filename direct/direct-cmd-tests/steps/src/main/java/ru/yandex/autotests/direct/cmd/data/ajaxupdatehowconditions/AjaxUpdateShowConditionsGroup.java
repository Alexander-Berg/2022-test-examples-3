package ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions;

import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;

public class AjaxUpdateShowConditionsGroup {
    @SerializedName("copied_from_pid")
    private String copiedFromPid;

    @SerializedName("banners")
    private Map<String, Banner> banners;

    @SerializedName("phrases")
    private Map<String, Phrase> phrases;

    @SerializedName("phrases_not_added")
    private Map<String, AjaxUpdateShowConditions> phrasesNotAdded;

    @SerializedName("errors")
    private List<String> errors;

    @SerializedName("errors_by_phrases")
    private List<ErrorPhrase> errorsByPhrases;

    @SerializedName("is_group_oversized")
    private String isGroupOversized;

    @SerializedName("groups_limit_exceeded")
    private String groupsLimitExceeded;

    @SerializedName("phrases_exceeds_limit_qty")
    private String phrasesExceedsLimitQty;

    public String getCopiedFromPid() {
        return copiedFromPid;
    }

    public AjaxUpdateShowConditionsGroup withCopiedFromPid(String copiedFromPid) {
        this.copiedFromPid = copiedFromPid;
        return this;
    }

    public void setCopiedFromPid(String copiedFromPid) {
        this.copiedFromPid = copiedFromPid;
    }

    public Map<String, Banner> getBanners() {
        return banners;
    }

    public AjaxUpdateShowConditionsGroup withBanners(Map<String, Banner> banners) {
        this.banners = banners;
        return this;
    }

    public void setBanners(Map<String, Banner> banners) {
        this.banners = banners;
    }

    public Map<String, Phrase> getPhrases() {
        return phrases;
    }

    public AjaxUpdateShowConditionsGroup withPhrases(Map<String, Phrase> phrases) {
        this.phrases = phrases;
        return this;
    }

    public void serPhrases(Map<String, Phrase> phrases) {
        this.phrases = phrases;
    }

    public List<String> getErrors() {
        return errors;
    }

    public AjaxUpdateShowConditionsGroup withErrors(List<String> errors) {
        this.errors = errors;
        return this;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public List<ErrorPhrase> getErrorsByPhrases() {
        return errorsByPhrases;
    }

    public AjaxUpdateShowConditionsGroup withErrorsByPhrases(List<ErrorPhrase> errorsByPhrases)
    {
        this.errorsByPhrases = errorsByPhrases;
        return this;
    }

    public Map<String, AjaxUpdateShowConditions> getPhrasesNotAdded() {
        return phrasesNotAdded;
    }

    public AjaxUpdateShowConditionsGroup withPhrasesNotAdded(Map<String, AjaxUpdateShowConditions> phrasesNotAdded) {
        this.phrasesNotAdded = phrasesNotAdded;
        return this;
    }

    public void setPhrasesNotAdded(Map<String, AjaxUpdateShowConditions> phrasesNotAdded) {
        this.phrasesNotAdded = phrasesNotAdded;
    }

    public String getIsGroupOversized() {
        return isGroupOversized;
    }

    public AjaxUpdateShowConditionsGroup withIsGroupOversized(String isGroupOversized) {
        this.isGroupOversized = isGroupOversized;
        return this;
    }

    public String getGroupsLimitExceeded() {
        return groupsLimitExceeded;
    }

    public AjaxUpdateShowConditionsGroup withGroupsLimitExceeded(String groupsLimitExceeded) {
        this.groupsLimitExceeded = groupsLimitExceeded;
        return this;
    }

    public void setGroupsLimitExceeded(String groupsLimitExceeded) {
        this.groupsLimitExceeded = groupsLimitExceeded;
    }

    public String getPhrasesExceedsLimitQty() {
        return phrasesExceedsLimitQty;
    }

    public void setPhrasesExceedsLimitQty(String phrasesExceedsLimitQty) {
        this.phrasesExceedsLimitQty = phrasesExceedsLimitQty;
    }

    public AjaxUpdateShowConditionsGroup withPhrasesExceedsLimitQty(String phrasesExceedsLimitQty) {
        this.phrasesExceedsLimitQty = phrasesExceedsLimitQty;
        return this;
    }
}
