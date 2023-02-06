package ru.yandex.autotests.direct.cmd.steps.images;

import ru.yandex.autotests.direct.utils.clients.s3.S3Helper;
import ru.yandex.autotests.directapi.model.User;

import java.io.File;

import static ru.yandex.autotests.direct.cmd.util.ImageUtils.createImageInTempFile;

public abstract class AbstractImageUploadHelper {

    public enum UploadType {
        FILE, URL
    }

    protected BannerImagesSteps steps;

    protected String client;
    protected ImageParams imageParams;

    protected File imageFile;
    protected String imageFileName;

    protected String storageImagePath;
    protected String storageImageUrl;
    protected String storageImageName;

    protected UploadType uploadType;

    public void upload() {
        try {
            createImageFile();

            switch (uploadType) {
                case URL: {
                    uploadImageToS3();
                    uploadImageFromUrl();
                }
                break;
                case FILE: {
                    uploadImageFromFile();
                }
            }
        } finally {
            clear();
        }
    }

    protected abstract void uploadImageFromUrl();

    protected abstract void uploadImageFromFile();

    public String getFileImageName() {
        return imageFileName;
    }

    public String getStorageImageUrl() {
        return storageImageUrl;
    }

    public String getStorageImageName() {
        return storageImageName;
    }

    public AbstractImageUploadHelper withClient(String client) {
        this.client = client;
        return this;
    }

    public AbstractImageUploadHelper withImageParams(ImageParams imageParams) {
        this.imageParams = imageParams;
        return this;
    }

    public AbstractImageUploadHelper withUploadType(UploadType uploadType) {
        this.uploadType = uploadType;
        return this;
    }

    public ImageParams imageParams() {
        return imageParams;
    }

    public AbstractImageUploadHelper withBannerImageSteps(BannerImagesSteps steps) {
        this.steps = steps;
        return this;
    }

    private void clear() {
        if (imageFile != null) {
            imageFile.delete();
            imageFile = null;
        }
        if (storageImageName != null) {
            S3Helper.getInstance().deleteTemporaryObject(storageImageName);
            storageImagePath = null;
        }
    }

    protected Long obtainClientUid() {
        return Long.valueOf(User.get(client).getPassportUID());
    }

    private void createImageFile() {
        imageFile = createImageInTempFile(imageParams.getWidth(), imageParams.getHeight(), imageParams.getFormat());
        imageFileName = imageFile.getName();
    }

    private void uploadImageToS3() {
        storageImageName = imageFile.getName();
        storageImageUrl = S3Helper.getInstance().putTemporaryObject(storageImageName, imageFile);
    }
}
