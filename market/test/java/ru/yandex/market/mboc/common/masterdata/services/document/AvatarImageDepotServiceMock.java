package ru.yandex.market.mboc.common.masterdata.services.document;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;

import ru.yandex.market.mbo.common.imageservice.UploadImageException;
import ru.yandex.market.mbo.common.imageservice.avatars.AvatarImageDepotService;
import ru.yandex.market.mbo.common.imageservice.avatars.AvatarsClient;
import ru.yandex.market.mbo.common.imageservice.avatars.AvatarsMetaInfo;

public class AvatarImageDepotServiceMock extends AvatarImageDepotService {

    //imageId -> ImageUrl
    private Map<String, String> images;

    public AvatarImageDepotServiceMock() {
        images = new HashMap<>();
    }

    @Override
    public String addImage(byte[] imageBytes, String fileName) throws UploadImageException {
        images.put(fileName, "url_" + fileName);
        return fileName;
    }

    @Override
    public AvatarsMetaInfo getMetaInfo(String imageId) {
        throw new NotImplementedException("Метод в моке не реализован");
    }

    @Override
    public boolean removeImage(String imageId) {
        boolean exist = images.containsKey(imageId);
        images.remove(imageId);
        return exist;
    }

    @Override
    public String getImageUrl(String imageId) {
        return images.get(imageId);
    }

    @Override
    public String getImageId(String downloadImageUrl) {
        for (Map.Entry<String, String> entry : images.entrySet()) {
            if (entry.getValue().equals(downloadImageUrl)) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public void setReadHostPort(String readHostPort) {
        throw new NotImplementedException("Метод в моке не реализован");
    }

    @Override
    public void setNamespace(String namespace) {
        throw new NotImplementedException("Метод в моке не реализован");
    }

    @Override
    public void setAvatarsClient(AvatarsClient avatarsClient) {
        throw new NotImplementedException("Метод в моке не реализован");
    }

    public boolean isImageExist(String imageUrl) {
        return images.containsValue(imageUrl);
    }

    public Map<String, String> getImages() {
        return new HashMap<>(images);
    }
}
