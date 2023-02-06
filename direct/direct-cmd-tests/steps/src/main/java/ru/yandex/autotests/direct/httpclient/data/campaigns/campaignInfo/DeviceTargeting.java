package ru.yandex.autotests.direct.httpclient.data.campaigns.campaignInfo;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 18.05.15
 */
public class DeviceTargeting {

    @JsonPath(responsePath = "android_phone")
    private Integer androidPhone;

    @JsonPath(responsePath = "android_tablet")
    private Integer androidTablet;

    @JsonPath(responsePath = "iphone")
    private Integer iphone;

    @JsonPath(responsePath = "ipad")
    private Integer ipad;

    @JsonPath(responsePath = "other_devices")
    private Integer otherDevices;

    public Integer getAndroidPhone() {
        return androidPhone;
    }

    public void setAndroidPhone(Integer androidPhone) {
        this.androidPhone = androidPhone;
    }

    public Integer getAndroidTablet() {
        return androidTablet;
    }

    public void setAndroidTablet(Integer androidTablet) {
        this.androidTablet = androidTablet;
    }

    public Integer getIphone() {
        return iphone;
    }

    public void setIphone(Integer iphone) {
        this.iphone = iphone;
    }

    public Integer getIpad() {
        return ipad;
    }

    public void setIpad(Integer ipad) {
        this.ipad = ipad;
    }

    public Integer getOtherDevices() {
        return otherDevices;
    }

    public void setOtherDevices(Integer otherDevices) {
        this.otherDevices = otherDevices;
    }

    public DeviceTargeting() {
    }

    public DeviceTargeting(Integer androidPhone, Integer androidTablet, Integer iphone, Integer ipad, Integer otherDevices) {
        this.androidPhone = androidPhone;
        this.androidTablet = androidTablet;
        this.iphone = iphone;
        this.ipad = ipad;
        this.otherDevices = otherDevices;
    }
}
