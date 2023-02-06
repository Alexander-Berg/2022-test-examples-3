package ru.yandex.market.ff.enrichment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.ff.client.enums.RequestItemErrorType;
import ru.yandex.market.ff.client.enums.VatRate;
import ru.yandex.market.ff.enrichment.builder.CheckRequestOnFulfillmentRequestItemsEnrichBuilder;
import ru.yandex.market.ff.model.bo.EnrichmentResultContainer;
import ru.yandex.market.ff.model.bo.RequestItemErrorInfo;
import ru.yandex.market.ff.model.bo.SupplierContentMapping;
import ru.yandex.market.ff.model.bo.SupplierSkuKey;
import ru.yandex.market.ff.model.entity.RequestItem;
import ru.yandex.market.ff.model.entity.RequestItemMarketData;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.model.entity.SupplierBusinessType;
import ru.yandex.market.ff.repository.RequestItemCargoTypesRepository;
import ru.yandex.market.ff.repository.RequestItemMarketBarcodeRepository;
import ru.yandex.market.ff.repository.RequestItemMarketVendorCodeRepository;
import ru.yandex.market.ff.service.CisValidationService;
import ru.yandex.market.ff.service.LmsClientCachingService;
import ru.yandex.market.ff.service.RequestItemService;
import ru.yandex.market.ff.service.StockService;
import ru.yandex.market.ff.service.SupplierMappingService;

import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OldMovementSupplyRequestItemsEnricherTest {

    private static final long ID_1 = 10L;
    private static final String ARTICLE_1 = "article1";
    private static final String ARTICLE_WITH_WRONG_SN_PARAMS = "article_with_wrong_sn_params";
    private static final String ARTICLE_WITH_WRONG_MAPPING = "article_with_wrong_mapping";
    private static final int SKU_AVAILABILITY_ACTIVE = 0;
    private static final Long MARSCHROUTE_ID = 145L;
    private static final Long INFOR_TOM_ID = 171L;

    private SupplierMappingService supplierMappingService = Mockito.mock(SupplierMappingService.class);
    private RequestItemService requestItemService = Mockito.mock(RequestItemService.class);
    private StockService stockService = Mockito.mock(StockService.class);
    private LmsClientCachingService lmsClientCachingService = Mockito.mock(LmsClientCachingService.class);
    private RequestItemMarketBarcodeRepository requestItemMarketBarcodeRepository =
            Mockito.mock(RequestItemMarketBarcodeRepository.class);
    private RequestItemMarketVendorCodeRepository requestItemMarketVendorCodeRepository =
            Mockito.mock(RequestItemMarketVendorCodeRepository.class);
    private RequestItemCargoTypesRepository requestItemCargoTypesRepository =
            Mockito.mock(RequestItemCargoTypesRepository.class);
    private CisValidationService cisValidationService = Mockito.mock(CisValidationService.class);
    private CheckRequestOnFulfillmentRequestItemsEnrichBuilder requestItemsEnrichBuilder =
            Mockito.mock(CheckRequestOnFulfillmentRequestItemsEnrichBuilder.class);
    private OldMovementSupplyRequestItemsEnricher enricher = new OldMovementSupplyRequestItemsEnricher(
        supplierMappingService,
        requestItemService,
        stockService,
        requestItemMarketVendorCodeRepository,
        requestItemMarketBarcodeRepository,
        requestItemCargoTypesRepository,
        cisValidationService,
        requestItemsEnrichBuilder
    );

    @BeforeEach
    void init() {
        when(requestItemService.findArticlesOfAlreadyAcceptedItems(anyLong(), anyLong(), anyList()))
                .thenReturn(Collections.emptySet());
    }

    @AfterEach
    public void invalidateCache() {
        lmsClientCachingService.invalidateCache();
    }


    /**
     * Проверяет, что проверки РАЗУМа не выполняются, поставка возможна.
     */
    @Test
    public void shouldNotRunMBOValidations() {
        final RequestItem item = item(VatRate.VAT_20, "100");
        item.setArticle(ARTICLE_1);

        ShopRequest request = request(SupplierType.THIRD_PARTY, null);
        request.setServiceId(INFOR_TOM_ID);

        final Map<SupplierSkuKey, SupplierContentMapping> mapping =
                Map.of(new SupplierSkuKey(1, ARTICLE_1), SupplierContentMapping.builder(ARTICLE_1, 1L, "title")
                        .setMarketCategoryId(1337L).build());
        when(supplierMappingService.getMarketSkuMapping(anyLong(), anyLong(), any(LocalDateTime.class), anySet()))
                .thenReturn(mapping);

        final Map<Long, EnrichmentResultContainer> errors =
                enricher.enrichSafe(request, Collections.singletonList(item));

        SoftAssertions.assertSoftly(assertions ->
                assertions.assertThat(errors).isEmpty());

        verify(supplierMappingService).getMarketSkuMapping(anyLong(), anyLong(), any(LocalDateTime.class), anySet());
        verify(lmsClientCachingService, times(0)).getCargoTypesForService(anyLong());
    }

    @Test
    public void getErrorOnWrongMapping() {
        final RequestItem item = item(VatRate.VAT_20, "100");
        item.setArticle(ARTICLE_WITH_WRONG_MAPPING);

        when(supplierMappingService.getMarketSkuMapping(anyLong(), anyLong(), any(LocalDateTime.class), anySet()))
                .thenReturn(emptyMap());
        final Map<Long, EnrichmentResultContainer> errors =
                enricher.enrichSafe(request(SupplierType.THIRD_PARTY, null), Collections.singletonList(item));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(errors).isNotEmpty();
            softly.assertThat(errors).hasSize(1);
            EnrichmentResultContainer itemErrorsContainer = errors.get(item.getId());
            softly.assertThat(itemErrorsContainer).isNotNull();
            softly.assertThat(itemErrorsContainer.getExternalErrors()).isEmpty();
            softly.assertThat(itemErrorsContainer.getValidationErrors()).hasSize(2);
            softly.assertThat(
                itemErrorsContainer.getValidationErrors().stream()
                    .map(RequestItemErrorInfo::getType).collect(Collectors.toList())
            ).containsExactly(RequestItemErrorType.NO_MARKET_SKU_MAPPING_FOUND,
                RequestItemErrorType.NO_PREVIOUS_SUPPLY_WITH_ARTICLE_AND_SUPPLIER_FOUND);
        });
        verify(supplierMappingService).getMarketSkuMapping(anyLong(), anyLong(), any(LocalDateTime.class), anySet());
    }

    @Test
    public void fallbackOnWrongMapping() {
        final RequestItem item = item(VatRate.VAT_20, "100");
        item.setArticle(ARTICLE_WITH_WRONG_MAPPING);

        when(supplierMappingService.getMarketSkuMapping(anyLong(), anyLong(), any(LocalDateTime.class), anySet()))
                .thenReturn(emptyMap());
        RequestItem oldItem = item(VatRate.VAT_10, "300");
        oldItem.setArticle(ARTICLE_WITH_WRONG_MAPPING);
        when(requestItemService.findLastSuppliedItemsBySupplierIdAndArticle(any(), anyList(), any()))
            .thenReturn(List.of(oldItem));
        when(requestItemService.buildMarketDataFromItem(any(), any()))
            .thenReturn(new RequestItemMarketData(List.of(), List.of()));
        final Map<Long, EnrichmentResultContainer> errors =
                enricher.enrichSafe(request(SupplierType.THIRD_PARTY, null), Collections.singletonList(item));

        SoftAssertions.assertSoftly(softly -> softly.assertThat(errors).isEmpty());
        SoftAssertions.assertSoftly(softly -> softly.assertThat(item.getVatRate()).isEqualTo(VatRate.VAT_10));
        SoftAssertions.assertSoftly(softly -> softly.assertThat(item.getSupplyPrice()).isEqualTo(new BigDecimal(300)));
        verify(supplierMappingService).getMarketSkuMapping(anyLong(), anyLong(), any(LocalDateTime.class), anySet());
    }

    @Test
    public void getErrorOnWrongSnFieldsCombination() {
        final RequestItem item = item(VatRate.VAT_20, "100");
        item.setArticle(ARTICLE_WITH_WRONG_SN_PARAMS);

        final Map<SupplierSkuKey, SupplierContentMapping> mapping =
                Map.of(new SupplierSkuKey(1, ARTICLE_WITH_WRONG_SN_PARAMS), SupplierContentMapping.builder(
                        ARTICLE_WITH_WRONG_SN_PARAMS, 1L, "title")
                        .setBoxCount(1)
                        .setMarketName("some market name")
                        .setOfferAvailability(SKU_AVAILABILITY_ACTIVE)
                        .setCheckSn(1)// and do not set SnMask
                        .build());
        when(supplierMappingService.getMarketSkuMapping(anyLong(), anyLong(), any(LocalDateTime.class), anySet()))
                .thenReturn(mapping);

        final Map<Long, EnrichmentResultContainer> errors =
                enricher.enrichSafe(request(SupplierType.THIRD_PARTY, null), Collections.singletonList(item));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(errors).isNotEmpty();
            softly.assertThat(errors).hasSize(1);
            EnrichmentResultContainer itemErrorsContainer = errors.get(item.getId());
            softly.assertThat(itemErrorsContainer).isNotNull();
            softly.assertThat(itemErrorsContainer.getExternalErrors()).isEmpty();
            softly.assertThat(itemErrorsContainer.getValidationErrors()).hasSize(2);
            softly.assertThat(
                itemErrorsContainer.getValidationErrors().stream()
                    .map(RequestItemErrorInfo::getType).collect(Collectors.toList())
            ).containsExactly(RequestItemErrorType.INCORRECT_SERIALNUM_OR_IMEI_FIELDS,
                RequestItemErrorType.NO_PREVIOUS_SUPPLY_WITH_ARTICLE_AND_SUPPLIER_FOUND);
        });
        verify(supplierMappingService).getMarketSkuMapping(anyLong(), anyLong(), any(LocalDateTime.class), anySet());
    }

    @Test
    public void getErrorOnBoxCountWithSpikeItem() {
        final RequestItem item = item(VatRate.VAT_20, "100");
        item.setArticle(ARTICLE_1);

        final Map<SupplierSkuKey, SupplierContentMapping> mapping =
                Map.of(new SupplierSkuKey(1, ARTICLE_1), SupplierContentMapping.builder(
                        ARTICLE_1, 1L, "title")
                        .setPackageNumInSpike(2)
                        .setBoxCount(2)
                        .setMarketName("some market name")
                        .setOfferAvailability(SKU_AVAILABILITY_ACTIVE)
                        .build());
        when(supplierMappingService.getMarketSkuMapping(anyLong(), anyLong(), any(LocalDateTime.class), anySet()))
                .thenReturn(mapping);

        final Map<Long, EnrichmentResultContainer> errors =
                enricher.enrichSafe(request(SupplierType.THIRD_PARTY, null), Collections.singletonList(item));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(errors).isNotEmpty();
            softly.assertThat(errors).hasSize(1);
            EnrichmentResultContainer itemErrorsContainer = errors.get(item.getId());
            softly.assertThat(itemErrorsContainer).isNotNull();
            softly.assertThat(itemErrorsContainer.getExternalErrors()).isEmpty();
            softly.assertThat(itemErrorsContainer.getValidationErrors()).hasSize(2);
            softly.assertThat(
                itemErrorsContainer.getValidationErrors().stream()
                    .map(RequestItemErrorInfo::getType).collect(Collectors.toList())
            ).containsExactly(RequestItemErrorType.BOX_COUNT_NOT_EQUAL_TO_ONE_FOR_SPIKE_ITEM,
                RequestItemErrorType.NO_PREVIOUS_SUPPLY_WITH_ARTICLE_AND_SUPPLIER_FOUND);
        });
        verify(supplierMappingService).getMarketSkuMapping(anyLong(), anyLong(), any(LocalDateTime.class), anySet());
    }

    private static ShopRequest request(SupplierType supplierType, String comment) {
        final ShopRequest request = new ShopRequest();
        request.setId(ID_1);
        request.setSupplier(new Supplier(1, "supplier1", null, null, supplierType, new SupplierBusinessType()));
        request.setRequestedDate(LocalDateTime.of(1984, 1, 1, 0, 0));
        request.setComment(comment);
        request.setServiceId(MARSCHROUTE_ID);
        return request;
    }

    private static RequestItem item(final VatRate vatRate, final String supplyPrice) {
        final RequestItem requestItem = new RequestItem();
        requestItem.setId(ID_1);
        requestItem.setArticle(ARTICLE_1);
        requestItem.setVatRate(vatRate);
        requestItem.setSupplyPrice(new BigDecimal(supplyPrice));
        requestItem.setCount(1);
        return requestItem;
    }
}
