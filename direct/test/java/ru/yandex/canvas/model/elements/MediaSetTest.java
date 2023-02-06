package ru.yandex.canvas.model.elements;

import java.util.Collections;
import java.util.Locale;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.model.Bundle;
import ru.yandex.canvas.model.CreativeData;
import ru.yandex.canvas.model.File;
import ru.yandex.canvas.model.MediaSet;
import ru.yandex.canvas.model.MediaSetItem;
import ru.yandex.canvas.model.MediaSetSubItem;
import ru.yandex.canvas.model.stillage.StillageFileInfo;
import ru.yandex.canvas.service.AvatarsService;
import ru.yandex.canvas.service.FileService;
import ru.yandex.canvas.service.StillageService;

import static org.junit.Assert.assertEquals;

/**
 * @author skirsanov
 */
@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MediaSetTest {

    @Autowired
    private LocalValidatorFactoryBean validator;

    @Autowired
    private AvatarsService avatarsService;

    @MockBean
    private StillageService stillageService;

    @MockBean
    private FileService fileService;

    @Before
    public void setUp() {
        Mockito.when(avatarsService.getReadServiceHost()).thenReturn("avatars.mdst.yandex.net");
        StillageFileInfo fileInfo = new StillageFileInfo();
        fileInfo.setFileSize(1);  // so the NotTooBigFileValidator passes

        Mockito.when(stillageService.getById("1")).thenReturn(Optional.of(fileInfo));
        /* These tests check localized error message, so we need to set up correct locale
         * to be able to run them on any environment
         * TODO add tanker key tag near the error message and check using it
         */
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        File file1 = new File();
        file1.setId("1");
        file1.setStillageFileInfo(fileInfo);

        Mockito.when(fileService.getByIdInternal("1")).thenReturn(Optional.of(file1));
    }

    @Test
    public void testMediaSetOkValidation() {
        final MediaSetSubItem mediaSetSubItem = new MediaSetSubItem();
        final MediaSetItem mediaSetItem = new MediaSetItem();
        final MediaSet mediaSet = new MediaSet();
        mediaSetItem.setType("image");
        mediaSetItem.setItems(Collections.singletonList(mediaSetSubItem));
        mediaSet.setItems(Collections.singletonList(mediaSetItem));

        final CreativeData creativeData = new CreativeData();
        final Bundle bundle = new Bundle("name", 1);
        final Image image = new Image();
        image.setType("image");
        image.setMediaSet("mediaSet");

        creativeData.setElements(Collections.singletonList(image));
        creativeData.setMediaSets(Collections.singletonMap("mediaSet", mediaSet));
        creativeData.setWidth(100);
        creativeData.setHeight(100);
        creativeData.setBundle(bundle);

        mediaSetSubItem.setWidth(100);
        mediaSetSubItem.setHeight(100);
        mediaSetSubItem.setFileId("1");
        mediaSetSubItem.setCroppedFileId("1");
        mediaSetSubItem.setUrl("https://avatars.mdst.yandex.net/ololo");

        assertEquals(0, validator.validate(creativeData).size());
    }
}
