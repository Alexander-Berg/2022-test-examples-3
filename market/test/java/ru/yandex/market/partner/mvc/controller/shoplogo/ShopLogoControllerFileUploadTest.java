package ru.yandex.market.partner.mvc.controller.shoplogo;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.avatars.AvatarsClient;
import ru.yandex.market.core.avatars.model.AvatarsImageDTO;
import ru.yandex.market.core.avatars.model.ThumbnailDTO;
import ru.yandex.market.core.avatars.model.custom.thumbnail.ThumbnailCommandDTO;
import ru.yandex.market.core.avatars.model.custom.thumbnail.ThumbnailPreferencesDTO;
import ru.yandex.market.core.logo.model.ImageType;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Функциональные тесты на валидацию и загрузку файла для {@link ShopLogoController}.
 * Остальные тесты в {@link ShopLogoControllerTest}.
 *
 * @author au-rikka
 */
@DbUnitDataSet(before = "ShopLogo.before.csv")
public class ShopLogoControllerFileUploadTest extends BaseLogoControllerTest {
    private static final long SHOP_LOGO_CAMPAIGN_ID = 1111L;
    private static final long SHOP_LOGO_WITH_RETINA_CAMPAIGN_ID = 2222L;
    private static final long NEW_SHOP_CAMPAIGN_ID = 3333L;
    private static final long FMCG_CAMPAIGN_ID = 4444L;

    private static final String TOO_BIG_LOGO_NAME = "too_big_logo.png";
    private static final String WRONG_EXTENSION_LOGO_NAME = "logo.jpg";
    private static final String WRONG_EXTENSION_TOO_BIG_LOGO_NAME = "too_big_logo.jpg";
    private static final String CORRECT_SVG_LOGO = "correct_logo.svg";

    private static final String NEW_IMAGE_NAME = "newImagename";

    private static final String IMAGE_DELETE_URL = "test.ru/delete";
    private static final String IMAGE_CUSTOM_THUMBNAIL_URL = "test.ru/thumbnail";

    private static final String BASE_UNSUPPORTED_FILE_EXTENSION_ERRORS = "[{" +
            "   \"code\":\"UNSUPPORTED_FILE_EXTENSION\"," +
            "   \"details\":{" +
            "       \"SUPPORTED_FILE_EXTENSIONS\":[%s]" +
            "   }" +
            "}]";
    private static final String UNSUPPORTED_FILE_EXTENSION_ERRORS =
            String.format(BASE_UNSUPPORTED_FILE_EXTENSION_ERRORS, "\"PNG\",\"SVG\"");
    private static final String UNSUPPORTED_FILE_EXTENSION_FOR_FMCG_ERRORS =
            String.format(BASE_UNSUPPORTED_FILE_EXTENSION_ERRORS, "\"SVG\"");
    private static final String FILE_TOO_LARGE_ERRORS = "[{" +
            "   \"code\":\"FILE_TOO_LARGE\"," +
            "   \"details\":{" +
            "       \"MAX_ALLOWED_FILE_SIZE_KB\":15" +
            "   }" +
            "}]";
    private static final String UNSUPPORTED_FILE_DIMENSIONS_PNG_ERRORS = "[{" +
            "  \"code\":\"UNSUPPORTED_FILE_DIMENSIONS\"," +
            "  \"message\":\"Given image sizes: height %dpx, width %dpx. " +
            "Small PNG images should have height 14px, width 4-112px. " +
            "Retina PNG images should have height 28px, width 8-224px.\"," +
            "  \"details\":{}" +
            "}]";
    private static final String UNSUPPORTED_FILE_DIMENSIONS_SVG_ERRORS = "[{" +
            "  \"code\":\"UNSUPPORTED_FILE_DIMENSIONS\"," +
            "  \"message\":\"Given image sizes: height %d, width %d. " +
            "SVG images converted to height 14, should have width from 4 to 112.\"," +
            "  \"details\":{}" +
            "}]";

    @Autowired
    private AvatarsClient avatarsClient;


    private String shopLogoUrl(long campaignId) {
        return baseUrl + String.format("/campaign/%d/logo", campaignId);
    }

    private ResponseEntity<String> uploadFile(long campaignId, String fileName, boolean isTurbo) {
        final String url = shopLogoUrl(campaignId) + "/upload" + (isTurbo ? "/turbo" : "");
        return doMultipartFileRequest(url, fileName);
    }

    private ResponseEntity<String> validateFile(long campaignId, String fileName, boolean isTurbo) {
        final String url = shopLogoUrl(campaignId) + "/validate" + (isTurbo ? "/turbo" : "");
        return doMultipartFileRequest(url, fileName);
    }

    /**
     * Тест для ручки {@code /campaign/{campaignId}/logo/validate}.
     * Валидация файла с корректным форматом, но весом больше 12Кб.
     */
    @Test
    @DbUnitDataSet(after = "ShopLogo.before.csv")
    void validateFileTooBig() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> validateFile(SHOP_LOGO_CAMPAIGN_ID, TOO_BIG_LOGO_NAME, false)
        );
        verifyNoMoreInteractions(avatarsClient);
        assertThat(exception, HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST));
        JsonTestUtil.assertResponseErrorMessage(FILE_TOO_LARGE_ERRORS, exception.getResponseBodyAsString());
    }

    /**
     * Тест для ручки {@code /campaign/{campaignId}/logo/validate}.
     * Валидация файла неправильного формата.
     */
    @Test
    @DbUnitDataSet(after = "ShopLogo.before.csv")
    void validateFileWrongExtension() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> validateFile(SHOP_LOGO_CAMPAIGN_ID, WRONG_EXTENSION_LOGO_NAME, false)
        );
        verifyNoMoreInteractions(avatarsClient);
        assertThat(exception, HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST));
        JsonTestUtil.assertResponseErrorMessage(UNSUPPORTED_FILE_EXTENSION_ERRORS, exception.getResponseBodyAsString());
    }

    /**
     * Тест проверяет, что для fmcg партнеров при валидации файл с расширением PNG не является некорректным.
     */
    @Test
    void validateExtensionForFmcg() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> validateFile(FMCG_CAMPAIGN_ID, CORRECT_PNG_LOGO, false)
        );
        verifyNoMoreInteractions(avatarsClient);
        assertThat(exception, HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST));
        JsonTestUtil.assertResponseErrorMessage(UNSUPPORTED_FILE_EXTENSION_FOR_FMCG_ERRORS,
                exception.getResponseBodyAsString());
    }

    /**
     * Тест для ручки {@code /campaign/{campaignId}/logo/validate}.
     * Валидация файла неправильного формата весом больше 12Кб. Должна отобразиться ошибка про расширение файла.
     */
    @Test
    @DbUnitDataSet(after = "ShopLogo.before.csv")
    void validateFileTooBigWrongExtension() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> validateFile(SHOP_LOGO_CAMPAIGN_ID, WRONG_EXTENSION_TOO_BIG_LOGO_NAME, false)
        );
        verifyNoMoreInteractions(avatarsClient);
        assertThat(exception, HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST));
        JsonTestUtil.assertResponseErrorMessage(UNSUPPORTED_FILE_EXTENSION_ERRORS, exception.getResponseBodyAsString());
    }

    /**
     * Тест для ручки {@code /campaign/{campaignId}/logo/validate}.
     * Валидация корректного png-файла в маленьком разрешении.
     */
    @Test
    @DbUnitDataSet(after = "ShopLogo.before.csv")
    void validateCorrectFilePng() {
        AvatarsImageDTO avatarsResponse = mockAvatarsResponse(true, ImageType.PNG.name(), HEIGHT, WIDTH);
        validateFile(SHOP_LOGO_CAMPAIGN_ID, CORRECT_PNG_LOGO, false);

        verify(avatarsClient).uploadImage(any(MultipartFile.class), eq(true));
        verify(avatarsClient).getImageDeleteUrl(avatarsResponse);
        verify(avatarsClient).getImageUrl(eq(new ThumbnailDTO(HEIGHT, WIDTH, "path/orig")));
        verifyNoMoreInteractions(avatarsClient);
    }

    /**
     * Тест для ручки {@code /campaign/{campaignId}/logo/validate}.
     * Валидация корректного png-файла в маленьком разрешении с нечетной длинной.
     */
    @Test
    @DbUnitDataSet(after = "ShopLogo.before.csv")
    void validateCorrectOddWidthFilePng() {
        AvatarsImageDTO avatarsResponse = mockAvatarsResponse(true, ImageType.PNG.name(), HEIGHT, WIDTH + 1);
        validateFile(SHOP_LOGO_CAMPAIGN_ID, CORRECT_PNG_LOGO, false);

        verify(avatarsClient).uploadImage(any(MultipartFile.class), eq(true));
        verify(avatarsClient).getImageDeleteUrl(avatarsResponse);
        verify(avatarsClient).getImageUrl(eq(new ThumbnailDTO(HEIGHT, WIDTH + 1, "path/orig")));
        verifyNoMoreInteractions(avatarsClient);
    }

    /**
     * Тест для ручки {@code /campaign/{campaignId}/logo/validate}.
     * Валидация корректного png-файла в большом разрешении.
     */
    @Test
    @DbUnitDataSet(after = "ShopLogo.before.csv")
    void validateCorrectRetinaFilePng() {
        AvatarsImageDTO avatarsResponse = mockAvatarsResponse(true, ImageType.PNG.name(), HEIGHT * 2, WIDTH * 2);
        validateFile(SHOP_LOGO_CAMPAIGN_ID, CORRECT_PNG_LOGO, false);

        verify(avatarsClient).uploadImage(any(MultipartFile.class), eq(true));
        verify(avatarsClient).getImageDeleteUrl(avatarsResponse);
        verify(avatarsClient).getImageUrl(eq(new ThumbnailDTO(HEIGHT * 2, WIDTH * 2, "path/orig")));
        verify(avatarsClient).getImageUrl(eq(new ThumbnailDTO(HEIGHT, WIDTH, "path/small")));
        verifyNoMoreInteractions(avatarsClient);
    }

    /**
     * Тест для ручки {@code /campaign/{campaignId}/logo/validate}.
     * Валидация корректного png-файла в большом разрешении c нечетной длиной.
     * Файл будет обрезан на 1 пиксель по длине.
     */
    @Test
    @DbUnitDataSet(after = "ShopLogo.before.csv")
    void validateCorrectOddWidthRetinaFilePng() {
        AvatarsImageDTO avatarsResponse = mockAvatarsResponse(true, ImageType.PNG.name(), HEIGHT * 2, WIDTH * 2 + 1);

        ThumbnailPreferencesDTO thumbnail = new ThumbnailPreferencesDTO
                .Builder(HEIGHT * 2, WIDTH * 2, ThumbnailCommandDTO.HEIGHT_PRIORITY).build();
        AvatarsImageDTO newAvatarsResponse = mockCustomThumbnailAvatarsResponse(true, avatarsResponse, thumbnail);

        doNothing().when(avatarsClient).deleteImage(IMAGE_DELETE_URL);
        doNothing().when(avatarsClient).deleteImage(eq(avatarsResponse));

        validateFile(SHOP_LOGO_CAMPAIGN_ID, CORRECT_PNG_LOGO, false);

        verify(avatarsClient).uploadImage(any(MultipartFile.class), eq(true));

        verify(avatarsClient).getCustomThumbnailUrl(eq(avatarsResponse), eq(thumbnail));
        verify(avatarsClient).uploadImage(eq(IMAGE_CUSTOM_THUMBNAIL_URL), eq(true));
        verify(avatarsClient).deleteImage(eq(avatarsResponse));

        verify(avatarsClient).getImageDeleteUrl(eq(newAvatarsResponse));
        verify(avatarsClient).getImageUrl(eq(new ThumbnailDTO(HEIGHT * 2, WIDTH * 2, "new_path/orig")));
        verify(avatarsClient).getImageUrl(eq(new ThumbnailDTO(HEIGHT, WIDTH, "new_path/small")));
        verifyNoMoreInteractions(avatarsClient);
    }

    /**
     * Тест для ручки {@code /campaign/{campaignId}/logo/upload}.
     * Валидация png-файла с некорректными значениями высоты и ширины.
     */
    @Test
    @DbUnitDataSet(after = "ShopLogo.before.csv")
    void validateFilePngWrongDimensions() {
        mockAvatarsResponse(true, ImageType.PNG.name(), HEIGHT / 2, WIDTH / 2);
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> validateFile(SHOP_LOGO_CAMPAIGN_ID, CORRECT_PNG_LOGO, false)
        );

        verify(avatarsClient).uploadImage(any(MultipartFile.class), eq(true));
        verifyNoMoreInteractions(avatarsClient);
        assertThat(exception, HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST));
        JsonTestUtil.assertResponseErrorMessage(
                String.format(UNSUPPORTED_FILE_DIMENSIONS_PNG_ERRORS, HEIGHT / 2, WIDTH / 2),
                exception.getResponseBodyAsString()
        );
    }

    /**
     * Тест для ручки {@code /campaign/{campaignId}/logo/validate}.
     * Валидация корректного svg-файла.
     */
    @Test
    @DbUnitDataSet(after = "ShopLogo.before.csv")
    void validateCorrectFileSvg() {
        AvatarsImageDTO avatarsResponse = mockAvatarsResponse(true, ImageType.SVG.name(), HEIGHT / 2, WIDTH / 2);
        validateFile(SHOP_LOGO_WITH_RETINA_CAMPAIGN_ID, CORRECT_SVG_LOGO, false);

        verify(avatarsClient).uploadImage(any(MultipartFile.class), eq(true));
        verify(avatarsClient).getImageDeleteUrl(avatarsResponse);
        verify(avatarsClient).getImageUrl(eq(new ThumbnailDTO(HEIGHT, WIDTH, "path/orig")));
        verifyNoMoreInteractions(avatarsClient);
    }

    /**
     * Тест для ручки {@code /campaign/{campaignId}/logo/upload}.
     * Валидация svg-файла с некорректными значениями высоты и ширины.
     */
    @Test
    @DbUnitDataSet(after = "ShopLogo.before.csv")
    void validateFileSvgWrongDimensions() {
        AvatarsImageDTO avatarsResponse = mockAvatarsResponse(true, ImageType.SVG.name(), HEIGHT * 50, WIDTH);
        doNothing().when(avatarsClient).deleteImage(avatarsResponse);

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> validateFile(SHOP_LOGO_CAMPAIGN_ID, CORRECT_SVG_LOGO, false)
        );

        verify(avatarsClient).uploadImage(any(MultipartFile.class), eq(true));
        verifyNoMoreInteractions(avatarsClient);
        assertThat(exception, HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST));
        JsonTestUtil.assertResponseErrorMessage(
                String.format(UNSUPPORTED_FILE_DIMENSIONS_SVG_ERRORS, HEIGHT, WIDTH / 50),
                exception.getResponseBodyAsString()
        );
    }


    /**
     * Тест для ручки {@code /campaign/{campaignId}/logo/upload}.
     * Валидация и попытка загрузки файла с корректным форматом, но весом больше 12Кб.
     */
    @Test
    @DbUnitDataSet(after = "ShopLogo.before.csv")
    void uploadFileTooBig() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> uploadFile(SHOP_LOGO_CAMPAIGN_ID, TOO_BIG_LOGO_NAME, false)
        );
        verifyNoMoreInteractions(avatarsClient);
        assertThat(exception, HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST));
        JsonTestUtil.assertResponseErrorMessage(FILE_TOO_LARGE_ERRORS, exception.getResponseBodyAsString());
    }

    /**
     * Тест для ручки {@code /campaign/{campaignId}/logo/upload}.
     * Валидация и попытка загрузки файла неправильного формата.
     */
    @Test
    @DbUnitDataSet(after = "ShopLogo.before.csv")
    void uploadFileWrongExtension() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> uploadFile(SHOP_LOGO_CAMPAIGN_ID, WRONG_EXTENSION_LOGO_NAME, false)
        );
        verifyNoMoreInteractions(avatarsClient);
        assertThat(exception, HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST));
        JsonTestUtil.assertResponseErrorMessage(UNSUPPORTED_FILE_EXTENSION_ERRORS, exception.getResponseBodyAsString());
    }

    /**
     * Тест проверяет, что для fmcg партнеров при загрузке файл с расширением PNG не является некорректным.
     */
    @Test
    void uploadExtensionForFmcg() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> uploadFile(FMCG_CAMPAIGN_ID, CORRECT_PNG_LOGO, false)
        );
        verifyNoMoreInteractions(avatarsClient);
        assertThat(exception, HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST));
        JsonTestUtil.assertResponseErrorMessage(UNSUPPORTED_FILE_EXTENSION_FOR_FMCG_ERRORS,
                exception.getResponseBodyAsString());
    }

    /**
     * Тест для ручки {@code /campaign/{campaignId}/logo/upload}.
     * Валидация и попытка загрузки файла неправильного формата весом больше 12Кб.
     * Должна отобразиться ошибка про расширение файла.
     */
    @Test
    @DbUnitDataSet(after = "ShopLogo.before.csv")
    void uploadFileTooBigWrongExtension() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> uploadFile(SHOP_LOGO_CAMPAIGN_ID, WRONG_EXTENSION_TOO_BIG_LOGO_NAME, false)
        );
        verifyNoMoreInteractions(avatarsClient);
        assertThat(exception, HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST));
        JsonTestUtil.assertResponseErrorMessage(UNSUPPORTED_FILE_EXTENSION_ERRORS, exception.getResponseBodyAsString());
    }

    /**
     * Тест для ручки {@code /campaign/{campaignId}/logo/upload}.
     * Валидация и загрузка корректного png-файла в маленьком разрешении.
     * Для магазина, у которого раньше не было указано информации о логотипе.
     */
    @Test
    @DbUnitDataSet(after = "ShopLogo.uploadPng.after.csv")
    void uploadCorrectFilePng() {
        AvatarsImageDTO avatarsResponse = mockAvatarsResponse(false, ImageType.PNG.name(), HEIGHT, WIDTH);
        uploadFile(NEW_SHOP_CAMPAIGN_ID, CORRECT_PNG_LOGO, false);

        verify(avatarsClient).uploadImage(any(MultipartFile.class), eq(false));
        verify(avatarsClient).getImageDeleteUrl(avatarsResponse);
        verify(avatarsClient).getImageUrl(eq(new ThumbnailDTO(HEIGHT, WIDTH, "path/orig")));
        verifyNoMoreInteractions(avatarsClient);
    }

    /**
     * Тест для ручки {@code /campaign/{campaignId}/logo/validate}.
     * Валидация и загрузка корректного png-файла в маленьком разрешении с нечетной длинной.
     * Для магазина, у которого уже была указана информация о логотипе.
     */
    @Test
    @DbUnitDataSet(after = "ShopLogo.uploadOddWidthPng.after.csv")
    void uploadCorrectOddWidthFilePng() {
        AvatarsImageDTO avatarsResponse = mockAvatarsResponse(false, ImageType.PNG.name(), HEIGHT, WIDTH + 1);
        uploadFile(SHOP_LOGO_CAMPAIGN_ID, CORRECT_PNG_LOGO, false);

        verify(avatarsClient).uploadImage(any(MultipartFile.class), eq(false));
        verify(avatarsClient).getImageDeleteUrl(avatarsResponse);
        verify(avatarsClient).getImageUrl(eq(new ThumbnailDTO(HEIGHT, WIDTH + 1, "path/orig")));
        verifyNoMoreInteractions(avatarsClient);
    }

    /**
     * Тест для ручки {@code /campaign/{campaignId}/logo/upload}.
     * Валидация и загрузка корректного png-файла в большом разрешении.
     * Для магазина, у которого раньше уже была указана информация о логотипе.
     */
    @Test
    @DbUnitDataSet(after = "ShopLogo.uploadRetinaPng.after.csv")
    void uploadCorrectRetinaFilePng() {
        AvatarsImageDTO avatarsResponse = mockAvatarsResponse(false, ImageType.PNG.name(), HEIGHT * 2, WIDTH * 2);
        doNothing().when(avatarsClient).deleteImage(IMAGE_DELETE_URL);
        uploadFile(SHOP_LOGO_CAMPAIGN_ID, CORRECT_PNG_LOGO, false);
        verify(avatarsClient).uploadImage(any(MultipartFile.class), eq(false));
        verify(avatarsClient).getImageDeleteUrl(avatarsResponse);
        verify(avatarsClient).getImageUrl(eq(new ThumbnailDTO(HEIGHT * 2, WIDTH * 2, "path/orig")));
        verify(avatarsClient).getImageUrl(eq(new ThumbnailDTO(HEIGHT, WIDTH, "path/small")));
        verifyNoMoreInteractions(avatarsClient);
    }


    /**
     * Тест для ручки {@code /campaign/{campaignId}/logo/validate}.
     * Валидация и загрузка корректного png-файла в большом разрешении c нечетной длиной.
     * Файл будет обрезан на 1 пиксель по длине.
     */
    @Test
    @DbUnitDataSet(after = "ShopLogo.uploadOddWidthRetinaPng.after.csv")
    void uploadCorrectOddWidthRetinaFilePng() {
        AvatarsImageDTO avatarsResponse = mockAvatarsResponse(false, ImageType.PNG.name(), HEIGHT * 2, WIDTH * 2 + 1);

        ThumbnailPreferencesDTO thumbnail = new ThumbnailPreferencesDTO
                .Builder(HEIGHT * 2, WIDTH * 2, ThumbnailCommandDTO.HEIGHT_PRIORITY).build();
        AvatarsImageDTO newAvatarsResponse = mockCustomThumbnailAvatarsResponse(false, avatarsResponse, thumbnail);

        doNothing().when(avatarsClient).deleteImage(IMAGE_DELETE_URL);
        doNothing().when(avatarsClient).deleteImage(eq(avatarsResponse));

        uploadFile(SHOP_LOGO_CAMPAIGN_ID, CORRECT_PNG_LOGO, false);

        verify(avatarsClient).uploadImage(any(MultipartFile.class), eq(false));

        verify(avatarsClient).getCustomThumbnailUrl(eq(avatarsResponse), eq(thumbnail));
        verify(avatarsClient).uploadImage(eq(IMAGE_CUSTOM_THUMBNAIL_URL), eq(false));
        verify(avatarsClient).deleteImage(eq(avatarsResponse));

        verify(avatarsClient).getImageDeleteUrl(eq(newAvatarsResponse));
        verify(avatarsClient).getImageUrl(eq(new ThumbnailDTO(HEIGHT * 2, WIDTH * 2, "new_path/orig")));
        verify(avatarsClient).getImageUrl(eq(new ThumbnailDTO(HEIGHT, WIDTH, "new_path/small")));

        verifyNoMoreInteractions(avatarsClient);
    }

    /**
     * Тест для ручки {@code /campaign/{campaignId}/logo/upload}.
     * Валидация и загрузка png-файла с некорректными значениями высоты и ширины.
     */
    @Test
    @DbUnitDataSet(after = "ShopLogo.before.csv")
    void uploadFilePngWrongDimensions() {
        AvatarsImageDTO avatarsResponse = mockAvatarsResponse(false, ImageType.PNG.name(), HEIGHT * 3, WIDTH * 3);
        doNothing().when(avatarsClient).deleteImage(avatarsResponse);

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> uploadFile(SHOP_LOGO_CAMPAIGN_ID, CORRECT_PNG_LOGO, false)
        );

        verify(avatarsClient).uploadImage(any(MultipartFile.class), eq(false));
        verify(avatarsClient).deleteImage(avatarsResponse);
        verifyNoMoreInteractions(avatarsClient);
        assertThat(exception, HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST));
        JsonTestUtil.assertResponseErrorMessage(
                String.format(UNSUPPORTED_FILE_DIMENSIONS_PNG_ERRORS, HEIGHT * 3, WIDTH * 3),
                exception.getResponseBodyAsString()
        );
    }

    /**
     * Тест для ручки {@code /campaign/{campaignId}/logo/upload}.
     * Валидация и загрузка корректного svg-файла.
     * Для магазина, у которого раньше уже была указана информация о логотипе.
     */
    @Test
    @DbUnitDataSet(after = "ShopLogo.uploadSvg.after.csv")
    void uploadCorrectFileSvg() {
        AvatarsImageDTO avatarsResponse = mockAvatarsResponse(false, ImageType.SVG.name(), HEIGHT * 4, WIDTH * 4);
        doNothing().when(avatarsClient).deleteImage(IMAGE_DELETE_URL);
        uploadFile(SHOP_LOGO_WITH_RETINA_CAMPAIGN_ID, CORRECT_SVG_LOGO, false);
        verify(avatarsClient).uploadImage(any(MultipartFile.class), eq(false));
        verify(avatarsClient).getImageDeleteUrl(avatarsResponse);
        verify(avatarsClient).getImageUrl(eq(new ThumbnailDTO(HEIGHT, WIDTH, "path/orig")));
        verifyNoMoreInteractions(avatarsClient);
    }

    /**
     * Тест для ручки {@code /campaign/{campaignId}/logo/upload}.
     * Валидация и попытка загрузить svg-файла с некорректными значениями высоты и ширины.
     */
    @Test
    @DbUnitDataSet(after = "ShopLogo.before.csv")
    void uploadFileSvgWrongDimensions() {
        AvatarsImageDTO avatarsResponse = mockAvatarsResponse(false, ImageType.SVG.name(), HEIGHT / 2, WIDTH * 2);
        doNothing().when(avatarsClient).deleteImage(avatarsResponse);

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> uploadFile(SHOP_LOGO_CAMPAIGN_ID, CORRECT_SVG_LOGO, false)
        );

        verify(avatarsClient).uploadImage(any(MultipartFile.class), eq(false));
        verify(avatarsClient).deleteImage(avatarsResponse);
        verifyNoMoreInteractions(avatarsClient);
        assertThat(exception, HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST));
        JsonTestUtil.assertResponseErrorMessage(
                String.format(UNSUPPORTED_FILE_DIMENSIONS_SVG_ERRORS, HEIGHT, WIDTH * 4),
                exception.getResponseBodyAsString()
        );
    }


    private AvatarsImageDTO mockCustomThumbnailAvatarsResponse(
            boolean isTemporary,
            AvatarsImageDTO initialResponse,
            ThumbnailPreferencesDTO thumbnail
    ) {
        doReturn(IMAGE_CUSTOM_THUMBNAIL_URL)
                .when(avatarsClient).getCustomThumbnailUrl(eq(initialResponse), eq(thumbnail));

        Map<String, ThumbnailDTO> sizes = new HashMap<>();
        sizes.put("orig", new ThumbnailDTO(thumbnail.getHeight(), thumbnail.getWidth(), "new_path/orig"));
        sizes.put("small", new ThumbnailDTO(thumbnail.getHeight() / 2, thumbnail.getWidth() / 2, "new_path/small"));
        AvatarsImageDTO avatarsResponse = new AvatarsImageDTO(
                GROUP_ID, NEW_IMAGE_NAME,
                initialResponse.getMeta(),
                sizes
        );

        doReturn(avatarsResponse).when(avatarsClient).uploadImage(eq(IMAGE_CUSTOM_THUMBNAIL_URL), eq(isTemporary));
        return avatarsResponse;
    }
}
