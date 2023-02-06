package ru.yandex.market.mbo.common.imageservice;

import ru.yandex.market.mbo.common.imageservice.avatars.AvatarsMetaInfo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author s-ermakov
 */
public class ImageDepotServiceMock implements ImageDepotService {

    private Set<String> imageIds = new HashSet<>();
    private int imageCounter = 0;

    @Override
    public String addImage(byte[] imageBytes, String fileName) throws UploadImageException {
        String imageId = String.valueOf(Arrays.hashCode(imageBytes)) + "_" + fileName + "_" + (imageCounter++);
        imageIds.add(imageId);
        return imageId;
    }

    @Override
    public boolean removeImage(String imageId) {
        return imageIds.remove(imageId);
    }

    @Override
    public String getImageUrl(String imageId) {
        return imageId + "_url";
    }

    @Override
    public String getImageId(String imageUrl) {
        return imageUrl.replace("_url", "");
    }

    @Override
    public AvatarsMetaInfo getMetaInfo(String imageId) {
        return null;
    }
}
