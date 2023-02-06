package ru.yandex.market.mbo.cms.core.image.thumbnail;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.cms.core.image.thumbnail.logger.ImageUploadLogger;
import ru.yandex.market.mbo.cms.core.models.ImageDescription;
import ru.yandex.market.mbo.cms.core.models.Thumbnail;

import static org.mockito.ArgumentMatchers.eq;
import static ru.yandex.market.mbo.cms.core.image.thumbnail.ThumbnailStorageService.AVATAR_OPTIMIZE;

/**
 * @author ayratgdl
 * @date 24.05.17
 */
public class ThumbnailStorageServiceTest {
    private static final String MARKET_16X16_PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAAXNSR0IArs4c6QAAAVpJREFUOBFjdCo/9Z8BCv4z/L" +
                    "+zv9NcDciFi8Hkc" +
                    "NFMyBKMDIwqjqUnPJHFCLGZ9nWaMYIww/9/vWDFjIy5hDQhyzPCOHblx3RZGFguwfjE0nAvHOq0ugwMg7PEaoSpgxsAEvj" +
                    "/n2EBTI" +
                    "JYGsWAb18ZlgM1/iJWM0gdigGnppq/BbpiC/EG/P+EYgBI479/RHrj//+///4z9LGg28Z45ut2BnOulwwMjOLocv" +
                    "///38CFNvJ+J9" +
                    "h568vv/YcmW77Hh6NyIody072MjIyFgE1fAeKHwTGzs6/fxl2He61uIasDsTGcAFIkPHvn0l" +
                    "/mZl3ML76fvjAAscfIDGaAWZkk5WU" +
                    "lKIFBQXXCggIfP/w4cM5ZDkQW1FRMUVISGgFUM279" +
                    "+/fXwaJocdCBlBMFYizQJJYAEgcJA9SBwYoLgDaDAplMWAAdgJtuAVVA6eAt" +
                    "j8FBqwwUKAD6MJ7IAkA/z1yUUvqw1YAAAAASUVORK5CYII=";

    private static final String THUMB_8X8_PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAYAAADED76LAAAAhElEQVR42mNwKj/1H4i" +
                    "/OVeeFGbABZzKT64DKlrrVHGyAR2DFTiWnfaDm" +
                    "oSBwQqM086wAjkvMRSUnfyDbE0fkuQPp7JTexzKTibDFTiWn9J2LDsx0b" +
                    "H0hJdbyU5uFEcqKiqKKysrB8D48vLykkAxX7gCoKSTkp" +
                    "LSFhhfQUHBHahgA4gNAIYWUMSvMHaiAAAAAElFTkSuQmCC";

    private static final String THUMB_16X16_PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAA30lEQVR42mNwKj/1H4Ydy0" +
                    "/eZmBgYGQgBSAbADak9IQXAznAqexED9iAs" +
                    "pPbyTLArvyYLrpriMEohgDD4AxFBjiUncyhyACz7JPCQMGfZBsA9kbZqbXEG3DyI4YB9iWnfInSXHbyD9DL9RgGODjsZwGa" +
                    "/AKbJm" +
                    "AUPwbiOU6lJ0NtMg8L4oxSoKJeqIZvoHThUHaiwLb4hBbxiar4qLx92QlXh" +
                    "4T9HAx0BUpKStFAfEtRUTEFmzxIHCQPUofLgMNA/B+" +
                    "o8BwOA86B5EHqGHAocAXiHUAFPjgs8AHJg9TBxAAKHBTX/vh7YgAAAABJRU5ErkJggg==";

    private static final String PREFIX_KEY = "prefix_key";
    private static final String IMAGE_EXTENSION = "png";
    private static final String IMAGE_PATH = PREFIX_KEY + "." + IMAGE_EXTENSION;

    private static final Pattern ORIGIN_STORAGE_KEY_PATTERN = Pattern.compile(PREFIX_KEY + "\\." + IMAGE_EXTENSION);

    private static final long USER_ID = 1;
    private static final int ORIGIN_IMAGE_SIZE = 16;
    private static final int THUMBNAIL1_IMAGE_SIZE = 8;
    private static final int THUMBNAIL2_IMAGE_SIZE = 4;

    private ThumbnailStorageService imageStorage;

    @Before
    public void setUp() {
        imageStorage = Mockito.mock(ThumbnailStorageService.class);
        Mockito
                .when(imageStorage.uploadImage(Mockito.any(byte[].class), Mockito.any(), Mockito.anyLong()))
                .thenCallRealMethod();
        Mockito
                .when(imageStorage.uploadImage(Mockito.any(byte[].class), Mockito.any(), Mockito.anyLong()))
                .thenAnswer(invocation -> {
                    String key = invocation.getArgument(1);
                    return "/something/" + key;
                });
        Mockito
                .when(imageStorage.uploadImage(Mockito.any(byte[].class), Mockito.any(), Mockito.anyLong()))
                .thenAnswer(invocation -> {
                    String storagePath = invocation.getArgument(0);
                    return "host" + storagePath;
                });

        CmsAvatarClient avatarClient = Mockito.mock(CmsAvatarClient.class);

        Mockito.when(avatarClient.uploadImageToAvatar(
                eq(ThumbnailMaker.toByteArray(getTestPNG(MARKET_16X16_PNG_BASE64), IMAGE_EXTENSION)),
                Mockito.anyString())).thenReturn(
                makeResponse(ORIGIN_IMAGE_SIZE, ORIGIN_IMAGE_SIZE));

        Mockito.when(avatarClient.uploadImageToAvatar(
                eq(ThumbnailMaker.toByteArray(getTestPNG(THUMB_8X8_PNG_BASE64), IMAGE_EXTENSION)),
                Mockito.anyString())).thenReturn(
                makeResponse(THUMBNAIL1_IMAGE_SIZE, THUMBNAIL1_IMAGE_SIZE));

        Mockito.when(avatarClient.uploadImageToAvatar(
                eq(ThumbnailMaker.toByteArray(getTestPNG(THUMB_16X16_PNG_BASE64), IMAGE_EXTENSION)),
                Mockito.anyString())).thenReturn(
                makeResponse(ORIGIN_IMAGE_SIZE, ORIGIN_IMAGE_SIZE));

        imageStorage = new ThumbnailStorageService(Mockito.mock(ImageUploadLogger.class), avatarClient, "");
    }

    @Test
    public void addImageWithoutThumbnailsTest() {
        ImageDescription imageDescription = new ImageDescription();
        BufferedImage imageBytes = getTestPNG(MARKET_16X16_PNG_BASE64);
        imageStorage.addImage(
                imageDescription, ThumbnailMaker.toByteArray(imageBytes, IMAGE_EXTENSION), IMAGE_EXTENSION, USER_ID
        );

        Assert.assertEquals(ORIGIN_IMAGE_SIZE, imageDescription.getWidth());
        Assert.assertEquals(ORIGIN_IMAGE_SIZE, imageDescription.getHeight());
        Assert.assertTrue(ORIGIN_STORAGE_KEY_PATTERN.matcher(imageDescription.getUrl()).find());
        Assert.assertTrue(imageDescription.getThumbnails().isEmpty());
    }

    @Test
    public void addImageWithThumbnailsTest() {
        ImageDescription imageDescription = new ImageDescription();

        Thumbnail thumbnail1 = Thumbnail.valueOf("8x8@x1/2");
        Thumbnail thumbnail2 = Thumbnail.valueOf("4x4@x2/8");
        imageDescription.addThumbnail(thumbnail1);
        imageDescription.addThumbnail(thumbnail2);

        BufferedImage imageBytes = getTestPNG(MARKET_16X16_PNG_BASE64);
        imageStorage.addImage(
                imageDescription, ThumbnailMaker.toByteArray(imageBytes, IMAGE_EXTENSION), IMAGE_EXTENSION, USER_ID
        );

        // Проверки для оригинального изображения
        Assert.assertEquals(ORIGIN_IMAGE_SIZE, imageDescription.getWidth());
        Assert.assertEquals(ORIGIN_IMAGE_SIZE, imageDescription.getHeight());
        Assert.assertTrue(ORIGIN_STORAGE_KEY_PATTERN.matcher(imageDescription.getUrl()).find());

        // Проверки для первой миниатюры
        Assert.assertEquals(THUMBNAIL1_IMAGE_SIZE, thumbnail1.getWidth());
        Assert.assertEquals(THUMBNAIL1_IMAGE_SIZE, thumbnail1.getHeight());
        Assert.assertTrue(
                ORIGIN_STORAGE_KEY_PATTERN
                        .matcher(thumbnail1.getDensities()
                                .stream().filter(d -> "1".equals(d.getId())).findFirst().get().getUrl())
                        .find()
        );
        Assert.assertTrue(
                ORIGIN_STORAGE_KEY_PATTERN
                        .matcher(thumbnail1.getDensities()
                                .stream().filter(d -> "2".equals(d.getId())).findFirst().get().getUrl())
                        .find()
        );

        // Проверки для второй миниатюры
        Assert.assertEquals(THUMBNAIL2_IMAGE_SIZE, thumbnail2.getWidth());
        Assert.assertEquals(THUMBNAIL2_IMAGE_SIZE, thumbnail2.getHeight());
        Assert.assertTrue(
                ORIGIN_STORAGE_KEY_PATTERN
                        .matcher(
                                thumbnail2.getDensities()
                                        .stream().filter(d -> "2".equals(d.getId())).findFirst().get().getUrl())
                        .find()
        );
        Assert.assertFalse("Thumbnails, when bigger of origin, are not being included.",
                thumbnail2.getDensities()
                        .stream().filter(d -> "8".equals(d.getId())).findFirst().isPresent()
        );
    }

    private BufferedImage getTestPNG(String src) {
        try {
            return ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(src)));
        } catch (IOException e) {
            return null;
        }
    }

    private AvatarsResponse makeResponse(int width, int height) {
        AvatarsResponse.Size size = new AvatarsResponse.Size();
        size.setWidth(width);
        size.setHeight(height);
        size.setPath(IMAGE_PATH);

        AvatarsResponse result = new AvatarsResponse();
        result.setSizes(new HashMap<>());
        result.getSizes().put(AVATAR_OPTIMIZE, size);
        return result;
    }
}
