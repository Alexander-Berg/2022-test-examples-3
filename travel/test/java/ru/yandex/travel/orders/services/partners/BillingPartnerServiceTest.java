package ru.yandex.travel.orders.services.partners;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.travel.commons.proto.EErrorCode;
import ru.yandex.travel.commons.proto.ErrorException;
import ru.yandex.travel.hotels.administrator.export.proto.HotelAgreement;
import ru.yandex.travel.hotels.common.orders.TravellineHotelItinerary;
import ru.yandex.travel.hotels.proto.EPartnerId;
import ru.yandex.travel.orders.cache.BalanceContractDictionary;
import ru.yandex.travel.orders.commons.proto.EVat;
import ru.yandex.travel.orders.entities.AeroflotOrderItem;
import ru.yandex.travel.orders.entities.BusOrderItem;
import ru.yandex.travel.orders.entities.DolphinOrderItem;
import ru.yandex.travel.orders.entities.ExpediaOrderItem;
import ru.yandex.travel.orders.entities.TrainOrderItem;
import ru.yandex.travel.orders.entities.TravellineOrderItem;
import ru.yandex.travel.orders.entities.partners.BillingPartnerConfig;
import ru.yandex.travel.orders.entities.partners.ExpediaBillingPartnerAgreement;
import ru.yandex.travel.orders.factories.SuburbanOrderItemEnvProviderFactory;
import ru.yandex.travel.orders.repository.BillingPartnerConfigRepository;
import ru.yandex.travel.orders.services.finances.HotelAgreementService;
import ru.yandex.travel.orders.services.finances.providers.DolphinFinancialDataProviderProperties;
import ru.yandex.travel.orders.services.hotels.Meters;
import ru.yandex.travel.orders.workflows.orderitem.aeroflot.configuration.AeroflotWorkflowProperties;
import ru.yandex.travel.orders.workflows.orderitem.bronevik.BronevikProperties;
import ru.yandex.travel.orders.workflows.orderitem.bus.BusProperties;
import ru.yandex.travel.orders.workflows.orderitem.expedia.ExpediaProperties;
import ru.yandex.travel.orders.workflows.orderitem.train.TrainWorkflowProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BillingPartnerServiceTest {
    private BillingPartnerConfigRepository partnerConfigRepository;
    private ExpediaProperties expediaProperties;
    private DolphinFinancialDataProviderProperties dolphinBillingProperties;
    private BronevikProperties bronevikProperties;
    private HotelAgreementService hotelAgreementService;
    private AeroflotWorkflowProperties aeroflotWorkflowProperties;
    private TrainWorkflowProperties trainWorkflowProperties;
    private BusProperties busProperties;
    private BillingPartnerService service;

    @Before
    public void init() {
        partnerConfigRepository = Mockito.mock(BillingPartnerConfigRepository.class);
        expediaProperties = new ExpediaProperties();
        dolphinBillingProperties = new DolphinFinancialDataProviderProperties();
        bronevikProperties = new BronevikProperties();
        hotelAgreementService = Mockito.mock(HotelAgreementService.class);
        aeroflotWorkflowProperties = new AeroflotWorkflowProperties();
        trainWorkflowProperties = new TrainWorkflowProperties();
        busProperties = new BusProperties();
        Meters meters = new Meters();
        BalanceContractDictionary balanceContractDictionary = Mockito.mock(BalanceContractDictionary.class);
        service = new BillingPartnerService(partnerConfigRepository, expediaProperties, dolphinBillingProperties,
                bronevikProperties, hotelAgreementService, meters, aeroflotWorkflowProperties,
                trainWorkflowProperties,
                SuburbanOrderItemEnvProviderFactory.createEnvProvider(),
                busProperties,
                balanceContractDictionary);
    }

    @Test
    public void isPartnerAgreementActive() {
        when(partnerConfigRepository.findById(1L)).thenReturn(Optional.of(config(true)));
        when(partnerConfigRepository.findById(2L)).thenReturn(Optional.of(config(false)));
        when(partnerConfigRepository.findById(3L)).thenReturn(Optional.empty());

        assertThat(service.isPartnerAgreementActive(1L)).isTrue();
        assertThat(service.isPartnerAgreementActive(2L)).isFalse();
        assertThatThrownBy(() -> service.isPartnerAgreementActive(3L))
                .isExactlyInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("No billing partner config for billing client id 3");
    }

    @Test
    public void addAgreementOrThrow_expediaOk() {
        expediaProperties.setBillingClientId(104L);
        when(partnerConfigRepository.findById(104L)).thenReturn(Optional.of(config(true)));

        ExpediaOrderItem orderItem = new ExpediaOrderItem();
        service.addAgreementOrThrow(orderItem);
        assertThat(orderItem.getBillingPartnerAgreement()).isNotNull()
                .satisfies(agreement -> assertThat(agreement.getBillingClientId()).isEqualTo(104L));
    }

    @Test
    public void addAgreementOrThrow_expediaDisabled() {
        expediaProperties.setBillingClientId(105L);
        when(partnerConfigRepository.findById(105L)).thenReturn(Optional.of(config(false)));

        ExpediaOrderItem orderItem = new ExpediaOrderItem();
        assertThatThrownBy(() -> service.addAgreementOrThrow(orderItem))
                .isExactlyInstanceOf(ErrorException.class)
                .satisfies(t -> {
                    ErrorException e = (ErrorException) t;
                    assertThat(e.getError().getCode()).isEqualTo(EErrorCode.EC_FAILED_PRECONDITION);
                    assertThat(e.getMessage()).contains("Partner agreement isn't active");
                });
    }

    @Test
    public void addAgreementOrThrow_dolphinOk() {
        dolphinBillingProperties.setBillingClientId(206L);
        when(partnerConfigRepository.findById(206L)).thenReturn(Optional.of(config(true)));

        DolphinOrderItem orderItem = new DolphinOrderItem();
        service.addAgreementOrThrow(orderItem);
        assertThat(orderItem.getBillingPartnerAgreement()).isNotNull()
                .satisfies(agreement -> assertThat(agreement.getBillingClientId()).isEqualTo(206L));
    }

    @Test
    public void addAgreementOrThrow_travellineOk() {
        TravellineHotelItinerary itinerary = mock(TravellineHotelItinerary.class, RETURNS_DEEP_STUBS);
        when(hotelAgreementService.getAgreementForTimestamp(any(), any(), any())).thenReturn(
                HotelAgreement.newBuilder()
                        .setId(1L)
                        .setHotelId("1")
                        .setPartnerId(EPartnerId.PI_TRAVELLINE)
                        .setInn("")
                        .setFinancialClientId(307L)
                        .setFinancialContractId(0L)
                        .setOrderConfirmedRate("0.1")
                        .setOrderRefundedRate("0.1")
                        .setAgreementStartDate(0)
                        .setEnabled(true)
                        .setVatType(EVat.VAT_NONE)
                        .setSendEmptyOrdersReport(true)
                        .build());
        when(partnerConfigRepository.findById(307L)).thenReturn(Optional.of(config(true)));

        TravellineOrderItem orderItem = new TravellineOrderItem();
        orderItem.setItinerary(itinerary);
        service.addAgreementOrThrow(orderItem);
        assertThat(orderItem.getBillingPartnerAgreement()).isNotNull()
                .satisfies(agreement -> assertThat(agreement.getBillingClientId()).isEqualTo(307L));
    }

    @Test
    public void addAgreementOrThrow_aeroflotOk() {
        aeroflotWorkflowProperties.setBillingClientId(408L);
        when(partnerConfigRepository.findById(408L)).thenReturn(Optional.of(config(true)));

        AeroflotOrderItem orderItem = new AeroflotOrderItem();
        service.addAgreementOrThrow(orderItem);
        assertThat(orderItem.getBillingPartnerAgreement()).isNotNull()
                .satisfies(agreement -> assertThat(agreement.getBillingClientId()).isEqualTo(408L));
    }

    @Test
    public void addAgreementOrThrow_trainOk() {
        trainWorkflowProperties.setBillingClientId(509L);
        when(partnerConfigRepository.findById(509L)).thenReturn(Optional.of(config(true)));

        TrainOrderItem orderItem = new TrainOrderItem();
        service.addAgreementOrThrow(orderItem);
        assertThat(orderItem.getBillingPartnerAgreement()).isNotNull()
                .satisfies(agreement -> assertThat(agreement.getBillingClientId()).isEqualTo(509L));
    }

    @Test
    public void addAgreementOrThrow_busOk() {
        busProperties.setBillingClientId(610L);
        when(partnerConfigRepository.findById(610L)).thenReturn(Optional.of(config(true)));

        BusOrderItem orderItem = new BusOrderItem();
        service.addAgreementOrThrow(orderItem);
        assertThat(orderItem.getBillingPartnerAgreement()).isNotNull()
                .satisfies(agreement -> assertThat(agreement.getBillingClientId()).isEqualTo(610L));
    }

    @Test
    public void ensureCanConfirmService() {
        expediaProperties.setBillingClientId(1205L);
        when(partnerConfigRepository.findById(1205L)).thenReturn(Optional.of(config(true)));

        ExpediaOrderItem orderItem = new ExpediaOrderItem();
        orderItem.setBillingPartnerAgreement(ExpediaBillingPartnerAgreement.builder()
                .billingClientId(1205L)
                .build());
        service.ensureCanConfirmService(orderItem);

        when(partnerConfigRepository.findById(1205L)).thenReturn(Optional.of(config(false)));
        assertThatThrownBy(() -> service.ensureCanConfirmService(orderItem))
                .isExactlyInstanceOf(ErrorException.class)
                .satisfies(t -> {
                    ErrorException e = (ErrorException) t;
                    assertThat(e.getError().getCode()).isEqualTo(EErrorCode.EC_FAILED_PRECONDITION);
                    assertThat(e.getMessage()).contains("Partner agreement isn't active");
                });
    }

    private BillingPartnerConfig config(boolean isActive) {
        return BillingPartnerConfig.builder()
                .agreementActive(isActive)
                .build();
    }
}
