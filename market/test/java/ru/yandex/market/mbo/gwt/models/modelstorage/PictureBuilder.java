package ru.yandex.market.mbo.gwt.models.modelstorage;

import ru.yandex.market.mbo.gwt.models.ModificationSource;

import java.util.Date;

/**
 * @author s-ermakov
 */
public class PictureBuilder {

    private Picture picture = new Picture();

    private PictureBuilder() {
    }

    public static PictureBuilder newBuilder() {
        return new PictureBuilder();
    }

    public static PictureBuilder newBuilder(String url, Integer width, Integer height) {
        return newBuilder(null, url, width, height);
    }

    public static PictureBuilder newBuilder(String xslName, String url, Integer width, Integer height) {
        return new PictureBuilder()
            .setXslName(xslName)
            .setUrl(url)
            .setWidth(width)
            .setHeight(height);
    }

    public PictureBuilder setXslName(String xslName) {
        picture.setXslName(xslName);
        return this;
    }

    public PictureBuilder setUrl(String url) {
        picture.setUrl(url);
        return this;
    }

    public PictureBuilder setWidth(Integer width) {
        picture.setWidth(width);
        return this;
    }

    public PictureBuilder setHeight(Integer height) {
        picture.setHeight(height);
        return this;
    }

    public PictureBuilder setUrlSource(String urlSource) {
        picture.setUrlSource(urlSource);
        return this;
    }

    public PictureBuilder setUrlOrig(String urlOrig) {
        picture.setUrlOrig(urlOrig);
        return this;
    }

    public PictureBuilder setModificationSource(ModificationSource modificationSource) {
        picture.setModificationSource(modificationSource);
        return this;
    }

    public PictureBuilder setLastModificationUid(Long lastModificationUid) {
        picture.setLastModificationUid(lastModificationUid);
        return this;
    }

    public PictureBuilder setLastModificationDate(Date lastModificationDate) {
        picture.setLastModificationDate(lastModificationDate);
        return this;
    }

    public PictureBuilder setColorness(Double colorness) {
        picture.setColorness(colorness);
        return this;
    }

    public PictureBuilder setColornessAvg(Double colornessAvg) {
        picture.setColornessAvg(colornessAvg);
        return this;
    }

    public PictureBuilder setIsWhiteBackground(Boolean isWhiteBackground) {
        picture.setIsWhiteBackground(isWhiteBackground);
        return this;
    }

    public Picture build() {
        return new Picture(this.picture);
    }
}
