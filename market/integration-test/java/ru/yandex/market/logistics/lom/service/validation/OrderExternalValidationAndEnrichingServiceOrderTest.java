package ru.yandex.market.logistics.lom.service.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.Getter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdPayload;
import ru.yandex.market.logistics.lom.jobs.processor.validation.OrderExternalValidationAndEnrichingService;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichResults;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.AssessedValueTotalValidator;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.BillingEntityBillableEnricher;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.CredentialsValidatorAndEnricher;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.DeliveryIntervalValidator;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.LastMileLocationEnricher;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.LogisticsPointValidatorAndEnricher;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.OrderCostTariffCodeEnricher;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.OrderValidatorAndEnricher;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.PartnerDataValidatorAndEnricher;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.PickupAddressValidatorAndEnricher;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.PostalCodeValidator;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.ReturnSortingCenterSegmentEnricher;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.ReturnWarehouseValidatorAndEnricher;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.SenderLegalInfoEnricher;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.WarehouseLegalInfoValidatorAndEnricher;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.WaybillCredentialsValidatorAndEnricher;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.WaybillSegmentB2BTagEnricher;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.WaybillSegmentExpressTagEnricher;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.WaybillSegmentTransferCodesEnricher;
import ru.yandex.market.logistics.lom.service.marketid.MarketIdService;
import ru.yandex.market.logistics.lom.utils.LmsFactory;
import ru.yandex.market.logistics.lom.utils.MarketIdFactory;
import ru.yandex.market.logistics.lom.utils.ResultCaptor;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PointType;

import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createOrderIdPayload;

@SpyBean({
    AssessedValueTotalValidator.class,
    CredentialsValidatorAndEnricher.class,
    DeliveryIntervalValidator.class,
    LastMileLocationEnricher.class,
    LogisticsPointValidatorAndEnricher.class,
    OrderCostTariffCodeEnricher.class,
    PartnerDataValidatorAndEnricher.class,
    PickupAddressValidatorAndEnricher.class,
    PostalCodeValidator.class,
    ReturnWarehouseValidatorAndEnricher.class,
    ReturnSortingCenterSegmentEnricher.class,
    SenderLegalInfoEnricher.class,
    WaybillCredentialsValidatorAndEnricher.class,
    WarehouseLegalInfoValidatorAndEnricher.class,
    WaybillSegmentTransferCodesEnricher.class,
    WaybillSegmentExpressTagEnricher.class,
    WaybillSegmentB2BTagEnricher.class,
    BillingEntityBillableEnricher.class,
})
class OrderExternalValidationAndEnrichingServiceOrderTest extends AbstractContextualTest {

    private static final Long ORDER_ID = 1L;
    private static final Long PICKUP_POINT_ID = 10L;
    private static final Long WAREHOUSE_ID = 11L;
    private static final Long MARKET_ID_FROM = 111L;
    private static final Long PARTNER_MARKET_ID = 3333L;

    private static final OrderIdPayload VALIDATION_PAYLOAD = createOrderIdPayload(ORDER_ID);
    protected static final long WAREHOUSE_PARTNER_ID = 111L;
    protected static final long PICKUP_PARTNER_ID = 96L;
    protected static final long WAREHOUSE_MARKET_ID = 400L;
    protected static final long PICKUP_MARKET_ID = 300L;

    private static final LogisticsPointFilter WAREHOUSE_FILTER = LogisticsPointFilter.newBuilder()
        .partnerIds(Set.of(1111L))
        .type(PointType.WAREHOUSE)
        .active(true)
        .build();

    private static final LogisticsPointFilter WAREHOUSE_PARTNER_FILTER = LogisticsPointFilter.newBuilder()
        .ids(Set.of(WAREHOUSE_ID))
        .build();

    @Autowired
    private OrderExternalValidationAndEnrichingService orderExternalValidationAndEnrichingService;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private MarketIdService marketIdService;

    @Autowired
    private AssessedValueTotalValidator assessedValueTotalValidator;
    @Autowired
    private DeliveryIntervalValidator deliveryIntervalValidator;
    @Autowired
    private CredentialsValidatorAndEnricher credentialsValidatorAndEnricher;
    @Autowired
    private LastMileLocationEnricher lastMileLocationEnricher;
    @Autowired
    private OrderCostTariffCodeEnricher orderCostTariffCodeEnricher;
    @Autowired
    private PartnerDataValidatorAndEnricher partnerDataValidatorAndEnricher;
    @Autowired
    private WaybillSegmentTransferCodesEnricher waybillSegmentTransferCodesEnricher;
    @Autowired
    private PickupAddressValidatorAndEnricher pickupAddressValidatorAndEnricher;
    @Autowired
    private PostalCodeValidator postalCodeValidator;
    @Autowired
    private ReturnWarehouseValidatorAndEnricher returnWarehouseValidatorAndEnricher;
    @Autowired
    private ReturnSortingCenterSegmentEnricher returnSortingCenterSegmentEnricher;
    @Autowired
    private WaybillCredentialsValidatorAndEnricher waybillCredentialsValidatorAndEnricher;
    @Autowired
    private LogisticsPointValidatorAndEnricher warehouseAddressesValidatorAndEnricher;
    @Autowired
    private WarehouseLegalInfoValidatorAndEnricher warehouseLegalInfoValidatorAndEnricher;
    @Autowired
    private WaybillSegmentExpressTagEnricher waybillSegmentExpressTagEnricher;
    @Autowired
    private WaybillSegmentB2BTagEnricher waybillSegmentB2BTagEnricher;
    @Autowired
    private BillingEntityBillableEnricher billingEntityBillableEnricher;
    @Autowired
    private SenderLegalInfoEnricher senderLegalInfoEnricher;

    @SuppressWarnings("unchecked")
    public static <R, G extends R> G spyLambda(final G lambda, final Class<R> lambdaType) {
        return (G) mock(lambdaType, delegatesTo(lambda));
    }

    @Getter
    public static class EnrichResultsCaptor extends ResultCaptor<ValidateAndEnrichResults> {

        private ResultCaptor<Function<Order, Order>> functionCaptor;

        EnrichResultsCaptor() {
            super(Mockito::spy);
        }

        @SuppressWarnings("ResultOfMethodCallIgnored")
        @Override
        public ValidateAndEnrichResults answer(InvocationOnMock invocation) throws Throwable {
            ValidateAndEnrichResults results = super.answer(invocation);
            functionCaptor = new ResultCaptor<>(r -> spyLambda(r, Function.class));
            doAnswer(functionCaptor).when(results).getOrderModifier();
            return results;
        }
    }

    @Test
    @DisplayName("Удачная валидация и обогащение заказа - порядок вызова энричеров")
    @DatabaseSetup("/service/externalvalidation/before/validating_success_order.xml")
    void validateOrderOrder() {
        mockLms();
        List<OrderValidatorAndEnricher> orderValidatorAndEnrichers = List.of(
            senderLegalInfoEnricher,
            deliveryIntervalValidator,
            pickupAddressValidatorAndEnricher,
            credentialsValidatorAndEnricher,
            returnWarehouseValidatorAndEnricher,
            returnSortingCenterSegmentEnricher,
            partnerDataValidatorAndEnricher,
            lastMileLocationEnricher,
            warehouseAddressesValidatorAndEnricher,
            warehouseLegalInfoValidatorAndEnricher,
            assessedValueTotalValidator,
            postalCodeValidator,
            waybillCredentialsValidatorAndEnricher,
            orderCostTariffCodeEnricher,
            waybillSegmentTransferCodesEnricher,
            waybillSegmentExpressTagEnricher,
            waybillSegmentB2BTagEnricher,
            billingEntityBillableEnricher
        );

        InOrder validateAndEnrichOrder = inOrder(orderValidatorAndEnrichers.toArray());

        List<EnrichResultsCaptor> captors = new ArrayList<>();

        orderValidatorAndEnrichers.forEach(v -> {
            EnrichResultsCaptor resultCaptor = new EnrichResultsCaptor();
            doAnswer(resultCaptor).when(v).validateAndEnrich(any(), any());
            captors.add(resultCaptor);
        });

        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);

        orderValidatorAndEnrichers.forEach(v -> validateAndEnrichOrder.verify(v).validateAndEnrich(any(), any()));

        List<Function<Order, Order>> functions = captors.stream()
            .map(EnrichResultsCaptor::getFunctionCaptor)
            .map(ResultCaptor::getResult)
            .collect(Collectors.toList());

        InOrder resultsFunctionsOrder = inOrder(functions.toArray());
        functions.forEach(f -> resultsFunctionsOrder.verify(f).apply(any()));
        resultsFunctionsOrder.verifyNoMoreInteractions();
        verifyLms();
        verifyGetCredentials();
        verifyNoMoreInteractions(lmsClient, marketIdService);
    }

    private void mockLms() {
        mockGetCredentials();

        List<PartnerResponse> partnerResponseList = List.of(
            LmsFactory.createPartnerResponse(48, PARTNER_MARKET_ID),
            LmsFactory.createPartnerResponse(1111, "SC", PARTNER_MARKET_ID)
        );

        when(lmsClient.getLogisticsPoints(WAREHOUSE_FILTER)).thenReturn(List.of(getWarehousePoint()));
        when(lmsClient.getLogisticsPoints(WAREHOUSE_PARTNER_FILTER)).thenReturn(List.of(getWarehousePoint()));
        when(lmsClient.searchPartners(partnerFilter(48L, 1111L))).thenReturn(partnerResponseList);
        when(lmsClient.searchPartners(partnerFilter(PICKUP_PARTNER_ID, WAREHOUSE_PARTNER_ID))).thenReturn(List.of(
            LmsFactory.createPartnerResponse(PICKUP_PARTNER_ID, PICKUP_MARKET_ID),
            LmsFactory.createPartnerResponse(WAREHOUSE_PARTNER_ID, WAREHOUSE_MARKET_ID)
        ));

        partnerResponseList.forEach(r -> when(lmsClient.getPartner(eq(r.getId()))).thenReturn(Optional.of(r)));

        when(lmsClient.getLogisticsPoint(PICKUP_POINT_ID)).thenReturn(Optional.of(getPickupPoint()));
    }

    private void verifyLms() {
        verify(lmsClient).getLogisticsPoint(PICKUP_POINT_ID);
        verify(lmsClient).getLogisticsPoints(WAREHOUSE_FILTER);
        verify(lmsClient).searchPartners(partnerFilter(48L, 1111L));
        verify(lmsClient).getLogisticsPoints(WAREHOUSE_PARTNER_FILTER);
        verify(lmsClient).searchPartners(partnerFilter(PICKUP_PARTNER_ID, WAREHOUSE_PARTNER_ID));
    }

    @Nonnull
    private SearchPartnerFilter partnerFilter(long l, long l2) {
        return SearchPartnerFilter.builder().setIds(Set.of(l, l2)).build();
    }

    private void mockGetCredentials() {
        Stream.of(MARKET_ID_FROM, WAREHOUSE_MARKET_ID, PARTNER_MARKET_ID)
            .forEach(id -> when(marketIdService.findAccountById(id))
                .thenReturn(Optional.of(MarketIdFactory.marketAccount()))
            );
    }

    private void verifyGetCredentials() {
        verify(marketIdService).findAccountById(MARKET_ID_FROM);
        verify(marketIdService).findAccountById(WAREHOUSE_MARKET_ID);
        verify(marketIdService, times(2)).findAccountById(PARTNER_MARKET_ID);
    }

    @Nonnull
    private LogisticsPointResponse getPickupPoint() {
        return LmsFactory.createPickupPointResponseBuilder(PICKUP_POINT_ID).partnerId(PICKUP_PARTNER_ID).build();
    }

    @Nonnull
    private LogisticsPointResponse getWarehousePoint() {
        return LmsFactory.createWarehouseResponseBuilder(WAREHOUSE_ID).partnerId(WAREHOUSE_PARTNER_ID).build();
    }
}
