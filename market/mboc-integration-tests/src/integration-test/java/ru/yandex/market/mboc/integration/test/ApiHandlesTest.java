package ru.yandex.market.mboc.integration.test;


import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.infrastructure.util.RetryHelper;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.http.MboCategory;
import ru.yandex.market.mboc.http.MboCategoryService;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.mboc.integration.test.config.HttpIntegrationTestConfig.CommonTestParameters;
import ru.yandex.market.mdm.http.MasterDataProto.MasterDataInfo;
import ru.yandex.market.mdm.http.MasterDataProto.ProviderProductMasterData;
import ru.yandex.market.mdm.http.MdmCommon;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 0. Подчищаем результаты предыдущих прогонов.
 * 1. /addProductInfo
 * 2. /saveTaskMappings
 * 3. Периодически проверяем, попал ли offer в Yt
 * 4. /searchProductInfoByYtStamp (для УК)
 * 4. /searchProductInfoLiteByYtStamp (для УК)
 * 5. /searchMappingsByKeys, /searchMappingsByMarketSkuId, /searchMappingsByShopId,
 * /searchApprovedMappingsByMarketSkuId, /searchApprovedMappingsByKeys.
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ApiHandlesTest extends BaseHttpIntegrationTestClass {

    private static final Logger log = LoggerFactory.getLogger(ApiHandlesTest.class);
    private static final String SHOP_SKU = "looner-x1-65-90";

    @Autowired
    private MboCategoryService mboCategoryService;
    @Autowired
    private MboMappingsService mboMappingsService;
    @Autowired
    private CommonTestParameters config;

    @Before
    public void setupServices() {
        log.info("Using {} as mboc.api.url for remote services.", config.getHost());
        log.info("Using {} as mboc.integration-test.root-uri for raw dev handles.", config.getIntTestHandlesHost());
    }

    @Ignore("https://st.yandex-team.ru/MBO-35473")
    @Test
    public void testHandles() {
        clearGarbage();
        createOffer();
        SupplierOffer.Offer offer = checkAndGetOffer();
        updateProcessingStatus(offer);
        saveTaskMappings(offer);
        long stamp = checkMappingInYt(offer);
        searchProductInfoByYtStamp(stamp, offer.getShopSkuId());
        searchProductInfoLiteByYtStamp(stamp, offer.getShopSkuId());
        checkSearchHandles(offer.getShopSkuId());
        checkUpdateHandles(offer.getShopSkuId());
    }

    private void clearGarbage() {
        String response = restTemplate.getForObject(config.getIntTestHandlesHost() +
            "/api/int-test/cleanup?supplierId=" + config.getSupplierId() + "&shopSkuId=" + SHOP_SKU, String.class);
        assertEquals("Done", response);
    }

    private void createOffer() {
        MboMappings.ProviderProductInfoRequest request = MboMappings.ProviderProductInfoRequest.newBuilder()
            .addProviderProductInfo(MboMappings.ProviderProductInfo.newBuilder()
                .setMarketSkuId(config.getMarketSkuId())
                .setShopId(config.getSupplierId())
                .setShopSkuId(SHOP_SKU)
                .setTitle("Looner X1 Pure dist.65-90")
                .setShopCategoryName("Лунные призмы")
                .addBarcode("8147933775581")
                .setMasterDataInfo(masterData())
                .build())
            .build();
        MboMappings.ProviderProductInfoResponse addMappingResponse = mboMappingsService.addProductInfo(request);
        assertThat(addMappingResponse.getResultsList()).isNotEmpty();
        addMappingResponse.getResultsList().forEach(res -> {
            assertThat(res.getErrorsList()).isEmpty();
        });
    }

    private SupplierOffer.Offer checkAndGetOffer() {
        MboMappings.SearchMappingsResponse searchMappingsResponse = mboMappingsService.searchMappingsByShopId(
            MboMappings.SearchMappingsBySupplierIdRequest.newBuilder()
                .setSupplierId(config.getSupplierId())
                .build()
        );
        assertEquals(1, searchMappingsResponse.getOffersCount());
        return searchMappingsResponse.getOffersList().get(0);
    }

    private void updateProcessingStatus(SupplierOffer.Offer offer) {
        String response = restTemplate.getForObject(config.getIntTestHandlesHost() +
                "/api/int-test/update-processing-status?offerId=" + offer.getInternalOfferId() +
                "&processingStatus=" + Offer.ProcessingStatus.IN_PROCESS,
            String.class);
        assertEquals("Done", response);
    }

    private void saveTaskMappings(SupplierOffer.Offer offer) {
        MboCategory.SaveTaskMappingsResponse saveTaskMappingsResponse = mboCategoryService.saveTaskMappings(
            MboCategory.SaveTaskMappingsRequest.newBuilder()
                .addMapping(SupplierOffer.ContentTaskResult.newBuilder()
                    .setOfferId(String.valueOf(offer.getInternalOfferId()))
                    .setStatus(SupplierOffer.SupplierOfferMappingStatus.MAPPED)
                    .setComment("Just DOIT!")
                    .setMarketSkuId(config.getMarketSkuId())
                    .setStaffLogin("api-integration-test")
                    .build())
                .build()
        );
        assertThat(saveTaskMappingsResponse.getResult().getStatus()).isEqualTo(SupplierOffer.OperationStatus.SUCCESS);
    }

    private long checkMappingInYt(SupplierOffer.Offer offer) {
        long ytStamp = RetryHelper.retry("check Yt for offer", 5, 15000L, attempt -> {
            String stampStr = restTemplate.getForObject(config.getIntTestHandlesHost() +
                    "/api/int-test/check-yt-offer?supplierId=" + offer.getSupplierId() +
                    "&shopSkuId=" + offer.getShopSkuId(),
                String.class);
            if (StringUtils.isEmpty(stampStr)) {
                throw new IllegalStateException("Offer is not in Yt yet.");
            }
            long stamp = Long.parseLong(stampStr);
            if (stamp == 0) {
                throw new IllegalStateException("Stamp is incorrect, offer is probably missing in Yt");
            }
            return stamp;
        });
        assertTrue(ytStamp > 0);
        return ytStamp;
    }

    private void searchProductInfoByYtStamp(long stamp, String shopSkuId) {
        MboMappings.SearchProductInfoByYtStampResponse response = mboMappingsService.searchProductInfoByYtStamp(
            MboMappings.SearchProductInfoByYtStampRequest.newBuilder()
                .setFromStamp(stamp)
                .setCount(1)
                .build()
        );

        assertThat(response.getProviderProductInfoList())
            .hasSize(1)
            .extracting(MboMappings.ProviderProductInfo::getUploadToYtStamp)
            .containsExactly(stamp);
        MboMappings.ProviderProductInfo info = response.getProviderProductInfoList().get(0);
        assertEquals(shopSkuId, info.getShopSkuId());
        assertEquals(config.getSupplierId(), info.getShopId());
    }

    private void searchProductInfoLiteByYtStamp(long stamp, String shopSkuId) {
        MboMappings.SearchProductInfoLiteByYtStampResponse response = mboMappingsService.searchProductInfoLiteByYtStamp(
            MboMappings.SearchProductInfoByYtStampRequest.newBuilder()
                .setFromStamp(stamp)
                .setCount(1)
                .build()
        );

        assertThat(response.getProviderProductInfoLiteList())
            .hasSize(1)
            .extracting(MboMappings.ProviderProductInfoLite::getUploadToYtStamp)
            .containsExactly(stamp);
        MboMappings.ProviderProductInfoLite info = response.getProviderProductInfoLiteList().get(0);
        assertEquals(shopSkuId, info.getShopSkuId());
        assertEquals(config.getSupplierId(), info.getShopId());
    }

    private void checkUpdateHandles(String shopSkuId) {
        mboMappingsService.updateAvailability(MboMappings.UpdateAvailabilityRequest.newBuilder()
            .addMappings(MboMappings.UpdateAvailabilityRequest.Locator.newBuilder()
                .setSupplierId(config.getSupplierId())
                .setShopSku(shopSkuId)
                .build()
            )
            .setAvailability(SupplierOffer.Availability.INACTIVE)
            .build()
        );

        MboMappings.SearchMappingsResponse response = mboMappingsService.searchMappingsByKeys(
            MboMappings.SearchMappingsByKeysRequest.newBuilder()
                .addKeys(MboMappings.SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                    .setSupplierId(config.getSupplierId())
                    .setShopSku(shopSkuId)
                    .build())
                .build());

        Assertions.assertThat(response.getOffers(0).getAvailability())
            .isEqualTo(SupplierOffer.Availability.INACTIVE);
    }

    private void checkSearchHandles(String shopSkuId) {
        MboMappings.SearchMappingsResponse response = mboMappingsService.searchMappingsByKeys(
            MboMappings.SearchMappingsByKeysRequest.newBuilder()
                .addKeys(MboMappings.SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                    .setSupplierId(config.getSupplierId())
                    .setShopSku(shopSkuId)
                    .build())
                .build());
        assertEquals(1, response.getOffersCount());
        SupplierOffer.Offer offer = response.getOffers(0);
        assertEquals(shopSkuId, offer.getShopSkuId());
        assertEquals(config.getSupplierId(), offer.getSupplierId());

        response = mboMappingsService.searchMappingsByMarketSkuId(
            MboMappings.SearchMappingsByMarketSkuIdRequest.newBuilder()
                .addMarketSkuId(config.getMarketSkuId())
                .addMappingKind(MboMappings.MappingKind.APPROVED_MAPPING)
                .build());
        List<SupplierOffer.Offer> supplierOffers = response.getOffersList().stream()
            .filter(o -> o.getSupplierId() == config.getSupplierId())
            .collect(Collectors.toList());
        assertEquals(1, supplierOffers.size());
        SupplierOffer.Offer fetchedOffer = supplierOffers.get(0);
        assertEquals(offer, fetchedOffer);

        MboMappings.SearchApprovedMappingsResponse approvedResponse =
            mboMappingsService.searchApprovedMappingsByMarketSkuId(
                MboMappings.SearchApprovedMappingsRequest.newBuilder()
                    .addMarketSkuId(config.getMarketSkuId())
                    .build());
        List<MboMappings.ApprovedMappingInfo> approvedOffers = approvedResponse.getMappingList().stream()
            .filter(m -> m.getSupplierId() == config.getSupplierId())
            .collect(Collectors.toList());
        assertEquals(1, approvedOffers.size());
        MboMappings.ApprovedMappingInfo mappingInfo = approvedOffers.get(0);
        assertEquals(config.getMarketSkuId(), mappingInfo.getMarketSkuId());
        assertEquals(offer.getShopSkuId(), mappingInfo.getShopSku());
        assertEquals(config.getSupplierId(), mappingInfo.getSupplierId());

        approvedResponse = mboMappingsService.searchApprovedMappingsByKeys(
            MboMappings.SearchMappingsByKeysRequest.newBuilder()
                .addKeys(MboMappings.SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                    .setSupplierId(config.getSupplierId())
                    .setShopSku(offer.getShopSkuId())
                    .build())
                .build());
        assertEquals(1, approvedResponse.getMappingCount());
        mappingInfo = approvedResponse.getMapping(0);
        assertEquals(config.getMarketSkuId(), mappingInfo.getMarketSkuId());
        assertEquals(offer.getShopSkuId(), mappingInfo.getShopSku());
        assertEquals(config.getSupplierId(), mappingInfo.getSupplierId());
    }

    private MasterDataInfo.Builder masterData() {
        return MasterDataInfo.newBuilder()
            .setProviderProductMasterData(
                ProviderProductMasterData.newBuilder().addManufacturerCountry("Россия")
                        .setWeightDimensionsInfo(MdmCommon.WeightDimensionsInfo.newBuilder().setWeightGrossMg(1000000)
                        .setBoxHeightUm(50000).setBoxLengthUm(50000).setBoxWidthUm(50000)).build()
            );
    }
}
