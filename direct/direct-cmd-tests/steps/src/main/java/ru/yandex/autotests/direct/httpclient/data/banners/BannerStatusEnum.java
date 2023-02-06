package ru.yandex.autotests.direct.httpclient.data.banners;

/**
 * Created by shmykov on 05.05.15.
 */
public enum BannerStatusEnum {

    ARCHIVE("arch"),
    ALL("all"),
    DRAFT("draft"),
    ACTIVE("active"),
    STOPPED("off"),
    DECLINED("decline"),
    WAIT("wait");

    private String value;

    BannerStatusEnum(String value) {
        this.value = value;
    }

    public String toString() {
        return this.value;
    }
}
