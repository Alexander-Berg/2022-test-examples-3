package ru.yandex.autotests.direct.cmd.data.images;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class AjaxResizeBannerImageRequest extends BasicDirectRequest {

    public static AjaxResizeBannerImageRequest forUploadedImage(UploadBannerImageResponse response) {
        return new AjaxResizeBannerImageRequest().
                withUploadId(Integer.valueOf(response.getUploadId())).
                withImage(response.getImage());
    }

    @SerializeKey("cid")
    private Long campaignId;
    @SerializeKey("upload_id")
    private Integer uploadId;
    @SerializeKey("image")
    private String image;
    @SerializeKey("x1")
    private Integer x1;
    @SerializeKey("x2")
    private Integer x2;
    @SerializeKey("y1")
    private Integer y1;
    @SerializeKey("y2")
    private Integer y2;

    public AjaxResizeBannerImageRequest withCampaignId(Long campaignId) {
        this.campaignId = campaignId;
        return this;
    }

    public AjaxResizeBannerImageRequest withUploadId(Integer uploadId) {
        this.uploadId = uploadId;
        return this;
    }

    public AjaxResizeBannerImageRequest withImage(String image) {
        this.image = image;
        return this;
    }

    public AjaxResizeBannerImageRequest withX1(Integer x1) {
        this.x1 = x1;
        return this;
    }

    public AjaxResizeBannerImageRequest withX2(Integer x2) {
        this.x2 = x2;
        return this;
    }

    public AjaxResizeBannerImageRequest withY1(Integer y1) {
        this.y1 = y1;
        return this;
    }

    public AjaxResizeBannerImageRequest withY2(Integer y2) {
        this.y2 = y2;
        return this;
    }
}
