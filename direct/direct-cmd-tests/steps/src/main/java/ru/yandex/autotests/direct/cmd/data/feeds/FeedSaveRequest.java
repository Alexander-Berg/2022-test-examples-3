package ru.yandex.autotests.direct.cmd.data.feeds;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.direct.db.beans.feeds.FeedType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.FeedsBusinessType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.FeedsSource;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.FeedsRecord;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeBy;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.impl.ValueToFileSerializer;

public class FeedSaveRequest extends BasicDirectRequest {

    public static FeedsRecord getDefaultFeed(String clientId) {
        FeedsRecord record = new FeedsRecord();
        record.setFeedType(FeedType.YANDEX_MARKET.getTypedValue());
        record.setBusinessType(FeedsBusinessType.retail);
        record.setSource(FeedsSource.url);
        record.setName("DefaultFeed");
        record.setUrl("http://yandex.ru");
        record.setRefreshInterval(86400L);
        record.setClientid(Long.valueOf(clientId));
        return record;
    }

    @SerializeKey("feed_id")
    private String feedId;

    @SerializeKey("business_type")
    private FeedBusinessType businessType;

    @SerializeKey("login")
    private String login;

    @SerializeKey("password")
    private String password;

    @SerializeKey("name")
    private String name;

    @SerializeKey("url")
    private String url;

    @SerializeKey("source")
    private String source;

    @SerializeKey("feed_file")
    @SerializeBy(ValueToFileSerializer.class)
    private String feedFile;

    @SerializeKey("is_remove_utm")
    private String isRemoveUtm;

    public FeedBusinessType getBusinessType() {
        return businessType;
    }

    public FeedSaveRequest withBusinessType(FeedBusinessType businessType) {
        this.businessType = businessType;
        return this;
    }

    public String getIsRemoveUtm() {
        return isRemoveUtm;
    }

    public FeedSaveRequest withRemoveUtm(String removeUtm) {
        this.isRemoveUtm = removeUtm;
        return this;
    }

    public String getLogin() {
        return login;
    }

    public FeedSaveRequest withLogin(String login) {
        this.login = login;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public FeedSaveRequest withPassword(String password) {
        this.password = password;
        return this;
    }

    public String getFeedId() {
        return feedId;
    }

    public void setFeedId(String feedId) {
        this.feedId = feedId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getFeedFile() {
        return feedFile;
    }

    public void setFeedFile(String feedFile) {
        this.feedFile = feedFile;
    }

    public FeedSaveRequest withFeedId(String feedId) {
        this.feedId = feedId;
        return this;
    }

    public FeedSaveRequest withName(String name) {
        this.name = name;
        return this;
    }

    public FeedSaveRequest withUrl(String url) {
        this.url = url;
        return this;
    }

    public FeedSaveRequest withSource(String source) {
        this.source = source;
        return this;
    }

    public FeedSaveRequest withFeedFile(String feedFile) {
        this.feedFile = feedFile;
        return this;
    }
}
