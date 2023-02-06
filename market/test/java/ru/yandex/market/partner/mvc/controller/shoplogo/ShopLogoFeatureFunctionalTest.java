package ru.yandex.market.partner.mvc.controller.shoplogo;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import ru.yandex.market.api.cpa.yam.entity.PartnerApplicationDocumentType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.avatars.AvatarsClient;
import ru.yandex.market.core.avatars.model.AvatarsImageDTO;
import ru.yandex.market.core.avatars.model.MetaDTO;
import ru.yandex.market.core.avatars.model.ThumbnailDTO;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.logo.model.ImageType;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

@DbUnitDataSet(before = "ShopLogo.before.csv")
public class ShopLogoFeatureFunctionalTest extends FunctionalTest {
    private static final int USER_ID = 10;
    private static final String TEST_DIR = "ru/yandex/market/partner/mvc/controller/shoplogo/images/";
    private static final int GROUP_ID = 123;
    private static final int HEIGHT = 14;
    private static final int WIDTH = 100;
    private static final String IMAGE_NAME = "imagename";
    private static final long SHOP_PARTNER_ID = 333L;
    private static final long SHOP_CAMPAIGN_ID = 3333L;
    private static final long FMCG_PARTNER_ID = 444L;
    private static final long FMCG_CAMPAIGN_ID = 4444L;
    private static final String CORRECT_LOGO = "correct_logo.svg";

    @Autowired
    private AvatarsClient avatarsClient;

    @Test
    void testShop() {
        checkLogo(SHOP_PARTNER_ID, SHOP_CAMPAIGN_ID);
    }

    @Test
    void testFMCG() {
        checkLogo(FMCG_PARTNER_ID, FMCG_CAMPAIGN_ID);
    }

    private void checkLogo(final long partnerId, final long campaignId) {
        mockAvatarsResponse(false, ImageType.SVG.name(), HEIGHT, WIDTH);
        uploadFile(campaignId, CORRECT_LOGO);

        // загрузка логотипа
        final ResponseEntity<String> responseDisable = featureInfo(campaignId, FeatureType.SHOP_LOGO.getId());
        final String expectedDisable = "{\"shop-id\":" + partnerId + ",\"feature-id\":\"126\",\"status\":\"DONT_WANT\",\"can-enable\":true,\"feature-name\":\"SHOP_LOGO\"}";
        JsonTestUtil.assertEquals(responseDisable, expectedDisable);

        // включение фичи
        final ResponseEntity<String> responseNew = enableFeature(campaignId, FeatureType.SHOP_LOGO.getId());
        final String expectedNew = "{\"shop-id\":" + partnerId + ",\"feature-id\":\"126\",\"feature-name\":\"SHOP_LOGO\",\"status\":\"NEW\"," +
                "\"cutoffs\":[{\"type\":\"TESTING\"}]}";
        JsonTestUtil.assertEquals(responseNew, expectedNew);

        // удаление логотипа
        deleteShopLogo(campaignId);
        final ResponseEntity<String> responseDelete = featureInfo(campaignId, FeatureType.SHOP_LOGO.getId());
        final String expectedDelete = "{\"shop-id\":" + partnerId + ",\"feature-id\":\"126\",\"feature-name\":\"SHOP_LOGO\"," +
                "\"status\":\"DONT_WANT\",\"can-enable\":false,\"failed-precondition\":[\"shop-logo-enabled\"]," +
                "\"cutoffs\":[{\"type\":\"PARTNER\"}]}";
        JsonTestUtil.assertEquals(responseDelete, expectedDelete);

        // повторная загрузка логотипа
        uploadFile(campaignId, CORRECT_LOGO);
        final ResponseEntity<String> responseNew2 = enableFeature(campaignId, FeatureType.SHOP_LOGO.getId());
        final String expectedNew2 = "{\"shop-id\":" + partnerId + ",\"feature-id\":\"126\",\"feature-name\":\"SHOP_LOGO\",\"status\":\"NEW\"," +
                "\"cutoffs\":[{\"type\":\"TESTING\"}]}";
        JsonTestUtil.assertEquals(responseNew2, expectedNew2);
    }

    private AvatarsImageDTO mockAvatarsResponse(boolean isTemporary, String format, int height, int width) {
        MetaDTO meta = new MetaDTO(false, format, 10);
        Map<String, ThumbnailDTO> sizes = new HashMap<>();
        sizes.put("orig", new ThumbnailDTO(height, width, "path/orig"));
        sizes.put("small", new ThumbnailDTO(height / 2, width / 2, "path/small"));
        AvatarsImageDTO avatarsResponse = new AvatarsImageDTO(GROUP_ID, IMAGE_NAME, meta, sizes);
        doReturn(avatarsResponse).when(avatarsClient).uploadImage(any(MultipartFile.class), eq(isTemporary));
        return avatarsResponse;
    }

    private void deleteShopLogo(long campaignId) {
        final String url = shopLogoUrl(campaignId) + "/delete";
        FunctionalTestHelper.delete(url);
    }

    private ResponseEntity<String> uploadFile(long campaignId, String fileName) {
        final String url = shopLogoUrl(campaignId) + "/upload";
        return doMultipartFileRequest(url, fileName);
    }

    private ResponseEntity<String> doMultipartFileRequest(String url, String fileName) {
        HttpEntity entity = FunctionalTestHelper.createMultipartHttpEntity(
                "file",
                new ClassPathResource(TEST_DIR + fileName),
                (params) -> {
                    params.add("type", String.valueOf(PartnerApplicationDocumentType.OTHER.getId()));
                }
        );
        return FunctionalTestHelper.post(url, entity);
    }

    private String shopLogoUrl(long campaignId) {
        return baseUrl + String.format("/campaign/%d/logo", campaignId);
    }

    private ResponseEntity<String> enableFeature(long campaignId, long featureId) {
        return FunctionalTestHelper.post(baseUrl + "/enableFeature?_user_id={userId}&id={campaignId}&feature-id={featureId}",
                null,
                USER_ID,
                campaignId,
                featureId
        );
    }

    private ResponseEntity<String> featureInfo(long campaignId, long featureId) {
        return FunctionalTestHelper.get(baseUrl + "/featureInfo?_user_id={userId}&id={campaignId}&feature-id={featureId}",
                USER_ID,
                campaignId,
                featureId
        );
    }
}
