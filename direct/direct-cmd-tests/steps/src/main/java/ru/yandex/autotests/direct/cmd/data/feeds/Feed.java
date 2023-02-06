package ru.yandex.autotests.direct.cmd.data.feeds;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by aleran on 02.09.2015.
 */
public class Feed {

    @SerializedName("source")
    private String source;
    @SerializedName("encrypted_password")
    private String encryptedPassword;
    @SerializedName("feed_id")
    private String feedId;
    @SerializedName("last_refreshed")
    private String lastRefreshed;
    @SerializedName("errors_count")
    private String errorsCount;
    @SerializedName("feed_type")
    private String feedType;
    @SerializedName("business_type")
    private FeedBusinessType businessType;
    @SerializedName("campaigns")
    private List<FeedCampaign> campaigns;
    @SerializedName("cached_file_url")
    private String cachedFileUrl;
    @SerializedName("refresh_interval")
    private String refreshInterval;
    @SerializedName("email")
    private String email;
    @SerializedName("update_status")
    private String updateStatus;
    @SerializedName("cached_file_hash")
    private String cachedFileHash;
    @SerializedName("url")
    private String url;
    @SerializedName("fetch_errors_count")
    private String fetchErrorsCount;
    @SerializedName("last_change")
    private String lastChange;
    @SerializedName("client_id")
    private String clientId;
    @SerializedName("name")
    private String name;
    @SerializedName("warnings_count")
    private String warningsCount;
    @SerializedName("login")
    private String login;
    @SerializedName("offers_count")
    private String offersCount;
    @SerializedName("filename")
    private String filename;
    @SerializedName("categories")
    private List<Category> categories;
    @SerializedName("is_remove_utm")
    private String isRemoveUtm;

    public String getIsRemoveUtm() {
        return isRemoveUtm;
    }

    public Feed withRemoveUtm(String removeUtm) {
        this.isRemoveUtm = removeUtm;
        return this;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public String getFeedId() {
        return feedId;
    }

    public void setFeedId(String feedId) {
        this.feedId = feedId;
    }

    public String getLastRefreshed() {
        return lastRefreshed;
    }

    public void setLastRefreshed(String lastRefreshed) {
        this.lastRefreshed = lastRefreshed;
    }

    public String getErrorsCount() {
        return errorsCount;
    }

    public void setErrorsCount(String errorsCount) {
        this.errorsCount = errorsCount;
    }

    public String getFeedType() {
        return feedType;
    }

    public void setFeedType(String feedType) {
        this.feedType = feedType;
    }

    public FeedBusinessType getBusinessType() {
        return businessType;
    }

    public void setBusinessType(FeedBusinessType businessType) {
        this.businessType = businessType;
    }

    public List<FeedCampaign> getCampaigns() {
        return campaigns;
    }

    public void setCampaigns(List<FeedCampaign> campaigns) {
        this.campaigns = campaigns;
    }

    public String getCachedFileUrl() {
        return cachedFileUrl;
    }

    public void setCachedFileUrl(String cachedFileUrl) {
        this.cachedFileUrl = cachedFileUrl;
    }

    public String getRefreshInterval() {
        return refreshInterval;
    }

    public void setRefreshInterval(String refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUpdateStatus() {
        return updateStatus;
    }

    public void setUpdateStatus(String updateStatus) {
        this.updateStatus = updateStatus;
    }

    public String getCachedFileHash() {
        return cachedFileHash;
    }

    public void setCachedFileHash(String cachedFileHash) {
        this.cachedFileHash = cachedFileHash;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFetchErrorsCount() {
        return fetchErrorsCount;
    }

    public void setFetchErrorsCount(String fetchErrorsCount) {
        this.fetchErrorsCount = fetchErrorsCount;
    }

    public String getLastChange() {
        return lastChange;
    }

    public void setLastChange(String lastChange) {
        this.lastChange = lastChange;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWarningsCount() {
        return warningsCount;
    }

    public void setWarningsCount(String warningsCount) {
        this.warningsCount = warningsCount;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getOffersCount() {
        return offersCount;
    }

    public void setOffersCount(String offersCount) {
        this.offersCount = offersCount;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public Feed withCategories(List<Category> categories) {
        this.categories = categories;
        return this;
    }

    public Feed withBusinessType(FeedBusinessType businessType) {
        this.businessType = businessType;
        return this;
    }
}
