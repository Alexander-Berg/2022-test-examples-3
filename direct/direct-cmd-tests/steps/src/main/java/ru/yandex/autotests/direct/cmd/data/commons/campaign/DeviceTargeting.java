package ru.yandex.autotests.direct.cmd.data.commons.campaign;

import com.google.gson.annotations.SerializedName;

import java.util.stream.Stream;

public class DeviceTargeting {

    public static DeviceTargeting fromString(String deviceTargetingStr) {
        if (deviceTargetingStr == null) {
            return null;
        }
        DeviceTargeting deviceTargeting = new DeviceTargeting();
        Stream.of(deviceTargetingStr.split(",")).forEach(d -> {
            switch (d) {
                case "other_devices": deviceTargeting.setOtherDevices(1);
                    break;
                case "iphone": deviceTargeting.setIphone(1);
                    break;
                case "ipad": deviceTargeting.setIpad(1);
                    break;
                case "android_phone": deviceTargeting.setAndroidPhone(1);
                    break;
                case "android_tablet": deviceTargeting.setAndroidTablet(1);
                    break;
            }
        });
        return deviceTargeting;
    }

    @SerializedName("android_phone")
    private Integer androidPhone;

    @SerializedName("android_tablet")
    private Integer androidTablet;

    @SerializedName("iphone")
    private Integer iphone;

    @SerializedName("ipad")
    private Integer ipad;

    @SerializedName("other_devices")
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
