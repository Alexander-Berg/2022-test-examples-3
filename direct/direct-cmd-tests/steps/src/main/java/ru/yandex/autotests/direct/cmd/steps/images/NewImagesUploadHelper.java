package ru.yandex.autotests.direct.cmd.steps.images;

import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.BannerType;
import ru.yandex.autotests.direct.cmd.data.commons.banner.ImageAd;
import ru.yandex.autotests.direct.cmd.data.images.UploadImageRequest;
import ru.yandex.autotests.direct.cmd.data.images.UploadImageResponse;
import ru.yandex.autotests.direct.cmd.util.ImageUtils;
import ru.yandex.autotests.directapi.beans.images.ImageFormat;

public class NewImagesUploadHelper extends AbstractImageUploadHelper {
    public final static ImageFormat DEFAULT_PICTURE_FORMAT = new ImageFormat("300", "250");

    public NewImagesUploadHelper() {
        uploadType = UploadType.FILE;
        imageParams = new ImageParams()
                .withHeight(Integer.valueOf(DEFAULT_PICTURE_FORMAT.getHeight()))
                .withWidth(Integer.valueOf(DEFAULT_PICTURE_FORMAT.getWidth()))
                .withFormat(ImageUtils.ImageFormat.JPG);
    }

    public NewImagesUploadHelper(int height, int width, ImageUtils.ImageFormat format) {
        uploadType = UploadType.FILE;
        imageParams = new ImageParams()
                .withHeight(height)
                .withWidth(width)
                .withFormat(format);
    }

    private UploadImageResponse uploadResponse;
    private String bannerId;
    private String bannerType;

    @Override
    protected void uploadImageFromUrl() {
        UploadImageRequest request = new UploadImageRequest()
                .withUrl(storageImageUrl)
                .withBannerType(bannerType)
                .withUlogin(client);
        uploadResponse = steps.postUploadImage(request);
    }

    @Override
    protected void uploadImageFromFile() {
        UploadImageRequest request = new UploadImageRequest()
                .withPicture(imageFile.getAbsolutePath())
                .withBannerId(getBannerId())
                .withBannerType(bannerType)
                .withUlogin(client);
        uploadResponse = steps.postUploadImage(request);
    }

    public void fillBannerByUploadedImage(Banner banner) {
        banner.withImageAd(
                new ImageAd().withHash(uploadResponse.getHash())
        );
    }

    public UploadImageResponse getUploadResponse() {
        return uploadResponse;
    }

    public String getBannerId() {
        return bannerId;
    }

    public NewImagesUploadHelper withBannerId(String bannerId) {
        this.bannerId = bannerId;
        return this;
    }

    public String getBannerType() {
        return bannerType;
    }

    public NewImagesUploadHelper withBannerType(String bannerType) {
        this.bannerType = bannerType;
        return this;
    }

    public NewImagesUploadHelper withBannerType(BannerType bannerType) {
        this.bannerType = (bannerType != null)? bannerType.toString(): null;
        return this;
    }

    public static Banner fromUploadPictureResponse(UploadImageResponse uploadPictureResponse) {
        return new Banner()
                .withImageAd(new ImageAd().withHash(uploadPictureResponse.getHash()))
                .withHeight(uploadPictureResponse.getHeight())
                .withWidth(uploadPictureResponse.getWidth());
    }
}
