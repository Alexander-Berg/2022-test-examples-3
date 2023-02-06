package ru.yandex.travel.orders.workflows.orderitem.train;

import java.math.BigDecimal;
import java.util.List;

import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.commons.proto.ProtoUtils;
import ru.yandex.travel.orders.commons.proto.EVat;
import ru.yandex.travel.orders.entities.FiscalItem;
import ru.yandex.travel.orders.entities.FiscalItemType;
import ru.yandex.travel.orders.entities.TrainOrder;
import ru.yandex.travel.orders.entities.TrainOrderItem;
import ru.yandex.travel.orders.entities.VatType;
import ru.yandex.travel.orders.factories.TrainOrderItemFactory;
import ru.yandex.travel.orders.management.StarTrekService;
import ru.yandex.travel.orders.repository.TrainOrderItemRepository;
import ru.yandex.travel.orders.services.finances.providers.TrainFinancialDataProvider;
import ru.yandex.travel.orders.services.train.FeeCalculationService;
import ru.yandex.travel.orders.services.train.RebookingService;
import ru.yandex.travel.orders.services.train.TrainMeters;
import ru.yandex.travel.orders.services.train.bandit.TrainBanditClient;
import ru.yandex.travel.orders.services.train.bandit.TrainBanditProperties;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.orders.workflow.orderitem.train.proto.TFeeCalculationCommit;
import ru.yandex.travel.orders.workflows.orderitem.train.handlers.CalculatingFeeStateHandler;
import ru.yandex.travel.train.model.FeeSource;
import ru.yandex.travel.train.model.ReservationPlaceType;
import ru.yandex.travel.train.model.TrainPlace;
import ru.yandex.travel.train_bandit_api.proto.TChargeStringCtx;
import ru.yandex.travel.train_bandit_api.proto.TGetChargeStringCtxResponse;
import ru.yandex.travel.train_bandit_api.proto.TTicketFee;
import ru.yandex.travel.workflow.entities.Workflow;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.orders.workflows.WorkflowTestUtils.testMessagingContext;

public class CalculatingFeeStateHandlerTest {

    private CalculatingFeeStateHandler handler;

    private TrainBanditClient trainBanditClient;

    private TrainBanditProperties trainBanditProperties;

    private TrainFinancialDataProvider trainFinancialDataProvider;

    @Before
    public void setUp() {
        var trainWorkflowProperties = new TrainWorkflowProperties();
        var billing = new TrainWorkflowProperties.BillingProperties();
        billing.setTicketFiscalTitle("Билет");
        billing.setServiceFiscalTitle("Доп услуги перевозчика");
        billing.setFeeFiscalTitle("Сбор Яндекса");
        billing.setFeeVat(EVat.VAT_20);
        trainWorkflowProperties.setBilling(billing);
        trainBanditClient = mock(TrainBanditClient.class);
        trainBanditProperties = new TrainBanditProperties();
        trainBanditProperties.setLocalPartnerFee(BigDecimal.valueOf(31.7));
        trainBanditProperties.setLocalPartnerRefundFee(BigDecimal.valueOf(31.7));
        trainBanditProperties.setLocalMinFeeFactor(BigDecimal.valueOf(0.01));
        trainBanditProperties.setLocalPermille(110);
        trainFinancialDataProvider = mock(TrainFinancialDataProvider.class);
        when(trainFinancialDataProvider.getYandexFeeVat(any())).thenReturn(VatType.VAT_20);
        FeeCalculationService feeCalculationService = new FeeCalculationService(trainBanditProperties, trainBanditClient, new TrainMeters());
        handler = new CalculatingFeeStateHandler(feeCalculationService, trainWorkflowProperties,
                mock(StarTrekService.class), mock(RebookingService.class), mock(TrainOrderItemRepository.class),
                trainFinancialDataProvider);
    }

    private FiscalItem findByInternalId(List<FiscalItem> fiscalItems, Integer internalId) {
        for (var item: fiscalItems) {
            if (item.getInternalId().equals(internalId)) {
                return item;
            }
        }
        throw new IllegalStateException(String.format("Fiscal item with internal id %s cannot be found in %s",
                internalId, fiscalItems.stream().map(FiscalItem::getInternalId).collect(toList())));
    }

    @Test
    public void testHandleFeeCalculationLocal() {
        var trainOrderItem = createTrainOrderItem();

        var ctx = testMessagingContext(trainOrderItem);
        handler.handleEvent(TFeeCalculationCommit.newBuilder().build(), ctx);

        assertThat(trainOrderItem.getPayload().getPermille()).isEqualTo(110);
        assertThat(trainOrderItem.getPayload().getFeeSource()).isEqualTo(FeeSource.LOCAL);

        var ticket = trainOrderItem.getPayload().getPassengers().get(0).getTicket();
        assertThat(ticket.getFeeAmount()).isEqualTo(Money.of(BigDecimal.valueOf(55.), ProtoCurrencyUnit.RUB));
        assertThat(ticket.getPartnerFee()).isEqualTo(Money.of(BigDecimal.valueOf(31.7), ProtoCurrencyUnit.RUB));
        assertThat(ticket.getPartnerRefundFee()).isEqualTo(Money.of(BigDecimal.valueOf(31.7), ProtoCurrencyUnit.RUB));
        assertThat(ticket.isBanditFeeApplied()).isFalse();

        var tariffFiscalId = ticket.getTariffFiscalItemInternalId();
        var tariffFiscalItem = findByInternalId(trainOrderItem.getFiscalItems(), tariffFiscalId);
        assertThat(tariffFiscalItem.getMoneyAmount()).isEqualTo(Money.of(BigDecimal.valueOf(300.01), ProtoCurrencyUnit.RUB));
        assertThat(tariffFiscalItem.getType()).isEqualTo(FiscalItemType.TRAIN_TICKET);
        assertThat(tariffFiscalItem.getVatType()).isEqualTo(VatType.VAT_20);
        assertThat(tariffFiscalItem.getInn()).isEqualTo("012345678912");
        assertThat(tariffFiscalItem.getTitle()).isEqualTo("Билет, место 011");

        var serviceFiscalId = ticket.getServiceFiscalItemInternalId();
        var serviceFiscalItem = findByInternalId(trainOrderItem.getFiscalItems(), serviceFiscalId);
        assertThat(serviceFiscalItem.getMoneyAmount()).isEqualTo(Money.of(BigDecimal.valueOf(200.01), ProtoCurrencyUnit.RUB));
        assertThat(serviceFiscalItem.getType()).isEqualTo(FiscalItemType.TRAIN_SERVICE);
        assertThat(serviceFiscalItem.getVatType()).isEqualTo(VatType.VAT_10);
        assertThat(serviceFiscalItem.getInn()).isEqualTo("012345678912");
        assertThat(serviceFiscalItem.getTitle()).isEqualTo("Доп услуги перевозчика, место 011");

        var feeFiscalId = ticket.getFeeFiscalItemInternalId();
        var feeFiscalItem = findByInternalId(trainOrderItem.getFiscalItems(), feeFiscalId);
        assertThat(feeFiscalItem.getMoneyAmount()).isEqualTo(Money.of(BigDecimal.valueOf(55), ProtoCurrencyUnit.RUB));
        assertThat(feeFiscalItem.getType()).isEqualTo(FiscalItemType.TRAIN_FEE);
        assertThat(feeFiscalItem.getVatType()).isEqualTo(VatType.VAT_20);
        assertThat(feeFiscalItem.getInn()).isNull();
        assertThat(feeFiscalItem.getTitle()).isEqualTo("Сбор Яндекса, место 011");
    }

    @Test
    public void testHandleFeeCalculationBandit() {
        trainBanditProperties.setEnabled(true);
        var trainOrderItem = createTrainOrderItem();
        trainOrderItem.getPayload().setBanditContext("<BanditContextString>");
        trainOrderItem.getPayload().getReservationRequestData().setBanditType("k-armed");
        when(trainBanditClient.getCharge(any())).thenReturn(createTGetChargeStringCtxResponse());

        var ctx = testMessagingContext(trainOrderItem);
        handler.handleEvent(TFeeCalculationCommit.newBuilder().build(), ctx);

        assertThat(trainOrderItem.getPayload().getPermille()).isEqualTo(120);
        assertThat(trainOrderItem.getPayload().getFeeSource()).isEqualTo(FeeSource.TRAIN_BANDIT_API);

        assertThat(trainOrderItem.getPayload().getBanditVersion()).isEqualTo(221);
        assertThat(trainOrderItem.getPayload().getBanditType()).isEqualTo("fix11");
        var ticket = trainOrderItem.getPayload().getPassengers().get(0).getTicket();
        assertThat(ticket.getFeeAmount()).isEqualTo(Money.of(BigDecimal.valueOf(111.11), ProtoCurrencyUnit.RUB));
        assertThat(ticket.getPartnerFee()).isEqualTo(Money.of(BigDecimal.valueOf(31.7), ProtoCurrencyUnit.RUB));
        assertThat(ticket.getPartnerRefundFee()).isEqualTo(Money.of(BigDecimal.valueOf(31.7), ProtoCurrencyUnit.RUB));
        assertThat(ticket.isBanditFeeApplied()).isTrue();

        var tariffFiscalId = ticket.getTariffFiscalItemInternalId();
        var tariffFiscalItem = findByInternalId(trainOrderItem.getFiscalItems(), tariffFiscalId);
        assertThat(tariffFiscalItem.getMoneyAmount()).isEqualTo(Money.of(BigDecimal.valueOf(300.01), ProtoCurrencyUnit.RUB));
        assertThat(tariffFiscalItem.getType()).isEqualTo(FiscalItemType.TRAIN_TICKET);
        assertThat(tariffFiscalItem.getVatType()).isEqualTo(VatType.VAT_20);
        assertThat(tariffFiscalItem.getInn()).isEqualTo("012345678912");
        assertThat(tariffFiscalItem.getTitle()).isEqualTo("Билет, место 011");

        var serviceFiscalId = ticket.getServiceFiscalItemInternalId();
        var serviceFiscalItem = findByInternalId(trainOrderItem.getFiscalItems(), serviceFiscalId);
        assertThat(serviceFiscalItem.getMoneyAmount()).isEqualTo(Money.of(BigDecimal.valueOf(200.01), ProtoCurrencyUnit.RUB));
        assertThat(serviceFiscalItem.getType()).isEqualTo(FiscalItemType.TRAIN_SERVICE);
        assertThat(serviceFiscalItem.getVatType()).isEqualTo(VatType.VAT_10);
        assertThat(serviceFiscalItem.getInn()).isEqualTo("012345678912");
        assertThat(serviceFiscalItem.getTitle()).isEqualTo("Доп услуги перевозчика, место 011");

        var feeFiscalId = ticket.getFeeFiscalItemInternalId();
        var feeFiscalItem = findByInternalId(trainOrderItem.getFiscalItems(), feeFiscalId);
        assertThat(feeFiscalItem.getMoneyAmount()).isEqualTo(Money.of(BigDecimal.valueOf(111.11), ProtoCurrencyUnit.RUB));
        assertThat(feeFiscalItem.getType()).isEqualTo(FiscalItemType.TRAIN_FEE);
        assertThat(feeFiscalItem.getVatType()).isEqualTo(VatType.VAT_20);
        assertThat(feeFiscalItem.getInn()).isNull();
        assertThat(feeFiscalItem.getTitle()).isEqualTo("Сбор Яндекса, место 011");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongRate() {
        var trainOrderItem = createTrainOrderItem();
        var ticket = trainOrderItem.getReservation().getPassengers().get(0).getTicket();
        ticket.setTariffVatRate(100500.0D);

        var ctx = testMessagingContext(trainOrderItem);
        handler.handleEvent(TFeeCalculationCommit.newBuilder().build(), ctx);
    }

    private TrainOrderItem createTrainOrderItem() {
        var factory = new TrainOrderItemFactory();
        factory.setOrderItemState(EOrderItemState.IS_RESERVED);
        var trainOrderItem = factory.createTrainOrderItem();
        trainOrderItem.getReservation().getPassengers().get(0).setCustomerId(555);
        var ticket = trainOrderItem.getReservation().getPassengers().get(0).getTicket();
        ticket.setTariffAmount(Money.of(BigDecimal.valueOf(300.01), ProtoCurrencyUnit.RUB));
        ticket.setTariffVatRate(20.);
        ticket.setServiceAmount(Money.of(BigDecimal.valueOf(200.01), ProtoCurrencyUnit.RUB));
        ticket.setServiceVatRate(10.);
        ticket.setCarrierInn("012345678912");
        ticket.getPlaces().clear();
        ticket.getPlaces().add(new TrainPlace("011", ReservationPlaceType.NEAR_TABLE));
        var order = new TrainOrder();
        order.setWorkflow(new Workflow());
        order.addOrderItem(trainOrderItem);
        return trainOrderItem;
    }

    private TGetChargeStringCtxResponse createTGetChargeStringCtxResponse() {
        var response = TGetChargeStringCtxResponse.newBuilder();
        var ticketFee = TTicketFee.newBuilder();
        ticketFee.setFee(ProtoUtils.toTPrice(Money.of(110.1, ProtoCurrencyUnit.RUB)));
        ticketFee.setServiceFee(ProtoUtils.toTPrice(Money.of(1.01, ProtoCurrencyUnit.RUB)));
        ticketFee.setIsBanditFeeApplied(true);
        var charges = TChargeStringCtx.newBuilder();
        charges.putTicketFees(0, ticketFee.build());
        charges.setBanditType("fix11");
        charges.setBanditVersion(221);
        charges.setPermille(120);
        response.addChargesByContexts(charges.build());
        response.setPartnerFee(ProtoUtils.toTPrice(Money.of(31.7, ProtoCurrencyUnit.RUB)));
        response.setPartnerRefundFee(ProtoUtils.toTPrice(Money.of(31.7, ProtoCurrencyUnit.RUB)));
        return response.build();
    }
}
