package ru.yandex.market.ff.enrichment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.client.enums.RequestItemErrorAttributeType;
import ru.yandex.market.ff.client.enums.RequestItemErrorType;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.client.enums.StockType;
import ru.yandex.market.ff.client.enums.VatRate;
import ru.yandex.market.ff.model.bo.EnrichmentResultContainer;
import ru.yandex.market.ff.model.bo.InboundAllowance;
import ru.yandex.market.ff.model.bo.RequestItemErrorInfo;
import ru.yandex.market.ff.model.bo.SupplierContentMapping;
import ru.yandex.market.ff.model.bo.SupplierSkuKey;
import ru.yandex.market.ff.model.entity.RequestItem;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.model.entity.SupplierBusinessType;
import ru.yandex.market.ff.repository.RequestItemCargoTypesRepository;
import ru.yandex.market.ff.repository.RequestItemMarketBarcodeRepository;
import ru.yandex.market.ff.repository.RequestItemMarketVendorCodeRepository;
import ru.yandex.market.ff.service.RequestItemService;
import ru.yandex.market.ff.service.ShopRequestFetchingService;
import ru.yandex.market.ff.service.StockService;
import ru.yandex.market.ff.service.SupplierMappingService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit тесты для {@link TransferItemsEnricher}.
 */
class TransferItemsEnricherTest {

    private static final long ITEM_ID_1 = 1L;
    private static final long ITEM_ID_2 = 2L;
    private static final long ITEM_ID_3 = 3L;

    private static final long TRANSFER_ID = 1L;
    private static final long INBOUND_ID = 10L;

    private static final String ARTICLE_1 = "art1";
    public static final SupplierContentMapping MAPPING_1 = SupplierContentMapping.builder(ARTICLE_1, 1L, "title")
        .setBoxCount(5)
        .setMarketName("some market name")
        .setInboundAllowance(InboundAllowance.inboundAllowed())
        .setCargoTypes(ImmutableSet.of(10, 20))
        .setInboundRemainingLifetimeDays(10)
        .setOutboundRemainingLifetimeDays(15)
        .setInboundRemainingLifetimePercentage(null)
        .setOutboundRemainingLifetimePercentage(25)
        .setHasExpirationDate(true)
        .build();
    private static final String ARTICLE_2 = "art2";
    public static final SupplierContentMapping MAPPING_2 = SupplierContentMapping.builder(ARTICLE_2, 1L, "title")
        .setBoxCount(1)
        .setPackageNumInSpike(5)
        .setMarketName("some market name")
        .setInboundAllowance(InboundAllowance.inboundAllowed())
        .setCargoTypes(ImmutableSet.of(20, 30, 40, 50))
        .setInboundRemainingLifetimeDays(10)
        .setOutboundRemainingLifetimeDays(15)
        .setInboundRemainingLifetimePercentage(20)
        .setOutboundRemainingLifetimePercentage(25)
        .build();
    private static final String ARTICLE_3 = "art3";

    public static final int SUPPLIER_ID = 10;
    public static final long SERVICE_ID = 1L;

    public static final LocalDateTime REQUESTED_DATE = LocalDateTime.now();

    private static final ShopRequest TRANSFER =
        createTransfer(TRANSFER_ID, INBOUND_ID, StockType.SURPLUS, StockType.FIT);
    private static final ShopRequest TRANSFER_FROM_FIT_TO_DEFECT = createTransfer(TRANSFER_ID, INBOUND_ID,
        StockType.FIT, StockType.DEFECT);
    private static final ShopRequest INBOUND = createInbound(INBOUND_ID, RequestStatus.FINISHED);
    private static final ShopRequest NOT_FINISHED_INBOUND = createInbound(INBOUND_ID, RequestStatus.PROCESSED);

    private static final RequestItem INBOUND_ITEM_1 =
        inboundItem(ITEM_ID_1, ARTICLE_1, "item1", 123, 1);
    private static final RequestItem INBOUND_ITEM_2 =
        inboundItem(ITEM_ID_2, ARTICLE_2, "item2", 321, 0);

    private static final RequestItem NICE_TRANSFER_ITEM =
        item(ITEM_ID_1, ARTICLE_1, 1, 0, null);

    private static final RequestItem WRONG_TRANSFER_ITEM =
        item(ITEM_ID_1, ARTICLE_1, 100, 0, null);

    private static final RequestItem ALIEN_TRANSFER_ITEM =
        item(ITEM_ID_3, ARTICLE_3, 1, 0, null);

    private static final RequestItem ITEM_WITHOUT_FACT_COUNT_AND_SURPLUS_COUNT =
        item(ITEM_ID_1, ARTICLE_1, 1, null, null);

    private TransferItemsEnricher enricher;

    private StockService stockService;

    private ShopRequestFetchingService shopRequestFetchingService;
    private RequestItemService requestItemService;

    @BeforeEach
    void init() {
        requestItemService = mock(RequestItemService.class);
        when(requestItemService.findLastSuppliedItemsBySupplierIdAndArticle(any(), anyList(), any()))
            .thenReturn(Arrays.asList(INBOUND_ITEM_1, INBOUND_ITEM_2));

        when(requestItemService.findAllByRequestId(INBOUND_ID))
            .thenReturn(Arrays.asList(INBOUND_ITEM_1, INBOUND_ITEM_2));

        stockService = mock(StockService.class);

        shopRequestFetchingService = mock(ShopRequestFetchingService.class);

        when(shopRequestFetchingService.getRequest(INBOUND_ID))
            .thenReturn(Optional.of(INBOUND));

        SupplierMappingService supplierMappingService = mock(SupplierMappingService.class);
        when(supplierMappingService.getMarketSkuMapping(SUPPLIER_ID, SERVICE_ID, REQUESTED_DATE,
            Set.of(ARTICLE_1))).thenReturn(ImmutableMap.of(new SupplierSkuKey(SUPPLIER_ID, ARTICLE_1), MAPPING_1));
        when(supplierMappingService.getMarketSkuMapping(SUPPLIER_ID, SERVICE_ID, REQUESTED_DATE,
            Set.of(ARTICLE_2))).thenReturn(ImmutableMap.of(new SupplierSkuKey(SUPPLIER_ID, ARTICLE_2), MAPPING_2));

        enricher = new TransferItemsEnricher(requestItemService,
            supplierMappingService,
            shopRequestFetchingService,
            mock(RequestItemMarketVendorCodeRepository.class),
            mock(RequestItemMarketBarcodeRepository.class),
            mock(RequestItemCargoTypesRepository.class),
            Set.of(RequestType.SUPPLY, RequestType.CROSSDOCK),
            new FreezeStockEnricherService(stockService));
    }

    /**
     * Если все хорошо, то должен инициироваться фриз.
     */
    @Test
    void enrichAllItemsWasInTransfer() {

        when(stockService.freezeOnStock(any(), anyList()))
            .thenReturn(Collections.emptyList());

        final Map<Long, EnrichmentResultContainer> errors = enricher.enrichSafe(TRANSFER,
            Collections.singletonList(NICE_TRANSFER_ITEM));

        verify(stockService, times(1)).freezeOnStock(any(), anyList());

        // Проверить ошибки
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(errors).isEmpty();
        });

        verify(requestItemService, times(1)).findLastSuppliedItemsBySupplierIdAndArticle(any(), anyList(), any());
        verify(requestItemService, times(1)).findAllByRequestId(INBOUND_ID);
        verify(shopRequestFetchingService, times(1)).getRequest(INBOUND_ID);
        verifyZeroInteractions(requestItemService, shopRequestFetchingService, stockService);
    }

    /**
     * Если создается перемещение для незавершенной поставки, то фриз не должен инициироваться.
     */
    @Test
    void enrichNotFinishedInbound() {
        when(shopRequestFetchingService.getRequest(INBOUND_ID))
            .thenReturn(Optional.of(NOT_FINISHED_INBOUND));

        Map<Long, EnrichmentResultContainer> errors = enricher.enrichSafe(TRANSFER,
            Collections.singletonList(NICE_TRANSFER_ITEM));

        verify(requestItemService, times(1)).findLastSuppliedItemsBySupplierIdAndArticle(any(), anyList(), any());
        verify(shopRequestFetchingService, times(1)).getRequest(INBOUND_ID);
        verifyZeroInteractions(requestItemService, shopRequestFetchingService, stockService);

        // Проверить ошибки
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(errors).hasSize(1);
            EnrichmentResultContainer errorsContainer = errors.get(ITEM_ID_1);
            softly.assertThat(errorsContainer).isNotNull();
            Set<RequestItemErrorInfo> validationErrors = errorsContainer.getValidationErrors();
            softly.assertThat(validationErrors).hasSize(1);

            Optional<RequestItemErrorInfo> infoOptional = validationErrors.stream()
                .findAny();
            softly.assertThat(infoOptional).isPresent();

            softly.assertThat(infoOptional.get()).isEqualTo(
                RequestItemErrorInfo.of(RequestItemErrorType.TRANSFER_FOR_NOT_FINISHED_INBOUND)
            );
        });
    }

    /**
     * Если у товара в перемещении отсутствует factCount, но он необходим, то фриз не должен инициироваться.
     */
    @Test
    void enrichItemShouldHaveFactCountButHaveNot() {
        assertFailWhenThereIsNoAvailableCountForTransferItem(TRANSFER_FROM_FIT_TO_DEFECT);
    }

    /**
     * Если у товара в перемещении отсутствует surplusCount, но он необходим, то фриз не должен инициироваться.
     */
    @Test
    void enrichItemShouldHaveSurplusCountButHaveNot() {
        assertFailWhenThereIsNoAvailableCountForTransferItem(TRANSFER);
    }

    private void assertFailWhenThereIsNoAvailableCountForTransferItem(ShopRequest transfer) {
        when(shopRequestFetchingService.getRequest(INBOUND_ID))
            .thenReturn(Optional.of(INBOUND));

        when(requestItemService.findAllByRequestId(INBOUND_ID))
            .thenReturn(Collections.singletonList(ITEM_WITHOUT_FACT_COUNT_AND_SURPLUS_COUNT));

        Map<Long, EnrichmentResultContainer> errors = enricher.enrichSafe(transfer,
            Collections.singletonList(NICE_TRANSFER_ITEM));

        verify(stockService, never()).freezeOnStock(any(), anyList());

        // Проверить ошибки
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(errors).hasSize(1);
            EnrichmentResultContainer errorsContainer = errors.get(ITEM_ID_1);
            softly.assertThat(errorsContainer).isNotNull();
            Set<RequestItemErrorInfo> validationErrors = errorsContainer.getValidationErrors();
            softly.assertThat(validationErrors).hasSize(1);

            Optional<RequestItemErrorInfo> infoOptional = validationErrors.stream()
                .findAny();
            softly.assertThat(infoOptional).isPresent();

            softly.assertThat(infoOptional.get()).isEqualTo(
                RequestItemErrorInfo.of(RequestItemErrorType.NO_AVAILABLE_COUNT_FOR_TRANSFER_INBOUND)
            );
        });
    }

    /**
     * Если запрашиваем больше чем было, то фриз не должен инициироваться.
     */
    @Test
    void enrichNotAllItemsWasInInbound() {
        final List<RequestItem> items = Collections.singletonList(WRONG_TRANSFER_ITEM);

        final Map<Long, EnrichmentResultContainer> errors = enricher.enrichSafe(TRANSFER, items);

        verify(stockService, never()).freezeOnStock(any(), anyList());

        // Проверить ошибки
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(errors).hasSize(1);
            EnrichmentResultContainer errorsContainer = errors.get(ITEM_ID_1);
            softly.assertThat(errorsContainer).isNotNull();

            Set<RequestItemErrorInfo> validationErrors = errorsContainer.getValidationErrors();
            softly.assertThat(validationErrors).hasSize(1);

            Optional<RequestItemErrorInfo> infoOptional = validationErrors.stream()
                .findAny();
            softly.assertThat(infoOptional).isPresent();

            softly.assertThat(infoOptional.get()).isEqualTo(
                RequestItemErrorInfo.of(RequestItemErrorType.NOT_ENOUGH_ON_STOCK,
                    Collections.singletonMap(RequestItemErrorAttributeType.ACTUAL_SUPPLY_ITEMS_COUNT, "10")
                )
            );
        });
    }

    /**
     * Если в маппинге от МБО есть не все строки заявки, то фриз не должен инициироваться.
     */
    @Test
    void enrichItemDoesNotExistInInbound() {
        final List<RequestItem> items = Collections.singletonList(ALIEN_TRANSFER_ITEM);

        final Map<Long, EnrichmentResultContainer> errors = enricher.enrichSafe(TRANSFER, items);

        verify(stockService, never()).freezeOnStock(any(), anyList());

        // Проверить ошибки
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(errors).hasSize(1);
            EnrichmentResultContainer errorsContainer = errors.get(ITEM_ID_3);
            softly.assertThat(errorsContainer).isNotNull();
            Set<RequestItemErrorInfo> validationErrors = errorsContainer.getValidationErrors();
            softly.assertThat(validationErrors).hasSize(2);

            softly.assertThat(validationErrors).contains(
                RequestItemErrorInfo.of(RequestItemErrorType.NO_MARKET_SKU_MAPPING_FOUND),
                RequestItemErrorInfo.of(RequestItemErrorType.NO_PREVIOUS_SUPPLY_WITH_ARTICLE_AND_SUPPLIER_FOUND)
            );
        });
    }

    private static ShopRequest createTransfer(Long transferId, Long inboundId, StockType from, StockType to) {
        final ShopRequest shopRequest = new ShopRequest();
        shopRequest.setId(transferId);
        shopRequest.setType(RequestType.TRANSFER);
        shopRequest.setStockType(from);
        shopRequest.setStockTypeTo(to);
        shopRequest.setSupplier(new Supplier(SUPPLIER_ID, null, null, null, null, new SupplierBusinessType()));
        shopRequest.setServiceId(SERVICE_ID);
        shopRequest.setRequestedDate(REQUESTED_DATE);
        shopRequest.setInboundId(inboundId);
        return shopRequest;
    }


    private static ShopRequest createInbound(Long inboundId, RequestStatus requestStatus) {
        final ShopRequest shopRequest = new ShopRequest();
        shopRequest.setId(inboundId);
        shopRequest.setType(RequestType.SUPPLY);
        shopRequest.setStatus(requestStatus);
        shopRequest.setSupplier(new Supplier(10, null, null, null, null, new SupplierBusinessType()));
        shopRequest.setServiceId(SERVICE_ID);
        shopRequest.setItems(Arrays.asList(INBOUND_ITEM_1, INBOUND_ITEM_2));
        return shopRequest;
    }

    private static RequestItem inboundItem(long id, String article, String name, long sku, Integer surplusCount) {
        final RequestItem item = new RequestItem();
        item.setId(id);
        item.setArticle(article);
        item.setName(name);
        item.setBarcodes(Arrays.asList("bar1", "bar2"));
        item.setVatRate(VatRate.VAT_18);
        item.setSku(sku);
        item.setSupplyPrice(new BigDecimal("110.10"));
        item.setUntaxedPrice(new BigDecimal("100"));
        item.setBoxCount(12);
        item.setHasExpirationDate(false);
        item.setSurplusCount(surplusCount);
        item.setCount(10);
        item.setFactCount(10);
        return item;
    }

    private static RequestItem item(final long id, final String article, int count, Integer factCount,
                                    Integer surplusCount) {
        final RequestItem requestItem = new RequestItem();
        requestItem.setId(id);
        requestItem.setArticle(article);
        requestItem.setCount(count);
        requestItem.setSurplusCount(surplusCount);
        requestItem.setFactCount(factCount);
        return requestItem;
    }
}
