package ru.yandex.market.pers.qa.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.qa.PersQATest;
import ru.yandex.market.pers.qa.client.avatarnica.AvararnicaInfoResponse;
import ru.yandex.market.pers.qa.client.avatarnica.AvatarnicaClient;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.Photo;
import ru.yandex.market.pers.qa.model.QaEntityType;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;


class PhotoServiceTest extends PersQATest {
    @Autowired
    private PhotoService photoService;
    @Autowired
    private AvatarnicaClient avatarnicaClient;

    @Test
    void createPhoto() {
        Photo photo = new Photo(QaEntityType.QUESTION,
            "12345",
            "test namespace",
            "test group id",
            "test image id",
            0);

        long id = photoService.createPhoto(photo);
        Photo bdPhoto = photoService.getPhotoById(id);

        assertAll(
            () -> assertEquals(photo.getEntityType(), bdPhoto.getEntityType()),
            () -> assertEquals(photo.getEntityId(), bdPhoto.getEntityId()),
            () -> assertEquals(photo.getNamespace(), bdPhoto.getNamespace()),
            () -> assertEquals(photo.getGroupId(), bdPhoto.getGroupId()),
            () -> assertEquals(photo.getImageName(), bdPhoto.getImageName()),
            () -> assertEquals(photo.getOrderNumber(), bdPhoto.getOrderNumber())
        );
    }

    @Test
    void testPhotoAutomoderation() {

        Photo photo = new Photo(QaEntityType.QUESTION,"123", "namespace","groupId","name",0);
        long id = photoService.createPhoto(photo);

        //set unknown state for not moderated yet photos
        Mockito.when(avatarnicaClient.getInfo("namespace","groupId","name"))
            .thenReturn(new AvararnicaInfoResponse(false));
        photoService.autoModerate();

        Photo actual = photoService.getPhotoById(id);
        assertEquals(ModState.NEW, actual.getModState());

        // set unknown status for gruesomely photo
        Mockito.when(avatarnicaClient.getInfo("namespace","groupId","name"))
            .thenReturn(new AvararnicaInfoResponse(true, 51, 0, 0));
        photoService.autoModerate();

        actual = photoService.getPhotoById(id);
        assertEquals(ModState.AUTO_FILTER_UNKNOWN, actual.getModState());

        //reset photo
        photo = new Photo(QaEntityType.QUESTION,"333", "namespace3","groupId3","name3",0);
        id = photoService.createPhoto(photo);

        // set rejected status for gruesomely photo
        Mockito.when(avatarnicaClient.getInfo("namespace3","groupId3","name3"))
            .thenReturn(new AvararnicaInfoResponse(true, 101, 0, 0));
        photoService.autoModerate();

        actual = photoService.getPhotoById(id);
        assertEquals(ModState.AUTO_FILTER_REJECTED, actual.getModState());

        //reset photo
        photo = new Photo(QaEntityType.QUESTION,"321", "namespace2","groupId2","name2",0);
        id = photoService.createPhoto(photo);

        // set passed status for gruesomely photo
        Mockito.when(avatarnicaClient.getInfo("namespace2","groupId2","name2"))
            .thenReturn(new AvararnicaInfoResponse(true, 10, 10, 10));
        photoService.autoModerate();

        actual = photoService.getPhotoById(id);
        assertEquals(ModState.AUTO_FILTER_PASSED, actual.getModState());
    }
}
