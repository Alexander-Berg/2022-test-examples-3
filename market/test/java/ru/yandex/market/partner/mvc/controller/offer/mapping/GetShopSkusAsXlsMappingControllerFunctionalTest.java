package ru.yandex.market.partner.mvc.controller.offer.mapping;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import Market.DataCamp.SyncAPI.SyncChangeOffer;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.Assert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.feed.supplier.SupplierXlsHelper;
import ru.yandex.market.core.tax.model.VatRate;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.mboc.http.SupplierOffer.Offer.MappingProcessingStatus;
import ru.yandex.market.mboc.http.SupplierOffer.Offer.MappingProcessingStatus.ChangeStatus;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;

@ParametersAreNonnullByDefault
@DbUnitDataSet(before = "MappingControllerFunctionalTest.csv")
// AbstractMappingControllerFunctionalTest экстендит общий для ПИ FunctionText
@DisplayName("Запрос списка оферов в виде Excel-файла")
class GetShopSkusAsXlsMappingControllerFunctionalTest
        extends AbstractMappingControllerFunctionalTest /* extends FunctionText */ {

    private static final long CAMPAIGN_ID = 10774L;
    private static final long SUPPLIER_ID = 774L;

    @Autowired
    private MboMappingsService patientMboMappingsService;

    @Autowired
    private SupplierXlsHelper supplierXlsHelper;

    @Autowired
    @Qualifier("unitedSupplierXlsHelper")
    private SupplierXlsHelper unitedSupplierXlsHelper;

    @Autowired
    private DataCampClient dataCampShopClient;

    @Test
    @DisplayName("Запрос списка оферов с пустым ответов")
    void testListSkusWithEmptyResult() throws IOException {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(Mockito.any()))
                .thenReturn(
                        MboMappings.SearchMappingsResponse.newBuilder()
                                .setTotalCount(0)
                                .build()
                );


        String url = String.format("%s", shopSkusAsXlsUrl(CAMPAIGN_ID));
        FunctionalTestHelper.post(url, "{}", byte[].class);
        Mockito.verify(unitedSupplierXlsHelper).fillTemplate(
                Mockito.anyList(),
                Mockito.argThat(Collection::isEmpty),
                Mockito.argThat(List::isEmpty),
                Mockito.any(),
                Mockito.anyBoolean()
        );
        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.hasSupplierId()
                        && request.getSupplierId() == SUPPLIER_ID
                        && !request.hasHasAnyMapping()
                        && request.getHasSupplierMappingStatusList().isEmpty()
                        && request.getHasNoSupplierMappingStatusList().isEmpty()
                        && request.getMarketCategoryIdsList().isEmpty()
                        && !request.hasTextQueryString()
                        && request.hasReturnTotalCount()
                        && !request.getReturnTotalCount()
                        && request.getReturnMasterData()));
    }

    @Test
    @DisplayName("Запрос списка оферов")
    void testListSkus() throws IOException {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(Mockito.any()))
                .thenReturn(
                        MboMappings.SearchMappingsResponse.newBuilder()
                                .addOffers(
                                        SupplierOffer.Offer.newBuilder()
                                                .setTitle("Test H123")
                                                .setSupplierId(SUPPLIER_ID)
                                                .setShopSkuId("H123")
                                                .setApprovedMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1288)
                                                                .setSkuName("MarketSku1288")
                                                                .setCategoryId(123)
                                                                .build())
                                                .setSupplierMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1214)
                                                                .setSkuName("MarketSku1214")
                                                                .setCategoryId(123)
                                                                .build()
                                                )
                                                .setSupplierMappingStatus(
                                                        MappingProcessingStatus.newBuilder()
                                                                .setStatus(ChangeStatus.MODERATION)
                                                                .build()
                                                )
                                                .addUrls("http://yandex.ru/123")
                                                .addUrls("http://yandex.ru/124")
                                                .build()
                                )
                                .addOffers(
                                        SupplierOffer.Offer.newBuilder()
                                                .setTitle("Test H124")
                                                .setSupplierId(SUPPLIER_ID)
                                                .setShopSkuId("H124")
                                                .setShopCategoryName("Shop/Category/Name")
                                                .setBarcode("sdkgjsdh12431254, sdjgh124314231, dskjghs124152")
                                                .setVendorCode("sgsd23523")
                                                .setShopVendor("Apple")
                                                .setDescription("Test H124 Description")
                                                .setApprovedMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1288)
                                                                .setSkuName("MarketSku1288")
                                                                .setCategoryId(123)
                                                                .build()
                                                )
                                                .setSupplierMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1214)
                                                                .setSkuName("MarketSku1214")
                                                                .setCategoryId(123)
                                                                .build()
                                                )
                                                .setSupplierMappingStatus(
                                                        MappingProcessingStatus.newBuilder()
                                                                .setStatus(ChangeStatus.REJECTED)
                                                                .build()
                                                )
                                                .build()
                                )
                                .addOffers(
                                        SupplierOffer.Offer.newBuilder()
                                                .setTitle("Test H125")
                                                .setSupplierId(SUPPLIER_ID)
                                                .setShopSkuId("H125")
                                                .setShopCategoryName("Shop/Category/Name/Other")
                                                .setBarcode("sdlkgls3257, sldjgfl547690, sdgnjlk59836798")
                                                .setVendorCode("sdkgfjlsk")
                                                .setShopVendor("Phillips")
                                                .setDescription("Test H125 Description")
                                                .setSupplierMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1214)
                                                                .setSkuName("MarketSku1214")
                                                                .setCategoryId(123)
                                                                .build()
                                                )
                                                .setSupplierMappingStatus(
                                                        MappingProcessingStatus.newBuilder()
                                                                .setStatus(ChangeStatus.REJECTED)
                                                                .build()
                                                )
                                                .build()
                                )
                                .setTotalCount(3)
                                .build()
                );


        String url = String.format("%s", shopSkusAsXlsUrl(CAMPAIGN_ID));
        doReturn(ProtoTestUtil.getProtoMessageByJson(
                SyncChangeOffer.FullOfferResponse.class,
                "proto/containsOffer.json",
                getClass()
        )).when(dataCampShopClient).getOffers(anyLong(), any(), any());
        FunctionalTestHelper.post(url, "{}", byte[].class);
        Mockito.verify(unitedSupplierXlsHelper).fillTemplate(
                Mockito.any(),
                Mockito.argThat(offers -> {
                    if (offers.size() != 3) {
                        return false;
                    } else {
                        List<ru.yandex.market.core.supplier.model.SupplierOffer> offerList = new ArrayList<>(offers);
                        ru.yandex.market.core.supplier.model.SupplierOffer offer1 = offerList.get(0);
                        ru.yandex.market.core.supplier.model.SupplierOffer offer2 = offerList.get(1);
                        ru.yandex.market.core.supplier.model.SupplierOffer offer3 = offerList.get(2);
                        return offer1.getShopSku().equals("H123")
                                && offer1.getName().equals("Test H123")
                                && offer1.getDescription() == null
                                && offer1.getCategory() == null
                                && offer1.getBarCode().equals("")
                                && offer1.getVendor() == null
                                && offer1.getVendorCode() == null
                                && offer1.getMarketSku().equals("1288")
                                && offer1.getErrors().equals("На проверке, SKU на Маркете на проверке: 1214")
                                && offer1.getUrl().equals("http://yandex.ru/123, http://yandex.ru/124")

                                && offer2.getShopSku().equals("H124")
                                && offer2.getName().equals("Test H124")
                                && offer2.getDescription().equals("Test H124 Description")
                                && offer2.getCategory().equals("Shop/Category/Name")
                                && offer2.getBarCode().equals("sdkgjsdh12431254,sdjgh124314231,dskjghs124152")
                                && offer2.getVendor().equals("Apple")
                                && offer2.getVendorCode().equals("sgsd23523")
                                && offer2.getMarketSku().equals("1288")
                                && offer2.getErrors().equals("На проверке, Нужно исправить SKU на Маркете: 1214")

                                && offer3.getShopSku().equals("H125")
                                && offer3.getName().equals("Test H125")
                                && offer3.getDescription().equals("Test H125 Description")
                                && offer3.getCategory().equals("Shop/Category/Name/Other")
                                && offer3.getBarCode().equals("sdlkgls3257,sldjgfl547690,sdgnjlk59836798")
                                && offer3.getVendor().equals("Phillips")
                                && offer3.getVendorCode().equals("sdkgfjlsk")
                                && offer3.getMarketSku() == null
                                && offer3.getErrors().equals("На проверке, Нужно исправить SKU на Маркете: 1214");
                    }
                }),
                Mockito.argThat(List::isEmpty),
                Mockito.any(),
                Mockito.anyBoolean()
        );
        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.hasSupplierId()
                        && request.getSupplierId() == SUPPLIER_ID
                        && !request.hasHasAnyMapping()
                        && request.getHasSupplierMappingStatusList().isEmpty()
                        && request.getHasNoSupplierMappingStatusList().isEmpty()
                        && request.getMarketCategoryIdsList().isEmpty()
                        && !request.hasTextQueryString()
                        && request.hasReturnTotalCount()
                        && !request.getReturnTotalCount()
                        && request.getReturnMasterData()));
    }

    @Test
    @DisplayName("Запрос списка оферов с указанием page_token для выбора порции данных")
    void testListSkusWithPageToken() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(Mockito.any()))
                .thenReturn(MboMappings.SearchMappingsResponse.newBuilder().build());

        String url = String.format("%s?page_token={pageToken}", shopSkusAsXlsUrl(CAMPAIGN_ID));
        FunctionalTestHelper.post(url, "{}", byte[].class, "TKN123");
        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.hasSupplierId()
                        && request.getSupplierId() == SUPPLIER_ID
                        && !request.hasHasAnyMapping()
                        && request.getHasSupplierMappingStatusList().isEmpty()
                        && request.getHasNoSupplierMappingStatusList().isEmpty()
                        && request.getMarketCategoryIdsList().isEmpty()
                        && !request.hasTextQueryString()
                        && request.hasReturnTotalCount()
                        && !request.getReturnTotalCount()
                        && request.getReturnMasterData()));
    }

    @Test
    @DisplayName("Запрос списка оферов с указанием offer_processing_status=READY для выбора порции данных")
    void testListSkusWithApprovedOffersStatusOnly() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(Mockito.any()))
                .thenReturn(MboMappings.SearchMappingsResponse.newBuilder().build());

        String url = shopSkusAsXlsUrl(CAMPAIGN_ID);
        //language=json
        String jsonBody = "{\"offerProcessingStatuses\": [\"READY\"]}";
        FunctionalTestHelper.post(url, jsonBody, byte[].class);
        List<SupplierOffer.OfferProcessingStatus> expectedChangeOfferStatusSet =
                Collections.singletonList(SupplierOffer.OfferProcessingStatus.READY);
        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.hasSupplierId()
                        && request.getSupplierId() == SUPPLIER_ID
                        && request.getOfferProcessingStatusList().equals(expectedChangeOfferStatusSet)
                        && request.getHasAnyMapping() == MboMappings.MappingKind.APPROVED_MAPPING
                        && request.getHasSupplierMappingStatusList().isEmpty()
                        && request.getHasNoSupplierMappingStatusList().isEmpty()
                        && request.getMarketCategoryIdsList().isEmpty()
                        && !request.hasTextQueryString()
                        && request.hasReturnTotalCount()
                        && !request.getReturnTotalCount()
                        && request.getReturnMasterData()));
    }

    @Test
    @DisplayName("Запрос списка оферов с указанием offer_processing_status=IN_WORK"
            + " и offer_processing_status=CONTENT_PROCESSING")
    void testListSkusWithMultipleOffersStatusOnly() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(Mockito.any()))
                .thenReturn(MboMappings.SearchMappingsResponse.newBuilder().build());

        String url = shopSkusAsXlsUrl(CAMPAIGN_ID);
        //language=json
        String jsonBody = "{\"offerProcessingStatuses\": [\"IN_WORK\", \"CONTENT_PROCESSING\"]}";
        doReturn(ProtoTestUtil.getProtoMessageByJson(
                SyncChangeOffer.FullOfferResponse.class,
                "proto/emptyOffers.json",
                getClass()
        )).when(dataCampShopClient).getOffers(anyLong(), any(), any());
        FunctionalTestHelper.post(url, jsonBody, byte[].class);
        Set<SupplierOffer.OfferProcessingStatus> expectedChangeOfferStatusSet =
                new HashSet<>(Arrays.asList(
                        SupplierOffer.OfferProcessingStatus.CONTENT_PROCESSING,
                        SupplierOffer.OfferProcessingStatus.REVIEW,
                        SupplierOffer.OfferProcessingStatus.IN_WORK
                ));
        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.hasSupplierId()
                        && request.getSupplierId() == SUPPLIER_ID
                        && new HashSet<>(request.getOfferProcessingStatusList()).equals(expectedChangeOfferStatusSet)
                        && request.getHasAnyMapping() == MboMappings.MappingKind.APPROVED_MAPPING
                        && request.getHasSupplierMappingStatusList().isEmpty()
                        && request.getHasNoSupplierMappingStatusList().isEmpty()
                        && request.getMarketCategoryIdsList().isEmpty()
                        && !request.hasTextQueryString()
                        && request.hasReturnTotalCount()
                        && !request.getReturnTotalCount()
                        && request.getReturnMasterData()));
    }

    @Test
    @DisplayName("Запрос списка офферов с указанием categoryId")
    void testListSkusWithCategoryId() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(Mockito.any()))
                .thenReturn(MboMappings.SearchMappingsResponse.newBuilder().build());

        String url = shopSkusAsXlsUrl(CAMPAIGN_ID);
        //language=json
        String jsonBody = "{\"categoryIds\": [1421]}";
        FunctionalTestHelper.post(url, jsonBody, byte[].class);
        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.hasSupplierId()
                        && request.getSupplierId() == SUPPLIER_ID
                        && !request.hasHasAnyMapping()
                        && request.getHasSupplierMappingStatusList().isEmpty()
                        && request.getHasNoSupplierMappingStatusList().isEmpty()
                        && request.getMarketCategoryIdsList().equals(Collections.singletonList(1421))
                        && !request.hasTextQueryString()
                        && request.hasReturnTotalCount()
                        && !request.getReturnTotalCount()
                        && request.getReturnMasterData()));
    }

    @Test
    @DisplayName("Запрос списка офферов с указанием категорий через запятую")
    void testListSkusWithMultipleCategoryIdsWithComma() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(Mockito.any()))
                .thenReturn(MboMappings.SearchMappingsResponse.newBuilder().build());

        String url = shopSkusAsXlsUrl(CAMPAIGN_ID);
        //language=json
        String jsonBody = "{\"categoryIds\": [1421, 1427]}";
        FunctionalTestHelper.post(url, jsonBody, byte[].class);
        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.hasSupplierId()
                        && request.getSupplierId() == SUPPLIER_ID
                        && !request.hasHasAnyMapping()
                        && request.getHasSupplierMappingStatusList().isEmpty()
                        && request.getHasNoSupplierMappingStatusList().isEmpty()
                        && request.getMarketCategoryIdsList().equals(Arrays.asList(1421, 1427))
                        && !request.hasTextQueryString()
                        && request.hasReturnTotalCount()
                        && !request.getReturnTotalCount()
                        && request.getReturnMasterData()));
    }

    @Test
    @DisplayName("Запрос списка офферов с указанием нескольких категорий")
    void testListSkusWithMultipleCategoryIds() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(Mockito.any()))
                .thenReturn(MboMappings.SearchMappingsResponse.newBuilder().build());

        String url = shopSkusAsXlsUrl(CAMPAIGN_ID);
        //language=json
        String jsonBody = "{\"categoryIds\": [1421, 1427]}";
        FunctionalTestHelper.post(url, jsonBody);
        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.hasSupplierId()
                        && request.getSupplierId() == SUPPLIER_ID
                        && !request.hasHasAnyMapping()
                        && request.getHasSupplierMappingStatusList().isEmpty()
                        && request.getHasNoSupplierMappingStatusList().isEmpty()
                        && request.getMarketCategoryIdsList().equals(Arrays.asList(1421, 1427))
                        && !request.hasTextQueryString()
                        && request.hasReturnTotalCount()
                        && !request.getReturnTotalCount()
                        && request.getReturnMasterData()));
    }

    @Test
    @DisplayName("Запрос списка офферов по поисковой строке")
    void testListSkusWithSearchQuery() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(Mockito.any()))
                .thenReturn(MboMappings.SearchMappingsResponse.newBuilder().build());

        String url = String.format("%s?q={queryString}", shopSkusAsXlsUrl(CAMPAIGN_ID));
        FunctionalTestHelper.post(url, "{}", byte[].class, "needle");
        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.hasSupplierId()
                        && request.getSupplierId() == SUPPLIER_ID
                        && !request.hasHasAnyMapping()
                        && request.getHasSupplierMappingStatusList().isEmpty()
                        && request.getHasNoSupplierMappingStatusList().isEmpty()
                        && request.getMarketCategoryIdsList().isEmpty()
                        && request.hasTextQueryString()
                        && request.getTextQueryString().equals("needle")
                        && request.hasReturnTotalCount()
                        && !request.getReturnTotalCount()
                        && request.getReturnMasterData()));
    }

    @Test
    @DisplayName("Запрос списка офферов, у которых есть маппинг на PSKU")
    void testListSkusWithPSkuMapping() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(Mockito.any()))
                .thenReturn(MboMappings.SearchMappingsResponse.newBuilder().build());

        String url = shopSkusAsXlsUrl(CAMPAIGN_ID);
        //language=json
        String jsonBody = "{\"contentType\": \"partner\"}";
        FunctionalTestHelper.post(url, jsonBody);
        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.getSupplierId() == SUPPLIER_ID
                        && request.getMappingFiltersList().equals(Collections.singletonList(
                        MboMappings.MappingFilter
                                .newBuilder()
                                .setMappingSkuKind(MboMappings.MappingSkuKind.PARTNER)
                                .build()))
                        && request.getHasSupplierMappingStatusList().isEmpty()
                        && request.getHasNoSupplierMappingStatusList().isEmpty()
                        && request.getMarketCategoryIdsList().isEmpty()
                        && !request.hasTextQueryString()
                        && request.hasReturnTotalCount()
                        && !request.getReturnTotalCount()
                        && request.getReturnMasterData()
        ));
    }

    @Test
    @DisplayName("Запрос списка офферов, у которых есть маппинг на MSKU")
    void testListSkusWithMSkuMapping() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(Mockito.any()))
                .thenReturn(MboMappings.SearchMappingsResponse.newBuilder().build());

        String url = shopSkusAsXlsUrl(CAMPAIGN_ID);
        //language=json
        String jsonBody = "{\"contentType\": \"market\"}";
        FunctionalTestHelper.post(url, jsonBody);
        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.getSupplierId() == SUPPLIER_ID
                        && request.getMappingFiltersList().equals(Collections.singletonList(
                        MboMappings.MappingFilter
                                .newBuilder()
                                .setMappingSkuKind(MboMappings.MappingSkuKind.MARKET)
                                .build()))
                        && request.getHasSupplierMappingStatusList().isEmpty()
                        && request.getHasNoSupplierMappingStatusList().isEmpty()
                        && request.getMarketCategoryIdsList().isEmpty()
                        && !request.hasTextQueryString()
                        && request.hasReturnTotalCount()
                        && !request.getReturnTotalCount()
                        && request.getReturnMasterData()
        ));
    }

    @Test
    @DisplayName("Запрос списка офферов по статусу доступности оффера")
    void testListSkusWithAvailabilityStatus() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(Mockito.any()))
                .thenReturn(MboMappings.SearchMappingsResponse.newBuilder().build());

        String url = shopSkusAsXlsUrl(CAMPAIGN_ID);
        //language=json
        String jsonBody = "{\"availabilityStatuses\": [\"INACTIVE\"]}";
        FunctionalTestHelper.post(url, jsonBody);
        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.getSupplierId() == SUPPLIER_ID
                        && request.getAvailabilityList().equals(
                        Collections.singletonList(SupplierOffer.Availability.INACTIVE))
                        && request.getHasSupplierMappingStatusList().isEmpty()
                        && request.getHasNoSupplierMappingStatusList().isEmpty()
                        && request.getMarketCategoryIdsList().isEmpty()
                        && !request.hasTextQueryString()
                        && request.hasReturnTotalCount()
                        && !request.getReturnTotalCount()
                        && request.getReturnMasterData()
        ));
    }

    @Test
    @DisplayName("Запрос списка офферов по списку производителей")
    void testListSkusWithVendors() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(Mockito.any()))
                .thenReturn(MboMappings.SearchMappingsResponse.newBuilder().build());

        String url = shopSkusAsXlsUrl(CAMPAIGN_ID);
        //language=json
        String jsonBody = "{\"vendors\": [\"Tesla\", \"SpaceX\"]}";
        doReturn(ProtoTestUtil.getProtoMessageByJson(
                SyncChangeOffer.FullOfferResponse.class,
                "proto/emptyOffers.json",
                getClass()
        )).when(dataCampShopClient).getOffers(anyLong(), any(), any());
        FunctionalTestHelper.post(url, jsonBody);
        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.getSupplierId() == SUPPLIER_ID
                        && request.getVendorsList().equals(Arrays.asList("Tesla", "SpaceX"))
                        && request.getAvailabilityList().isEmpty()
                        && request.getHasSupplierMappingStatusList().isEmpty()
                        && request.getHasNoSupplierMappingStatusList().isEmpty()
                        && request.getMarketCategoryIdsList().isEmpty()
                        && !request.hasTextQueryString()
                        && request.hasReturnTotalCount()
                        && !request.getReturnTotalCount()
                        && request.getReturnMasterData()
        ));
    }


    @Test
    @DisplayName("Запрос списка офферов по нескольким статусам доступности оффера")
    void testListSkusWithSeveralAvailabilityStatuses() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(Mockito.any()))
                .thenReturn(MboMappings.SearchMappingsResponse.newBuilder().build());

        String url = shopSkusAsXlsUrl(CAMPAIGN_ID);
        //language=json
        String jsonBody = "{\"availabilityStatuses\": [\"INACTIVE\", \"DELISTED\"]}";
        FunctionalTestHelper.post(url, jsonBody);
        Mockito.verify(patientMboMappingsService).searchMappingsByShopId(ArgumentMatchers.argThat(request ->
                request.getSupplierId() == SUPPLIER_ID
                        && new HashSet<>(request.getAvailabilityList()).equals(
                        ImmutableSet.of(SupplierOffer.Availability.INACTIVE, SupplierOffer.Availability.DELISTED))
                        && request.getHasSupplierMappingStatusList().isEmpty()
                        && request.getHasNoSupplierMappingStatusList().isEmpty()
                        && request.getMarketCategoryIdsList().isEmpty()
                        && !request.hasTextQueryString()
                        && request.hasReturnTotalCount()
                        && !request.getReturnTotalCount()
                        && request.getReturnMasterData()
        ));
    }

    @DisplayName("Заполнение цен из ответа индексатора для фидов пуш-партнера")
    @Test
    @DbUnitDataSet(before = "GetShopSkusAsXlsMappingControllerFunctionalTest.before.csv")
    void pushPartnerFeed() throws IOException {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(Mockito.any()))
                .thenReturn(
                        MboMappings.SearchMappingsResponse.newBuilder()
                                .addOffers(
                                        SupplierOffer.Offer.newBuilder()
                                                .setTitle("Test H123")
                                                .setSupplierId(99774L)
                                                .setShopSkuId("1")
                                                .build()
                                )
                                .addOffers(
                                        SupplierOffer.Offer.newBuilder()
                                                .setTitle("Absent in Datacamp")
                                                .setSupplierId(99774L)
                                                .setShopSkuId("Not in Datacamp")
                                                .build())
                                .setTotalCount(2)
                                .build()
                );

        String url = String.format("%s", shopSkusAsXlsUrl(555L));
        doReturn(ProtoTestUtil.getProtoMessageByJson(
                SyncChangeOffer.FullOfferResponse.class,
                "proto/containsOffer.json",
                getClass()
        )).when(dataCampShopClient).getOffers(anyLong(), any(), any());
        FunctionalTestHelper.post(url, "{}", byte[].class);
        Mockito.verify(unitedSupplierXlsHelper).fillTemplate(
                Mockito.any(),
                Mockito.argThat(offers -> {
                    if (offers.size() != 2) {
                        return false;
                    } else {
                        ru.yandex.market.core.supplier.model.SupplierOffer offer = new ArrayList<>(offers).get(0);
                        ru.yandex.market.core.supplier.model.SupplierOffer empty = new ArrayList<>(offers).get(1);
                        return offer.getShopSku().equals("1")
                                && offer.getName().equals("Test H123")
                                && offer.getPrice().compareTo(new BigDecimal(3)) == 0
                                && offer.getOldPrice().compareTo(new BigDecimal(2)) == 0
                                && offer.getVat().equals(VatRate.VAT_20)
                                && offer.isDisabled()
                                && empty.getShopSku().equals("Not in Datacamp")
                                && empty.getPrice() == null;
                    }
                }),
                Mockito.argThat(List::isEmpty),
                Mockito.any(),
                Mockito.anyBoolean()
        );
    }

    @DisplayName("Проверка заполнения полей саджестов от MBOC")
    @Test
    @Disabled//Разобраться позже
    @DbUnitDataSet(before = "GetShopSkusAsXlsMappingControllerFunctionalTest.before.csv")
    void checkMbocSuggest() throws IOException {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(Mockito.any()))
                .thenReturn(
                        MboMappings.SearchMappingsResponse.newBuilder()
                                .addOffers(
                                        SupplierOffer.Offer.newBuilder()
                                                .setTitle("Offer with suggest")
                                                .setSupplierId(99774L)
                                                .setShopSkuId("OfferWithSuggest")
                                                .setSuggestMapping(SupplierOffer.Mapping.newBuilder()
                                                        .setCategoryId(1L)
                                                        .setCategoryName("Товары для эльфов")
                                                        .setSkuId(123454321L)
                                                        .setSkuName("Эльфийский амулет от проклятий 15 см серебро")
                                                        .build())
                                                .setProcessingStatus(SupplierOffer.OfferProcessingStatus.NEED_MAPPING)
                                                .build()
                                )
                                .addOffers(
                                        SupplierOffer.Offer.newBuilder()
                                                .setTitle("Offer without suggest")
                                                .setSupplierId(99774L)
                                                .setShopSkuId("OfferWithoutSuggest")
                                                .setProcessingStatus(SupplierOffer.OfferProcessingStatus.NEED_MAPPING)
                                                .build())
                                .setTotalCount(2)
                                .build()
                );

        String url = String.format("%s", shopSkusAsXlsUrl(555L));
        doReturn(ProtoTestUtil.getProtoMessageByJson(
                SyncChangeOffer.FullOfferResponse.class,
                "proto/containsOffer.json",
                getClass()
        )).when(dataCampShopClient).getOffers(anyLong(), any(), any());
        ResponseEntity<byte[]> responseEntity = FunctionalTestHelper.post(url, "{}", byte[].class);
        ByteArrayInputStream is = new ByteArrayInputStream(Objects.requireNonNull(responseEntity.getBody()));

        //проверяем, что в итоговую эксельку записалась вся необходимая информация
        try (Workbook workbook = WorkbookFactory.create(is)) {
            Sheet actualSheet = workbook.getSheetAt(1);

            Map<Integer, Set<String>> expectedRows = ImmutableMap.of(
                    1, ImmutableSet.of("Offer with suggest", "OfferWithSuggest",
                            "Товары для эльфов", "Эльфийский амулет от проклятий 15 см серебро", "123454321"),
                    2, ImmutableSet.of("Offer without suggest", "OfferWithoutSuggest",
                            "Найдите карточку")
            );

            //количество строк заголовка
            int offset = 4 ;

            for (int i = 1; i < 3; i++) {
                Row row = actualSheet.getRow(i + offset);
                Set<String> cells = new HashSet<>();
                for (Iterator<Cell> cellIterator = row.cellIterator(); cellIterator.hasNext(); ) {
                    Cell next = cellIterator.next();
                    cells.add(next.getStringCellValue());
                }
                Assert.assertTrue(cells.containsAll(expectedRows.get(i)));
            }
        }
    }
}
