package ru.yandex.autotests.direct.cmd.data.images;

import com.google.gson.annotations.SerializedName;

public class UploadBannerImageResponse {

    public static final String ERROR_IMG_SIZE_TOO_BIG = "Размер изображения не должен превышать 5000 пикселей по длинной стороне";
    public static final String ERROR_IMG_SIZE_TOO_SMALL = "Размер высоты и ширины изображения должен быть не менее 450 пикселей";
    public static final String ERROR_IMG_FORMAT_INVALID = "Недопустимый тип файла изображения, используйте графические форматы GIF, JPEG, PNG";

    private String image;
    private String name;
    @SerializedName("upload_id")
    private String uploadId;
    private Integer width;
    private Integer height;
    @SerializedName("orig_width")
    private Integer origWidth;
    @SerializedName("orig_height")
    private Integer origHeight;
    private String error;

    public String getImage() {
        return image;
    }

    public UploadBannerImageResponse withImage(String image) {
        this.image = image;
        return this;
    }

    public String getName() {
        return name;
    }

    public UploadBannerImageResponse withName(String name) {
        this.name = name;
        return this;
    }

    public String getUploadId() {
        return uploadId;
    }

    public UploadBannerImageResponse withUploadId(String uploadId) {
        this.uploadId = uploadId;
        return this;
    }

    public Integer getWidth() {
        return width;
    }

    public UploadBannerImageResponse withWidth(Integer width) {
        this.width = width;
        return this;
    }

    public Integer getHeight() {
        return height;
    }

    public UploadBannerImageResponse withHeight(Integer height) {
        this.height = height;
        return this;
    }

    public Integer getOrigWidth() {
        return origWidth;
    }

    public UploadBannerImageResponse withOrigWidth(Integer origWidth) {
        this.origWidth = origWidth;
        return this;
    }

    public Integer getOrigHeight() {
        return origHeight;
    }

    public UploadBannerImageResponse withOrigHeight(Integer origHeight) {
        this.origHeight = origHeight;
        return this;
    }

    public String getError() {
        return error;
    }

    public UploadBannerImageResponse withError(String error) {
        this.error = error;
        return this;
    }
}
