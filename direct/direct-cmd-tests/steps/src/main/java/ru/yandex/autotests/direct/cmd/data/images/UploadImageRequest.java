package ru.yandex.autotests.direct.cmd.data.images;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeBy;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.impl.ValueToFileSerializer;

public class UploadImageRequest extends BasicDirectRequest {

    @SerializeKey("url")
    private String url;

    @SerializeKey("picture")
    @SerializeBy(ValueToFileSerializer.class)
    private String picture;

    @SerializeKey("callback")
    private String callback;

    @SerializeKey("bid")
    private String bannerId;

    @SerializeKey("banner_type")
    private String bannerType;

    public String getBannerId() {
        return bannerId;
    }

    public UploadImageRequest withBannerId(String bannerId) {
        this.bannerId = bannerId;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public UploadImageRequest withUrl(String url) {
        this.url = url;
        return this;
    }

    public String getPicture() {
        return picture;
    }

    public UploadImageRequest withPicture(String picture) {
        this.picture = picture;
        return this;
    }

    public String getCallback() {
        return callback;
    }

    public UploadImageRequest withCallback(String callback) {
        this.callback = callback;
        return this;
    }

    public String getBannerType() {
        return bannerType;
    }

    public UploadImageRequest withBannerType(String bannerType) {
        this.bannerType = bannerType;
        return this;
    }
}
