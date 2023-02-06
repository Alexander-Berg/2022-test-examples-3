package ru.yandex.autotests.direct.cmd.steps.images;

import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.images.AjaxResizeBannerImageResponse;
import ru.yandex.autotests.direct.cmd.data.images.UploadBannerImageResponse;
import ru.yandex.autotests.direct.cmd.util.ImageUtils;

/*
* todo javadoc
*/
public class ImageUploadHelper extends AbstractImageUploadHelper {

    public ImageUploadHelper() {
        uploadType = UploadType.FILE;
        imageParams = new ImageParams()
                .withHeight(450)
                .withWidth(450)
                .withFormat(ImageUtils.ImageFormat.JPG);
    }

    private long campaignId;

    private UploadBannerImageResponse uploadResponse;
    private AjaxResizeBannerImageResponse resizeResponse;

    public void resize() {
        resizeResponse = resizeImage(uploadResponse, campaignId);
    }

    public void fillBannerByUploadedImage(Banner banner) {
        switch (uploadType) {
            case URL:
                banner.withUploadedImageFromUrl(resizeResponse, storageImageUrl);
                break;
            case FILE:
                banner.withUploadedImageFromFile(resizeResponse);
        }
    }

    public void uploadAndResize() {
        upload();
        resize();
    }

    public UploadBannerImageResponse getUploadResponse() {
        return uploadResponse;
    }

    public AjaxResizeBannerImageResponse getResizeResponse() {
        return resizeResponse;
    }

    @Override
    protected void uploadImageFromUrl() {
        uploadResponse = steps.uploadBannerImage(obtainClientUid(), campaignId, storageImageUrl);
    }

    @Override
    protected void uploadImageFromFile() {
        uploadResponse = steps.uploadBannerImage(obtainClientUid(), campaignId, imageFile);
    }

    public ImageUploadHelper forCampaign(long campaignId) {
        this.campaignId = campaignId;
        return this;
    }

    protected AjaxResizeBannerImageResponse resizeImage(UploadBannerImageResponse uploadResponse, long campaignId) {
        return steps.resizeBannerImage(
                uploadResponse, client, campaignId,
                imageParams.getResizeX1(), imageParams.getResizeX2(),
                imageParams.getResizeY1(), imageParams.getResizeY2());
    }
}
