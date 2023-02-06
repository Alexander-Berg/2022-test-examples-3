package ru.yandex.autotests.direct.cmd.data.banners;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.utils.model.PerlBoolean;

public class BannerStatuses {

    private String statusShow;

    public String getStatusShow() {
        return statusShow;
    }

    public void setStatusShow(String statusShow) {
        this.statusShow = statusShow;
    }

    public BannerStatuses withStatusShow(String statusShow) {
        this.statusShow = statusShow;
        return this;
    }
}
