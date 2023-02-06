package ru.yandex.autotests.direct.cmd.data.commons.banner;

import com.google.gson.annotations.SerializedName;

/*
* todo javadoc
*/
public class ImageAd {

    @SerializedName("hash")
    private String hash;

    @SerializedName("height")
    private Integer height;

    @SerializedName("width")
    private Integer width;

    @SerializedName("name")
    private String name;

    @SerializedName("scale")
    private String scale;


    public String getHash() {
        return hash;
    }

    public Integer getHeight() {
        return height;
    }

    public Integer getWidth() {
        return width;
    }

    public String getName() {
        return name;
    }

    public String getScale() {
        return scale;
    }

    public ImageAd withHash(String hash) {
        this.hash = hash;
        return this;
    }

    public ImageAd withHeight(Integer height) {
        this.height = height;
        return this;
    }

    public ImageAd withWidth(Integer width) {
        this.width = width;
        return this;
    }

    public ImageAd withName(String name) {
        this.name = name;
        return this;
    }

    public ImageAd withScale(String scale) {
        this.scale = scale;
        return this;
    }
}
