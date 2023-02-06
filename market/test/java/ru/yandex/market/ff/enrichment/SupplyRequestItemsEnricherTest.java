package ru.yandex.market.ff.enrichment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.ff.client.enums.CisHandleMode;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.RequestItemAttribute;
import ru.yandex.market.ff.client.enums.RequestItemErrorAttributeType;
import ru.yandex.market.ff.client.enums.RequestItemErrorType;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.client.enums.VatRate;
import ru.yandex.market.ff.enrichment.builder.CheckRequestOnFulfillmentRequestItemsEnrichBuilder;
import ru.yandex.market.ff.enums.WarehouseServiceType;
import ru.yandex.market.ff.model.bo.EnrichmentResultContainer;
import ru.yandex.market.ff.model.bo.ExternalRequestItemErrorInfo;
import ru.yandex.market.ff.model.bo.InboundAllowance;
import ru.yandex.market.ff.model.bo.RequestItemErrorInfo;
import ru.yandex.market.ff.model.bo.SupplierContentMapping;
import ru.yandex.market.ff.model.bo.SupplierSkuKey;
import ru.yandex.market.ff.model.dto.registry.RegistryUnitId;
import ru.yandex.market.ff.model.dto.registry.UnitPartialId;
import ru.yandex.market.ff.model.entity.Identifier;
import ru.yandex.market.ff.model.entity.RequestItem;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.model.entity.SupplierBusinessType;
import ru.yandex.market.ff.model.enums.IdentifierType;
import ru.yandex.market.ff.repository.RequestItemAssortmentSkuRepository;
import ru.yandex.market.ff.repository.RequestItemCargoTypesRepository;
import ru.yandex.market.ff.repository.RequestItemMarketBarcodeRepository;
import ru.yandex.market.ff.repository.RequestItemMarketVendorCodeRepository;
import ru.yandex.market.ff.repository.RequestItemRepository;
import ru.yandex.market.ff.service.CisValidationService;
import ru.yandex.market.ff.service.DeepmindClientWrapperService;
import ru.yandex.market.ff.service.EnvironmentParamService;
import ru.yandex.market.ff.service.LmsClientCachingService;
import ru.yandex.market.ff.service.LogisticManagementService;
import ru.yandex.market.ff.service.RequestItemAttributeService;
import ru.yandex.market.ff.service.RequestItemService;
import ru.yandex.market.ff.service.StockService;
import ru.yandex.market.ff.service.SupplierMappingService;
import ru.yandex.market.ff.service.implementation.AssortmentEnrichServiceImpl;
import ru.yandex.market.ff.service.implementation.AssortmentServiceImpl;
import ru.yandex.market.ff.service.implementation.AssortmentValidateServiceImpl;
import ru.yandex.market.ff.service.implementation.CisValidationServiceImpl;
import ru.yandex.market.ff.service.implementation.ConcreteEnvironmentParamServiceImpl;
import ru.yandex.market.ff.service.implementation.SkuArticleValidationService;
import ru.yandex.market.ff.service.util.AssortmentEnrichService;
import ru.yandex.market.ff.service.util.AssortmentValidateService;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit тесты для {@link SupplyRequestItemsEnricher}.
 * Более общие тесты находятся в {@link ru.yandex.market.ff.service.RequestValidationServiceTest}.
 *
 * @author avetokhin 25/01/18.
 */
class SupplyRequestItemsEnricherTest {

    private static final ShopRequest EMPTY_REQUEST = request();
    private static final long ID_1 = 10L;
    private static final long ADDITIONAL_DELIVERY_INTERVAL = 5L;
    private static final String ARTICLE_1 = "article1";
    private static final String ARTICLE_2 = "article2";
    private static final String ARTICLE_3 = "article3";
    private static final String ARTICLE_4 = "article4";
    private static final String ARTICLE_5 = "article5";
    private static final String ARTICLE_WITH_IMEI_SN_PARAMS = "article_with_imei_params";
    private static final String ARTICLE_WITH_WRONG_IMEI_PARAMS = "article_with_wrong_imei_params";
    private static final String ARTICLE_WITH_WRONG_SN_PARAMS = "article_with_wrong_sn_params";
    private static final String ARTICLE_FOR_CIS_VALIDATIONS = "article_for_cis_validations";
    private static final int SKU_AVAILABILITY_ACTIVE = 0;
    private static final int SKU_AVAILABILITY_DELISTED = 2;
    private static final Set<Integer> MARSCHROUTE_CARGO_TYPES = ImmutableSet.of(10, 20, 30);
    private static final String CIS_1 = "000000011111";
    private static final int CARGO_TYPE_CIS_REQUIRED = 980;
    private static final int CARGO_TYPE_CIS_OPTIONAL = 990;
    private static final int CARGO_TYPE_CIS_DISTINCT = 985;
    private static final long SUPPLIER_ID = 1;
    private static final long MARSCHROUTE_ID = 145;
    private static final long INFOR_TOM_ID = 171;

    private EnvironmentParamService environmentParamService = Mockito.mock(EnvironmentParamService.class);
    private ConcreteEnvironmentParamServiceImpl concreteEnvironmentParamService =
        Mockito.spy(new ConcreteEnvironmentParamServiceImpl(environmentParamService));
    private SupplierMappingService supplierMappingService = Mockito.mock(SupplierMappingService.class);
    private RequestItemService requestItemService = Mockito.mock(RequestItemService.class);
    private StockService stockService = Mockito.mock(StockService.class);
    private SkuArticleValidationService skuArticleValidationService = Mockito.mock(SkuArticleValidationService.class);
    private LogisticManagementService logisticManagementService = Mockito.mock(LogisticManagementService.class);
    private LmsClientCachingService lmsClientCachingService = Mockito.mock(LmsClientCachingService.class);
    private RequestItemMarketBarcodeRepository requestItemMarketBarcodeRepository =
        Mockito.mock(RequestItemMarketBarcodeRepository.class);
    private RequestItemMarketVendorCodeRepository requestItemMarketVendorCodeRepository =
        Mockito.mock(RequestItemMarketVendorCodeRepository.class);
    private RequestItemCargoTypesRepository requestItemCargoTypesRepository =
            Mockito.mock(RequestItemCargoTypesRepository.class);
    private CisValidationService cisValidationService = new CisValidationServiceImpl(concreteEnvironmentParamService,
            requestItemService);
    private MboMappingsService mboMappingsService = Mockito.mock(MboMappingsService.class);
    private RequestItemAssortmentSkuRepository requestItemAssortmentSkuRepository =
            Mockito.mock(RequestItemAssortmentSkuRepository.class);
    private DeepmindClientWrapperService deepmindClientWrapperService =
            Mockito.mock(DeepmindClientWrapperService.class);
    private RequestItemRepository requestItemRepository = Mockito.mock(RequestItemRepository.class);
    private RequestItemAttributeService requestItemAttributeService = mock(RequestItemAttributeService.class);
    private AssortmentValidateService assortmentValidateService =
            new AssortmentValidateServiceImpl(concreteEnvironmentParamService, deepmindClientWrapperService);
    private AssortmentEnrichService assortmentEnrichService =
            new AssortmentEnrichServiceImpl(new AssortmentServiceImpl(mboMappingsService,
                    requestItemAssortmentSkuRepository), concreteEnvironmentParamService,
                    requestItemAssortmentSkuRepository, supplierMappingService, requestItemRepository);
    private CheckRequestOnFulfillmentRequestItemsEnrichBuilder checkRequestOnFulfillmentRequestItemsEnrichBuilder =
            Mockito.mock(CheckRequestOnFulfillmentRequestItemsEnrichBuilder.class);

    private SupplyRequestItemsEnricher enricher = new SupplyRequestItemsEnricher(supplierMappingService,
            requestItemService,
            stockService,
            skuArticleValidationService,
            logisticManagementService,
            lmsClientCachingService,
            requestItemMarketVendorCodeRepository,
            requestItemMarketBarcodeRepository,
            requestItemCargoTypesRepository,
            cisValidationService, concreteEnvironmentParamService,
            assortmentValidateService,
            assortmentEnrichService,
            requestItemAttributeService,
            checkRequestOnFulfillmentRequestItemsEnrichBuilder
    );

    @BeforeEach
    void init() {
        final Map<SupplierSkuKey, SupplierContentMapping> mapping = Map.of(
                new SupplierSkuKey(1, ARTICLE_1), SupplierContentMapping.builder(ARTICLE_1, 1L, "title")
                    .setBoxCount(5)
                    .setMarketName("some market name")
                    .setInboundAllowance(InboundAllowance.inboundAllowed())
                    .setCargoTypes(ImmutableSet.of(10, 20))
                    .setInboundRemainingLifetimeDays(10)
                    .setOutboundRemainingLifetimeDays(15)
                    .setInboundRemainingLifetimePercentage(20)
                    .setOutboundRemainingLifetimePercentage(25)
                    .setHasExpirationDate(true)
                    .setMskuCargoTypes(Set.of())
                    .build(),
                new SupplierSkuKey(1, ARTICLE_2), SupplierContentMapping.builder(ARTICLE_2, 1L, "title")
                    .setBoxCount(6)
                    .setPackageNumInSpike(5)
                    .setMarketName("some market name")
                    .setOfferAvailability(SKU_AVAILABILITY_ACTIVE)
                    .setInboundAllowance(InboundAllowance.inboundAllowed())
                    .setCargoTypes(ImmutableSet.of(20, 30, 40, 50))
                    .setInboundRemainingLifetimeDays(10)
                    .setOutboundRemainingLifetimeDays(15)
                    .setInboundRemainingLifetimePercentage(20)
                    .setOutboundRemainingLifetimePercentage(25)
                    .setCisHandleMode(CisHandleMode.ACCEPT_ONLY_DECLARED)
                    .setMskuCargoTypes(Set.of())
                    .build(),
                new SupplierSkuKey(1, ARTICLE_3), SupplierContentMapping.builder(ARTICLE_3, 1L, "title")
                    .setBoxCount(1)
                    .setPackageNumInSpike(5)
                    .setMarketName("some market name")
                    .setOfferAvailability(SKU_AVAILABILITY_DELISTED)
                    .setInboundAllowance(InboundAllowance.inboundNotAllowed(
                        "TestCode", "TestAttributes", "TestTemplate", null, null, null)
                    )
                    .setMskuCargoTypes(Set.of())
                    .setCargoTypes(ImmutableSet.of(10, 20, 30))
                    .build(),
                new SupplierSkuKey(1, ARTICLE_4), SupplierContentMapping.builder(ARTICLE_4, 1L, "title")
                    .setBoxCount(1)
                    .setPackageNumInSpike(5)
                    .setMarketName("some market name")
                    .setOfferAvailability(SKU_AVAILABILITY_ACTIVE)
                    .setInboundAllowance(InboundAllowance.inboundAllowed())
                    .setMskuCargoTypes(Set.of())
                    .setCargoTypes(ImmutableSet.of(20, 30, 40, 50))
                    .build(),
                new SupplierSkuKey(1, ARTICLE_5), SupplierContentMapping.builder(ARTICLE_5, 1L, "title")
                    .setBoxCount(1)
                    .setMarketName("some market name")
                    .setOfferAvailability(SKU_AVAILABILITY_ACTIVE)
                    .setInboundAllowance(InboundAllowance.inboundAllowed())
                    .setInboundRemainingLifetimeDays(10)
                    .setOutboundRemainingLifetimeDays(15)
                    .setInboundRemainingLifetimePercentage(20)
                    .setOutboundRemainingLifetimePercentage(25)
                    .setMskuCargoTypes(Set.of())
                    .build(),
                new SupplierSkuKey(1, ARTICLE_WITH_IMEI_SN_PARAMS), SupplierContentMapping.builder(
                        ARTICLE_WITH_IMEI_SN_PARAMS, 1L, "title")
                    .setBoxCount(1)
                    .setMarketName("some market name")
                    .setOfferAvailability(SKU_AVAILABILITY_ACTIVE)
                    .setInboundAllowance(InboundAllowance.inboundAllowed())
                    .setCheckImei(1)
                    .setImeiMask(".*")
                    .setCheckSn(1)
                    .setSnMask(".+")
                    .setCisHandleMode(CisHandleMode.ACCEPT_ONLY_DECLARED)
                    .setMskuCargoTypes(Set.of())
                    .build(),
                new SupplierSkuKey(1, ARTICLE_WITH_WRONG_IMEI_PARAMS), SupplierContentMapping.builder(
                        ARTICLE_WITH_WRONG_IMEI_PARAMS, 1L, "title")
                    .setBoxCount(1)
                    .setMarketName("some market name")
                    .setOfferAvailability(SKU_AVAILABILITY_ACTIVE)
                    .setInboundAllowance(InboundAllowance.inboundAllowed())
                    .setCheckImei(1)// and do not set ImeiMask
                    .setMskuCargoTypes(Set.of())
                    .build(),
                new SupplierSkuKey(1, ARTICLE_WITH_WRONG_SN_PARAMS), SupplierContentMapping.builder(
                        ARTICLE_WITH_WRONG_SN_PARAMS, 1L, "title")
                    .setBoxCount(1)
                    .setMarketName("some market name")
                    .setOfferAvailability(SKU_AVAILABILITY_ACTIVE)
                    .setInboundAllowance(InboundAllowance.inboundAllowed())
                    .setCheckSn(1)// and do not set SnMask
                    .setMskuCargoTypes(Set.of())
                    .build(),
                new SupplierSkuKey(1, ARTICLE_FOR_CIS_VALIDATIONS),
                        SupplierContentMapping.builder(ARTICLE_1, 1L, "title")
                        .setBoxCount(5)
                        .setMarketName("some market name")
                        .setInboundAllowance(InboundAllowance.inboundAllowed())
                        .setCargoTypes(ImmutableSet.of(CARGO_TYPE_CIS_DISTINCT, CARGO_TYPE_CIS_REQUIRED))
                        .setInboundRemainingLifetimeDays(10)
                        .setOutboundRemainingLifetimeDays(15)
                        .setInboundRemainingLifetimePercentage(20)
                        .setOutboundRemainingLifetimePercentage(25)
                        .setCisHandleMode(CisHandleMode.ACCEPT_ONLY_DECLARED)
                        .setHasExpirationDate(true)
                        .build()
            );

        when(supplierMappingService
                .getMarketSkuMapping(anyLong(), anyLong(), any(LocalDateTime.class), anySet(), anyMap()))
                .thenReturn(mapping);
        when(requestItemService.findArticlesOfAlreadyAcceptedItems(anyLong(), anyLong(), anyList()))
            .thenReturn(Collections.emptySet());
        when(requestItemAttributeService.checkAttribute(anyLong(), eq(RequestItemAttribute.CTM))).thenReturn(false);
    }

    @AfterEach
    public void invalidateCache() {
        lmsClientCachingService.invalidateCache();
        Mockito.reset(concreteEnvironmentParamService);
    }

    @Test
    void untaxedPrice() {
        enrichAndAssertUntaxedPrice(VatRate.VAT_20, "100", "83.33");
        enrichAndAssertUntaxedPrice(VatRate.VAT_20, "50", "41.67");
        enrichAndAssertUntaxedPrice(VatRate.VAT_20, "99", "82.50");
        enrichAndAssertUntaxedPrice(VatRate.VAT_20, "99.99", "83.32");

        enrichAndAssertUntaxedPrice(VatRate.VAT_18, "100", "84.75");
        enrichAndAssertUntaxedPrice(VatRate.VAT_18, "50", "42.37");
        enrichAndAssertUntaxedPrice(VatRate.VAT_18, "99", "83.90");
        enrichAndAssertUntaxedPrice(VatRate.VAT_18, "99.99", "84.74");

        enrichAndAssertUntaxedPrice(VatRate.VAT_10, "100", "90.91");
        enrichAndAssertUntaxedPrice(VatRate.VAT_10, "50", "45.45");
        enrichAndAssertUntaxedPrice(VatRate.VAT_10, "99", "90.00");
        enrichAndAssertUntaxedPrice(VatRate.VAT_10, "99.99", "90.90");

        enrichAndAssertUntaxedPrice(VatRate.VAT_0, "100", "100");
        enrichAndAssertUntaxedPrice(VatRate.NO_VAT, "100.5", "100.5");

    }

    @Test
    void testCheckLimitsDefault() {
        final RequestItem item = item(VatRate.VAT_20, "100");
        final Map<Long, EnrichmentResultContainer> errors =
            enricher.enrichSafe(EMPTY_REQUEST, Collections.singletonList(item));
        assertThat(errors.isEmpty(), equalTo(true));
    }

    @Test
    void testCisHandleModes() {
        final RequestItem item1 = item(VatRate.VAT_20, "100");
        final RequestItem item2 = item(VatRate.VAT_20, "100");
        item1.setId(1L);
        item1.setArticle(ARTICLE_1);
        item2.setId(2L);
        item2.setArticle(ARTICLE_WITH_IMEI_SN_PARAMS);
        var errors = enricher.enrichSafe(request(SupplierType.FIRST_PARTY, null),
                    List.of(item1, item2));
        assertThat(errors.isEmpty(), equalTo(true));
        assertThat(item1.getCisHandleMode(), nullValue());
        assertThat(item2.getCisHandleMode(), equalTo(CisHandleMode.ACCEPT_ONLY_DECLARED));
    }

    @Test
    void thatBoxCountWasSetFor1PSupplier() {
        final RequestItem item = item(VatRate.VAT_20, "100");
        final Map<Long, EnrichmentResultContainer> errors =
            enricher.enrichSafe(request(SupplierType.FIRST_PARTY, null), Collections.singletonList(item));
        assertThat(errors.isEmpty(), equalTo(true));
        assertThat(item.getBoxCount(), equalTo(5));
    }

    @Test
    void thatMappingBoxCountWasSetForSpikeItemWithNullBoxCount() {
        when(lmsClientCachingService.getCargoTypesForService(anyLong())).thenReturn(Set.of(20, 30, 40, 50));
        final RequestItem item = item(VatRate.VAT_20, "100");
        item.setArticle(ARTICLE_4);
        item.setBoxCount(null);
        final Map<Long, EnrichmentResultContainer> errors =
            enricher.enrichSafe(request(SupplierType.THIRD_PARTY, null), Collections.singletonList(item));
        assertThat(errors.isEmpty(), equalTo(true));
        assertThat(item.getBoxCount(), equalTo(1));
    }

    @Test
    void thatMappingBoxCountWasSetForItemWithSpecifiedBoxCountFor3PSupplier() {
        final RequestItem item = item(VatRate.VAT_20, "100");
        final Map<Long, EnrichmentResultContainer> errors =
            enricher.enrichSafe(request(SupplierType.THIRD_PARTY, null), Collections.singletonList(item));
        assertThat(errors.isEmpty(), equalTo(true));
        assertThat(item.getBoxCount(), equalTo(5));
    }

    /**
     * Строка заявки имеет статус отличный от DELISTED, но не null,
     * возвращается список ошибок без SHOP_SKU_IS_DELISTED (выведен из ассортимента поставщика)
     */
    @Test
    public void shouldNotContainsDelistedErrorIfItemDoNotHaveDelistedStatus() {
        when(lmsClientCachingService.getCargoTypesForService(anyLong())).thenReturn(Set.of(20, 30, 40, 50));
        final RequestItem item = item(VatRate.VAT_20, "100");
        item.setArticle(ARTICLE_4);
        item.setBoxCount(null);
        final Map<Long, EnrichmentResultContainer> errors =
            enricher.enrichSafe(request(SupplierType.THIRD_PARTY, null), Collections.singletonList(item));

        assertThat(errors.isEmpty(), equalTo(true));
    }

    /**
     * Строка заявки имеет статус null,
     * возвращается список ошибок без SHOP_SKU_IS_DELISTED (выведен из ассортимента поставщика)
     */
    @Test
    public void shouldNotContainsDelistedErrorIfItemAvailabilityStatusIsNull() {
        final RequestItem item = item(VatRate.VAT_20, "100");
        item.setArticle(ARTICLE_1);
        item.setBoxCount(null);
        final Map<Long, EnrichmentResultContainer> errors =
            enricher.enrichSafe(request(SupplierType.THIRD_PARTY, null), Collections.singletonList(item));

        assertThat(errors.isEmpty(), equalTo(true));
    }

    /**
     * Проверяет, что поставка некрупногабаритных 1P товаров на томилино возможна.
     */
    @Test
    public void shouldAllowToCreateNotLargeScaleFor1P() {
        final RequestItem item = item(VatRate.VAT_20, "100");
        item.setArticle(ARTICLE_1);

        ShopRequest request = request(SupplierType.FIRST_PARTY, null);
        request.setServiceId(INFOR_TOM_ID);

        final Map<SupplierSkuKey, SupplierContentMapping> mapping =
                Map.of(new SupplierSkuKey(1, ARTICLE_1), SupplierContentMapping.builder(ARTICLE_1, 1L, "title")
                .setBoxCount(1)
                .setInboundAllowance(InboundAllowance.inboundAllowed())
                .setMskuCargoTypes(Set.of())
                .setMarketCategoryId(1337L).build());

        when(supplierMappingService.getMarketSkuMapping(anyLong(), anyLong(), any(LocalDateTime.class), anySet()))
            .thenReturn(mapping);

        final Map<Long, EnrichmentResultContainer> errors =
            enricher.enrichSafe(request, Collections.singletonList(item));

        SoftAssertions.assertSoftly(assertions ->
            assertions.assertThat(errors).isEmpty());

        verify(supplierMappingService)
                .getMarketSkuMapping(anyLong(), anyLong(), any(LocalDateTime.class), anySet(), anyMap());
    }


    /**
     * Проверяет, что поставка без НДС нройдет валидацию
     */
    @Test
    public void shouldAllowToCreateWithoutVatRate() {
        final RequestItem item = item(null, "100");
        item.setArticle(ARTICLE_1);

        ShopRequest request = request(SupplierType.FIRST_PARTY, null);
        request.setServiceId(INFOR_TOM_ID);

        final Map<SupplierSkuKey, SupplierContentMapping> mapping =
                Map.of(new SupplierSkuKey(1, ARTICLE_1), SupplierContentMapping.builder(ARTICLE_1, 1L, "title")
                .setBoxCount(1)
                .setInboundAllowance(InboundAllowance.inboundAllowed())
                .setMskuCargoTypes(Set.of())
                .setMarketCategoryId(1337L).build());

        when(supplierMappingService.getMarketSkuMapping(anyLong(), anyLong(), any(LocalDateTime.class), anySet()))
            .thenReturn(mapping);

        final Map<Long, EnrichmentResultContainer> errors =
            enricher.enrichSafe(request, Collections.singletonList(item));

        SoftAssertions.assertSoftly(assertions ->
            assertions.assertThat(errors).isEmpty());

        verify(supplierMappingService)
                .getMarketSkuMapping(anyLong(), anyLong(), any(LocalDateTime.class), anySet(), anyMap());
    }

    /**
     * Валидируем xDoc поставку.
     * Должны получить добавычный интервал доставки из lms и добавить к дате xDoc.
     * Не проверяем лимиты в случае xDoc поставки.
     */
    @Test
    public void shouldAddDeliveryIntervalAndNotCheckLimitsForXDocSupply() {
        when(logisticManagementService.getXDocSupplyAdditionalDateInterval(2L, MARSCHROUTE_ID))
            .thenReturn(Optional.of(ADDITIONAL_DELIVERY_INTERVAL));

        RequestItem item = item(VatRate.VAT_20, "100");
        item.setArticle(ARTICLE_1);
        ShopRequest request = xDocRequest(SupplierType.THIRD_PARTY);
        request.setServiceId(MARSCHROUTE_ID);
        Map<Long, EnrichmentResultContainer> errors =
            enricher.enrichSafe(request, Collections.singletonList(item));

        assertThat(errors.isEmpty(), equalTo(true));
        assertThat(request.getRequestedDate(),
            equalTo(request.getxDocRequestedDate().plusDays(ADDITIONAL_DELIVERY_INTERVAL)));
    }

    @Test
    public void validateXDocSupplyWhenThereIsNotXDocFulfillmentRelation() {
        when(logisticManagementService.getXDocSupplyAdditionalDateInterval(2L, MARSCHROUTE_ID))
                .thenReturn(Optional.empty());

        RequestItem item = item(VatRate.VAT_20, "100");
        item.setArticle(ARTICLE_1);
        ShopRequest request = xDocRequest(SupplierType.THIRD_PARTY);
        request.setServiceId(MARSCHROUTE_ID);
        Map<Long, EnrichmentResultContainer> errors =
                enricher.enrichSafe(request, Collections.singletonList(item));

        SoftAssertions.assertSoftly(assertions -> {

            assertions.assertThat(errors).hasSize(1);
            EnrichmentResultContainer errorsContainer = errors.get(ID_1);
            assertions.assertThat(errorsContainer).isNotNull();
            assertions.assertThat(errorsContainer.getValidationErrors()).hasSize(1);

            Optional<RequestItemErrorInfo> errorInfoOptional = errorsContainer.getValidationErrors().stream()
                    .findAny();
            assertions.assertThat(errorInfoOptional).isPresent();

            RequestItemErrorInfo errorInfo = errorInfoOptional.get();
            assertions.assertThat(errorInfo.getType())
                    .isEqualTo(RequestItemErrorType.XDOC_SUPPLY_FOR_NOT_EXISTING_XDOC_FULFILLMENT_RELATION);
        });
    }

    @Test
    public void mboAssortmentControlDisabledShouldNotInteractWithLms() {
        RequestItem item = item(VatRate.VAT_20, "100");
        item.setArticle(ARTICLE_1);
        ShopRequest request = request(SupplierType.THIRD_PARTY, "No Comment");
        request.setServiceId(MARSCHROUTE_ID);
        Map<Long, EnrichmentResultContainer> errors =
            enricher.enrichSafe(request, Collections.singletonList(item));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(errors).isEmpty();
        });
        verifyZeroInteractions(logisticManagementService);
    }


    @Test
    public void mboAssortmentEnabledShouldCallLms() {
        when(lmsClientCachingService.getCargoTypesForService(anyLong()))
            .thenReturn(MARSCHROUTE_CARGO_TYPES);

        RequestItem item = item(VatRate.VAT_20, "100");
        item.setArticle(ARTICLE_1);
        ShopRequest request = request(SupplierType.THIRD_PARTY, "No Comment");
        request.setServiceId(MARSCHROUTE_ID);
        Map<Long, EnrichmentResultContainer> errors =
            enricher.enrichSafe(request, Collections.singletonList(item));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(errors).isEmpty();
        });
        verify(lmsClientCachingService).getCargoTypesForService(anyLong());
    }

    @Test
    public void mboAssortmentEnabledButInboundIsNotAllowed() {
        when(lmsClientCachingService.getCargoTypesForService(anyLong()))
            .thenReturn(MARSCHROUTE_CARGO_TYPES);

        RequestItem item = item(VatRate.VAT_20, "100");
        item.setArticle(ARTICLE_3);
        ShopRequest request = request(SupplierType.THIRD_PARTY, "No Comment");
        request.setServiceId(MARSCHROUTE_ID);

        Map<Long, EnrichmentResultContainer> errors =
            enricher.enrichSafe(request, Collections.singletonList(item));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(errors).isNotEmpty();
            softly.assertThat(errors).hasSize(1);
            EnrichmentResultContainer itemErrorsContainer = errors.get(item.getId());
            softly.assertThat(itemErrorsContainer).isNotNull();
            softly.assertThat(itemErrorsContainer.getValidationErrors()).isEmpty();
            softly.assertThat(itemErrorsContainer.getExternalErrors()).hasSize(1);
            Optional<ExternalRequestItemErrorInfo> errorInfoOptional =
                itemErrorsContainer.getExternalErrors().stream().findAny();
            softly.assertThat(errorInfoOptional).isPresent();
            ExternalRequestItemErrorInfo errorInfo = errorInfoOptional.get();
            softly.assertThat(errorInfo.getErrorCode()).isEqualTo("TestCode");
            softly.assertThat(errorInfo.getErrorParams()).isEqualTo("TestAttributes");
        });
        verify(lmsClientCachingService).getCargoTypesForService(anyLong());
    }


    @Test
    public void mboAssortmentEnabledButInboundIsAllowedButCargoTypesDoNotCorrespond() {
        when(lmsClientCachingService.getCargoTypesForService(anyLong()))
            .thenReturn(MARSCHROUTE_CARGO_TYPES);

        RequestItem item = item(VatRate.VAT_20, "100");
        item.setArticle(ARTICLE_4);
        ShopRequest request = request(SupplierType.THIRD_PARTY, "No Comment");
        request.setServiceId(MARSCHROUTE_ID);

        Map<Long, EnrichmentResultContainer> errors =
            enricher.enrichSafe(request, Collections.singletonList(item));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(errors).isNotEmpty();
            softly.assertThat(errors).hasSize(1);
            EnrichmentResultContainer itemErrorsContainer = errors.get(item.getId());
            softly.assertThat(itemErrorsContainer).isNotNull();
            softly.assertThat(itemErrorsContainer.getExternalErrors()).isEmpty();
            softly.assertThat(itemErrorsContainer.getValidationErrors()).hasSize(1);
            Optional<RequestItemErrorInfo> errorInfoOptional =
                itemErrorsContainer.getValidationErrors().stream().findAny();
            softly.assertThat(errorInfoOptional).isPresent();
            RequestItemErrorInfo errorInfo = errorInfoOptional.get();
            softly.assertThat(errorInfo.getType()).isEqualTo(RequestItemErrorType.UNACCEPTABLE_ITEM_CARGO_TYPES);
            softly.assertThat(errorInfo.getAttributes()).hasSize(1);
            softly.assertThat(errorInfo.getAttributes()
                .get(RequestItemErrorAttributeType.ITEM_CARGO_TYPES_NOT_ALLOWED_FOR_SERVICE)).isEqualTo("40,50");
        });
        verify(lmsClientCachingService).getCargoTypesForService(anyLong());
    }

    @Test
    public void shouldSuccessEnrichRemainingLifetimes() {
        final RequestItem item = item(VatRate.VAT_20, "100");
        final Map<Long, EnrichmentResultContainer> errors =
            enricher.enrichSafe(request(SupplierType.THIRD_PARTY, null), Collections.singletonList(item));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(errors).isEmpty();
            softly.assertThat(item.getInboundRemainingLifetimeDays()).isEqualTo(10);
            softly.assertThat(item.getOutboundRemainingLifetimeDays()).isEqualTo(15);
            softly.assertThat(item.getInboundRemainingLifetimePercentage()).isEqualTo(20);
            softly.assertThat(item.getOutboundRemainingLifetimePercentage()).isEqualTo(25);
        });
        verify(supplierMappingService)
                .getMarketSkuMapping(anyLong(), anyLong(), any(LocalDateTime.class), anySet(), anyMap());
    }

    @Test
    public void shouldEnrichRemainingLifetimesIfHasExpirationDateDoesNotPresent() {
        final RequestItem item = item(VatRate.VAT_20, "100");
        item.setArticle(ARTICLE_5);

        final Map<Long, EnrichmentResultContainer> errors =
            enricher.enrichSafe(request(SupplierType.THIRD_PARTY, null), Collections.singletonList(item));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(errors).isEmpty();
            softly.assertThat(item.getInboundRemainingLifetimeDays()).isNotNull();
            softly.assertThat(item.getOutboundRemainingLifetimeDays()).isNotNull();
            softly.assertThat(item.getInboundRemainingLifetimePercentage()).isNotNull();
            softly.assertThat(item.getOutboundRemainingLifetimePercentage()).isNotNull();
        });
        verify(supplierMappingService)
                .getMarketSkuMapping(anyLong(), anyLong(), any(LocalDateTime.class), anySet(), anyMap());
    }

    @Test
    public void shouldEnrichImeiAndSerialnumberFields() {
        final RequestItem item = item(VatRate.VAT_20, "100");
        item.setArticle(ARTICLE_WITH_IMEI_SN_PARAMS);

        final Map<Long, EnrichmentResultContainer> errors =
            enricher.enrichSafe(request(SupplierType.THIRD_PARTY, null), Collections.singletonList(item));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(errors).isEmpty();
            softly.assertThat(item.getImeiCount()).isEqualTo(1);
            softly.assertThat(item.getImeiMask()).isEqualTo(".*");
            softly.assertThat(item.getSerialNumberCount()).isEqualTo(1);
            softly.assertThat(item.getSerialNumberMask()).isEqualTo(".+");
        });
        verify(supplierMappingService)
                .getMarketSkuMapping(anyLong(), anyLong(), any(LocalDateTime.class), anySet(), anyMap());
    }

    @Test
    public void getErrorOnWrongImeiFieldsCombination() {
        final RequestItem item = item(VatRate.VAT_20, "100");
        item.setArticle(ARTICLE_WITH_WRONG_IMEI_PARAMS);

        final Map<Long, EnrichmentResultContainer> errors =
            enricher.enrichSafe(request(SupplierType.THIRD_PARTY, null), Collections.singletonList(item));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(errors).isNotEmpty();
            softly.assertThat(errors).hasSize(1);
            EnrichmentResultContainer itemErrorsContainer = errors.get(item.getId());
            softly.assertThat(itemErrorsContainer).isNotNull();
            softly.assertThat(itemErrorsContainer.getExternalErrors()).isEmpty();
            softly.assertThat(itemErrorsContainer.getValidationErrors()).hasSize(1);
            Optional<RequestItemErrorInfo> errorInfoOptional =
                    itemErrorsContainer.getValidationErrors().stream().findAny();
            softly.assertThat(errorInfoOptional).isPresent();
            RequestItemErrorInfo errorInfo = errorInfoOptional.get();
            softly.assertThat(errorInfo.getType()).isEqualTo(RequestItemErrorType.INCORRECT_SERIALNUM_OR_IMEI_FIELDS);
        });
        verify(supplierMappingService)
                .getMarketSkuMapping(anyLong(), anyLong(), any(LocalDateTime.class), anySet(), anyMap());
    }

    @Test
    public void getErrorOnWrongSnFieldsCombination() {
        final RequestItem item = item(VatRate.VAT_20, "100");
        item.setArticle(ARTICLE_WITH_WRONG_SN_PARAMS);

        final Map<Long, EnrichmentResultContainer> errors =
            enricher.enrichSafe(request(SupplierType.THIRD_PARTY, null), Collections.singletonList(item));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(errors).isNotEmpty();
            softly.assertThat(errors).hasSize(1);
            EnrichmentResultContainer itemErrorsContainer = errors.get(item.getId());
            softly.assertThat(itemErrorsContainer).isNotNull();
            softly.assertThat(itemErrorsContainer.getExternalErrors()).isEmpty();
            softly.assertThat(itemErrorsContainer.getValidationErrors()).hasSize(1);
            Optional<RequestItemErrorInfo> errorInfoOptional =
                    itemErrorsContainer.getValidationErrors().stream().findAny();
            softly.assertThat(errorInfoOptional).isPresent();
            RequestItemErrorInfo errorInfo = errorInfoOptional.get();
            softly.assertThat(errorInfo.getType()).isEqualTo(RequestItemErrorType.INCORRECT_SERIALNUM_OR_IMEI_FIELDS);
        });
        verify(supplierMappingService)
                .getMarketSkuMapping(anyLong(), anyLong(), any(LocalDateTime.class), anySet(), anyMap());
    }

    @Test
    void failWhenNonUniqueCisFound() {
        RequestItem item1 = requestItem(List.of(CIS_1), 1L, 1, ARTICLE_FOR_CIS_VALIDATIONS,
                VatRate.VAT_20, "100");
        RequestItem item2 = requestItem(List.of(CIS_1), 2L, 1, ARTICLE_FOR_CIS_VALIDATIONS,
                VatRate.VAT_20, "100");

        EMPTY_REQUEST.setNeedConfirmation(false);
        EMPTY_REQUEST.setId(1L);
        doReturn(Set.of(0L)).when(concreteEnvironmentParamService).getAllowedSuppliersCreateRequestsForItemsWithCis();
        when(concreteEnvironmentParamService.isCisValidationsEnabled()).thenReturn(true);
        when(lmsClientCachingService.getCargoTypesForService(anyLong()))
                .thenReturn(Set.of(CARGO_TYPE_CIS_DISTINCT, CARGO_TYPE_CIS_OPTIONAL, CARGO_TYPE_CIS_REQUIRED));
        when(requestItemService.findAllWithIdentifiersOrderById(anyLong())).thenReturn(Set.of(item1, item2));
        doReturn(Set.of(RequestType.MOVEMENT_SUPPLY)).when(concreteEnvironmentParamService)
                .requestTypesNotForCisValidation();

        Map<Long, EnrichmentResultContainer> errors = enricher.enrichSafe(EMPTY_REQUEST, List.of(item1, item2));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(errors).hasSize(2);
            EnrichmentResultContainer errorsContainer1 = errors.get(1L);
            EnrichmentResultContainer errorsContainer2 = errors.get(2L);

            Set<RequestItemErrorInfo> validationErrors1 = errorsContainer1.getValidationErrors();
            Set<RequestItemErrorInfo> validationErrors2 = errorsContainer2.getValidationErrors();
            softly.assertThat(validationErrors1).hasSize(1);
            softly.assertThat(validationErrors2).hasSize(1);

            Optional<RequestItemErrorInfo> infoOptional1 = validationErrors1.stream()
                    .findAny();
            Optional<RequestItemErrorInfo> infoOptional2 = validationErrors2.stream()
                    .findAny();
            softly.assertThat(infoOptional1).isPresent();
            softly.assertThat(infoOptional2).isPresent();

            softly.assertThat(infoOptional1.get()).isEqualTo(RequestItemErrorInfo.of(
                    RequestItemErrorType.NON_UNIQUE_CIS,
                    Collections.singletonMap(RequestItemErrorAttributeType.NON_UNIQUE_CIS, CIS_1)));
            softly.assertThat(infoOptional1.get()).isEqualTo(RequestItemErrorInfo.of(
                    RequestItemErrorType.NON_UNIQUE_CIS,
                    Collections.singletonMap(RequestItemErrorAttributeType.NON_UNIQUE_CIS, CIS_1)));
        });
    }

    @Test
    void failOnWrongNumberOfUniqueCisFound() {
        RequestItem item = requestItem(List.of(CIS_1), 1L, 2, ARTICLE_FOR_CIS_VALIDATIONS,
                VatRate.VAT_20, "100");

        EMPTY_REQUEST.setNeedConfirmation(false);
        EMPTY_REQUEST.setId(1L);
        EMPTY_REQUEST.setType(RequestType.SUPPLY);
        doReturn(true).when(concreteEnvironmentParamService)
                .allowSupplierCreateRequestsForItemsWithCis(eq(SUPPLIER_ID));
        when(concreteEnvironmentParamService.isCisValidationsEnabled()).thenReturn(true);
        when(lmsClientCachingService.getCargoTypesForService(anyLong()))
                .thenReturn(Set.of(CARGO_TYPE_CIS_DISTINCT, CARGO_TYPE_CIS_OPTIONAL, CARGO_TYPE_CIS_REQUIRED));
        when(requestItemService.findAllWithIdentifiersOrderById(anyLong())).thenReturn(Set.of(item));
        doReturn(Set.of(RequestType.MOVEMENT_SUPPLY)).when(concreteEnvironmentParamService)
                .requestTypesNotForCisValidation();
        Map<Long, EnrichmentResultContainer> errors = enricher.enrichSafe(EMPTY_REQUEST, List.of(item));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(errors).hasSize(1);
            EnrichmentResultContainer errorsContainer = errors.get(1L);

            Set<RequestItemErrorInfo> validationErrors = errorsContainer.getValidationErrors();
            softly.assertThat(validationErrors).hasSize(1);

            Optional<RequestItemErrorInfo> infoOptional = validationErrors.stream()
                    .findAny();
            softly.assertThat(infoOptional).isPresent();

            softly.assertThat(infoOptional.get()).isEqualTo(RequestItemErrorInfo.of(
                    RequestItemErrorType.INVALID_COUNT_FOR_DISTINCT_CIS_IDENTIFIER));
        });
    }

    @Test
    void failOnRestrictedSuppliersForItemsWithCis() {
        RequestItem item = requestItem(List.of(CIS_1), 1L, 1, ARTICLE_FOR_CIS_VALIDATIONS,
                VatRate.VAT_20, "100");

        EMPTY_REQUEST.setNeedConfirmation(false);
        EMPTY_REQUEST.setId(1L);
        EMPTY_REQUEST.setType(RequestType.SUPPLY);
        Set<Long> allowedSuppliers = Set.of(SUPPLIER_ID + 1, SUPPLIER_ID + 2);

        when(concreteEnvironmentParamService.isCisValidationsEnabled()).thenReturn(true);
        doReturn(allowedSuppliers).when(concreteEnvironmentParamService)
                .getAllowedSuppliersCreateRequestsForItemsWithCis();
        doReturn(Set.of(RequestType.MOVEMENT_SUPPLY)).when(concreteEnvironmentParamService)
                .requestTypesNotForCisValidation();
        doCallRealMethod().when(concreteEnvironmentParamService)
                .allowSupplierCreateRequestsForItemsWithCis(eq(SUPPLIER_ID));
        doCallRealMethod().when(concreteEnvironmentParamService).allValues(eq(allowedSuppliers));
        when(lmsClientCachingService.getCargoTypesForService(anyLong()))
                .thenReturn(Set.of(CARGO_TYPE_CIS_DISTINCT, CARGO_TYPE_CIS_OPTIONAL, CARGO_TYPE_CIS_REQUIRED));
        when(requestItemService.findAllWithIdentifiersOrderById(anyLong())).thenReturn(Set.of(item));
        Map<Long, EnrichmentResultContainer> errors = enricher.enrichSafe(EMPTY_REQUEST, List.of(item));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(errors).hasSize(1);
            EnrichmentResultContainer errorsContainer = errors.get(1L);

            Set<RequestItemErrorInfo> validationErrors = errorsContainer.getValidationErrors();
            softly.assertThat(validationErrors).hasSize(1);

            Optional<RequestItemErrorInfo> infoOptional = validationErrors.stream()
                    .findAny();
            softly.assertThat(infoOptional).isPresent();

            softly.assertThat(infoOptional.get()).isEqualTo(RequestItemErrorInfo.of(
                    RequestItemErrorType.REQUESTS_WITH_CIS_ITEMS_IS_NOT_ALLOWED));
        });
    }

    @Test
    void failAssortmentSkuNotFound() {
        Map<SupplierSkuKey, SupplierContentMapping> mapping =
                Map.of(new SupplierSkuKey(1L, ARTICLE_1),
                        SupplierContentMapping.builder(ARTICLE_1, 10L, "title")
                                .setBoxCount(5)
                                .setWarehouseServices(List.of(WarehouseServiceType.IS_NEED_SORT))
                                .build());
        when(concreteEnvironmentParamService.isEnableAssortmentService()).thenReturn(true);
        when(supplierMappingService
                .getMarketSkuMapping(anyLong(), anyLong(), any(LocalDateTime.class), any(), anyMap()))
                .thenReturn(mapping);
        when(supplierMappingService
                .getMarketSkuMapping(anyLong(), anyLong(), any(LocalDateTime.class), eq(new HashSet<>()), anyMap()))
                .thenReturn(Collections.emptyMap());
        when(mboMappingsService.getAssortmentChildSskus(any()))
                .thenReturn(MboMappings.GetAssortmentChildSskusResponse.newBuilder().build());

        RequestItem item = requestItem(List.of(CIS_1), 1L, 1, ARTICLE_1, VatRate.VAT_20, "100");
        Map<Long, EnrichmentResultContainer> resultContainerMap =
                enricher.enrichSafe(request(SupplierType.REAL_SUPPLIER, null), List.of(item));

        assertThat(resultContainerMap.size(), equalTo(1));
        Set<RequestItemErrorInfo> errors = resultContainerMap.values().iterator().next().getValidationErrors();

        assertThat(errors.size(), equalTo(1));
        assertThat(errors.iterator().next().getType(), equalTo(RequestItemErrorType.ASSORTMENT_SKU_NOT_FOUND));
    }

    private void enrichAndAssertComment(final ShopRequest request, final String expectedComment) {
        enricher.enrichSafe(request, Collections.emptyList());
        assertThat(request.getComment(), equalTo(expectedComment));
    }

    private void enrichAndAssertUntaxedPrice(final VatRate vatRate,
                                             final String supplyPrice,
                                             final String expectedUntaxedPrice) {
        final RequestItem item = item(vatRate, supplyPrice);
        final Map<Long, EnrichmentResultContainer> errors =
            enricher.enrichSafe(EMPTY_REQUEST, Collections.singletonList(item));
        assertThat(errors, notNullValue());
        assertThat(errors.size(), equalTo(0));
        assertThat(item.getUntaxedPrice(), equalTo(new BigDecimal(expectedUntaxedPrice)));
    }

    private static ShopRequest request() {
        final ShopRequest request = new ShopRequest();
        request.setSupplier(new Supplier(SUPPLIER_ID, "supplier1", null, null, null, new SupplierBusinessType()));
        request.setRequestedDate(LocalDateTime.of(1984, 1, 1, 0, 0));
        request.setServiceId(MARSCHROUTE_ID);
        return request;
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

    private static ShopRequest xDocRequest(SupplierType supplierType) {
        final ShopRequest request = new ShopRequest();
        request.setId(ID_1);
        request.setSupplier(new Supplier(1, "supplier1", null, null, supplierType, new SupplierBusinessType()));
        request.setxDocRequestedDate(LocalDateTime.of(1984, 1, 1, 0, 0));
        request.setServiceId(MARSCHROUTE_ID);
        request.setxDocServiceId(2L);
        request.setType(RequestType.SUPPLY);
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

    private static RequestItem requestItem(List<String> cises, Long itemId, int itemCount, String article,
                                           VatRate vatRate, String supplyPrice) {
        Set<UnitPartialId> unitPartialIdSet = new HashSet<>();

        for (String cis: cises) {
            UnitPartialId unitPartialId = new UnitPartialId(RegistryUnitIdType.CIS, cis);
            unitPartialIdSet.add(unitPartialId);
        }

        Set<UnitPartialId> parts = new HashSet<>(unitPartialIdSet);
        RegistryUnitId registryUnitId = new RegistryUnitId(parts);

        Identifier identifier = new Identifier(itemId, registryUnitId, IdentifierType.DECLARED, null);
        Set<Identifier> requestItemIdentifiers = new HashSet<>(Set.of(identifier));

        RequestItem requestItem = new RequestItem();
        requestItem.setId(itemId);
        requestItem.setCount(itemCount);
        requestItem.setArticle(article);
        requestItem.setRequestItemIdentifiers(requestItemIdentifiers);
        requestItem.setVatRate(vatRate);
        requestItem.setSupplyPrice(new BigDecimal(supplyPrice));
        return requestItem;
    }
}
