package ru.yandex.market.mbi.api.controller.direct;

import java.math.BigDecimal;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.api.ApiObject;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.geobase.model.RegionConstants;
import ru.yandex.market.core.passport.PassportService;
import ru.yandex.market.core.passport.model.UserInfo;
import ru.yandex.market.mbi.api.client.direct.dto.AddDirectFeedRequestDTO;
import ru.yandex.market.mbi.api.client.direct.dto.AddDirectFeedResponseDTO;
import ru.yandex.market.mbi.api.client.direct.dto.AddDirectFileRequestDTO;
import ru.yandex.market.mbi.api.client.direct.dto.AddDirectSiteRequestDTO;
import ru.yandex.market.mbi.api.client.direct.dto.SetDirectFeedFeatureRequestDTO;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.open.api.client.model.DatacampPartnerProperties;
import ru.yandex.market.mbi.open.api.client.model.DatacampUpdatePartnerPropertiesRequest;
import ru.yandex.market.mbi.open.api.client.model.DeleteSitePreviewsRequest;
import ru.yandex.market.mbi.open.api.client.model.DeleteSitePreviewsResponse;
import ru.yandex.market.mbi.open.api.client.model.DirectSitePreviewIds;
import ru.yandex.market.mbi.open.api.client.model.FeedRegisterUrlRequest;
import ru.yandex.market.mbi.open.api.client.model.RegisterFeedResponse;
import ru.yandex.market.mbi.open.api.client.model.SiteRegisterUrlRequest;
import ru.yandex.market.mbi.open.api.exception.MbiOpenApiClientResponseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link ru.yandex.market.mbi.api.controller.DirectFeedController}.
 *
 * @author moskovkin@yandex-team.ru
 * @since 28.12.2020
 */
@DbUnitDataSet(before = "DirectFeedControllerTest.csv")
class DirectFeedControllerTest extends FunctionalTest {
    private static final long DIRECT_CLIENT_ID_1 = 11;
    private static final long DIRECT_FEED_1 = 12;
    private static final long DIRECT_OWNER_1 = 13;
    private static final long BUSINESS_ID_1 = 14;
    private static final long PARTNER_ID_1_1 = 15;
    private static final long PARTNER_ID_1_2 = 16;
    private static final long FEED_ID_1_1 = 16;
    private static final long FEED_ID_1_2 = 17;
    private static final String URL_1_1 = "http://test.me/feed";
    private static final String URL_1_2 = "http://test.me/site";

    private static final long DIRECT_CLIENT_ID_2 = 21;
    private static final long DIRECT_FEED_2 = 22;
    private static final long DIRECT_OWNER_2 = 23;
    private static final String URL_2 = "http://mds.yandex.ru/file.yml";

    private static final String DOMAIN_2 = "market.yandex.ru";
    private static final String SCHEMA_2 = "http";
    private static final String PATH_2 = "some/path";

    private static final long TEST_UID = 1635683719L;

    @Autowired
    private PassportService passportService;

    @BeforeEach
    void setup() {
        when(passportService.getUserInfo(anyLong())).thenAnswer(i ->
                new UserInfo(
                        i.<Long>getArgument(0),
                        "testUser",
                        null,
                        "testUser"
                )
        );
    }

    @Test
    void testRegisterExisingDatacampFeed() {
        RegisterFeedResponse response = getMbiOpenApiClient().datacampFeedRegisterUrl(new FeedRegisterUrlRequest()
                .url(URL_1_1)
                .businessId(BUSINESS_ID_1)
        );
        assertThat(response)
                .usingRecursiveComparison()
                .ignoringAllOverriddenEquals()
                .isEqualTo(new RegisterFeedResponse()
                        .businessId(BUSINESS_ID_1)
                        .partnerId(PARTNER_ID_1_1)
                        .feedId(FEED_ID_1_1)
                );
    }

    @Test
    void testRegisterNewDatacampFeed() {
        RegisterFeedResponse response = getMbiOpenApiClient().datacampFeedRegisterUrl(new FeedRegisterUrlRequest()
                .ownerUid(10L)
                .url("http://unknown.url")
        );
        assertThat(response)
                .usingRecursiveComparison()
                .ignoringAllOverriddenEquals()
                .isEqualTo(new RegisterFeedResponse()
                        .businessId(1L)
                        .partnerId(2L)
                        .feedId(1L)
                );
    }

    @Test
    @DbUnitDataSet(before = "DirectFeedControllerTest.registerOnContactWithTpl.csv")
    void testRegisterNewDatacampFeedOnContactWithTpl() {
        Assertions.assertThatCode(() -> getMbiOpenApiClient().datacampFeedRegisterUrl(new FeedRegisterUrlRequest()
                .ownerUid(10L)
                .url("http://unknown.url")
        )).hasMessageMatching("Bad Request");
    }

    @Test
    @DbUnitDataSet(before = "DirectFeedControllerTest.registerOnContactWithShop.csv")
    void testRegisterNewDatacampFeedOnContactWithShop() {
        RegisterFeedResponse response = getMbiOpenApiClient().datacampFeedRegisterUrl(new FeedRegisterUrlRequest()
                .ownerUid(10L)
                .url("http://unknown.url")
        );
        assertThat(response)
                .usingRecursiveComparison()
                .ignoringAllOverriddenEquals()
                .isEqualTo(new RegisterFeedResponse()
                        .businessId(1L)
                        .partnerId(2L)
                        .feedId(1L)
                );
    }

    @Test
    @DbUnitDataSet(before = "DirectFeedControllerTest.registerOnContactWithDirect.csv")
    void testRegisterNewDatacampFeedOnContactWithDirect() {
        RegisterFeedResponse response = getMbiOpenApiClient().datacampFeedRegisterUrl(new FeedRegisterUrlRequest()
                .ownerUid(10L)
                .url("http://unknown.url")
        );
        assertThat(response)
                .usingRecursiveComparison()
                .ignoringAllOverriddenEquals()
                .isEqualTo(new RegisterFeedResponse()
                        .businessId(1L)
                        .partnerId(2L)
                        .feedId(1L)
                );
    }

    @Test
    @DbUnitDataSet(before = {"DirectFeedControllerTest.registerOnContactWithShop.csv",
            "DirectFeedControllerTest.registerOnContactWithTpl.csv"})
    void testRegisterNewDatacampFeedOnContactWithTplAndShop() {
        RegisterFeedResponse response = getMbiOpenApiClient().datacampFeedRegisterUrl(new FeedRegisterUrlRequest()
                .ownerUid(10L)
                .url("http://unknown.url")
        );
        assertThat(response)
                .usingRecursiveComparison()
                .ignoringAllOverriddenEquals()
                .isEqualTo(new RegisterFeedResponse()
                        .businessId(1L)
                        .partnerId(2L)
                        .feedId(1L)
                );
    }

    @Test
    void testRegisterExisingDatacampSite() {
        RegisterFeedResponse response = getMbiOpenApiClient().datacampFeedRegisterSite(new SiteRegisterUrlRequest()
                .datacampPartnerProperties(new DatacampPartnerProperties().verticalShare(true))
                .url(URL_1_2)
                .businessId(BUSINESS_ID_1)
        );
        assertThat(response)
                .usingRecursiveComparison()
                .ignoringAllOverriddenEquals()
                .isEqualTo(new RegisterFeedResponse()
                        .businessId(BUSINESS_ID_1)
                        .partnerId(PARTNER_ID_1_2)
                        .feedId(FEED_ID_1_2)
                );
    }

    @Test
    void testRegisterNewDatacampSite() {
        RegisterFeedResponse response = getMbiOpenApiClient().datacampFeedRegisterSite(new SiteRegisterUrlRequest()
                .datacampPartnerProperties(new DatacampPartnerProperties().verticalShare(true))
                .ownerUid(10L)
                .url("http://unknown.url")
        );
        assertThat(response)
                .usingRecursiveComparison()
                .ignoringAllOverriddenEquals()
                .isEqualTo(new RegisterFeedResponse()
                        .businessId(1L)
                        .partnerId(2L)
                        .feedId(1L)
                );
    }

    @Test
    @DbUnitDataSet(after = "DirectFeedControllerTest.testSetDatacampProperties.csv")
    void testSetDatacampProperties() {
        Assertions.assertThatCode(() -> getMbiOpenApiClient().datacampFeedUpdateFeatures(
                new DatacampUpdatePartnerPropertiesRequest()
                        .partnerId(PARTNER_ID_1_1)
                        .datacampPartnerProperties(new DatacampPartnerProperties()
                                .verticalShare(true)
                                .directStandby(false)
                                .homeRegion(RegionConstants.RUSSIA)
                                .regions(List.of(RegionConstants.MOSCOW_OBLAST))
                        )
        )).doesNotThrowAnyException();
    }

    @Test
    @DbUnitDataSet(before = "DirectFeedControllerTest.testDatacampFeedOffByPartner.csv")
    void testDatacampFeedOffByPartner() {
        assertThatThrownBy(() -> getMbiOpenApiClient().datacampFeedOffByPartner(BigDecimal.valueOf(17L)));

        getMbiOpenApiClient().datacampFeedOffByPartner(BigDecimal.valueOf(18L));
    }

    @Test
    void testRefreshExisingFeed() {
        AddDirectFeedRequestDTO addDirectFeedRequestDTO = AddDirectFeedRequestDTO.builder()
                .uid(DIRECT_OWNER_1)
                .directFeedId(DIRECT_FEED_1)
                .clientId(DIRECT_CLIENT_ID_1)
                .feedUrl(URL_1_1)
                .build();
        AddDirectFeedResponseDTO response = mbiApiClient.refreshDirectFeed(addDirectFeedRequestDTO);
        assertThat(response)
                .usingRecursiveComparison()
                .ignoringAllOverriddenEquals()
                .isEqualTo(AddDirectFeedResponseDTO.builder()
                        .businessId(BUSINESS_ID_1)
                        .partnerId(PARTNER_ID_1_1)
                        .marketFeedId(FEED_ID_1_1)
                        .build()
                );
    }

    @Test
    @DisplayName("Обновление урла директового фида")
    @DbUnitDataSet(after = "DirectFeedControllerTest.updateUrl.after.csv")
    void testRefreshUrlInFeed() {
        AddDirectFeedRequestDTO addDirectFeedRequestDTO = AddDirectFeedRequestDTO.builder()
                .uid(DIRECT_OWNER_1)
                .directFeedId(DIRECT_FEED_1)
                .clientId(DIRECT_CLIENT_ID_1)
                .feedUrl("http://new_url.ru")
                .enableDirectPlacement(true)
                .build();
        AddDirectFeedResponseDTO response = mbiApiClient.refreshDirectFeed(addDirectFeedRequestDTO);
        assertThat(response)
                .usingRecursiveComparison()
                .ignoringAllOverriddenEquals()
                .isEqualTo(AddDirectFeedResponseDTO.builder()
                        .businessId(BUSINESS_ID_1)
                        .partnerId(PARTNER_ID_1_1)
                        .marketFeedId(FEED_ID_1_1)
                        .build()
                );
    }

    @Test
    @DisplayName("Обновление логина директового фида")
    @DbUnitDataSet(after = "DirectFeedControllerTest.updateLogin.after.csv")
    void testRefreshLoginInFeed() {
        AddDirectFeedRequestDTO addDirectFeedRequestDTO = AddDirectFeedRequestDTO.builder()
                .uid(DIRECT_OWNER_1)
                .directFeedId(DIRECT_FEED_1)
                .clientId(DIRECT_CLIENT_ID_1)
                .feedUrl(URL_1_1)
                .feedHttpLogin("http-login")
                .enableDirectPlacement(true)
                .build();
        AddDirectFeedResponseDTO response = mbiApiClient.refreshDirectFeed(addDirectFeedRequestDTO);
        assertThat(response)
                .usingRecursiveComparison()
                .ignoringAllOverriddenEquals()
                .isEqualTo(AddDirectFeedResponseDTO.builder()
                        .businessId(BUSINESS_ID_1)
                        .partnerId(PARTNER_ID_1_1)
                        .marketFeedId(FEED_ID_1_1)
                        .build()
                );
    }

    @Test
    @DisplayName("Обновление пароля директового фида")
    @DbUnitDataSet(after = "DirectFeedControllerTest.updatePass.after.csv")
    void testRefreshPassInFeed() {
        AddDirectFeedRequestDTO addDirectFeedRequestDTO = AddDirectFeedRequestDTO.builder()
                .uid(DIRECT_OWNER_1)
                .directFeedId(DIRECT_FEED_1)
                .clientId(DIRECT_CLIENT_ID_1)
                .feedUrl(URL_1_1)
                .feedHttpPassword("http-pass")
                .enableDirectPlacement(true)
                .build();
        AddDirectFeedResponseDTO response = mbiApiClient.refreshDirectFeed(addDirectFeedRequestDTO);
        assertThat(response)
                .usingRecursiveComparison()
                .ignoringAllOverriddenEquals()
                .isEqualTo(AddDirectFeedResponseDTO.builder()
                        .businessId(BUSINESS_ID_1)
                        .partnerId(PARTNER_ID_1_1)
                        .marketFeedId(FEED_ID_1_1)
                        .build()
                );
    }

    @Test
    void testRefreshNewSite() {
        AddDirectSiteRequestDTO addDirectSiteRequestDTO = AddDirectSiteRequestDTO.builder()
                .uid(DIRECT_OWNER_2)
                .directFeedId(DIRECT_FEED_2)
                .clientId(DIRECT_CLIENT_ID_2)
                .domain(DOMAIN_2)
                .schema(SCHEMA_2)
                .path(PATH_2)
                .build();
        AddDirectFeedResponseDTO response = mbiApiClient.refreshDirectSite(addDirectSiteRequestDTO);
        assertThat(response.getBusinessId()).isNotNull();
        assertThat(response.getPartnerId()).isNotNull();
        assertThat(response.getMarketFeedId()).isNotNull();
    }

    @Test
    void testRefreshNewFile() {
        AddDirectFileRequestDTO addDirectFileRequestDTO = AddDirectFileRequestDTO.builder()
                .uid(DIRECT_OWNER_2)
                .directFeedId(DIRECT_FEED_2)
                .clientId(DIRECT_CLIENT_ID_2)
                .feedUrl(URL_2)
                .build();
        AddDirectFeedResponseDTO response = mbiApiClient.refreshDirectFile(addDirectFileRequestDTO);
        assertThat(response.getBusinessId()).isNotNull();
        assertThat(response.getPartnerId()).isNotNull();
        assertThat(response.getMarketFeedId()).isNotNull();
    }

    @Test
    void testCyrillicSite() {
        AddDirectSiteRequestDTO addDirectSiteRequestDTO = AddDirectSiteRequestDTO.builder()
                .uid(DIRECT_OWNER_2)
                .directFeedId(DIRECT_FEED_2)
                .clientId(DIRECT_CLIENT_ID_2)
                .domain("кто.рф")
                .schema(SCHEMA_2)
                .path(PATH_2)
                .build();
        AddDirectFeedResponseDTO response = mbiApiClient.refreshDirectSite(addDirectSiteRequestDTO);
        assertThat(response.getBusinessId()).isNotNull();
        assertThat(response.getPartnerId()).isNotNull();
        assertThat(response.getMarketFeedId()).isNotNull();
    }

    @Test
    @DbUnitDataSet(after = "DirectFeedControllerTest.setFeature.after.csv")
    void testSetShopFeatures() {
        SetDirectFeedFeatureRequestDTO request = new SetDirectFeedFeatureRequestDTO();
        request.setShopId(15L);
        request.setStandby(true);
        request.setSearchSnippetGallery(true);
        request.setGoodsAds(false);
        var response = mbiApiClient.setShopFeatures(request);
        assertThat(response).isInstanceOf(ApiObject.Ok.class);
    }

    /**
     * Проверяем, что ранее включенный магазин получает катоф PARTNER и фичу в статусе DONT_WANT.
     */
    @Test
    @DbUnitDataSet(after = "DirectFeedControllerTest.testDisableDirectShop.after.csv")
    void testDisableDirectShop() {
        getMbiOpenApiClient().requestDirectPartnerDisable(PARTNER_ID_1_1, TEST_UID);
    }

    /**
     * Проверяем, что магазин с единственным катофом PARTNER и фичей в статусе DONT_WANT
     * переходит в состояние фичи SUCCESS без катофов.
     */
    @Test
    @DbUnitDataSet(
            before = "DirectFeedControllerTest.testEnableDirectShop.before.csv",
            after = "DirectFeedControllerTest.testEnableDirectShop.after.csv"
    )
    void testEnableDirectShop() {
        getMbiOpenApiClient().requestDirectPartnerEnable(17L, TEST_UID);
    }

    @Test
    @DbUnitDataSet(
            before = "DirectFeedControllerTest.testEnableDirectShopWithDefaultFeatureState.before.csv",
            after = "DirectFeedControllerTest.testEnableDirectShop.after.csv"
    )
    void testEnableDirectShopWithDefaultFeatureState() {
        getMbiOpenApiClient().requestDirectPartnerEnable(17L, TEST_UID);
    }

    /**
     * Проверяем идемпотентность метода для отключения размещения в директе.
     */
    @Test
    @DbUnitDataSet(
            before = "DirectFeedControllerTest.testEnableDirectShop.before.csv",
            after = "DirectFeedControllerTest.testDisableDirectShopIsIdempotent.after.csv"
    )
    void testDisableDirectShopIsIdempotent() {
        getMbiOpenApiClient().requestDirectPartnerDisable(17L, TEST_UID);
    }

    /**
     * Проверяем идемпотентность метода для включения размещения в директе.
     */
    @Test
    @DbUnitDataSet(after = "DirectFeedControllerTest.csv")
    void testEnableDirectShopIsIdempotent() {
        getMbiOpenApiClient().requestDirectPartnerEnable(PARTNER_ID_1_1, TEST_UID);
    }

    /**
     * Проверяем, что размещение в директе не происходит, если остаются открытыми другие отключения.
     */
    @Test
    @DbUnitDataSet(
            before = {
                    "DirectFeedControllerTest.testEnableDirectShop.before.csv",
                    "DirectFeedControllerTest.testDirectShopIsEnabledWithExistingCutoffs.before.csv"
            },
            after = "DirectFeedControllerTest.testDirectShopIsEnabledWithExistingCutoffs.after.csv"
    )
    void testDirectShopIsEnabledWithExistingCutoffs() {
        getMbiOpenApiClient().requestDirectPartnerEnable(17L, TEST_UID);
    }

    @Test
    @DbUnitDataSet(
            before = "DirectFeedControllerTest.testDirectDeleteSitePreviews.before.csv",
            after = "DirectFeedControllerTest.testDirectDeleteSitePreviews.after.csv"
    )
    void testDirectDeleteSitePreviews() {
        var request = new DeleteSitePreviewsRequest()
                .addFeedIdsItem(new DirectSitePreviewIds().partnerId(21L).feedId(31L))
                .addFeedIdsItem(new DirectSitePreviewIds().partnerId(22L).feedId(32L))
                .addFeedIdsItem(new DirectSitePreviewIds().partnerId(20L).feedId(30L))
                .addFeedIdsItem(new DirectSitePreviewIds().partnerId(23L).feedId(24L));

        DeleteSitePreviewsResponse result = getMbiOpenApiClient().deleteSitePreviews(request);

        assertThat(result.getFeedIds()).hasSize(2);
    }

    @Test
    @DbUnitDataSet(
            before = "DirectFeedControllerTest.testDirectDeleteSitePreviews.before.csv",
            after = "DirectFeedControllerTest.testDirectDeleteSitePreviews.before.csv"
    )
    void testDirectDeleteSitePreviewsWithoutFeedIds() {
        var request = new DeleteSitePreviewsRequest();

        MbiOpenApiClientResponseException mbiOpenApiClientResponseException =
                Assertions.catchThrowableOfType(() -> getMbiOpenApiClient().deleteSitePreviews(request),
                        MbiOpenApiClientResponseException.class);

        assertThat(mbiOpenApiClientResponseException).isNotNull();
        assertThat(mbiOpenApiClientResponseException.getHttpErrorCode()).isEqualTo(400);
        assertThat(mbiOpenApiClientResponseException.getApiError().getMessage())
                .isEqualTo("feedIds size must be between 1 and 1000");
    }

    @Test
    @DbUnitDataSet(
            before = "DirectFeedControllerTest.testDirectDeleteSitePreviews.before.csv",
            after = "DirectFeedControllerTest.testDirectDeleteSitePreviews.before.csv"
    )
    void testDirectDeleteSitePreviewsWithMoreThanMaxFeedIds() {
        var request = new DeleteSitePreviewsRequest();
        for (int i = 0; i < 1001; i++) {
            request.addFeedIdsItem(new DirectSitePreviewIds().partnerId(21L).feedId(31L));
        }

        MbiOpenApiClientResponseException mbiOpenApiClientResponseException =
                Assertions.catchThrowableOfType(() -> getMbiOpenApiClient().deleteSitePreviews(request),
                        MbiOpenApiClientResponseException.class);

        assertThat(mbiOpenApiClientResponseException).isNotNull();
        assertThat(mbiOpenApiClientResponseException.getHttpErrorCode()).isEqualTo(400);
        assertThat(mbiOpenApiClientResponseException.getApiError().getMessage())
                .isEqualTo("feedIds size must be between 1 and 1000");
    }
}
