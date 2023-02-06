package ru.yandex.autotests.direct.cmd.data.images;

import com.google.gson.annotations.SerializedName;

public class AjaxResizeBannerImageResponse {

    public static final String ERROR_INVALID_IMG_SIZE = "Размер изображения некорректен";

    @SerializedName("image")
    private String image;
    @SerializedName("name")
    private String name;
    @SerializedName("mds_group_id")
    private String mdsGroupId;
    @SerializedName("error")
    private String error;

    public String getImage() {
        return image;
    }

    public AjaxResizeBannerImageResponse withImage(String image) {
        this.image = image;
        return this;
    }

    public String getName() {
        return name;
    }

    public AjaxResizeBannerImageResponse withName(String name) {
        this.name = name;
        return this;
    }

    public String getMdsGroupId() {
        return mdsGroupId;
    }

    public AjaxResizeBannerImageResponse withMdsGroupId(String mdsGroupId) {
        this.mdsGroupId = mdsGroupId;
        return this;
    }

    public String getError() {
        return error;
    }

    public AjaxResizeBannerImageResponse withError(String error) {
        this.error = error;
        return this;
    }
}
