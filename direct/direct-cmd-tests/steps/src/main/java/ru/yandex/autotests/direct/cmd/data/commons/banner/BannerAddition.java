package ru.yandex.autotests.direct.cmd.data.commons.banner;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.commons.StatusModerate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.AdditionsItemCalloutsStatusmoderate;

public class BannerAddition {

    @SerializedName("client_id")
    private String clientId;

    @SerializedName("status_moderate")
    private AdditionsItemCalloutsStatusmoderate statusModerate;

    @SerializedName("hash")
    private String hash;

    @SerializedName("flags")
    private String flags;

    @SerializedName("banner_id")
    private String bannerId;

    @SerializedName("additions_item_id")
    private Long additionsItemId;

    @SerializedName("create_time")
    private String createTime;

    @SerializedName("moderate_time")
    private String moderateTime;

    @SerializedName("error")
    private String error;

    @SerializedName("result")
    private String result;

    @SerializedName("additions_item_type")
    private BannersAdditionsType additionsItemType;

    /**
     *
     * @return
     * The clientId
     */
    public String getClientId() {
        return clientId;
    }

    /**
     *
     * @param clientId
     * The client_id
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public BannerAddition withClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    /**
     *
     * @return
     * The statusModerate
     */
    public AdditionsItemCalloutsStatusmoderate getStatusModerate() {
        return statusModerate;
    }

    /**
     *
     * @param statusModerate
     * The status_moderate
     */
    public void setStatusModerate(AdditionsItemCalloutsStatusmoderate statusModerate) {
        this.statusModerate = statusModerate;
    }

    public BannerAddition withStatusModerate(AdditionsItemCalloutsStatusmoderate statusModerate) {
        this.statusModerate = statusModerate;
        return this;
    }

    /**
     *
     * @return
     * The hash
     */
    public String getHash() {
        return hash;
    }

    /**
     *
     * @param hash
     * The hash
     */
    public void setHash(String hash) {
        this.hash = hash;
    }

    public BannerAddition withHash(String hash) {
        this.hash = hash;
        return this;
    }

    /**
     *
     * @return
     * The flags
     */
    public String getFlags() {
        return flags;
    }

    /**
     *
     * @param flags
     * The flags
     */
    public void setFlags(String flags) {
        this.flags = flags;
    }

    public BannerAddition withFlags(String flags) {
        this.flags = flags;
        return this;
    }

    /**
     *
     * @return
     * The bannerId
     */
    public String getBannerId() {
        return bannerId;
    }

    /**
     *
     * @param bannerId
     * The banner_id
     */
    public void setBannerId(String bannerId) {
        this.bannerId = bannerId;
    }

    public BannerAddition withBannerId(String bannerId) {
        this.bannerId = bannerId;
        return this;
    }

    public Long getAdditionsItemId() {
        return additionsItemId;
    }

    public BannerAddition withAdditionsItemId(Long additionsItemId) {
        this.additionsItemId = additionsItemId;
        return this;
    }

    public String getError() {
        return error;
    }

    public BannerAddition withError(String error) {
        this.error = error;
        return this;
    }

    /**
     *
     * @return
     * The createTime
     */
    public String getCreateTime() {
        return createTime;
    }

    /**
     *
     * @param createTime
     * The create_time
     */
    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public BannerAddition withCreateTime(String createTime) {
        this.createTime = createTime;
        return this;
    }

    /**
     *
     * @return
     * The moderateTime
     */
    public String getModerateTime() {
        return moderateTime;
    }

    /**
     *
     * @param moderateTime
     * The moderate_time
     */
    public void setModerateTime(String moderateTime) {
        this.moderateTime = moderateTime;
    }

    public BannerAddition withModerateTime(String moderateTime) {
        this.moderateTime = moderateTime;
        return this;
    }

    /**
     *
     * @return
     * The additionsItemType
     */
    public BannersAdditionsType getAdditionsItemType() {
        return additionsItemType;
    }

    /**
     *
     * @param additionsItemType
     * The additions_item_type
     */
    public void setAdditionsItemType(BannersAdditionsType additionsItemType) {
        this.additionsItemType = additionsItemType;
    }

    public BannerAddition withAdditionsItemType(BannersAdditionsType additionsItemType) {
        this.additionsItemType = additionsItemType;
        return this;
    }

    public String getResult() {
        return result;
    }

    public BannerAddition withResult(String result) {
        this.result = result;
        return this;
    }
}