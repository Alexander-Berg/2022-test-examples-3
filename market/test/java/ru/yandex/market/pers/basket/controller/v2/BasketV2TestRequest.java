package ru.yandex.market.pers.basket.controller.v2;

import java.util.UUID;

import ru.yandex.market.pers.list.model.UserIdType;
import ru.yandex.market.pers.list.model.v2.enums.MarketplaceColor;
import ru.yandex.market.pers.list.model.v2.enums.RequestPlatform;

/**
 * @author ifilippov5
 */
public abstract class BasketV2TestRequest implements Cloneable {

    private String platformHeader = RequestPlatform.WEB.getName();
    private String reqIdHeader = UUID.randomUUID().toString();

    private String userIdType = UserIdType.UUID.name();
    private String userAnyId = "tesasasaassastUuid23";
    private String rgb = MarketplaceColor.BLUE.getName();

    private Integer regionId;

    public String getPlatformHeader() {
        return platformHeader;
    }

    public void setPlatformHeader(String platformHeader) {
        this.platformHeader = platformHeader;
    }

    public String getReqIdHeader() {
        return reqIdHeader;
    }

    public void setReqIdHeader(String reqIdHeader) {
        this.reqIdHeader = reqIdHeader;
    }


    public String getUserIdType() {
        return userIdType;
    }

    public void setUserIdType(String userIdType) {
        this.userIdType = userIdType;
    }

    public void setUserIdType(UserIdType userIdType) {
        this.userIdType = userIdType.name();
    }

    public String getUserAnyId() {
        return userAnyId;
    }

    public void setUserAnyId(String userAnyId) {
        this.userAnyId = userAnyId;
    }

    public String getRgb() {
        return rgb;
    }

    public void setRgb(String rgb) {
        this.rgb = rgb;
    }

    public BasketV2TestRequest clone() throws CloneNotSupportedException {
        return (BasketV2TestRequest) super.clone();
    }

    public Integer getRegionId() {
        return regionId;
    }

    public void setRegionId(Integer regionId) {
        this.regionId = regionId;
    }
}
