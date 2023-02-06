package ru.yandex.market.ff.enrichment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Tuple3;
import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.ff.client.enums.VatRate;
import ru.yandex.market.ff.enums.WarehouseServiceType;
import ru.yandex.market.ff.model.bo.InboundAllowance;
import ru.yandex.market.ff.model.bo.SupplierContentMapping;
import ru.yandex.market.ff.model.bo.SupplierSkuKey;
import ru.yandex.market.ff.model.entity.RequestItem;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.model.entity.SupplierBusinessType;
import ru.yandex.market.ff.repository.RequestItemAssortmentSkuRepository;
import ru.yandex.market.ff.repository.RequestItemCargoTypesRepository;
import ru.yandex.market.ff.repository.RequestItemMarketBarcodeRepository;
import ru.yandex.market.ff.repository.RequestItemMarketVendorCodeRepository;
import ru.yandex.market.ff.repository.RequestItemRepository;
import ru.yandex.market.ff.service.CisValidationService;
import ru.yandex.market.ff.service.EnvironmentParamService;
import ru.yandex.market.ff.service.RequestItemService;
import ru.yandex.market.ff.service.StockService;
import ru.yandex.market.ff.service.SupplierMappingService;
import ru.yandex.market.ff.service.implementation.AssortmentEnrichServiceImpl;
import ru.yandex.market.ff.service.implementation.AssortmentServiceImpl;
import ru.yandex.market.ff.service.implementation.CisValidationServiceImpl;
import ru.yandex.market.ff.service.implementation.ConcreteEnvironmentParamServiceImpl;
import ru.yandex.market.ff.service.util.AssortmentEnrichService;
import ru.yandex.market.ff.service.util.AssortmentValidateService;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.MbocCommon;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class InventoryingSupplyRequestItemsEnricherTest {

    private static final String ARTICLE_1 = "article1";
    private static final String ARTICLE_3 = "article3";
    private static final long ID_1 = 10L;
    private static final long MARSCHROUTE_ID = 145;

    private final ConcreteEnvironmentParamServiceImpl environmentParamService =
            Mockito.spy(new ConcreteEnvironmentParamServiceImpl(Mockito.mock(EnvironmentParamService.class)));
    private final SupplierMappingService supplierMappingService = Mockito.mock(SupplierMappingService.class);
    private final RequestItemService requestItemService = Mockito.mock(RequestItemService.class);
    private final StockService stockService = Mockito.mock(StockService.class);
    private final RequestItemMarketBarcodeRepository requestItemMarketBarcodeRepository =
            Mockito.mock(RequestItemMarketBarcodeRepository.class);
    private final RequestItemMarketVendorCodeRepository requestItemMarketVendorCodeRepository =
            Mockito.mock(RequestItemMarketVendorCodeRepository.class);
    private final RequestItemCargoTypesRepository requestItemCargoTypesRepository =
            Mockito.mock(RequestItemCargoTypesRepository.class);
    private final CisValidationService cisValidationService = new CisValidationServiceImpl(environmentParamService,
            requestItemService);
    private final MboMappingsService mboMappingsService = Mockito.mock(MboMappingsService.class);
    private final AssortmentValidateService assortmentValidateService = Mockito.mock(AssortmentValidateService.class);
    private final RequestItemAssortmentSkuRepository requestItemAssortmentSkuRepository =
            Mockito.mock(RequestItemAssortmentSkuRepository.class);
    private final RequestItemRepository requestItemRepository = Mockito.mock(RequestItemRepository.class);
    private final AssortmentEnrichService assortmentEnrichService =
            new AssortmentEnrichServiceImpl(new AssortmentServiceImpl(mboMappingsService,
                    requestItemAssortmentSkuRepository), environmentParamService,
                    requestItemAssortmentSkuRepository, supplierMappingService, requestItemRepository);
    private final InventoryingSupplyRequestItemsEnricher enricher = new InventoryingSupplyRequestItemsEnricher(
            stockService,
            requestItemService,
            requestItemMarketVendorCodeRepository,
            requestItemMarketBarcodeRepository,
            requestItemCargoTypesRepository,
            supplierMappingService,
            cisValidationService,
            assortmentValidateService,
            assortmentEnrichService
    );

    @BeforeEach
    void init() {
        final Map<SupplierSkuKey, SupplierContentMapping> mapping = Map.of(
                new SupplierSkuKey(1, ARTICLE_1), SupplierContentMapping.builder(ARTICLE_1, 1L, "title")
                        .setBoxCount(5)
                        .setMarketName("some market name")
                        .setInboundAllowance(InboundAllowance.inboundNotAllowed("", "", "", "", null, null))
                        .setCargoTypes(ImmutableSet.of(10, 20))
                        .setInboundRemainingLifetimeDays(10)
                        .setOutboundRemainingLifetimeDays(15)
                        .setInboundRemainingLifetimePercentage(20)
                        .setOutboundRemainingLifetimePercentage(25)
                        .setHasExpirationDate(true)
                        .setMskuCargoTypes(Set.of())
                        .build()
        );
        when(supplierMappingService.getMarketSkuMapping(anyLong(), anyLong(), any(LocalDateTime.class), anySet()))
                .thenReturn(mapping);
        when(requestItemService.findArticlesOfAlreadyAcceptedItems(anyLong(), anyLong(), anyList()))
                .thenReturn(Collections.emptySet());
    }

    /**
     * Проверяет, что обогатали, хотя в маппинге inbound.isNotAllowed.
     */
    @Test
    void testInboundIsNotAllowed() {
        final RequestItem item1 = item(VatRate.VAT_20, "100");
        item1.setId(1L);
        item1.setArticle(ARTICLE_1);
        var errors = enricher.enrichSafe(request(SupplierType.FIRST_PARTY, null),
                List.of(item1));
        assertThat(errors.isEmpty(), equalTo(true));
        assertThat(item1.getBoxCount(), equalTo(5));
    }

    /**
     * Проверяет, что обогатили информацию по прошлой поставке
     */
    @Test
    void testNoMapping() {
        final RequestItem item1 = item(VatRate.VAT_20, "100");
        item1.setArticle(ARTICLE_3);
        final RequestItem previousItem = item(VatRate.VAT_20, "200");
        previousItem.setArticle(ARTICLE_3);
        when(requestItemService.findLastSuppliedItemsBySupplierIdAndArticle(any(), any(), any()))
                .thenReturn(List.of(previousItem));
        var errors = enricher.enrichSafe(request(SupplierType.FIRST_PARTY, null),
                List.of(item1));
        assertThat(errors.isEmpty(), equalTo(true));
        assertThat(item1.getSupplyPrice(), equalTo(BigDecimal.valueOf(200)));
    }

    /**
     * Тест, когда часть с маппингом, а часть без
     */
    @Test
    void testNotAllWithMappings() {
        final RequestItem item1 = item(VatRate.VAT_20, "100");
        item1.setArticle(ARTICLE_1);
        item1.setId(1L);
        final RequestItem item2 = item(VatRate.VAT_20, "100");
        item2.setArticle(ARTICLE_3);
        item2.setId(3L);
        final RequestItem previousItem2 = item(VatRate.VAT_20, "200");
        previousItem2.setArticle(ARTICLE_3);
        previousItem2.setId(3L);
        when(requestItemService.findLastSuppliedItemsBySupplierIdAndArticle(any(), any(), any()))
                .thenReturn(List.of(previousItem2));
        var errors = enricher.enrichSafe(request(SupplierType.FIRST_PARTY, null),
                List.of(item1, item2));
        assertThat(errors.isEmpty(), equalTo(true));
        assertThat(item1.getBoxCount(), equalTo(5));
        assertThat(item2.getSupplyPrice(), equalTo(BigDecimal.valueOf(200)));
    }

    @Test
    void testAssortmentErichSuccess() {
        String title1 = "title";
        String title2 = "title2";
        var mapping = generateSupplierSkuContentMapping(1L, ARTICLE_1, title1);
        var mapping2 = generateSupplierSkuContentMapping(1L, ARTICLE_3, title2);

        when(environmentParamService.isEnableAssortmentService()).thenReturn(true);
        when(supplierMappingService
                .getMarketSkuMapping(anyLong(), anyLong(), any(LocalDateTime.class), anySet()))
                .thenReturn(mapping);
        when(supplierMappingService
                .getMarketSkuMapping(anyLong(), anyLong(), any(LocalDateTime.class), anySet(), any()))
                .thenReturn(mapping2);
        when(supplierMappingService
                .getMarketSkuMapping(anyLong(), anyLong(), any(LocalDateTime.class), eq(new HashSet<>()), anyMap()))
                .thenReturn(Collections.emptyMap());
        when(requestItemRepository.save(anyList())).thenAnswer(arg -> {
            var items = (List<RequestItem>) arg.getArguments()[0];
            return items.stream().peek(item -> {
                if (item.getId() == null) {
                    item.setId(2L);
                }
            }).collect(Collectors.toList());
        });
        mockMboMappingsService(new Tuple3(1L, ARTICLE_1, List.of("sku2")));

        var item1 = item(VatRate.VAT_20, "100");
        item1.setId(1L);
        item1.setArticle(ARTICLE_1);

        var items = new ArrayList<RequestItem>();
        items.add(item1);
        var errors = enricher.enrichSafe(request(SupplierType.FIRST_PARTY, null), items);
        assertThat(errors.isEmpty(), equalTo(true));
        assertThat(items.size(), equalTo(2));
        assertTrue(items.stream().anyMatch(item -> StringUtils.equals(item.getName(), title2)));
    }

    private void mockMboMappingsService(Tuple3<Long, String, List<String>> data) {
        var builder = MboMappings.GetAssortmentChildSskusResponse.GetAssortmentChildSskusResult
                .newBuilder()
                .setAssortmentShopSkuKey(MbocCommon.ShopSkuKey.newBuilder()
                        .setSupplierSkuId(data.get2())
                        .setSupplierId(data.get1().intValue())
                        .build())
                .addAllChildSkus(List.of(ARTICLE_3));
        var results = List.of(builder.build());
        when(mboMappingsService.getAssortmentChildSskus(any()))
                .thenReturn(MboMappings.GetAssortmentChildSskusResponse.newBuilder().addAllResults(results).build());
    }

    private Map<SupplierSkuKey, SupplierContentMapping> generateSupplierSkuContentMapping(Long supplierId,
                                                                                          String sku,
                                                                                          String title) {
        Map<SupplierSkuKey, SupplierContentMapping> mapping = new HashMap<>();
        mapping.put(new SupplierSkuKey(supplierId, sku),
                SupplierContentMapping.builder(ARTICLE_1, 10L, title)
                        .setBoxCount(0)
                        .setWarehouseServices(List.of(WarehouseServiceType.IS_NEED_SORT))
                        .build());
        return mapping;
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
