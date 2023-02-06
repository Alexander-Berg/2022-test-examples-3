package ru.yandex.autotests.direct.cmd.data.images;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeBy;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.impl.ValueToFileSerializer;

public class UploadBannerImageRequest extends BasicDirectRequest {

    @SerializeKey("cid")
    private Long campaignId;

    @SerializeKey("uid")
    private Long uid;

    @SerializeKey("image")
    @SerializeBy(ValueToFileSerializer.class)
    private String imagePath;

    @SerializeKey("url")
    private String url;

    public UploadBannerImageRequest withCampaignId(Long campaignId) {
        this.campaignId = campaignId;
        return this;
    }

    public UploadBannerImageRequest withUid(Long uid) {
        this.uid = uid;
        return this;
    }

    public UploadBannerImageRequest withImagePath(String imagePath) {
        this.imagePath = imagePath;
        return this;
    }

    public UploadBannerImageRequest withUrl(String url) {
        this.url = url;
        return this;
    }
}
