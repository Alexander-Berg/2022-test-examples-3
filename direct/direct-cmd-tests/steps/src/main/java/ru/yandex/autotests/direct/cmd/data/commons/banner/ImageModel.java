package ru.yandex.autotests.direct.cmd.data.commons.banner;

import com.google.gson.annotations.SerializedName;

public class ImageModel {

    @SerializedName("image")
    private String image;

    @SerializedName("image_name")
    private String imageName;

    @SerializedName("source_image")
    private String imageSource;

    @SerializedName("image_source_url")
    private String imageSourceUrl;

    public String getImage() {
        return image;
    }

    public ImageModel withImage(String image) {
        this.image = image;
        return this;
    }

    public String getImageName() {
        return imageName;
    }

    public ImageModel withImageName(String imageName) {
        this.imageName = imageName;
        return this;
    }

    public String getImageSource() {
        return imageSource;
    }

    public ImageModel withImageSource(String imageSource) {
        this.imageSource = imageSource;
        return this;
    }

    public String getImageSourceUrl() {
        return imageSourceUrl;
    }

    public ImageModel withImageSourceUrl(String imageSourceUrl) {
        this.imageSourceUrl = imageSourceUrl;
        return this;
    }
}
