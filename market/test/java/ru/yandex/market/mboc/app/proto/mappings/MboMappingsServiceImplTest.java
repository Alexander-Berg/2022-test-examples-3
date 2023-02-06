//CHECKSTYLE:OFF
package ru.yandex.market.mboc.app.proto.mappings;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.mbo.lightmapper.criteria.Criteria;
import ru.yandex.market.mbo.lightmapper.criteria.NotCriteria;
import ru.yandex.market.mbo.lightmapper.criteria.OrCriteria;
import ru.yandex.market.mboc.app.mapping.RecheckMappingService;
import ru.yandex.market.mboc.app.proto.MboMappingsServiceImpl;
import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.contentprocessing.to.model.ContentProcessingOffer;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.Msku;
import ru.yandex.market.mboc.common.dict.ChildSskuResult;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.offers.model.AntiMapping;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.ContentCommentType;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryMock;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.offers.repository.search.OffersForServiceFilter;
import ru.yandex.market.mboc.common.services.business.BusinessSupplierService;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.proto.MboMappingsHelperService;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappings.SearchLiteMappingsByKeysRequest;
import ru.yandex.market.mboc.http.MboMappings.SearchLiteMappingsByMarketSkuIdRequest;
import ru.yandex.market.mboc.http.MboMappings.SearchMappingsByKeysRequest;
import ru.yandex.market.mboc.http.MboMappings.SearchMappingsByMarketSkuIdRequest;
import ru.yandex.market.mboc.http.MboMappings.SearchMappingsResponse;
import ru.yandex.market.mboc.http.MboMappings.UpdateContentProcessingTasksRequest.ContentProcessingTask;
import ru.yandex.market.mboc.http.MbocCommon;
import ru.yandex.market.mboc.http.MbocCommon.MappingInfoLite;
import ru.yandex.market.mboc.http.SupplierOffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.mboc.common.offers.model.Offer.SkuType.FAST_SKU;
import static ru.yandex.market.mboc.common.offers.model.Offer.SkuType.MARKET;
import static ru.yandex.market.mboc.common.services.proto.AddProductInfoHelperService.SaveMode.ALL_OR_NOTHING;

/**
 * @author yuramalinov
 * @created 06.07.18
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class MboMappingsServiceImplTest extends AbstractMboMappingsServiceImplTest {

    @Test
    public void testFindByYtStamp() {
        MboMappings.SearchProductInfoByYtStampResponse response = service.searchProductInfoByYtStamp(
            MboMappings.SearchProductInfoByYtStampRequest.newBuilder()
                .setFromStamp(12L)
                .setCount(100)
                .build()
        );

        Assertions.assertThat(response.getProviderProductInfoList())
            .hasSize(4)
            .extracting(MboMappings.ProviderProductInfo::getUploadToYtStamp)
            .containsExactlyInAnyOrder(12L, 13L, 14L, 14L);
    }

    @Test
    public void testFindByYtStampContainsUpdatedTs() {
        MboMappings.SearchProductInfoByYtStampResponse response = service.searchProductInfoByYtStamp(
            MboMappings.SearchProductInfoByYtStampRequest.newBuilder()
                .setFromStamp(14L)
                .setCount(100)
                .build()
        );

        Assertions.assertThat(response.getProviderProductInfoList())
            .hasSize(2)
            .extracting(MboMappings.ProviderProductInfo::getUploadToYtStamp)
            .containsExactlyInAnyOrder(14L, 14L);

        // verify that timestamps are matching that of offers in the DB
        List<BusinessSkuKey> offerKeys = response.getProviderProductInfoList().stream()
            .map(ppi -> new BusinessSkuKey(ppi.getShopId(), ppi.getShopSkuId()))
            .collect(Collectors.toList());
        List<Long> updatedTimestamps = offerRepository.findOffersByBusinessSkuKeys(offerKeys).stream()
            .map(o -> o.getUpdated().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
            .collect(Collectors.toList());

        Assertions.assertThat(response.getProviderProductInfoList())
            .extracting(MboMappings.ProviderProductInfo::getOfferUpdateTs)
            .containsOnlyElementsOf(updatedTimestamps);
    }

    @Test
    public void testFindByYtStampWithBusiness() {
        supplierRepository.insertBatch(
            new Supplier(2000, "biz").setType(MbocSupplierType.BUSINESS),
            new Supplier(2001, "biz-child1").setBusinessId(2000),
            new Supplier(2002, "biz-child2").setBusinessId(2000)
                .setType(MbocSupplierType.REAL_SUPPLIER).setRealSupplierId("BIZ_REAL"),
            new Supplier(2003, "biz-child23").setBusinessId(2000)
        );

        insertOffersKeepStamps(
            offer(1001, 2000, "bizsku0").setServiceOffers(List.of(
                new Offer.ServiceOffer(2001, MbocSupplierType.THIRD_PARTY, Offer.AcceptanceStatus.OK),
                new Offer.ServiceOffer(2002, MbocSupplierType.REAL_SUPPLIER, Offer.AcceptanceStatus.OK)
            )),
            offer(1002, 2001, "bizsku1").setServiceOffers(List.of(
                new Offer.ServiceOffer(2001, MbocSupplierType.THIRD_PARTY, Offer.AcceptanceStatus.OK)
            )),
            offer(1003, 2002, "bizsku2").setServiceOffers(List.of(
                new Offer.ServiceOffer(2002, MbocSupplierType.REAL_SUPPLIER, Offer.AcceptanceStatus.OK)
            ))
        );

        MboMappings.SearchProductInfoByYtStampResponse response = service.searchProductInfoByYtStamp(
            MboMappings.SearchProductInfoByYtStampRequest.newBuilder()
                .setFromStamp(11L)
                .setCount(100)
                .build()
        );

        Assertions.assertThat(response.getProviderProductInfoList())
            .hasSize(11)
            .extracting(MboMappings.ProviderProductInfo::getShopId)
            // suppliers: 2001, 2002, 63, 63, 77
            // offers duplicated from business: 2000, 2001, 2002
            // and beruId 465852, 465852, 465852 for real_suppliers (77, 2002 and duplicated for 2002)
            .containsExactlyInAnyOrder(2001, 2002, 63, 63, 77,
                2000, 2001, 2002,
                465852, 465852, 465852);

        Assertions.assertThat(response.getProviderProductInfoList())
            .extracting(MboMappings.ProviderProductInfo::getShopSkuId)
            .containsExactlyInAnyOrder("bizsku0", "bizsku0", "bizsku0", "bizsku1", "bizsku2", "sku12", "sku13",
                "sku14", "000042.sku14", "BIZ_REAL.bizsku0", "BIZ_REAL.bizsku2");
    }


    @Test
    public void testFindForContentLabSimple() {
        SearchMappingsResponse response = service.searchMappingsForContentLab(
            MboMappings.SearchMappingsForContentLab.newBuilder().build()
        );

        Assertions.assertThat(response.getOffersList())
            .extracting(SupplierOffer.Offer::getInternalOfferId)
            .containsExactlyInAnyOrder(17L, 18L, 19L);
    }

    @Test
    public void testFindForContentLabHasState() {
        SearchMappingsResponse response = service.searchMappingsForContentLab(
            MboMappings.SearchMappingsForContentLab.newBuilder()
                .addHasState(SupplierOffer.ContentLabState.CL_PLANNED)
                .addHasState(SupplierOffer.ContentLabState.CL_CONTENT)
                .build()
        );

        Assertions.assertThat(response.getOffersList())
            .extracting(SupplierOffer.Offer::getInternalOfferId)
            .containsExactlyInAnyOrder(17L, 19L);
    }

    @Test
    public void testFindForContentLabHasNoState() {
        SearchMappingsResponse response = service.searchMappingsForContentLab(
            MboMappings.SearchMappingsForContentLab.newBuilder()
                .addHasNoState(SupplierOffer.ContentLabState.CL_PLANNED)
                .build()
        );

        Assertions.assertThat(response.getOffersList())
            .extracting(SupplierOffer.Offer::getInternalOfferId)
            .containsExactlyInAnyOrder(18L, 19L);
    }

    @Test
    public void testFindByYtStampBeru() {
        MboMappings.SearchProductInfoByYtStampResponse response = service.searchProductInfoByYtStamp(
            MboMappings.SearchProductInfoByYtStampRequest.newBuilder()
                .setFromStamp(14L)
                .setCount(100)
                .build()
        );

        Assertions.assertThat(response.getProviderProductInfoList())
            .hasSize(2)
            .extracting(p -> new ShopSkuKey(p.getShopId(), p.getShopSkuId()))
            .containsExactly(
                new ShopSkuKey(77, "sku14"),
                new ShopSkuKey(BERU_ID, "000042.sku14"));
    }

    @Test
    public void testFindByYtStampPage() {
        MboMappings.SearchProductInfoByYtStampResponse response = service.searchProductInfoByYtStamp(
            MboMappings.SearchProductInfoByYtStampRequest.newBuilder()
                .setFromStamp(12L)
                .setCount(2)
                .build()
        );

        Assertions.assertThat(response.getProviderProductInfoList())
            .hasSize(2)
            .extracting(MboMappings.ProviderProductInfo::getUploadToYtStamp)
            .containsExactly(12L, 13L);
    }

    @Test
    public void testFindByYtStampGap() {
        MboMappings.SearchProductInfoByYtStampResponse response = service.searchProductInfoByYtStamp(
            MboMappings.SearchProductInfoByYtStampRequest.newBuilder()
                .setFromStamp(14L)
                .setCount(1000)
                .build()
        );

        Assertions.assertThat(response.getProviderProductInfoList())
            .hasSize(2)
            .extracting(MboMappings.ProviderProductInfo::getUploadToYtStamp)
            .allMatch(l -> l >= 14L);
    }

    @Test
    public void testShouldReturnApprovedMappingsEvenNotUploadedToYtButWithStamp() {
        offerRepository.deleteAllInTest();
        insertOfferKeepStamp(OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(303030L), Offer.MappingConfidence.CONTENT)
            .setUploadToYtStamp(1L));

        MboMappings.SearchProductInfoByYtStampResponse response = service.searchProductInfoByYtStamp(
            MboMappings.SearchProductInfoByYtStampRequest.newBuilder()
                .setFromStamp(0)
                .setCount(1000)
                .build()
        );

        Assertions.assertThat(response.getProviderProductInfoList())
            .hasSize(1)
            .extracting(MboMappings.ProviderProductInfo::getShopSkuId)
            .containsExactly(OfferTestUtils.DEFAULT_SHOP_SKU);
    }

    @Test
    public void testFindByShopSku1P() {
        SearchMappingsResponse response = service.searchMappingsByKeys(SearchMappingsByKeysRequest.newBuilder()
            .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                .setSupplierId(BERU_ID)
                .setShopSku("000042.sku5") // Composed
                .build())
            .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                .setSupplierId(BERU_ID)
                .setShopSku("TEST-WROMG.sku5")
                .build())
            .build());

        assertEquals(1, response.getOffersCount());

        SupplierOffer.Offer offer = response.getOffers(0);
        assertEquals(5, offer.getInternalOfferId());
        assertEquals("pokupki.market.yandex.ru", offer.getSupplierName());
        assertEquals(SupplierOffer.SupplierType.TYPE_FIRST_PARTY, offer.getSupplierType());
        assertEquals("MCPTEST-10", offer.getProcessingTicket());
        assertTrue(offer.hasCategoryRestriction());
        assertTrue(offer.getCategoryRestriction().hasType());
    }

    @Test
    public void testFindByMarketSkuMasterData() {
        SearchMappingsResponse response = service.searchMappingsByMarketSkuId(
            SearchMappingsByMarketSkuIdRequest.newBuilder()
                .setReturnMasterData(true)
                .addMarketSkuId(171717L)
                .addMappingKind(MboMappings.MappingKind.APPROVED_MAPPING)
                .build());

        assertEquals(1, response.getOffersCount());
        Assertions.assertThat(response.getOffers(0).getMasterDataInfo()
            .getProviderProductMasterData()
            .getCustomsCommodityCode()
        ).isEqualTo("cccode4");
    }

    @Test
    public void testFindByMarketSkuMasterDataDeletedMappingAdded() {
        SearchMappingsResponse response = service.searchMappingsByMarketSkuId(
            SearchMappingsByMarketSkuIdRequest.newBuilder()
                .setReturnMasterData(true)
                .addMarketSkuId(171717L)
                .addMappingKind(MboMappings.MappingKind.APPROVED_MAPPING)
                .build());

        assertEquals(1, response.getOffersCount());
        Assertions.assertThat(response.getOffers(0).hasDeletedMapping()).isTrue();
        Assertions.assertThat(response.getOffers(0).getDeletedMapping().getSkuType())
            .isEqualTo(SupplierOffer.SkuType.TYPE_MARKET);
    }

    @Test
    public void testFindByMarketSkuNoMasterData() {
        SearchMappingsResponse response = service.searchMappingsByMarketSkuId(
            SearchMappingsByMarketSkuIdRequest.newBuilder()
                .addMarketSkuId(171717L)
                .addMappingKind(MboMappings.MappingKind.APPROVED_MAPPING)
                .build());

        assertEquals(1, response.getOffersCount());
        Assertions.assertThat(response.getOffers(0).hasMasterDataInfo()).isFalse();
    }

    @Test
    public void testFindMappingsByKeysMasterData() {
        SearchMappingsResponse response = service.searchMappingsByKeys(SearchMappingsByKeysRequest.newBuilder()
            .setReturnMasterData(true)
            .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                .setSupplierId(77)
                .setShopSku("sku5")
                .build())
            .build());

        assertEquals(1, response.getOffersCount());
        Assertions.assertThat(response.getOffers(0).getMasterDataInfo()
            .getProviderProductMasterData()
            .getCustomsCommodityCode()
        ).isEqualTo("cccode5");
    }

    @Test
    public void testFindMappingsByKeysNoMasterData() {
        SearchMappingsResponse response = service.searchMappingsByKeys(SearchMappingsByKeysRequest.newBuilder()
            .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                .setSupplierId(77)
                .setShopSku("sku5")
                .build())
            .build());

        assertEquals(1, response.getOffersCount());
        Assertions.assertThat(response.getOffers(0).hasMasterDataInfo()).isFalse();
    }

    @Test
    public void testSearchMappingsByKeysSkipIncorrectKeys() {
        SearchMappingsResponse response = service.searchMappingsByKeys(SearchMappingsByKeysRequest.newBuilder()
            .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                .setSupplierId(BERU_ID)
                .setShopSku("000042.sku5")
                .build())
            .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                .setSupplierId(BERU_ID)
                .setShopSku("incorrect_shop_sku")
                .build())
            .build());

        Assertions.assertThat(response.getOffersList())
            .extracting(
                o -> o.getSupplierId(),
                o -> o.getShopSkuId()
            )
            .containsExactlyInAnyOrder(
                Assertions.tuple((long) BERU_ID, "000042.sku5")
            );
    }

    @Test
    public void testFindByKeys1PandReal() {
        SearchMappingsResponse response = service.searchMappingsByKeys(SearchMappingsByKeysRequest.newBuilder()
            .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                .setSupplierId(BERU_ID)
                .setShopSku("000042.sku5") // Composed
                .build())
            .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                .setSupplierId(77)
                .setShopSku("sku5") // Same but for real supplier
                .build())
            .build());

        Map<Integer, List<SupplierOffer.Offer>> map = response.getOffersList().stream()
            .collect(Collectors.groupingBy(offer -> (int) offer.getSupplierId()));
        Assertions.assertThat(map).hasSize(2);

        List<SupplierOffer.Offer> offers = map.get(BERU_ID);
        assertEquals(1, offers.size());
        assertEquals(5, offers.get(0).getInternalOfferId());
        assertEquals("pokupki.market.yandex.ru", offers.get(0).getSupplierName());
        assertEquals(SupplierOffer.SupplierType.TYPE_FIRST_PARTY, offers.get(0).getSupplierType());
        assertEquals("MCPTEST-10", offers.get(0).getProcessingTicket());

        offers = map.get(77);
        assertEquals(1, offers.size());
        assertEquals(5, offers.get(0).getInternalOfferId());
        assertEquals("Test supplier 77", offers.get(0).getSupplierName());
        assertEquals(SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER, offers.get(0).getSupplierType());
        assertEquals("MCPTEST-10", offers.get(0).getProcessingTicket());
    }

    @Test
    public void testFindByKeysRealOnly() {
        SearchMappingsResponse response = service.searchMappingsByKeys(SearchMappingsByKeysRequest.newBuilder()
            .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                .setSupplierId(77)
                .setShopSku("sku5") // Real supplier only
                .build())
            .build());

        assertEquals(1, response.getOffersCount());

        SupplierOffer.Offer offer = response.getOffers(0);
        assertEquals(5, offer.getInternalOfferId());
        assertEquals("Test supplier 77", offer.getSupplierName());
        assertEquals(SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER, offer.getSupplierType());
        assertEquals("MCPTEST-10", offer.getProcessingTicket());
    }

    @Test
    public void testFindByBusinessKeys() {
        SearchMappingsResponse response = service.searchMappingsByBusinessKeys(
            MboMappings.SearchMappingsByBusinessKeysRequest.newBuilder()
                .addKeys(MbocCommon.BusinessSkuKey.newBuilder()
                    .setBusinessId(77)
                    .setOfferId("sku5") // Real supplier only
                    .build())
                .build());

        assertEquals(1, response.getOffersCount());

        SupplierOffer.Offer offer = response.getOffers(0);
        assertEquals(5, offer.getInternalOfferId());
        assertEquals("Test supplier 77", offer.getSupplierName());
        assertEquals(SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER, offer.getSupplierType());
        assertEquals("MCPTEST-10", offer.getProcessingTicket());
    }

    @Test
    public void testSearchMappingsByKeysShouldFindOnLinkedBusiness() {
        supplierRepository.insertBatch(
            new Supplier(2000, "biz").setType(MbocSupplierType.BUSINESS),
            new Supplier(2001, "biz-child1").setBusinessId(2000),
            new Supplier(2002, "biz-child2").setBusinessId(2000)
                .setType(MbocSupplierType.REAL_SUPPLIER).setRealSupplierId("BIZ_REAL")
        );

        offerRepository.insertOffers(
            offer(1001, 2000, "bizsku0").setServiceOffers(List.of(
                new Offer.ServiceOffer(2001, MbocSupplierType.THIRD_PARTY, Offer.AcceptanceStatus.OK),
                new Offer.ServiceOffer(2002, MbocSupplierType.THIRD_PARTY, Offer.AcceptanceStatus.OK)
            ))
        );

        SearchMappingsResponse response = service.searchMappingsByKeys(SearchMappingsByKeysRequest.newBuilder()
            .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                .setSupplierId(2001)
                .setShopSku("bizsku0")
                .build())
            .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                .setSupplierId(BERU_ID)
                .setShopSku("BIZ_REAL.bizsku0")
                .build())
            .build());

        Map<Integer, List<SupplierOffer.Offer>> map = response.getOffersList().stream()
            .collect(Collectors.groupingBy(offer -> (int) offer.getSupplierId()));
        Assertions.assertThat(map).hasSize(2);

        List<SupplierOffer.Offer> offers = map.get(2001);
        assertEquals(1, offers.size());
        assertEquals(1001, offers.get(0).getInternalOfferId());
        assertEquals("biz-child1", offers.get(0).getSupplierName());
        assertEquals("bizsku0", offers.get(0).getShopSkuId());
        assertEquals(SupplierOffer.SupplierType.TYPE_THIRD_PARTY, offers.get(0).getSupplierType());

        offers = map.get(BERU_ID);
        assertEquals(1, offers.size());
        assertEquals(1001, offers.get(0).getInternalOfferId());
        assertEquals("pokupki.market.yandex.ru", offers.get(0).getSupplierName());
        assertEquals("BIZ_REAL.bizsku0", offers.get(0).getShopSkuId());
        assertEquals(SupplierOffer.SupplierType.TYPE_FIRST_PARTY, offers.get(0).getSupplierType());
    }

    @Test
    public void testSearchMappingsByKeysShouldFindOnBusiness() {
        supplierRepository.insertBatch(
            new Supplier(2000, "biz").setType(MbocSupplierType.BUSINESS)
        );

        offerRepository.insertOffers(
            offer(1001, 2000, "bizsku0")
        );

        SearchMappingsResponse response = service.searchMappingsByKeys(SearchMappingsByKeysRequest.newBuilder()
            .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                .setSupplierId(2000)
                .setShopSku("bizsku0")
                .build())
            .build());

        Map<Integer, List<SupplierOffer.Offer>> map = response.getOffersList().stream()
            .collect(Collectors.groupingBy(offer -> (int) offer.getSupplierId()));
        Assertions.assertThat(map).hasSize(1);

        List<SupplierOffer.Offer> offers = map.get(2000);
        assertEquals(1, offers.size());
        assertEquals(1001, offers.get(0).getInternalOfferId());
        assertEquals("biz", offers.get(0).getSupplierName());
        assertEquals("bizsku0", offers.get(0).getShopSkuId());
        assertEquals(SupplierOffer.SupplierType.TYPE_BUSINESS, offers.get(0).getSupplierType());
    }

    @Test
    public void testSearchMappingsByKeysWithBusinessHardModeExternalKeys() {
        supplierRepository.insertBatch(
            new Supplier(2000, "biz").setType(MbocSupplierType.BUSINESS),
            new Supplier(2001, "biz-child1").setBusinessId(2000),
            new Supplier(2002, "biz-child2").setBusinessId(2000)
                .setType(MbocSupplierType.REAL_SUPPLIER).setRealSupplierId("BIZ_REAL"),
            new Supplier(2003, "biz-child3").setBusinessId(2000)
        );

        offerRepository.insertOffers(
            offer(1001, 2000, "bizsku0").setServiceOffers(List.of(
                new Offer.ServiceOffer(2001, MbocSupplierType.THIRD_PARTY, Offer.AcceptanceStatus.OK),
                new Offer.ServiceOffer(2002, MbocSupplierType.THIRD_PARTY, Offer.AcceptanceStatus.OK),
                new Offer.ServiceOffer(2003, MbocSupplierType.THIRD_PARTY, Offer.AcceptanceStatus.OK)
            )),
            offer(1002, 2000, "bizsku1").setServiceOffers(List.of(
                new Offer.ServiceOffer(2003, MbocSupplierType.THIRD_PARTY, Offer.AcceptanceStatus.OK)
            ))
        );

        SearchMappingsResponse response = service.searchMappingsByKeys(SearchMappingsByKeysRequest.newBuilder()
            .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                .setSupplierId(2001)
                .setShopSku("bizsku0")
                .build())
            .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                .setSupplierId(BERU_ID)
                .setShopSku("BIZ_REAL.bizsku0")
                .build())
            .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                .setSupplierId(2002)
                .setShopSku("bizsku0")
                .build())
            .build());

        Map<Integer, List<SupplierOffer.Offer>> map = response.getOffersList().stream()
            .collect(Collectors.groupingBy(offer -> (int) offer.getSupplierId()));
        Assertions.assertThat(map).hasSize(3);

        List<SupplierOffer.Offer> offers = map.get(2001);
        assertEquals(1, offers.size());
        assertEquals(1001, offers.get(0).getInternalOfferId());
        assertEquals("biz-child1", offers.get(0).getSupplierName());
        assertEquals("bizsku0", offers.get(0).getShopSkuId());
        assertEquals(SupplierOffer.SupplierType.TYPE_THIRD_PARTY, offers.get(0).getSupplierType());

        offers = map.get(BERU_ID);
        assertEquals(1, offers.size());
        assertEquals(1001, offers.get(0).getInternalOfferId());
        assertEquals("pokupki.market.yandex.ru", offers.get(0).getSupplierName());
        assertEquals("BIZ_REAL.bizsku0", offers.get(0).getShopSkuId());
        assertEquals(SupplierOffer.SupplierType.TYPE_FIRST_PARTY, offers.get(0).getSupplierType());

        offers = map.get(2002);
        assertEquals(1, offers.size());
        assertEquals(1001, offers.get(0).getInternalOfferId());
        assertEquals("biz-child2", offers.get(0).getSupplierName());
        assertEquals("bizsku0", offers.get(0).getShopSkuId());
        assertEquals(SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER, offers.get(0).getSupplierType());
    }

    @Test
    public void testSearchMappingsByKeysWithBusinessFilterByServiceOffer() {
        supplierRepository.insertBatch(
            new Supplier(2000, "biz").setType(MbocSupplierType.BUSINESS),
            new Supplier(2001, "biz-child1").setBusinessId(2000),
            new Supplier(2002, "biz-child2").setBusinessId(2000)
        );

        offerRepository.insertOffers(
            offer(1000, 2000, "bizsku0").setServiceOffers(List.of(
                new Offer.ServiceOffer(2002, MbocSupplierType.THIRD_PARTY, Offer.AcceptanceStatus.OK)
            )),
            offer(1001, 2000, "bizsku1").setServiceOffers(List.of(
                new Offer.ServiceOffer(2001, MbocSupplierType.THIRD_PARTY, Offer.AcceptanceStatus.OK)
            ))
        );

        SearchMappingsResponse response = service.searchMappingsByKeys(SearchMappingsByKeysRequest.newBuilder()
            .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                .setSupplierId(2001)
                .setShopSku("bizsku0")
                .build())
            .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                .setSupplierId(2002)
                .setShopSku("bizsku0")
                .build())
            .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                .setSupplierId(2001)
                .setShopSku("bizsku1")
                .build())
            .build());

        Map<Integer, List<SupplierOffer.Offer>> map = response.getOffersList().stream()
            .collect(Collectors.groupingBy(offer -> (int) offer.getSupplierId()));
        Assertions.assertThat(map).hasSize(2);

        List<SupplierOffer.Offer> offers = map.get(2001);
        assertEquals(1, offers.size());
        assertEquals(1001, offers.get(0).getInternalOfferId());
        assertEquals("biz-child1", offers.get(0).getSupplierName());
        assertEquals("bizsku1", offers.get(0).getShopSkuId());
        assertEquals(SupplierOffer.SupplierType.TYPE_THIRD_PARTY, offers.get(0).getSupplierType());

        offers = map.get(2002);
        assertEquals(1, offers.size());
        assertEquals(1000, offers.get(0).getInternalOfferId());
        assertEquals("biz-child2", offers.get(0).getSupplierName());
        assertEquals("bizsku0", offers.get(0).getShopSkuId());
        assertEquals(SupplierOffer.SupplierType.TYPE_THIRD_PARTY, offers.get(0).getSupplierType());
    }

    @Test
    public void testFindByMarketSku() {
        SearchMappingsResponse response = service.searchMappingsByMarketSkuId(
            SearchMappingsByMarketSkuIdRequest.newBuilder()
                .addMarketSkuId(101010L)
                .addMappingKind(MboMappings.MappingKind.APPROVED_MAPPING)
                .build());

        assertEquals(1, response.getOffersCount());
        assertEquals(4, response.getOffers(0).getInternalOfferId());

        response = service.searchMappingsByMarketSkuId(
            SearchMappingsByMarketSkuIdRequest.newBuilder()
                .addMarketSkuId(101010L)
                .addMappingKind(MboMappings.MappingKind.SUPPLIER_MAPPING)
                .build());

        List<SupplierOffer.Offer> offers = response.getOffersList().stream()
            .sorted(Comparator.comparing(SupplierOffer.Offer::getInternalOfferId)
                .thenComparing(SupplierOffer.Offer::getSupplierType))
            .collect(Collectors.toList());

        assertEquals(3, offers.size());
        assertEquals(1, offers.get(0).getInternalOfferId());
        assertEquals(SupplierOffer.SupplierType.TYPE_THIRD_PARTY, offers.get(0).getSupplierType());

        // Дублируется от имени pokupki.market.yandex.ru
        assertEquals(5, offers.get(1).getInternalOfferId());
        assertEquals(SupplierOffer.SupplierType.TYPE_FIRST_PARTY, offers.get(1).getSupplierType());

        assertEquals(5, offers.get(2).getInternalOfferId());
        assertEquals(SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER, offers.get(2).getSupplierType());
    }

    @Test
    public void testBaseOffersFindByMarketSku() {
        SearchMappingsResponse response = service.searchBaseOfferMappingsByMarketSkuId(
            MboMappings.SearchBaseOfferMappingsByMarketSkuIdRequest.newBuilder()
                .addMarketSkuId(101010L)
                .addMappingKind(MboMappings.MappingKind.APPROVED_MAPPING)
                .build());

        assertEquals(1, response.getOffersCount());
        assertEquals(4, response.getOffers(0).getInternalOfferId());

        supplierRepository.insertBatch(
            new Supplier(2000, "biz").setType(MbocSupplierType.BUSINESS),
            new Supplier(2001, "biz-child1").setBusinessId(2000),
            new Supplier(2002, "biz-child2").setBusinessId(2000)
        );

        var mappingId = 12345L;
        offerRepository.insertOffers(
            offer(1000, 2000, "bizsku0")
                .setApprovedSkuMappingInternal(
                    new Offer.Mapping(mappingId, LocalDateTime.now(), MARKET))
                .setServiceOffers(List.of(
                    new Offer.ServiceOffer(2002, MbocSupplierType.THIRD_PARTY, Offer.AcceptanceStatus.OK)
                )),
            offer(1001, 2000, "bizsku1")
                .setApprovedSkuMappingInternal(
                    new Offer.Mapping(mappingId, LocalDateTime.now(), MARKET))
                .setServiceOffers(List.of(
                    new Offer.ServiceOffer(2001, MbocSupplierType.THIRD_PARTY, Offer.AcceptanceStatus.OK)
                )),
            offer(1002, 2000, "bizsku2")
                .setServiceOffers(List.of(
                    new Offer.ServiceOffer(2001, MbocSupplierType.THIRD_PARTY, Offer.AcceptanceStatus.OK)
                ))
        );

        response = service.searchBaseOfferMappingsByMarketSkuId(
            MboMappings.SearchBaseOfferMappingsByMarketSkuIdRequest.newBuilder()
                .addMarketSkuId(mappingId)
                .addMappingKind(MboMappings.MappingKind.APPROVED_MAPPING)
                .build());

        Assertions.assertThat(response.getOffersList())
            .extracting(SupplierOffer.Offer::getInternalOfferId).containsExactlyInAnyOrder(1000L, 1001L);

        Assertions.assertThat(response.getOffersList())
            .extracting(SupplierOffer.Offer::getSupplierId).containsExactly(2000L, 2000L);
    }

    @Test
    public void searchBaseDsbsOfferSuggestsWithoutApprovedByMarketSkuId() {
        SearchMappingsResponse response = service.searchBaseOfferMappingsByMarketSkuId(
            MboMappings.SearchBaseOfferMappingsByMarketSkuIdRequest.newBuilder()
                .addMappingKind(MboMappings.MappingKind.SUGGEST_MAPPING)
                .addMarketSkuId(101011L)
                .addMappingDestination(MboMappings.MboMappingDestination.DSBS)
                .addNotHasMappingKind(MboMappings.MappingKind.APPROVED_MAPPING)
                .build());

        Assertions.assertThat(response.getOffersList())
            .extracting(SupplierOffer.Offer::getInternalOfferId).containsExactlyInAnyOrder(39L);
    }

    @Test
    public void testFindByMarketSkuFiltered() {
        SearchMappingsResponse response = service.searchMappingsByMarketSkuId(
            SearchMappingsByMarketSkuIdRequest.newBuilder()
                .addMarketSkuId(101010L)
                .addMappingKind(MboMappings.MappingKind.SUPPLIER_MAPPING)
                .addSupplierTypes(SupplierOffer.SupplierType.TYPE_THIRD_PARTY)
                .addSupplierTypes(SupplierOffer.SupplierType.TYPE_FIRST_PARTY)
                .build());


        List<SupplierOffer.Offer> offers = response.getOffersList().stream()
            .sorted(Comparator.comparing(SupplierOffer.Offer::getInternalOfferId)
                .thenComparing(SupplierOffer.Offer::getSupplierType))
            .collect(Collectors.toList());

        assertEquals(2, offers.size());
        assertEquals(1, offers.get(0).getInternalOfferId());
        assertEquals(SupplierOffer.SupplierType.TYPE_THIRD_PARTY, offers.get(0).getSupplierType());

        // Дублируется от имени pokupki.market.yandex.ru
        assertEquals(5, offers.get(1).getInternalOfferId());
        assertEquals(SupplierOffer.SupplierType.TYPE_FIRST_PARTY, offers.get(1).getSupplierType());
    }

    @Test
    public void testFindFmcgByMarketSkuFiltered() {
        SearchMappingsResponse response = service.searchMappingsByMarketSkuId(
            SearchMappingsByMarketSkuIdRequest.newBuilder()
                .addMarketSkuId(101011L)
                .addMappingKind(MboMappings.MappingKind.APPROVED_MAPPING)
                .addSupplierTypes(SupplierOffer.SupplierType.TYPE_FMCG)
                .build());
        Assertions.assertThat(response.getOffersList())
            .extracting(SupplierOffer.Offer::getInternalOfferId)
            .containsExactlyInAnyOrder(20L, 21L);

        response = service.searchMappingsByMarketSkuId(
            SearchMappingsByMarketSkuIdRequest.newBuilder()
                .addMarketSkuId(101011L)
                .addMappingKind(MboMappings.MappingKind.SUGGEST_MAPPING)
                .addSupplierTypes(SupplierOffer.SupplierType.TYPE_FMCG)
                .build());
        Assertions.assertThat(response.getOffersList())
            .extracting(SupplierOffer.Offer::getInternalOfferId)
            .containsExactlyInAnyOrder(22L, 24L);

        response = service.searchMappingsByMarketSkuId(
            SearchMappingsByMarketSkuIdRequest.newBuilder()
                .addMarketSkuId(101011L)
                .addMappingKind(MboMappings.MappingKind.SUPPLIER_MAPPING)
                .addSupplierTypes(SupplierOffer.SupplierType.TYPE_FMCG)
                .build());
        assertEquals(1, response.getOffersCount());
    }

    @Test
    public void testFindFmcgAndOtherByMarketSkuFiltered() {
        SearchMappingsResponse response = service.searchMappingsByMarketSkuId(
            SearchMappingsByMarketSkuIdRequest.newBuilder()
                .addMarketSkuId(101011L)
                .addMappingKind(MboMappings.MappingKind.APPROVED_MAPPING)
                .build());
        assertEquals(2, response.getOffersCount());

        response = service.searchMappingsByMarketSkuId(
            SearchMappingsByMarketSkuIdRequest.newBuilder()
                .addMarketSkuId(101011L)
                .addMappingKind(MboMappings.MappingKind.SUGGEST_MAPPING)
                .build());
        assertEquals(3, response.getOffersCount());

        response = service.searchMappingsByMarketSkuId(
            SearchMappingsByMarketSkuIdRequest.newBuilder()
                .addMarketSkuId(101011L)
                .addMappingKind(MboMappings.MappingKind.SUPPLIER_MAPPING)
                .build());
        assertEquals(1, response.getOffersCount());
    }

    @Test
    public void testSearchFmcgOffersWithApprovedMapping() {
        // if 'good' fmcg offer (with suggested mapping) also has real approved mapping,
        // it should be returned only for SKU ID of real approved mapping

        // offer 25 from app-search-test.yml is counted as approved only for 101013, not for 101012

        SearchMappingsResponse response = service.searchMappingsByMarketSkuId(
            SearchMappingsByMarketSkuIdRequest.newBuilder()
                .addMarketSkuId(101012L)
                .addMappingKind(MboMappings.MappingKind.APPROVED_MAPPING)
                .build());
        assertEquals(0, response.getOffersCount());

        response = service.searchMappingsByMarketSkuId(
            SearchMappingsByMarketSkuIdRequest.newBuilder()
                .addMarketSkuId(101013L)
                .addMappingKind(MboMappings.MappingKind.APPROVED_MAPPING)
                .build());
        assertEquals(1, response.getOffersCount());

        response = service.searchMappingsByMarketSkuId(
            SearchMappingsByMarketSkuIdRequest.newBuilder()
                .addMarketSkuId(101012L)
                .addMappingKind(MboMappings.MappingKind.SUGGEST_MAPPING)
                .build());
        assertEquals(0, response.getOffersCount());

        response = service.searchMappingsByMarketSkuId(
            SearchMappingsByMarketSkuIdRequest.newBuilder()
                .addMarketSkuId(101013L)
                .addMappingKind(MboMappings.MappingKind.SUGGEST_MAPPING)
                .build());
        assertEquals(0, response.getOffersCount());
    }

    @Test
    public void searchMappingsShouldCreateCorrectOffersFilterCall() {
        ArgumentCaptor<OffersForServiceFilter> captor = ArgumentCaptor.forClass(OffersForServiceFilter.class);
        masterSpy = Mockito.mock(OfferRepository.class);
        Mockito.when(masterSpy.findOffersForService(captor.capture(), Mockito.anyBoolean()))
            .thenReturn(Collections.emptyList());
        setupService();

        service.searchMappingsByMarketSkuId(
            SearchMappingsByMarketSkuIdRequest.newBuilder()
                .addMarketSkuId(1L)
                .addMappingKind(MboMappings.MappingKind.APPROVED_MAPPING)
                .addSupplierTypes(SupplierOffer.SupplierType.TYPE_THIRD_PARTY)
                .build());
        OffersFilter.MarketSkuSearch marketSkuSearch = captor.getValue().getOffersFilter().getMarketSkuSearch();
        Assertions.assertThat(marketSkuSearch.getMarketSkuIds()).containsExactlyInAnyOrder(1L);
        Assertions.assertThat(marketSkuSearch.isSearchApproved()).isTrue();
        Assertions.assertThat(marketSkuSearch.isSearchSupplier()).isFalse();
        Assertions.assertThat(marketSkuSearch.isSearchSuggest()).isFalse();

        service.searchMappingsByMarketSkuId(
            SearchMappingsByMarketSkuIdRequest.newBuilder()
                .addMarketSkuId(1L)
                .addMappingKind(MboMappings.MappingKind.APPROVED_MAPPING)
                .build());
        List<Criteria<Offer>> offerCriteriaList = captor.getValue().getOffersFilter().getCriterias();
        Assertions.assertThat(offerCriteriaList).hasAtLeastOneElementOfType(NotCriteria.class);
        Assertions.assertThat(offerCriteriaList).hasAtLeastOneElementOfType(OrCriteria.class);

        service.searchMappingsByMarketSkuId(
            SearchMappingsByMarketSkuIdRequest.newBuilder()
                .addMarketSkuId(2L)
                .addMappingKind(MboMappings.MappingKind.SUPPLIER_MAPPING)
                .addSupplierTypes(SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER)
                .build());
        marketSkuSearch = captor.getValue().getOffersFilter().getMarketSkuSearch();
        Assertions.assertThat(marketSkuSearch.getMarketSkuIds()).containsExactlyInAnyOrder(2L);
        Assertions.assertThat(marketSkuSearch.isSearchApproved()).isFalse();
        Assertions.assertThat(marketSkuSearch.isSearchSupplier()).isTrue();
        Assertions.assertThat(marketSkuSearch.isSearchSuggest()).isFalse();


        service.searchMappingsByMarketSkuId(
            SearchMappingsByMarketSkuIdRequest.newBuilder()
                .addMarketSkuId(2L)
                .addMappingKind(MboMappings.MappingKind.SUPPLIER_MAPPING)
                .build());
        offerCriteriaList = captor.getValue().getOffersFilter().getCriterias();
        Assertions.assertThat(offerCriteriaList).hasAtLeastOneElementOfType(NotCriteria.class);
        Assertions.assertThat(offerCriteriaList).hasAtLeastOneElementOfType(OrCriteria.class);

        service.searchMappingsByMarketSkuId(
            SearchMappingsByMarketSkuIdRequest.newBuilder()
                .addMarketSkuId(3L)
                .addMappingKind(MboMappings.MappingKind.SUGGEST_MAPPING)
                .addSupplierTypes(SupplierOffer.SupplierType.TYPE_THIRD_PARTY)
                .build());
        marketSkuSearch = captor.getValue().getOffersFilter().getMarketSkuSearch();
        Assertions.assertThat(marketSkuSearch.getMarketSkuIds()).containsExactlyInAnyOrder(3L);
        Assertions.assertThat(marketSkuSearch.isSearchApproved()).isFalse();
        Assertions.assertThat(marketSkuSearch.isSearchSupplier()).isFalse();
        Assertions.assertThat(marketSkuSearch.isSearchSuggest()).isTrue();

        service.searchMappingsByMarketSkuId(
            SearchMappingsByMarketSkuIdRequest.newBuilder()
                .addMarketSkuId(3L)
                .addMappingKind(MboMappings.MappingKind.SUGGEST_MAPPING)
                .build());
        offerCriteriaList = captor.getValue().getOffersFilter().getCriterias();
        Assertions.assertThat(offerCriteriaList).hasAtLeastOneElementOfType(NotCriteria.class);
        Assertions.assertThat(offerCriteriaList).hasAtLeastOneElementOfType(OrCriteria.class);
    }

    @Test
    public void testSearchMappingsByMarketSkuIdHasMasterData() {
        SearchMappingsResponse response = service.searchMappingsByMarketSkuId(
            SearchMappingsByMarketSkuIdRequest.newBuilder()
                .setReturnMasterData(true)
                .addMappingKind(MboMappings.MappingKind.APPROVED_MAPPING)
                .addMarketSkuId(303030)
                .build()
        );

        Assertions.assertThat(response.getOffersList())
            .extracting(
                o -> o.getSupplierId(),
                o -> o.getShopSkuId(),
                o -> o.getMasterDataInfo().getProviderProductMasterData().getCustomsCommodityCode()
            )
            .containsExactlyInAnyOrder(
                Assertions.tuple((long) BERU_ID, "000042.sku5", "cccode5"),
                Assertions.tuple(77L, "sku5", "cccode5")
            );
    }

    @Test
    public void testSearchMappingsByKeysHasMasterData() {
        SearchMappingsResponse response = service.searchMappingsByKeys(SearchMappingsByKeysRequest.newBuilder()
            .setReturnMasterData(true)
            .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                .setSupplierId(BERU_ID)
                .setShopSku("000042.sku5") // Real supplier only
                .build())
            .build());

        Assertions.assertThat(response.getOffersList())
            .extracting(
                o -> o.getSupplierId(),
                o -> o.getShopSkuId(),
                o -> o.getMasterDataInfo().getProviderProductMasterData().getCustomsCommodityCode()
            )
            .containsExactlyInAnyOrder(
                Assertions.tuple((long) BERU_ID, "000042.sku5", "cccode5")
            );
    }

    @Test
    public void testSearchApprovedMappingsByMarketSkuHasMasterData() {
        MboMappings.SearchApprovedMappingsResponse response = service.searchApprovedMappingsByMarketSkuId(
            MboMappings.SearchApprovedMappingsRequest.newBuilder()
                .addMarketSkuId(303030)
                .build()
        );

        Assertions.assertThat(response.getMappingList())
            .extracting(
                m -> m.getSupplierId(),
                m -> m.getShopSku(),
                m -> m.getMasterDataInfo().getProviderProductMasterData().getCustomsCommodityCode()
            )
            .containsExactlyInAnyOrder(
                Assertions.tuple(BERU_ID, "000042.sku5", "cccode5"),
                Assertions.tuple(77, "sku5", "cccode5")
            );
    }

    @Test
    public void testSearchApprovedMappingsByKeysHasMasterData() {
        MboMappings.SearchApprovedMappingsResponse response = service.searchApprovedMappingsByKeys(
            SearchMappingsByKeysRequest.newBuilder()
                .setReturnMasterData(true)
                .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                    .setSupplierId(BERU_ID)
                    .setShopSku("000042.sku5")
                    .build()
                )
                .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                    .setSupplierId(61)
                    .setShopSku("sku4")
                    .build()
                )
                .build()
        );

        Assertions.assertThat(response.getMappingList())
            .extracting(
                m -> m.getSupplierId(),
                m -> m.getShopSku(),
                m -> m.getMasterDataInfo().getProviderProductMasterData().getCustomsCommodityCode()
            )
            .containsExactlyInAnyOrder(
                Assertions.tuple(BERU_ID, "000042.sku5", "cccode5"),
                Assertions.tuple(61, "sku4", "cccode4")
            );
    }

    @Test
    public void testSearchApprovedMappingsByKeysHasNoMasterData() {
        MboMappings.SearchApprovedMappingsResponse response = service.searchApprovedMappingsByKeys(
            SearchMappingsByKeysRequest.newBuilder()
                .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                    .setSupplierId(BERU_ID)
                    .setShopSku("000042.sku5")
                    .build()
                )
                .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                    .setSupplierId(61)
                    .setShopSku("sku4")
                    .build()
                )
                .build()
        );

        Assertions.assertThat(response.getMappingList())
            .extracting(
                m -> m.getSupplierId(),
                m -> m.getShopSku(),
                m -> m.hasMasterDataInfo()
            )
            .containsExactlyInAnyOrder(
                Assertions.tuple(BERU_ID, "000042.sku5", false),
                Assertions.tuple(61, "sku4", false)
            );
    }

    @Test
    public void testSearchApprovedMappings() {
        MboMappings.SearchApprovedMappingsResponse response = service.searchApprovedMappingsByMarketSkuId(
            MboMappings.SearchApprovedMappingsRequest.newBuilder()
                .addMarketSkuId(101010L)
                .build());

        assertEquals(1, response.getMappingCount());
        MboMappings.ApprovedMappingInfo mapping = response.getMapping(0);

        assertEquals(101010L, mapping.getMarketSkuId());
        assertEquals(60, mapping.getSupplierId());
        assertEquals("sku4", mapping.getShopSku());
        assertEquals(1509185720000L, mapping.getMappingTimestamp());

        // NOTE: Category is from sku
        assertEquals(10, mapping.getMarketCategoryId());
        Assertions.assertThat(mapping.getMskuBarcodeList()).containsExactly("A", "B");
        Assertions.assertThat(mapping.getMskuVendorcodeList()).contains("TEST", "TSET");

        assertEquals(1, mapping.getModelParamCount());
        assertEquals("test", mapping.getModelParam(0).getXslName());

        assertEquals("Напильники для PGaaS!", mapping.getShopTitle());
        assertEquals("Тулзы!", mapping.getShopCategoryName());
        assertEquals("barcode", mapping.getShopBarcode());
        assertEquals("vendorcode", mapping.getShopVendorcode());
        assertEquals("Apple", mapping.getShopVendor());
    }

    @Test
    public void testSearchApprovedMappingsByKeys() {
        MboMappings.SearchApprovedMappingsResponse response = service.searchApprovedMappingsByKeys(
            SearchMappingsByKeysRequest.newBuilder()
                .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                    .setSupplierId(60)
                    .setShopSku("sku4")
                    .build())
                .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                    .setSupplierId(BERU_ID)
                    .setShopSku("000042.sku5")
                    .build())
                .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                    .setSupplierId(BERU_ID)
                    .setShopSku("ABSENT.absent-sku")
                    .build())
                .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                    .setSupplierId(60)
                    .setShopSku("absent-sku")
                    .build())
                .build());

        // absent sku is just not exported
        assertEquals(2, response.getMappingCount());

        // Check what's found
        Assertions.assertThat(response.getMappingList())
            .extracting(MboMappings.ApprovedMappingInfo::getShopSku)
            .containsExactlyInAnyOrder("sku4", "000042.sku5");

        // Conversion is tested above
    }

    @Test
    public void testSearchLiteApprovedMappingsByKeys() {
        MboMappings.SearchLiteMappingsResponse response = service.searchLiteApprovedMappingsByKeys(
            SearchLiteMappingsByKeysRequest.newBuilder()
                .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                    .setSupplierId(60) // 3P
                    .setShopSku("sku4")
                    .build())
                .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                    .setSupplierId(BERU_ID) // 1P
                    .setShopSku("000042.sku5")
                    .build())
                .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                    .setSupplierId(BERU_ID)
                    .setShopSku("ABSENT.absent-sku")
                    .build())
                .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                    .setSupplierId(60)
                    .setShopSku("absent-sku")
                    .build())
                .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                    .setSupplierId(42)
                    .setShopSku("sku3") // no mapping
                    .build())
                .build());

        // absent sku is just not exported
        assertEquals(2, response.getMappingCount());

        // Check what's found
        Assertions.assertThat(response.getMappingList())
            .extracting(MappingInfoLite::getShopSku)
            .containsExactlyInAnyOrder("sku4", "000042.sku5");
        Assertions.assertThat(response.getMappingList())
            .extracting(MappingInfoLite::getSupplierId)
            .containsExactlyInAnyOrder(60, BERU_ID);
    }

    @Test
    public void testSearchLiteApprovedMappingsBySkuId() {
        MboMappings.SearchLiteMappingsResponse response = service.searchLiteApprovedMappingsByMarketSkuId(
            SearchLiteMappingsByMarketSkuIdRequest.newBuilder()
                .addMarketSkuId(101010L)
                .addMarketSkuId(303030L)
                .addMarketSkuId(202020L) // suggested
                .addMarketSkuId(1010101012323232L) // nonexistent
                .build()
        );

        assertEquals(2, response.getMappingCount());

        // Check what's found
        Assertions.assertThat(response.getMappingList())
            .extracting(MappingInfoLite::getShopSku)
            .containsExactlyInAnyOrder("sku4", "000042.sku5");
    }

    @Test
    public void testSearchApprovedMappingsByKeysForKeyWithoutMapping() {
        MboMappings.SearchApprovedMappingsResponse response = service.searchApprovedMappingsByKeys(
            SearchMappingsByKeysRequest.newBuilder()
                .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                    .setSupplierId(42)
                    .setShopSku("sku3")
                    .build())
                .build());

        // absent sku is just not exported
        assertEquals(0, response.getMappingCount());
    }

    @Test
    public void testIncorrectBeruShopSku() {
        MboMappings.SearchApprovedMappingsResponse response = service.searchApprovedMappingsByKeys(
            SearchMappingsByKeysRequest.newBuilder()
                .addKeys(SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                    .setSupplierId(465852)
                    .setShopSku("wrong-sku without dot")
                    .build())
                .build());

        // absent sku is just not exported
        assertEquals(0, response.getMappingCount());
    }

    @Test
    public void testSearchApprovedMappings1P() {
        MboMappings.SearchApprovedMappingsResponse response = service.searchApprovedMappingsByMarketSkuId(
            MboMappings.SearchApprovedMappingsRequest.newBuilder()
                .addMarketSkuId(303030L)
                .build());

        assertEquals(2, response.getMappingCount());
        Assertions.assertThat(response.getMappingList())
            .extracting(MboMappings.ApprovedMappingInfo::getSupplierId)
            .containsExactlyInAnyOrder(77, 465852);
    }

    @Test
    public void testCannotSearchZeroMapping() {
        Assertions.assertThatThrownBy(() -> {
            MboMappings.SearchApprovedMappingsResponse response = service.searchApprovedMappingsByMarketSkuId(
                MboMappings.SearchApprovedMappingsRequest.newBuilder()
                    .addMarketSkuId(0L)
                    .build());
        }).hasMessageContaining("Cannot search 0 mapping");
    }

    @Test
    public void testSearchMappingWithNoInfo() {
        modelStorageCachingService.clear();

        MboMappings.SearchApprovedMappingsResponse response = service.searchApprovedMappingsByMarketSkuId(
            MboMappings.SearchApprovedMappingsRequest.newBuilder()
                .addMarketSkuId(303030L)
                .build());

        // For now treat this as absent result
        assertEquals(0, response.getMappingCount());
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testUpdateOfferProcessingStatusIncorrectRequests() {
        // empty fields
        MboMappings.UpdateOfferProcessingStatusResponse response = service.updateOfferProcessingStatus(
            MboMappings.UpdateOfferProcessingStatusRequest.newBuilder()
                .setOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.NEED_CONTENT)
                .build());
        Assertions.assertThat(response.getStatus())
            .isEqualTo(MboMappings.UpdateOfferProcessingStatusResponse.Status.ERROR);

        response = service.updateOfferProcessingStatus(
            MboMappings.UpdateOfferProcessingStatusRequest.newBuilder()
                .addMappings(MboMappings.UpdateOfferProcessingStatusRequest.Locator.newBuilder().build())
                .build());
        Assertions.assertThat(response.getStatus())
            .isEqualTo(MboMappings.UpdateOfferProcessingStatusResponse.Status.ERROR);
        // incorrect status
        response = service.updateOfferProcessingStatus(
            MboMappings.UpdateOfferProcessingStatusRequest.newBuilder()
                .setOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.SUSPENDED)
                .addMappings(MboMappings.UpdateOfferProcessingStatusRequest.Locator.newBuilder().build())
                .build());
        Assertions.assertThat(response.getStatus())
            .isEqualTo(MboMappings.UpdateOfferProcessingStatusResponse.Status.ERROR);
        // incorrect status in offers
        response = service.updateOfferProcessingStatus(
            MboMappings.UpdateOfferProcessingStatusRequest.newBuilder()
                .setOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.NEED_CONTENT)
                .addMappings(MboMappings.UpdateOfferProcessingStatusRequest.Locator.newBuilder()
                    .setSupplierId(42)
                    .setShopSku("sku3")
                    .build())
                .build());
        Assertions.assertThat(response.getStatus())
            .isEqualTo(MboMappings.UpdateOfferProcessingStatusResponse.Status.ERROR);
        // internal error
        var offerRepository = Mockito.mock(OfferRepositoryMock.class);
        businessSupplierService = Mockito.mock(BusinessSupplierService.class);
        var service = new MboMappingsServiceImpl(
            offerRepository,
            slaveOfferRepository, null,
            protoConverter,
            supplierRepository,
            offer1PService,
            modelStorageCachingService,
            Mockito.mock(ComplexMonitoring.class),
            categoryCachingService,
            null,
            BERU_ID,
            offerStatService,
            new MboMappingsHelperService(supplierRepository, masterDataHelperService,
                businessSupplierService, BERU_ID), businessSupplierService,
            supplierService, storageKeyValueService, null, migrationService,
            antiMappingRepository, antiMappingService,
            offersProcessingStatusService, contentProcessingQueue, transactionHelper, warehouseServiceService,
            dataCampConverterService, offerMappingActionService, new RecheckMappingService(offerRepository,
            offerMappingActionService, offersProcessingStatusService, transactionHelper, antiMappingService));
        Mockito.when(businessSupplierService.findOffersForServiceByShopSkuKeys(Mockito.any(), Mockito.anyCollection()))
            .thenThrow(new RuntimeException("test message"));

        response = service.updateOfferProcessingStatus(
            MboMappings.UpdateOfferProcessingStatusRequest.newBuilder()
                .setOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.NEED_CONTENT)
                .addMappings(MboMappings.UpdateOfferProcessingStatusRequest.Locator.newBuilder()
                    .setSupplierId(61)
                    .setShopSku("sku5")
                    .build())
                .build());
        verify(businessSupplierService, Mockito.times(1))
            .findOffersForServiceByShopSkuKeys(Mockito.any(), anyCollection());
        Assertions.assertThat(response.getStatus())
            .isEqualTo(MboMappings.UpdateOfferProcessingStatusResponse.Status.ERROR);
        Assertions.assertThat(response.getMessage())
            .isEqualTo("RuntimeException: test message");
    }

    @Test
    public void testUpdateOfferProcessingStatusCorrectRequestNoGoodCategory() {
        Category category = new Category().setCategoryId(33).setName("Category #33");
        categoryCachingService.addCategory(category);

        MboMappings.UpdateOfferProcessingStatusResponse response = service.updateOfferProcessingStatus(
            MboMappings.UpdateOfferProcessingStatusRequest.newBuilder()
                .setOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.NEED_CONTENT)
                .addMappings(MboMappings.UpdateOfferProcessingStatusRequest.Locator.newBuilder()
                    .setSupplierId(62)
                    .setShopSku("sku18")
                    .build())
                .build());

        Assertions.assertThat(response.getStatus())
            .isEqualTo(MboMappings.UpdateOfferProcessingStatusResponse.Status.ERROR);
    }

    @Test
    public void testUpdateOfferProcessingStatusCorrectRequestToNeedContent() {
        Category category = new Category()
            .setCategoryId(33).setName("Category #33")
            .setAcceptGoodContent(true);
        categoryCachingService.addCategory(category);

        var offers = offerRepository.findOffersByBusinessSkuKeys(new BusinessSkuKey(62, "sku18"));
        for (var offer : offers) {
            offer.updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .setBindingKind(Offer.BindingKind.APPROVED);
        }
        offerRepository.updateOffers(offers);

        MboMappings.UpdateOfferProcessingStatusResponse response = service.updateOfferProcessingStatus(
            MboMappings.UpdateOfferProcessingStatusRequest.newBuilder()
                .setOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.NEED_CONTENT)
                .addMappings(MboMappings.UpdateOfferProcessingStatusRequest.Locator.newBuilder()
                    .setSupplierId(62)
                    .setShopSku("sku18")
                    .build())
                .build());

        Assertions.assertThat(response.getStatus())
            .isEqualTo(MboMappings.UpdateOfferProcessingStatusResponse.Status.OK);
        offers = offerRepository.findOffers(new OffersFilter().setOfferIds(18L));
        Assertions.assertThat(offers).hasSize(1)
            .allMatch(o -> o.getProcessingStatus() == Offer.ProcessingStatus.NEED_CONTENT);
    }

    @Test
    public void testIncorrectUpdateContentProcessingTasksRequest() {
        // empty list
        MboMappings.UpdateContentProcessingTasksResponse response = service.updateContentProcessingTasks(
            MboMappings.UpdateContentProcessingTasksRequest.newBuilder()
                .build());
        Assertions.assertThat(response.getStatus())
            .isEqualTo(MboMappings.UpdateContentProcessingTasksResponse.Status.ERROR);

        // empty fields
        response = service.updateContentProcessingTasks(
            MboMappings.UpdateContentProcessingTasksRequest.newBuilder()
                .addContentProcessingTask(ContentProcessingTask.newBuilder()
                    .build())
                .build());
        Assertions.assertThat(response.getStatus())
            .isEqualTo(MboMappings.UpdateContentProcessingTasksResponse.Status.ERROR);

        // internal error
        var offerRepository = Mockito.mock(OfferRepositoryMock.class);
        businessSupplierService = Mockito.mock(BusinessSupplierService.class);
        var service = new MboMappingsServiceImpl(
            offerRepository,
            slaveOfferRepository, null,
            protoConverter,
            supplierRepository,
            offer1PService,
            modelStorageCachingService,
            Mockito.mock(ComplexMonitoring.class),
            categoryCachingService,
            null,
            BERU_ID,
            offerStatService,
            new MboMappingsHelperService(supplierRepository, masterDataHelperService,
                businessSupplierService, BERU_ID),
            businessSupplierService,
            supplierService, storageKeyValueService, null, migrationService,
            antiMappingRepository, antiMappingService,
            offersProcessingStatusService, contentProcessingQueue, transactionHelper, warehouseServiceService,
            dataCampConverterService, offerMappingActionService, new RecheckMappingService(offerRepository,
            offerMappingActionService, offersProcessingStatusService, transactionHelper, antiMappingService));
        Mockito.when(businessSupplierService.findOffersForServiceByShopSkuKeys(Mockito.any(), Mockito.anyCollection()))
            .thenThrow(new RuntimeException("test message"));
        response = service.updateContentProcessingTasks(
            MboMappings.UpdateContentProcessingTasksRequest.newBuilder()
                .addContentProcessingTask(ContentProcessingTask.newBuilder()
                    .setShopSku("shop_sku")
                    .setSupplierId(1)
                    .setContentProcessingTaskId(1)
                    .setContentProcessingState(ContentProcessingTask.State.STARTED)
                    .build())
                .build());
        verify(businessSupplierService, Mockito.times(1))
            .findOffersForServiceByShopSkuKeys(Mockito.any(), anyCollection());
        Assertions.assertThat(response.getStatus())
            .isEqualTo(MboMappings.UpdateContentProcessingTasksResponse.Status.ERROR);
        Assertions.assertThat(response.getMessage())
            .isEqualTo("RuntimeException: test message");
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testSkippingUpdateContentProcessingTasks() {
        offerRepository.deleteAllInTest();
        offerRepository.insertOffers(
            OfferTestUtils.simpleOffer()
                .setId(1)
                .setShopSku("not-in-allowed-status")
                .setProcessingStatusInternal(Offer.ProcessingStatus.OPEN),
            OfferTestUtils.simpleOffer()
                .setId(2)
                .setShopSku("SUCCESS-without-approved-mapping")
                .setProcessingStatusInternal(Offer.ProcessingStatus.CONTENT_PROCESSING),
            OfferTestUtils.simpleOffer()
                .setId(3)
                .setShopSku("should-update")
                .setProcessingStatusInternal(Offer.ProcessingStatus.NEED_CONTENT)
        );

        MboMappings.UpdateContentProcessingTasksResponse response = service.updateContentProcessingTasks(
            MboMappings.UpdateContentProcessingTasksRequest.newBuilder()
                .addContentProcessingTask(ContentProcessingTask.newBuilder()
                    .setShopSku("offer-not-found")
                    .setSupplierId(OfferTestUtils.TEST_SUPPLIER_ID)
                    .setContentProcessingTaskId(1)
                    .setContentProcessingState(ContentProcessingTask.State.STARTED)
                    .build())
                .addContentProcessingTask(ContentProcessingTask.newBuilder()
                    .setShopSku("not-in-allowed-status")
                    .setSupplierId(OfferTestUtils.TEST_SUPPLIER_ID)
                    .setContentProcessingTaskId(2)
                    .setContentProcessingState(ContentProcessingTask.State.STARTED)
                    .build())
                .addContentProcessingTask(ContentProcessingTask.newBuilder()
                    .setShopSku("SUCCESS-without-approved-mapping")
                    .setSupplierId(OfferTestUtils.TEST_SUPPLIER_ID)
                    .setContentProcessingTaskId(3)
                    .setContentProcessingState(ContentProcessingTask.State.SUCCESS)
                    .build())
                .addContentProcessingTask(ContentProcessingTask.newBuilder()
                    .setShopSku("should-update")
                    .setSupplierId(OfferTestUtils.TEST_SUPPLIER_ID)
                    .setContentProcessingTaskId(4)
                    .setContentProcessingState(ContentProcessingTask.State.STARTED)
                    .build())
                .build());

        Assertions.assertThat(response.getStatus())
            .isEqualTo(MboMappings.UpdateContentProcessingTasksResponse.Status.OK);
        Assertions.assertThat(response.getMessage())
            .isEqualTo("Updated offers total:1");

        Assertions.assertThat(offerRepository.getOfferById(1).getContentProcessingTaskId()).isNull();
        Assertions.assertThat(offerRepository.getOfferById(2).getContentProcessingTaskId()).isNull();

        Offer updatedOffer = offerRepository.getOfferById(3);
        Assertions.assertThat(updatedOffer.getContentProcessingTaskId()).isEqualTo(4);
        Assertions.assertThat(updatedOffer.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.CONTENT_PROCESSING);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testCorrectUpdateContentProcessingTasksRequest() {
        offerRepository.deleteAllInTest();
        offerRepository.insertOffers(
            OfferTestUtils.simpleOkOffer()
                .setId(1)
                .setShopSku("for-starting")
                .setProcessingStatusInternal(Offer.ProcessingStatus.NEED_CONTENT),
            OfferTestUtils.simpleOkOffer()
                .setId(2)
                .setShopSku("for-success")
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(303030L),
                    Offer.MappingConfidence.PARTNER_SELF)
                .setProcessingStatusInternal(Offer.ProcessingStatus.CONTENT_PROCESSING),
            OfferTestUtils.simpleOkOffer()
                .setId(3)
                .setShopSku("for-error")
                .setBindingKind(Offer.BindingKind.APPROVED)
                .setProcessingStatusInternal(Offer.ProcessingStatus.CONTENT_PROCESSING),
            OfferTestUtils.simpleOkOffer()
                .setId(4)
                .setShopSku("for-starting-in-PROCESSED")
                .setProcessingStatusInternal(Offer.ProcessingStatus.PROCESSED),
            OfferTestUtils.simpleOkOffer()
                .setId(5)
                .setShopSku("for-error-with-approved-mapping")
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(303030L),
                    Offer.MappingConfidence.PARTNER_SELF)
                .setProcessingStatusInternal(Offer.ProcessingStatus.CONTENT_PROCESSING)
        );

        MboMappings.UpdateContentProcessingTasksResponse response = service.updateContentProcessingTasks(
            MboMappings.UpdateContentProcessingTasksRequest.newBuilder()
                .addContentProcessingTask(ContentProcessingTask.newBuilder()
                    .setShopSku("for-starting")
                    .setSupplierId(OfferTestUtils.TEST_SUPPLIER_ID)
                    .setContentProcessingTaskId(1)
                    .setContentProcessingState(ContentProcessingTask.State.STARTED)
                    .build())
                .addContentProcessingTask(ContentProcessingTask.newBuilder()
                    .setShopSku("for-success")
                    .setSupplierId(OfferTestUtils.TEST_SUPPLIER_ID)
                    .setContentProcessingTaskId(2)
                    .setContentProcessingState(ContentProcessingTask.State.SUCCESS)
                    .build())
                .addContentProcessingTask(ContentProcessingTask.newBuilder()
                    .setShopSku("for-error")
                    .setSupplierId(OfferTestUtils.TEST_SUPPLIER_ID)
                    .setContentProcessingTaskId(3)
                    .setContentProcessingState(ContentProcessingTask.State.ERROR)
                    .build())
                .addContentProcessingTask(ContentProcessingTask.newBuilder()
                    .setShopSku("for-starting-in-PROCESSED")
                    .setSupplierId(OfferTestUtils.TEST_SUPPLIER_ID)
                    .setContentProcessingTaskId(4)
                    .setContentProcessingState(ContentProcessingTask.State.STARTED)
                    .build())
                .addContentProcessingTask(ContentProcessingTask.newBuilder()
                    .setShopSku("for-error-with-approved-mapping")
                    .setSupplierId(OfferTestUtils.TEST_SUPPLIER_ID)
                    .setContentProcessingTaskId(5)
                    .setContentProcessingState(ContentProcessingTask.State.ERROR)
                    .build())
                .build());

        Assertions.assertThat(response.getStatus())
            .isEqualTo(MboMappings.UpdateContentProcessingTasksResponse.Status.OK);
        Assertions.assertThat(response.getMessage())
            .isEqualTo("Updated offers total:5");
        // started
        Offer updatedOffer = offerRepository.getOfferById(1);
        Assertions.assertThat(updatedOffer.getContentProcessingTaskId()).isEqualTo(1);
        Assertions.assertThat(updatedOffer.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.CONTENT_PROCESSING);
        // success
        updatedOffer = offerRepository.getOfferById(2);
        Assertions.assertThat(updatedOffer.getContentProcessingTaskId()).isEqualTo(2);
        Assertions.assertThat(updatedOffer.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.AUTO_PROCESSED);
        // error
        updatedOffer = offerRepository.getOfferById(3);
        Assertions.assertThat(updatedOffer.getContentProcessingTaskId()).isEqualTo(3);
        Assertions.assertThat(updatedOffer.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.IN_PROCESS);
        Assertions.assertThat(updatedOffer.getContentComments().get(0).getType())
            .isEqualTo(ContentCommentType.INCORRECT_MODEL);
        // started from processed
        updatedOffer = offerRepository.getOfferById(4);
        Assertions.assertThat(updatedOffer.getContentProcessingTaskId()).isEqualTo(4);
        Assertions.assertThat(updatedOffer.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.CONTENT_PROCESSING);
        // error with approved mapping
        updatedOffer = offerRepository.getOfferById(5);
        Assertions.assertThat(updatedOffer.getContentProcessingTaskId()).isEqualTo(5);
        Assertions.assertThat(updatedOffer.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.AUTO_PROCESSED);
        Assertions.assertThat(updatedOffer.getContentComments().get(0).getType())
            .isEqualTo(ContentCommentType.INCORRECT_MODEL);
    }


    @Test
    public void testUpdateMappingsForcesBlueMappingType() {
        MboMappings.ProductUpdateRequestInfo requestInfo = MboMappings.ProductUpdateRequestInfo.newBuilder()
            .setChangeSource(MboMappings.ProductUpdateRequestInfo.ChangeSource.SUPPLIER)
            .build();
        service.updateMappings(MboMappings.UpdateMappingsRequest.newBuilder()
            .setRequestInfo(requestInfo)
            .addUpdates(MboMappings.UpdateMappingsRequest.MappingUpdate.newBuilder()
                .setMarketSkuId(11L)
                .setShopSku("SOME-SKU")
                .setSupplierId(3)
                .build())
            .build());

        verify(productInfoHelperService, Mockito.times(1))
            .addProductInfo(
                Mockito.eq(MboMappings.ProviderProductInfoRequest.newBuilder()
                    .setRequestInfo(requestInfo)
                    .addProviderProductInfo(MboMappings.ProviderProductInfo.newBuilder()
                        .setMarketSkuId(11L)
                        .setShopSkuId("SOME-SKU")
                        .setShopId(3)
                        .clearMappingType()
                        .build())
                    .build()),
                Mockito.argThat(ctx ->
                    !ctx.isAllowInsert()
                        && !ctx.isEnrichOffers()
                        && ctx.getSaveMode() == ALL_OR_NOTHING
                )
            );
    }

    @Test
    public void testUpdateMappingCallsMethodWithoutEnrichment() {
        service.updateMappings(MboMappings.UpdateMappingsRequest.newBuilder().build());

        verify(productInfoHelperService, times(1))
            .addProductInfo(
                any(MboMappings.ProviderProductInfoRequest.class),
                Mockito.argThat(ctx ->
                    !ctx.isAllowInsert()
                        && !ctx.isEnrichOffers()
                        && ctx.getSaveMode() == ALL_OR_NOTHING
                )
            );
    }

    private void setUpForSearchOfferVendors() {
        final int supplier1 = 1;
        final int supplier2 = 2;
        final int nonexistentSupplier = 55;
        offerRepository.deleteAllInTest();
        offerRepository.insertOffers(
            OfferTestUtils.simpleOffer()
                .setId(1)
                .setVendor("Super vendor")
                .setBusinessId(supplier1)
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(1L),
                    Offer.MappingConfidence.CONTENT)
                .setShopSku("1"),
            OfferTestUtils.simpleOffer()
                .setId(2)
                .setVendor("second vendor")
                .setBusinessId(supplier1)
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(2L),
                    Offer.MappingConfidence.CONTENT)
                .setShopSku("2"),
            OfferTestUtils.simpleOffer()
                .setId(3)
                .setVendor("thirdVendor")
                .setBusinessId(supplier2)
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(3L),
                    Offer.MappingConfidence.CONTENT)
                .setShopSku("3"),
            OfferTestUtils.simpleOffer()
                .setId(4)
                .setVendor("thirdVendor")
                .setBusinessId(supplier2)
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(4L),
                    Offer.MappingConfidence.CONTENT)
                .setShopSku("4"),
            OfferTestUtils.simpleOffer()
                .setId(5)
                .setVendor(null)
                .setBusinessId(supplier2)
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(5L),
                    Offer.MappingConfidence.CONTENT)
                .setShopSku("5")
        );
    }

    @Test
    public void testSearchOfferVendors() {
        final int supplier1 = 1;
        final int supplier2 = 2;
        final int nonexistentSupplier = 55;
        setUpForSearchOfferVendors();


        Mockito.reset(masterSpy, slaveSpy);
        MboMappings.SearchVendorsResponse searchResultSupplier1 = service.searchVendorsByShopId(
            MboMappings.SearchVendorsBySupplierIdRequest.newBuilder().setSupplierId(supplier1).build());
        Assertions.assertThat(searchResultSupplier1.getVendorsList())
            .containsExactly("second vendor", "Super vendor");

        MboMappings.SearchVendorsResponse searchResultSupplier2 = service.searchVendorsByShopId(
            MboMappings.SearchVendorsBySupplierIdRequest.newBuilder().setSupplierId(supplier2).build());
        Assertions.assertThat(searchResultSupplier2.getVendorsList())
            .containsExactlyInAnyOrder("thirdVendor");

        MboMappings.SearchVendorsResponse searchResultNoSupplier = service.searchVendorsByShopId(
            MboMappings.SearchVendorsBySupplierIdRequest.newBuilder().setSupplierId(nonexistentSupplier).build());
        Assertions.assertThat(searchResultNoSupplier.getVendorsList()).isEmpty();

        verify(masterSpy, Mockito.times(0)).findOfferVendors(Mockito.any());
        verify(slaveSpy, Mockito.times(3)).findOfferVendors(Mockito.any());
    }

    @Test
    public void testSearchOfferVendorsLinkedSuppliers() {
        final int businessId = 10000;
        final int linkedSupplier1 = 10001;
        final int linkedSupplier2 = 10002;
        offerRepository.deleteAllInTest();
        offerRepository.insertOffers(
            OfferTestUtils.simpleOffer()
                .setId(1)
                .setVendor("Super vendor")
                .setBusinessId(businessId)
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(1L),
                    Offer.MappingConfidence.CONTENT)
                .setShopSku("1")
                .setServiceOffers(List.of(
                    new Offer.ServiceOffer(
                        linkedSupplier1,
                        MbocSupplierType.THIRD_PARTY,
                        Offer.AcceptanceStatus.OK),
                    new Offer.ServiceOffer(
                        linkedSupplier2,
                        MbocSupplierType.THIRD_PARTY,
                        Offer.AcceptanceStatus.OK)
                )),
            OfferTestUtils.simpleOffer()
                .setId(2)
                .setVendor("second vendor")
                .setBusinessId(businessId)
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(1L),
                    Offer.MappingConfidence.CONTENT)
                .setShopSku("2")
                .setServiceOffers(List.of(
                    new Offer.ServiceOffer(
                        linkedSupplier1,
                        MbocSupplierType.THIRD_PARTY,
                        Offer.AcceptanceStatus.OK)
                )),
            OfferTestUtils.simpleOffer()
                .setId(3)
                .setVendor("third vendor")
                .setBusinessId(businessId)
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(1L),
                    Offer.MappingConfidence.CONTENT)
                .setShopSku("3")
                .setServiceOffers(List.of())
        );

        MboMappings.SearchVendorsResponse searchResultSupplier1 = service.searchVendorsByShopId(
            MboMappings.SearchVendorsBySupplierIdRequest.newBuilder().setSupplierId(linkedSupplier1).build());
        Assertions.assertThat(searchResultSupplier1.getVendorsList())
            .containsExactly("second vendor", "Super vendor");

        MboMappings.SearchVendorsResponse searchResultSupplier2 = service.searchVendorsByShopId(
            MboMappings.SearchVendorsBySupplierIdRequest.newBuilder().setSupplierId(linkedSupplier2).build());
        Assertions.assertThat(searchResultSupplier2.getVendorsList())
            .containsExactlyInAnyOrder("Super vendor");
    }

    @Test
    public void reprocessOffersClassificationTest() {
        final int supplier1 = 1;
        final int supplier2 = 2;
        final String shopSku1 = "1";
        final String shopSku2 = "2";
        final String shopSku3 = "3";
        offerRepository.deleteAllInTest();
        offerRepository.insertOffers(
            OfferTestUtils.simpleOffer()
                .setId(1)
                .setBusinessId(supplier1)
                .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
                .setContentSkuMapping(OfferTestUtils.mapping(1L))
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.CONTENT_PROCESSING)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .setShopSku(shopSku1)
                .setServiceOffers(List.of(new Offer.ServiceOffer(supplier1))),
            OfferTestUtils.simpleOffer()
                .setId(2)
                .setBusinessId(supplier1)
                .setContentSkuMapping(OfferTestUtils.mapping(2L))
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.CONTENT_PROCESSING)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .setShopSku(shopSku2)
                .setServiceOffers(List.of(new Offer.ServiceOffer(supplier1))),
            OfferTestUtils.simpleOffer()
                .setId(3)
                .setBusinessId(supplier1)
                .setContentSkuMapping(OfferTestUtils.mapping(3L))
                .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
                .setAutomaticClassification(false)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .setShopSku(shopSku3)
                .setServiceOffers(List.of(new Offer.ServiceOffer(supplier1))),
            OfferTestUtils.simpleOffer()
                .setId(4)
                .setBusinessId(supplier2)
                .setContentSkuMapping(OfferTestUtils.mapping(4L))
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .setShopSku(shopSku1)
                .setServiceOffers(List.of(new Offer.ServiceOffer(supplier2))),
            OfferTestUtils.simpleOffer()
                .setId(5)
                .setBusinessId(supplier2)
                .setContentSkuMapping(OfferTestUtils.mapping(5L))
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.CONTENT_PROCESSING)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .setShopSku(shopSku2)
                .setServiceOffers(List.of(new Offer.ServiceOffer(supplier2)))
        );

        MboMappings.ReprocessOffersClassificationResponse reprocessOffersClassificationResponse =
            service.reprocessOffersClassification(MboMappings.ReprocessOffersClassificationRequest.newBuilder()
                .addSkuKey(
                    MbocCommon.ShopSkuKey.newBuilder().setSupplierId(supplier1).setSupplierSkuId(shopSku1).build())
                .addSkuKey(
                    MbocCommon.ShopSkuKey.newBuilder().setSupplierId(supplier1).setSupplierSkuId(shopSku2).build())
                .addSkuKey(
                    MbocCommon.ShopSkuKey.newBuilder().setSupplierId(supplier1).setSupplierSkuId(shopSku3).build())
                .addSkuKey(
                    MbocCommon.ShopSkuKey.newBuilder().setSupplierId(supplier2).setSupplierSkuId(shopSku1).build())
                .addSkuKey(
                    MbocCommon.ShopSkuKey.newBuilder().setSupplierId(supplier2).setSupplierSkuId(shopSku2).build())
                // does not exist:
                .addSkuKey(
                    MbocCommon.ShopSkuKey.newBuilder().setSupplierId(supplier2).setSupplierSkuId(shopSku3).build())
                .build());

        MboMappings.ReprocessOffersClassificationResponse expected = MboMappings.ReprocessOffersClassificationResponse
            .newBuilder()
            .addOfferResults(MboMappings.ReprocessOffersClassificationResponse.ProcessedOfferResult.newBuilder()
                .setSkuKey(MbocCommon.ShopSkuKey.newBuilder().setSupplierId(supplier1)
                    .setSupplierSkuId(shopSku1).build())
                .setStatus(MboMappings.ReprocessOffersClassificationResponse.Status.ALREADY_IN_REPROCESS)
                .setMessage("Offer is already being reprocessed or classified in status CONTENT_PROCESSING " +
                    "with reprocessing flag set to false and its category_id is 1 (APPROVED)")
                .build())
            .addOfferResults(MboMappings.ReprocessOffersClassificationResponse.ProcessedOfferResult.newBuilder()
                .setSkuKey(MbocCommon.ShopSkuKey.newBuilder().setSupplierId(supplier1)
                    .setSupplierSkuId(shopSku2).build())
                .setStatus(MboMappings.ReprocessOffersClassificationResponse.Status.OK)
                .build())
            .addOfferResults(MboMappings.ReprocessOffersClassificationResponse.ProcessedOfferResult.newBuilder()
                .setSkuKey(MbocCommon.ShopSkuKey.newBuilder().setSupplierId(supplier1)
                    .setSupplierSkuId(shopSku3).build())
                .setStatus(MboMappings.ReprocessOffersClassificationResponse.Status.ALREADY_IN_REPROCESS)
                .setMessage("Offer is already being reprocessed or classified in status IN_CLASSIFICATION " +
                    "with reprocessing flag set to false and its category_id is 1 (SUGGESTED)")
                .build())
            .addOfferResults(MboMappings.ReprocessOffersClassificationResponse.ProcessedOfferResult.newBuilder()
                .setSkuKey(MbocCommon.ShopSkuKey.newBuilder().setSupplierId(supplier2)
                    .setSupplierSkuId(shopSku1).build())
                .setStatus(MboMappings.ReprocessOffersClassificationResponse.Status.ERROR)
                .setMessage("Could not send offer to reprocess, since its status is NEED_CONTENT")
                .build())
            .addOfferResults(MboMappings.ReprocessOffersClassificationResponse.ProcessedOfferResult.newBuilder()
                .setSkuKey(MbocCommon.ShopSkuKey.newBuilder().setSupplierId(supplier2)
                    .setSupplierSkuId(shopSku2).build())
                .setStatus(MboMappings.ReprocessOffersClassificationResponse.Status.OK)
                .build())
            .addOfferResults(MboMappings.ReprocessOffersClassificationResponse.ProcessedOfferResult.newBuilder()
                .setSkuKey(MbocCommon.ShopSkuKey.newBuilder().setSupplierId(supplier2)
                    .setSupplierSkuId(shopSku3).build())
                .setStatus(MboMappings.ReprocessOffersClassificationResponse.Status.ERROR)
                .setMessage("Offer not found")
                .build())
            .build();

        Assertions.assertThat(reprocessOffersClassificationResponse).isEqualToComparingFieldByField(expected);
        Map<Long, Offer> offers =
            offerRepository.getOffersByIds(1L, 2L, 3L, 4L, 5L).stream().collect(Collectors.toMap(
                Offer::getId, Function.identity()
            ));
        // check offers 2, 5 sent to reprocessing
        checkOfferSentToReprocessing(offers.get(2L));
        checkOfferSentToReprocessing(offers.get(5L));
        // check that already processed offers 1,3 stay in the same binding kind
        MbocAssertions.assertThat(offers.get(1L)).hasBindingKind(Offer.BindingKind.APPROVED);
        MbocAssertions.assertThat(offers.get(3L)).hasBindingKind(Offer.BindingKind.SUGGESTED);
        // check error offer 4 left in old status
        MbocAssertions.assertThat(offers.get(4L)).hasProcessingStatus(Offer.ProcessingStatus.NEED_CONTENT);
    }

    @Test
    public void testCountBusinessOffers() {
        Mockito.when(offerStatService.countBusinessOffers(1)).thenReturn(123);
        assertEquals(123, service.countBusinessOffers(
            MboMappings.CountBusinessOffersRequest.newBuilder().setBusinessId(1).build()).getOffersCount());
        verify(offerStatService).countBusinessOffers(1);
    }

    private void checkOfferSentToReprocessing(Offer offer) {
        MbocAssertions.assertThat(offer).hasProcessingStatus(Offer.ProcessingStatus.CONTENT_PROCESSING);
        MbocAssertions.assertThat(offer).hasBindingKind(Offer.BindingKind.SUGGESTED);
        MbocAssertions.assertThat(offer).hasReprocessRequested(true);
        MbocAssertions.assertThat(offer).hasAutomaticClassification(false);
        Assertions.assertThat(offer.getContentSkuMapping()).isNull();
    }

    @Test
    public void testFindByYtStampLite() {
        MboMappings.SearchProductInfoLiteByYtStampResponse response = service.searchProductInfoLiteByYtStamp(
            MboMappings.SearchProductInfoByYtStampRequest.newBuilder()
                .setFromStamp(12L)
                .setCount(100)
                .build()
        );

        Assertions.assertThat(response.getProviderProductInfoLiteList())
            .hasSize(4)
            .extracting(MboMappings.ProviderProductInfoLite::getUploadToYtStamp)
            .containsExactlyInAnyOrder(12L, 13L, 14L, 14L);
    }

    @Test
    public void testFindByYtStampLiteContainsUpdatedTs() {
        MboMappings.SearchProductInfoLiteByYtStampResponse response = service.searchProductInfoLiteByYtStamp(
            MboMappings.SearchProductInfoByYtStampRequest.newBuilder()
                .setFromStamp(14L)
                .setCount(100)
                .build()
        );

        Assertions.assertThat(response.getProviderProductInfoLiteList())
            .hasSize(2)
            .extracting(MboMappings.ProviderProductInfoLite::getUploadToYtStamp)
            .containsExactlyInAnyOrder(14L, 14L);
    }

    @Test
    public void testFindByYtStampLiteWithBusiness() {
        supplierRepository.insertBatch(
            new Supplier(2000, "biz").setType(MbocSupplierType.BUSINESS),
            new Supplier(2001, "biz-child1").setBusinessId(2000),
            new Supplier(2002, "biz-child2").setBusinessId(2000)
                .setType(MbocSupplierType.REAL_SUPPLIER).setRealSupplierId("BIZ_REAL"),
            new Supplier(2003, "biz-child23").setBusinessId(2000)
        );

        insertOffersKeepStamps(
            offer(1001, 2000, "bizsku0").setServiceOffers(List.of(
                new Offer.ServiceOffer(2001, MbocSupplierType.THIRD_PARTY, Offer.AcceptanceStatus.OK),
                new Offer.ServiceOffer(2002, MbocSupplierType.REAL_SUPPLIER, Offer.AcceptanceStatus.OK)
            )),
            offer(1002, 2001, "bizsku1").setServiceOffers(List.of(
                new Offer.ServiceOffer(2001, MbocSupplierType.THIRD_PARTY, Offer.AcceptanceStatus.OK)
            )),
            offer(1003, 2002, "bizsku2").setServiceOffers(List.of(
                new Offer.ServiceOffer(2002, MbocSupplierType.REAL_SUPPLIER, Offer.AcceptanceStatus.OK)
            )),
            offer(1004, 2003, "bizsku3").setServiceOffers(List.of())
        );

        MboMappings.SearchProductInfoLiteByYtStampResponse response = service.searchProductInfoLiteByYtStamp(
            MboMappings.SearchProductInfoByYtStampRequest.newBuilder()
                .setFromStamp(11L)
                .setCount(100)
                .build()
        );

        Assertions.assertThat(response.getProviderProductInfoLiteList())
            .hasSize(10)
            .extracting(MboMappings.ProviderProductInfoLite::getShopId)
            // suppliers: 2001, 2002, 63, 63, 77
            // offers duplicated from business: 2001, 2002
            // and beruId 465852, 465852, 465852 for real_suppliers (77, 2002 and duplicated for 2002)
            .containsExactlyInAnyOrder(2001, 2002, 63, 63, 77,
                2001, 2002,
                465852, 465852, 465852);

        Assertions.assertThat(response.getProviderProductInfoLiteList())
            .extracting(MboMappings.ProviderProductInfoLite::getShopSkuId)
            .containsExactlyInAnyOrder("bizsku0", "bizsku0", "bizsku1", "bizsku2", "sku12", "sku13",
                "sku14", "000042.sku14", "BIZ_REAL.bizsku0", "BIZ_REAL.bizsku2");
    }

    @Test
    public void testFindByYtStampLiteBeru() {
        MboMappings.SearchProductInfoLiteByYtStampResponse response = service.searchProductInfoLiteByYtStamp(
            MboMappings.SearchProductInfoByYtStampRequest.newBuilder()
                .setFromStamp(14L)
                .setCount(100)
                .build()
        );

        Assertions.assertThat(response.getProviderProductInfoLiteList())
            .hasSize(2)
            .extracting(p -> new ShopSkuKey(p.getShopId(), p.getShopSkuId()))
            .containsExactly(
                new ShopSkuKey(77, "sku14"),
                new ShopSkuKey(BERU_ID, "000042.sku14"));
    }

    @Test
    public void testFindByYtStampLitePage() {
        MboMappings.SearchProductInfoLiteByYtStampResponse response = service.searchProductInfoLiteByYtStamp(
            MboMappings.SearchProductInfoByYtStampRequest.newBuilder()
                .setFromStamp(12L)
                .setCount(2)
                .build()
        );

        Assertions.assertThat(response.getProviderProductInfoLiteList())
            .hasSize(2)
            .extracting(MboMappings.ProviderProductInfoLite::getUploadToYtStamp)
            .containsExactly(12L, 13L);
    }

    @Test
    public void testFindByYtStampLiteGap() {
        MboMappings.SearchProductInfoLiteByYtStampResponse response = service.searchProductInfoLiteByYtStamp(
            MboMappings.SearchProductInfoByYtStampRequest.newBuilder()
                .setFromStamp(14L)
                .setCount(1000)
                .build()
        );

        Assertions.assertThat(response.getProviderProductInfoLiteList())
            .hasSize(2)
            .extracting(MboMappings.ProviderProductInfoLite::getUploadToYtStamp)
            .allMatch(l -> l >= 14L);
    }

    @Test
    public void testFindByYtStampLiteNotDataCamp() {
        MboMappings.SearchProductInfoLiteByYtStampResponse response = service.searchProductInfoLiteByYtStampNotDataCamp(
            MboMappings.SearchProductInfoByYtStampRequest.newBuilder()
                .setFromStamp(12L)
                .setCount(100)
                .build()
        );

        Assertions.assertThat(response.getProviderProductInfoLiteList())
            .hasSize(2)
            .extracting(p -> new ShopSkuKey(p.getShopId(), p.getShopSkuId()))
            .containsExactly(
                new ShopSkuKey(77, "sku14"),
                new ShopSkuKey(BERU_ID, "000042.sku14"));
    }

    @Test
    public void testShouldReturnApprovedMappingsEvenNotUploadedToYtButWithStampLite() {
        offerRepository.deleteAllInTest();
        insertOfferKeepStamp(OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(303030L), Offer.MappingConfidence.CONTENT)
            .setUploadToYtStamp(1L));

        MboMappings.SearchProductInfoLiteByYtStampResponse response = service.searchProductInfoLiteByYtStamp(
            MboMappings.SearchProductInfoByYtStampRequest.newBuilder()
                .setFromStamp(0)
                .setCount(1000)
                .build()
        );

        Assertions.assertThat(response.getProviderProductInfoLiteList())
            .hasSize(1)
            .extracting(MboMappings.ProviderProductInfoLite::getShopSkuId)
            .containsExactly(OfferTestUtils.DEFAULT_SHOP_SKU);
    }

    @Test
    public void testFindByYtStampFastSku() {
        supplierRepository.insertBatch(
            new Supplier(2000, "biz").setType(MbocSupplierType.BUSINESS),
            new Supplier(2001, "biz-child1").setBusinessId(2000)
        );

        var offerNormal = offer(1001, 2000, "bizsku0")
            .setServiceOffers(List.of(
                new Offer.ServiceOffer(2001, MbocSupplierType.THIRD_PARTY, Offer.AcceptanceStatus.OK)
            ));
        var offerFastSkuWithMappedCat = offer(1002, 2000, "bizsku1")
            .setServiceOffers(List.of(
                new Offer.ServiceOffer(2001, MbocSupplierType.THIRD_PARTY, Offer.AcceptanceStatus.OK)
            ))
            .setApprovedSkuMappingInternal(new Offer.Mapping(22L, LocalDateTime.now(), FAST_SKU))
            .setMappedCategoryId(123L);

        var offerFastSkuWithoutCat = offer(1003, 2000, "bizsku2")
            .setServiceOffers(List.of(
                new Offer.ServiceOffer(2001, MbocSupplierType.THIRD_PARTY, Offer.AcceptanceStatus.OK)
            ))
            .setApprovedSkuMappingInternal(new Offer.Mapping(22L, LocalDateTime.now(), FAST_SKU));

        insertOffersKeepStamps(offerNormal, offerFastSkuWithMappedCat, offerFastSkuWithoutCat);

        MboMappings.SearchProductInfoLiteByYtStampResponse response = service.searchProductInfoLiteByYtStamp(
            MboMappings.SearchProductInfoByYtStampRequest.newBuilder()
                .setFromStamp(0L)
                .setCount(100)
                .setIncludeFastSkuOffers(false)
                .build()
        );

        Assertions.assertThat(response.getProviderProductInfoLiteList())
            .hasSize(6)
            .extracting(MboMappings.ProviderProductInfoLite::getShopSkuId)
            // Last 4 are from test data
            .containsExactlyInAnyOrder("bizsku0", "bizsku1", "sku12", "sku13", "sku14", "000042.sku14");

        Assertions.assertThat(response.getProviderProductInfoLiteList())
            .filteredOn(it -> it.getShopSkuId().equals("bizsku1"))
            .hasSize(1)
            .element(0)
            .matches(it -> !it.hasMarketSkuId() && it.getMappedCategoryId() == 123);
    }

    @Test
    public void testUpdateAntiMappings() {
        Offer offer = offer(1001, 10000, "bizsku1").setServiceOffers(List.of(
            new Offer.ServiceOffer(10001, MbocSupplierType.THIRD_PARTY, Offer.AcceptanceStatus.OK)
        ));
        offerRepository.insertOffers(offer);

        AntiMapping existingAntiMapping = antiMapping(offer.getId())
            .setNotSkuId(OfferTestUtils.TEST_SKU_ID);
        antiMappingRepository.insert(existingAntiMapping);

        offerUpdateSequenceService.copyOfferChangesFromStaging();
        Long lastSeq = offerUpdateSequenceService.getLastModifiedSequenceId();

        service.updateAntiMappings(MboMappings.UpdateAntiMappingsRequest.newBuilder()
            .addAllUpdates(List.of(
                MboMappings.UpdateAntiMappingsRequest.AntiMappingUpdate.newBuilder()
                    .setOfferId(offer.getId())
                    .setNotMarketSkuId(OfferTestUtils.TEST_SKU_ID)
                    .setIsDeleted(true)
                    .build(),
                MboMappings.UpdateAntiMappingsRequest.AntiMappingUpdate.newBuilder()
                    .setOfferId(offer.getId())
                    .setNotMarketSkuId(OfferTestUtils.TEST_SKU_ID + 1)
                    .build()
            ))
            .build());

        List<AntiMapping> antiMappings = antiMappingRepository.findAll();
        Assertions.assertThat(antiMappings)
            .usingElementComparatorOnFields("offerId", "notSkuId", "deleted")
            .containsExactlyInAnyOrder(
                antiMapping(offer.getId())
                    .setNotSkuId(OfferTestUtils.TEST_SKU_ID)
                    .setDeletedTs(Instant.now()),
                antiMapping(offer.getId())
                    .setNotSkuId(OfferTestUtils.TEST_SKU_ID + 1)
            );

        offerUpdateSequenceService.copyOfferChangesFromStaging();
        var modifiedRow = offerUpdateSequenceService.getModifiedRecordsIdBatch(lastSeq, 1);
        Assertions.assertThat(modifiedRow.get(0).getKey()).isEqualTo(offer.getId());
    }

    @Test
    public void testPaginationFindAntiMappingWithServiceOffersByStamp() {
        int count = 5;

        List<AntiMapping> antiMappings = LongStream.rangeClosed(1, count)
            .mapToObj(id -> antiMapping(1L)
                .setNotSkuId(id)
                .setUploadStamp(id))
            .collect(Collectors.toList());

        antiMappingRepository.insertBatch(antiMappings);

        int pageSize = 2;
        var pagedRequest = MboMappings.SearchProductInfoByYtStampRequest.newBuilder()
            .setCount(pageSize);

        long nextStamp = 0L;
        List<List<MboMappings.AntiMappingInfo>> responsePages = new ArrayList<>();

        for (int i = count; i > 0; i = i - pageSize) {
            MboMappings.SearchAntiMappingInfoByStampResponse response = service
                .searchAntiMappingInfoByStamp(pagedRequest.setFromStamp(nextStamp).build());

            List<MboMappings.AntiMappingInfo> antiMappingInfos = response.getAntiMappingInfoList();

            if (antiMappingInfos.isEmpty()) {
                break;
            }

            responsePages.add(antiMappingInfos);

            nextStamp = antiMappingInfos.stream()
                .map(MboMappings.AntiMappingInfo::getUploadStamp)
                .max(Long::compare)
                .map(x -> x + 1)
                .get();
        }

        var baseAntiMapping = MboMappings.AntiMappingInfo.newBuilder()
            .setShopId(42)
            .setShopSkuId("sku1");

        Assertions.assertThat(responsePages).hasSize(3);
        Assertions.assertThat(responsePages.get(0))
            .usingElementComparatorOnFields("shopId", "shopSkuId", "notMarketSkuId")
            .containsExactlyInAnyOrder(
                baseAntiMapping.setNotMarketSkuId(1).build(),
                baseAntiMapping.setNotMarketSkuId(2).build()
            );

        Assertions.assertThat(responsePages.get(1))
            .usingElementComparatorOnFields("shopId", "shopSkuId", "notMarketSkuId")
            .containsExactlyInAnyOrder(
                baseAntiMapping.setNotMarketSkuId(3).build(),
                baseAntiMapping.setNotMarketSkuId(4).build()
            );

        Assertions.assertThat(responsePages.get(2))
            .usingElementComparatorOnFields("shopId", "shopSkuId", "notMarketSkuId")
            .containsExactlyInAnyOrder(
                baseAntiMapping.setNotMarketSkuId(5).build()
            );
    }

    @Test
    public void findAntiMappingWithServiceOffersByStamp() {
        Offer offer1 = offer(1001, 10000, "bizsku1").setServiceOffers(List.of(
            new Offer.ServiceOffer(10001, MbocSupplierType.THIRD_PARTY, Offer.AcceptanceStatus.OK)
        ));
        Offer offer2 = offer(1002, 10000, "bizsku2").setServiceOffers(List.of(
            new Offer.ServiceOffer(10001, MbocSupplierType.THIRD_PARTY, Offer.AcceptanceStatus.OK)
        ));
        Offer offer3 = offer(1003, 10000, "bizsku3").setServiceOffers(List.of(
            new Offer.ServiceOffer(10001, MbocSupplierType.THIRD_PARTY, Offer.AcceptanceStatus.OK),
            new Offer.ServiceOffer(10002, MbocSupplierType.THIRD_PARTY, Offer.AcceptanceStatus.OK)
        ));
        offerRepository.insertOffers(offer1, offer2, offer3);

        Instant deletedTs = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);

        AntiMapping antiMapping1 = antiMapping(offer1.getId());
        AntiMapping antiMapping21 = antiMapping(offer2.getId())
            .setNotSkuId(OfferTestUtils.TEST_SKU_ID);
        AntiMapping antiMapping22 = antiMapping(offer2.getId())
            .setNotSkuId(OfferTestUtils.TEST_SKU_ID + 1)
            .setDeletedTs(deletedTs);
        AntiMapping antiMapping3 = antiMapping(offer3.getId());

        antiMappingRepository.insertBatch(antiMapping1, antiMapping21, antiMapping22, antiMapping3);

        MboMappings.SearchAntiMappingInfoByStampResponse response = service
            .searchAntiMappingInfoByStamp(MboMappings.SearchProductInfoByYtStampRequest.newBuilder()
                .setFromStamp(0)
                .setCount(1000)
                .build());

        Assertions.assertThat(response.getAntiMappingInfoList())
            .hasSize(5);
        Assertions.assertThat(response.getAntiMappingInfoList())
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                MboMappings.AntiMappingInfo.newBuilder()
                    .setShopId(10001)
                    .setShopSkuId("bizsku1")
                    .setNotMarketModelId(OfferTestUtils.TEST_MODEL_ID)
                    .setNotMarketSkuId(OfferTestUtils.TEST_SKU_ID)
                    .setUploadStamp(1L)
                    .build(),

                MboMappings.AntiMappingInfo.newBuilder()
                    .setShopId(10001)
                    .setShopSkuId("bizsku2")
                    .setNotMarketModelId(OfferTestUtils.TEST_MODEL_ID)
                    .setNotMarketSkuId(OfferTestUtils.TEST_SKU_ID)
                    .setUploadStamp(1L)
                    .build(),
                MboMappings.AntiMappingInfo.newBuilder()
                    .setShopId(10001)
                    .setShopSkuId("bizsku2")
                    .setNotMarketModelId(OfferTestUtils.TEST_MODEL_ID)
                    .setNotMarketSkuId(OfferTestUtils.TEST_SKU_ID + 1)
                    .setDeletedTs(deletedTs.toEpochMilli())
                    .setUploadStamp(1L)
                    .build(),

                MboMappings.AntiMappingInfo.newBuilder()
                    .setShopId(10001)
                    .setShopSkuId("bizsku3")
                    .setNotMarketModelId(OfferTestUtils.TEST_MODEL_ID)
                    .setNotMarketSkuId(OfferTestUtils.TEST_SKU_ID)
                    .setUploadStamp(1L)
                    .build(),
                MboMappings.AntiMappingInfo.newBuilder()
                    .setShopId(10002)
                    .setShopSkuId("bizsku3")
                    .setNotMarketModelId(OfferTestUtils.TEST_MODEL_ID)
                    .setNotMarketSkuId(OfferTestUtils.TEST_SKU_ID)
                    .setUploadStamp(1L)
                    .build()
            );
    }

    @Test
    public void testGetShopSKUForSimpleOffer() {
        offerRepository.deleteAllInTest();
        insertOffersKeepStamps(
            OfferTestUtils.simpleOffer()
                .setShopSku("shopSku1")
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(303031L), Offer.MappingConfidence.CONTENT),
            OfferTestUtils.simpleOffer()
                .setShopSku("shopSku2")
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(303031L), Offer.MappingConfidence.CONTENT)
        );

        final MboMappings.SKUMappingResponse response = service.getShopSKU(
            MboMappings.ShopSKURequest.newBuilder()
                .setShopId(OfferTestUtils.TEST_SUPPLIER_ID)
                .addMarketSkuId(303031L)
                .build()
        );

        Assertions.assertThat(response.getShopId()).isEqualTo(OfferTestUtils.TEST_SUPPLIER_ID);
        Assertions.assertThat(response.getSkuMappingList())
            .hasSize(1)
            .extracting(
                MboMappings.SKUMappingResponse.SKUMapping::getMarketSkuId,
                skuMapping -> skuMapping.getShopSkuList().stream().sorted().collect(Collectors.toUnmodifiableList()))
            .containsExactlyInAnyOrder(
                Assertions.tuple(303031L, List.of("shopSku1", "shopSku2"))
            );
    }

    @Test
    public void testGetShopSKUForServiceOffer() {
        offerRepository.deleteAllInTest();

        var serviceSupplierId = 7700;
        var serviceSupplier = new Supplier()
            .setId(serviceSupplierId)
            .setName("service supplier 7700")
            .setFulfillment(true)
            .setType(MbocSupplierType.THIRD_PARTY)
            .setBusinessId(42);

        supplierRepository.insert(serviceSupplier);

        insertOffersKeepStamps(
            OfferTestUtils.simpleOffer()
                .setShopSku("shopSku1")
                .addNewServiceOfferIfNotExistsForTests(serviceSupplier)
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(303031L), Offer.MappingConfidence.CONTENT)
        );

        final MboMappings.SKUMappingResponse response = service.getShopSKU(
            MboMappings.ShopSKURequest.newBuilder()
                .setShopId(serviceSupplierId)
                .addMarketSkuId(303031L)
                .build()
        );

        Assertions.assertThat(response.getShopId()).isEqualTo(serviceSupplierId);
        Assertions.assertThat(response.getSkuMappingList())
            .hasSize(1)
            .extracting(
                MboMappings.SKUMappingResponse.SKUMapping::getMarketSkuId,
                skuMapping -> skuMapping.getShopSkuList().stream().sorted().collect(Collectors.toUnmodifiableList()))
            .containsExactlyInAnyOrder(
                Assertions.tuple(303031L, List.of("shopSku1"))
            );
    }

    @Test
    public void testEmptyGetShopSKU() {
        offerRepository.deleteAllInTest();
        insertOffersKeepStamps(
            OfferTestUtils.simpleOffer()
                .setShopSku("shopSku1")
                .setServiceOffers(77)
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(303031L), Offer.MappingConfidence.CONTENT),
            OfferTestUtils.simpleOffer()
                .setShopSku("shopSku2")
                .setServiceOffers(78)
                .setSuggestSkuMapping(OfferTestUtils.mapping(303032L))
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
        );

        MboMappings.SKUMappingResponse response = service.getShopSKU(
            MboMappings.ShopSKURequest.newBuilder()
                .addMarketSkuId(303031L)
                .build()
        );

        Assertions.assertThat(response.hasShopId()).isFalse();
        Assertions.assertThat(response.getSkuMappingList()).isEmpty();

        response = service.getShopSKU(
            MboMappings.ShopSKURequest.newBuilder()
                .setShopId(77)
                .build()
        );

        Assertions.assertThat(response.hasShopId()).isFalse();
        Assertions.assertThat(response.getSkuMappingList()).isEmpty();

        response = service.getShopSKU(
            MboMappings.ShopSKURequest.newBuilder()
                .setShopId(78)
                .addMarketSkuId(303032L)
                .build()
        );

        Assertions.assertThat(response.hasShopId()).isFalse();
        Assertions.assertThat(response.getSkuMappingList()).isEmpty();

    }

    private AntiMapping antiMapping(long offerId) {
        return new AntiMapping()
            .setOfferId(offerId)
            .setNotModelId(OfferTestUtils.TEST_MODEL_ID)
            .setNotSkuId(OfferTestUtils.TEST_SKU_ID)
            .setCreatedTs(Instant.now().minus(3, ChronoUnit.DAYS))
            .setUpdatedTs(Instant.now().minus(2, ChronoUnit.DAYS))
            .setUpdatedUser("test user updated")
            .setDeletedUser("test user deleted")
            .setUploadStamp(1L);
    }

    @Test
    public void addOfferToContentProcessing_forceGroupedOfferError() {
        offerRepository.deleteAllInTest();

        insertOffersKeepStamps(
            OfferTestUtils.simpleOffer().setShopSku("shopSku1").setBusinessId(1).setGroupId(1)
        );

        MbocCommon.BusinessSkuKey offerKey = MbocCommon.BusinessSkuKey.newBuilder()
            .setBusinessId(1)
            .setOfferId("shopSku1")
            .build();
        MboMappings.AddToContentProcessingResponse response =
            service.addOfferToContentProcessing(MboMappings.AddToContentProcessingRequest.newBuilder()
                .addBusinessSkuKey(offerKey)
                .setIsForce(true)
                .build());

        List<MboMappings.AddToContentProcessingResponse.Result> resultList = response.getResultList();
        Assertions.assertThat(resultList).hasSize(1);
        MboMappings.AddToContentProcessingResponse.Result result = resultList.get(0);
        Assertions.assertThat(result).extracting(MboMappings.AddToContentProcessingResponse.Result::getMessage)
            .isEqualTo("Offer has groupId 1 - only non-grouped offers allowed to be force-send.");
        Assertions.assertThat(result).extracting(MboMappings.AddToContentProcessingResponse.Result::getStatus)
            .isEqualTo(MboMappings.AddToContentProcessingResponse.Status.ERROR);
        Assertions.assertThat(result).extracting(MboMappings.AddToContentProcessingResponse.Result::getBusinessSkuKey)
            .isEqualTo(offerKey);
    }

    @Test
    public void addOfferToContentProcessing_forceDeduplicatedOfferError() {
        offerRepository.deleteAllInTest();

        insertOffersKeepStamps(
            OfferTestUtils.simpleOffer().setShopSku("shopSku1").setBusinessId(1)
        );

        MbocCommon.BusinessSkuKey offerKey = MbocCommon.BusinessSkuKey.newBuilder()
            .setBusinessId(1)
            .setOfferId("shopSku1")
            .build();
        MboMappings.AddToContentProcessingResponse response =
            service.addOfferToContentProcessing(MboMappings.AddToContentProcessingRequest.newBuilder()
                .addBusinessSkuKey(offerKey)
                .setIsForce(true)
                .setIsDeduplicated(true)
                .build());

        List<MboMappings.AddToContentProcessingResponse.Result> resultList = response.getResultList();
        Assertions.assertThat(resultList).hasSize(1);
        MboMappings.AddToContentProcessingResponse.Result result = resultList.get(0);
        Assertions.assertThat(result).extracting(MboMappings.AddToContentProcessingResponse.Result::getMessage)
            .isEqualTo("Offer can't be forced and deduplicated");
        Assertions.assertThat(result).extracting(MboMappings.AddToContentProcessingResponse.Result::getStatus)
            .isEqualTo(MboMappings.AddToContentProcessingResponse.Status.ERROR);
        Assertions.assertThat(result).extracting(MboMappings.AddToContentProcessingResponse.Result::getBusinessSkuKey)
            .isEqualTo(offerKey);
    }

    @Test
    public void addOfferToContentProcessing_success() {
        offerRepository.deleteAllInTest();
        supplierRepository.deleteAll();

        Supplier supplier1 = OfferTestUtils.simpleSupplier()
            .setType(MbocSupplierType.BUSINESS)
            .setNewContentPipeline(true);
        Supplier supplier2 = OfferTestUtils.simpleSupplier(OfferTestUtils.BLUE_SUPPLIER_ID_1)
            .setType(MbocSupplierType.BUSINESS)
            .setNewContentPipeline(true);
        supplierRepository.insertBatch(supplier1, supplier2);
        supplierService.invalidateCache();

        Category category = new Category()
            .setCategoryId(1L)
            .setHasKnowledge(true) // один из флагов, который запрещает создание карточки в AllowModelCreateUpdate
            .setAcceptContentFromWhiteShops(true);
        categoryCachingService.addCategory(category);

        var offer1 = OfferTestUtils.simpleOffer(supplier1).setShopSku("shopSku1")
            .setDataCampOffer(true)
            .setCategoryIdForTests(category.getCategoryId(), Offer.BindingKind.APPROVED);
        var offer2 = OfferTestUtils.simpleOffer(supplier2).setShopSku("shopSku2").setGroupId(1)
            .setDataCampOffer(true)
            .setCategoryIdForTests(category.getCategoryId(), Offer.BindingKind.APPROVED);
        insertOffersKeepStamps(offer1, offer2);

        MbocCommon.BusinessSkuKey offerKey1 = MbocCommon.BusinessSkuKey.newBuilder()
            .setBusinessId(offer1.getBusinessId())
            .setOfferId(offer1.getShopSku())
            .build();
        MbocCommon.BusinessSkuKey offerKey2 = MbocCommon.BusinessSkuKey.newBuilder()
            .setBusinessId(offer2.getBusinessId())
            .setOfferId(offer2.getShopSku())
            .build();
        MboMappings.AddToContentProcessingResponse response =
            service.addOfferToContentProcessing(MboMappings.AddToContentProcessingRequest.newBuilder()
                .addBusinessSkuKey(offerKey1)
                .addBusinessSkuKey(offerKey2)
                .build());

        List<MboMappings.AddToContentProcessingResponse.Result> resultList = response.getResultList();
        Assertions.assertThat(resultList).hasSize(2);
        Assertions.assertThat(resultList).extracting(MboMappings.AddToContentProcessingResponse.Result::getStatus)
            .containsOnly(MboMappings.AddToContentProcessingResponse.Status.OK);
        Assertions.assertThat(resultList)
            .extracting(MboMappings.AddToContentProcessingResponse.Result::getBusinessSkuKey)
            .containsOnly(offerKey1, offerKey2);

        Collection<ContentProcessingOffer> shopSku1Item = contentProcessingQueue.findAllByBusinessSkuKeys(
            offer1.getBusinessId(), List.of(offer1.getShopSku())
        );
        Assertions.assertThat(shopSku1Item).hasSize(1);
        Assertions.assertThat(shopSku1Item.stream().findAny().orElseThrow().isForce()).isFalse();
        Assertions.assertThat(shopSku1Item.stream().findAny().orElseThrow().isDeduplicated()).isFalse();

        Collection<ContentProcessingOffer> shopSku2Item = contentProcessingQueue.findAllByBusinessSkuKeys(
            offer2.getBusinessId(), List.of(offer2.getShopSku())
        );
        Assertions.assertThat(shopSku2Item).hasSize(1);
        Assertions.assertThat(shopSku2Item.stream().findAny().orElseThrow().isForce()).isFalse();
        Assertions.assertThat(shopSku1Item.stream().findAny().orElseThrow().isDeduplicated()).isFalse();
    }

    @Test
    public void addOfferToContentProcessing_canNotFindOffer() {
        offerRepository.deleteAllInTest();

        MbocCommon.BusinessSkuKey offerKey1 = MbocCommon.BusinessSkuKey.newBuilder()
            .setBusinessId(1)
            .setOfferId("shopSku1")
            .build();
        MbocCommon.BusinessSkuKey offerKey2 = MbocCommon.BusinessSkuKey.newBuilder()
            .setBusinessId(2)
            .setOfferId("shopSku2")
            .build();
        MboMappings.AddToContentProcessingResponse response =
            service.addOfferToContentProcessing(MboMappings.AddToContentProcessingRequest.newBuilder()
                .addBusinessSkuKey(offerKey1)
                .addBusinessSkuKey(offerKey2)
                .build());

        List<MboMappings.AddToContentProcessingResponse.Result> resultList = response.getResultList();
        Assertions.assertThat(resultList).hasSize(2);
        Assertions.assertThat(resultList).extracting(MboMappings.AddToContentProcessingResponse.Result::getStatus)
            .containsOnly(MboMappings.AddToContentProcessingResponse.Status.ERROR);
        Assertions.assertThat(resultList)
            .extracting(MboMappings.AddToContentProcessingResponse.Result::getBusinessSkuKey)
            .containsOnly(offerKey1, offerKey2);

        Collection<ContentProcessingOffer> shopSku1Item = contentProcessingQueue.findAllByBusinessSkuKeys(
            1, List.of("shopSku1")
        );
        Assertions.assertThat(shopSku1Item).isEmpty();

        Collection<ContentProcessingOffer> shopSku2Item = contentProcessingQueue.findAllByBusinessSkuKeys(
            2, List.of("shopSku2")
        );
        Assertions.assertThat(shopSku2Item).isEmpty();
        Assertions.assertThat(resultList.get(0)).extracting(MboMappings.AddToContentProcessingResponse.Result::getMessage)
            .isEqualTo("Failed to find an offer");
        Assertions.assertThat(resultList.get(1)).extracting(MboMappings.AddToContentProcessingResponse.Result::getMessage)
            .isEqualTo("Failed to find an offer");
    }

    @Test
    public void addOfferToContentProcessing_updateFlags() {
        offerRepository.deleteAllInTest();
        supplierRepository.deleteAll();

        Supplier supplier = OfferTestUtils.simpleSupplier()
            .setType(MbocSupplierType.BUSINESS)
            .setNewContentPipeline(true);
        supplierRepository.insertBatch(supplier);
        supplierService.invalidateCache();

        Category category = new Category()
            .setCategoryId(1L)
            .setHasKnowledge(true)
            .setAcceptContentFromWhiteShops(true);
        categoryCachingService.addCategory(category);

        Offer offer = OfferTestUtils.simpleOffer(supplier)
            .setDataCampOffer(true)
            .setCategoryIdForTests(category.getCategoryId(), Offer.BindingKind.APPROVED);

        insertOffersKeepStamps(offer);

        MbocCommon.BusinessSkuKey offerKey1 = MbocCommon.BusinessSkuKey.newBuilder()
            .setBusinessId(offer.getBusinessId())
            .setOfferId(offer.getShopSku())
            .build();
        MboMappings.AddToContentProcessingResponse response =
            service.addOfferToContentProcessing(MboMappings.AddToContentProcessingRequest.newBuilder()
                .addBusinessSkuKey(offerKey1)
                .setIsForce(true)
                .setIsDeduplicated(false)
                .build());

        List<MboMappings.AddToContentProcessingResponse.Result> resultList = response.getResultList();
        Assertions.assertThat(resultList).hasSize(1);
        Assertions.assertThat(resultList).extracting(MboMappings.AddToContentProcessingResponse.Result::getStatus)
            .containsOnly(MboMappings.AddToContentProcessingResponse.Status.OK);
        Assertions.assertThat(resultList)
            .extracting(MboMappings.AddToContentProcessingResponse.Result::getBusinessSkuKey)
            .containsOnly(offerKey1);

        Collection<ContentProcessingOffer> shopSku1Item = contentProcessingQueue.findAllByBusinessSkuKeys(
            offer.getBusinessId(), List.of(offer.getShopSku())
        );
        Assertions.assertThat(shopSku1Item).hasSize(1);
        Assertions.assertThat(shopSku1Item.stream().findAny().orElseThrow().isForce()).isTrue();
        Assertions.assertThat(shopSku1Item.stream().findAny().orElseThrow().isDeduplicated()).isFalse();

        response = service.addOfferToContentProcessing(MboMappings.AddToContentProcessingRequest.newBuilder()
            .addBusinessSkuKey(offerKey1)
            .setIsForce(false)
            .setIsDeduplicated(true)
            .build());

        resultList = response.getResultList();
        Assertions.assertThat(resultList).hasSize(1);
        Assertions.assertThat(resultList).extracting(MboMappings.AddToContentProcessingResponse.Result::getStatus)
            .containsOnly(MboMappings.AddToContentProcessingResponse.Status.OK);
        Assertions.assertThat(resultList)
            .extracting(MboMappings.AddToContentProcessingResponse.Result::getBusinessSkuKey)
            .containsOnly(offerKey1);

        shopSku1Item = contentProcessingQueue.findAllByBusinessSkuKeys(
            offer.getBusinessId(), List.of(offer.getShopSku())
        );
        Assertions.assertThat(shopSku1Item).hasSize(1);
        Assertions.assertThat(shopSku1Item.stream().findAny().orElseThrow().isForce()).isFalse();
        Assertions.assertThat(shopSku1Item.stream().findAny().orElseThrow().isDeduplicated()).isTrue();
    }

    @Test
    public void addOfferToContentProcessing_forbid_to_create_card() {
        offerRepository.deleteAllInTest();
        supplierRepository.deleteAll();

        Supplier supplier = OfferTestUtils.simpleSupplier()
            .setType(MbocSupplierType.BUSINESS)
            .setNewContentPipeline(true);
        supplierRepository.insertBatch(supplier);
        supplierService.invalidateCache();

        Category categoryWithNoKnowledge = new Category()
            .setCategoryId(1L)
            .setHasKnowledge(false) // один из флагов, который запрещает создание карточки в AllowModelCreateUpdate
            .setAcceptContentFromWhiteShops(true);
        categoryCachingService.addCategory(categoryWithNoKnowledge);

        Offer offer = OfferTestUtils.simpleOffer(supplier)
            .setDataCampOffer(true)
            .setCategoryIdForTests(categoryWithNoKnowledge.getCategoryId(), Offer.BindingKind.APPROVED);

        insertOffersKeepStamps(offer);

        MbocCommon.BusinessSkuKey offerKey1 = MbocCommon.BusinessSkuKey.newBuilder()
            .setBusinessId(offer.getBusinessId())
            .setOfferId(offer.getShopSku())
            .build();
        MboMappings.AddToContentProcessingResponse response =
            service.addOfferToContentProcessing(MboMappings.AddToContentProcessingRequest.newBuilder()
                .addBusinessSkuKey(offerKey1)
                .setIsDeduplicated(false)
                .build());

        assertEquals(
            MboMappings.AddToContentProcessingResponse.Status.GC_IS_FORBIDDEN,
            response.getResult(0).getStatus()
        );
    }

    @Test
    public void getAssortmentChildSskus_willReturnResult() {
        offerRepository.deleteAllInTest();
        supplierRepository.deleteAll();
        mskuRepository.deleteAll();

        var requestShopSku = new ShopSkuKey(1, "shopsku1");
        var expectedResponseShopSku = new ShopSkuKey(requestShopSku.getSupplierId(), "shopsku2");

        var supplier1 = supplierRepository.insert(
            new Supplier(requestShopSku.getSupplierId(), "test")
                .setBusinessId(requestShopSku.getSupplierId())
                .setType(MbocSupplierType.THIRD_PARTY)
        );

        Msku childSku1 = createMsku(1, 1);
        Msku childSku2 = createMsku(2, 1);
        Msku notChildSku3 = createMsku(3, 2);

        mskuRepository.save(childSku1, childSku2, notChildSku3);

        var baseChildOffer1 = createOfferWithApprovedSku(
            supplier1.getBusinessId(), requestShopSku.getShopSku(), childSku1.getMarketSkuId(), supplier1
        );

        var childOffer2 = createOfferWithApprovedSku(
            supplier1.getBusinessId(), expectedResponseShopSku.getShopSku(), childSku2.getMarketSkuId(), supplier1
        );

        var notChildOffer3 = createOfferWithApprovedSku(
            supplier1.getBusinessId(), "unexpectedShopsku3", notChildSku3.getMarketSkuId(), supplier1
        );

        offerRepository.insertOffers(baseChildOffer1, childOffer2, notChildOffer3);

        var request = MboMappings.GetAssortmentChildSskusRequest.newBuilder()
            .addAssortmentShopSkuKeys(MbocCommon.ShopSkuKey.newBuilder()
                .setSupplierId(supplier1.getId())
                .setSupplierSkuId(baseChildOffer1.getShopSku())
                .build())
            .build();
        var response = service.getAssortmentChildSskus(request);

        var resultsList = response.getResultsList();

        assertEquals(1, resultsList.size());
        var childSskusResult = resultsList.get(0);

        assertEquals(childSskusResult.getAssortmentShopSkuKey().getSupplierId(), requestShopSku.getSupplierId());
        assertEquals(childSskusResult.getAssortmentShopSkuKey().getSupplierSkuId(), requestShopSku.getShopSku());
        assertEquals(1, childSskusResult.getChildSkusCount());
        assertEquals(expectedResponseShopSku.getShopSku(), childSskusResult.getChildSkus(0));
    }

    @Test
    public void getAssortmentChildSskus_willReturnErrorMessage() {
        offerRepository.deleteAllInTest();
        supplierRepository.deleteAll();
        mskuRepository.deleteAll();

        var request = MboMappings.GetAssortmentChildSskusRequest.newBuilder()
            .addAssortmentShopSkuKeys(MbocCommon.ShopSkuKey.newBuilder()
                .setSupplierId(1)
                .setSupplierSkuId("shopsku")
                .build())
            .build();

        var response = service.getAssortmentChildSskus(request);

        var getAssortmentChildSskusResult = response.getResultsList().get(0);

        assertEquals(
            ChildSskuResult.ErrorType.SUPPLIER_NOT_FOUND.getDescription(),
            getAssortmentChildSskusResult.getErrorMessage()
        );
    }

    private void insertOffersKeepStamps(Collection<Offer> offers) {
        offers.forEach(this::insertOfferKeepStamp);
    }

    private void insertOffersKeepStamps(Offer... offers) {
        Arrays.stream(offers).forEach(this::insertOfferKeepStamp);
    }

    private void insertOfferKeepStamp(Offer offer) {
        var stamp = offer.getUploadToYtStamp();
        offerRepository.insertOffer(offer);
        OfferTestUtils.hardSetYtStamp(namedParameterJdbcTemplate, offer.getId(), stamp);
        offer.setUploadToYtStamp(stamp);
    }
}
