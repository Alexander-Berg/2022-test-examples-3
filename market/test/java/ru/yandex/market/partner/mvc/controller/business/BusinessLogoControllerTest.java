package ru.yandex.market.partner.mvc.controller.business;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.avatars.model.AvatarsImageDTO;
import ru.yandex.market.core.avatars.model.ThumbnailDTO;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.logo.model.ImageType;
import ru.yandex.market.partner.mvc.controller.shoplogo.BaseLogoControllerTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Тесты для {@link BusinessLogoController}.
 *
 * @author Vadim Lyalin
 */
@DbUnitDataSet(before = "BusinessLogoControllerTest.before.csv")
public class BusinessLogoControllerTest extends BaseLogoControllerTest {

    @Test
    void testGetLogo() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/businesses/1/logo");
        JsonTestUtil.assertEquals(response, this.getClass(), "expected/logo_info.json");
    }

    @Test
    void testGetAbsentLogo() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/businesses/3/logo");
        Assertions.assertNull(JsonTestUtil.parseJson(response.getBody()).getAsJsonObject().get("result"));
    }

    @Test
    @DbUnitDataSet(after = "BusinessLogoControllerTest.delete.after.csv")
    void testDeleteLogo() {
        FunctionalTestHelper.delete(baseUrl + "/businesses/1/logo");
        verifyNoMoreInteractions(avatarsClient);
    }

    @Test
    @DbUnitDataSet(after = "BusinessLogoControllerTest.upload.after.csv")
    void uploadCorrectFilePng() {
        checkUploadLogo(1L);
    }

    /**
     * Комплексно проверяет корректную работу с фичей лого на уровне бизнеса.
     */
    @Test
    void featureLogoTest() {
        // проверяем, что по умолчанию фича в DONT_WANT
        final ResponseEntity<String> responseDisable = featureInfo();
        final String expectedDisable = "{\"shop-id\":1,\"feature-id\":\"126\",\"status\":\"DONT_WANT\"," +
                "\"can-enable\":true,\"feature-name\":\"SHOP_LOGO\"}";
        JsonTestUtil.assertEquals(responseDisable, expectedDisable);

        // включаем фичу
        final ResponseEntity<String> responseNew = enableFeature();
        final String expectedNew = "{\"shop-id\":1,\"feature-id\":\"126\",\"feature-name\":\"SHOP_LOGO\"," +
                "\"status\":\"NEW\",\"cutoffs\":[{\"type\":\"TESTING\"}]}";
        JsonTestUtil.assertEquals(responseNew, expectedNew);

        // удаляем логотип
        FunctionalTestHelper.delete(baseUrl + "/businesses/1/logo");
        final ResponseEntity<String> responseDelete = featureInfo();
        final String expectedDelete = "{\"shop-id\":1,\"feature-id\":\"126\",\"feature-name\":\"SHOP_LOGO\"," +
                "\"status\":\"DONT_WANT\",\"can-enable\":false,\"failed-precondition\":[\"shop-logo-enabled\"]," +
                "\"cutoffs\":[{\"type\":\"PARTNER\"}]}";
        JsonTestUtil.assertEquals(responseDelete, expectedDelete);

        // повторно загружаем логотип
        mockAvatarsResponse(false, ImageType.PNG.name(), HEIGHT, WIDTH);
        doMultipartFileRequest(baseUrl + "/businesses/1/logo", CORRECT_PNG_LOGO);
        final ResponseEntity<String> responseNew2 = featureInfo();
        final String expectedNew2 = "{\"shop-id\":1,\"feature-id\":\"126\",\"feature-name\":\"SHOP_LOGO\"," +
                "\"status\":\"NEW\",\"cutoffs\":[{\"type\":\"TESTING\"}]}";
        JsonTestUtil.assertEquals(responseNew2, expectedNew2);
    }


    @Test
    @DbUnitDataSet(after = "BusinessLogoControllerTest.uploadAfterFail.after.csv")
    void testUploadLogo() {
        checkUploadLogo(2L);
    }

    @Test
    @DbUnitDataSet(before = "BusinessLogoControllerTest.beforeSuccessLogo.csv",
            after = "BusinessLogoControllerTest.beforeSuccessLogo.csv")
    void testUploadLogoAfterSuccess() {
        checkUploadLogo(2L);
    }

    @Test
    @DbUnitDataSet(before = "BusinessLogoControllerTest.beforeRevokeLogo.csv",
            after = "BusinessLogoControllerTest.beforeRevokeLogo.csv")
    void testUploadLogoAfterRevoke() {
        checkUploadLogo(2L);
    }

    @Test
    @DbUnitDataSet(before = "BusinessLogoControllerTest.beforeFailLogo.csv",
            after = "BusinessLogoControllerTest.uploadAfterFail.after.csv")
    void testUploadLogoAfterFail() {
        checkUploadLogo(2L);
    }

    private void checkUploadLogo(long businessId) {
        AvatarsImageDTO avatarsResponse = mockAvatarsResponse(false, ImageType.PNG.name(), HEIGHT, WIDTH);
        doMultipartFileRequest(baseUrl + "/businesses/" + businessId + "/logo", CORRECT_PNG_LOGO);

        verify(avatarsClient).uploadImage(any(MultipartFile.class), eq(false));
        verify(avatarsClient).getImageDeleteUrl(avatarsResponse);
        verify(avatarsClient).getImageUrl(eq(new ThumbnailDTO(HEIGHT, WIDTH, "path/orig")));
        verifyNoMoreInteractions(avatarsClient);
    }


    private ResponseEntity<String> featureInfo() {
        return FunctionalTestHelper.get(baseUrl + "/businesses/1/featureInfo?_user_id=1&feature-id={featureId}",
                FeatureType.SHOP_LOGO.getId()
        );
    }

    private ResponseEntity<String> enableFeature() {
        return FunctionalTestHelper.post(baseUrl + "/businesses/1/enableFeature?_user_id=1&feature-id={featureId}",
                null,
                FeatureType.SHOP_LOGO.getId()
        );
    }
}
