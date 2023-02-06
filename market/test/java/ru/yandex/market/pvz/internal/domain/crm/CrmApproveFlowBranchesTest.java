package ru.yandex.market.pvz.internal.domain.crm;

import java.time.LocalDate;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.mbi.api.client.entity.shops.SimpleShopRegistrationResponse;
import ru.yandex.market.pvz.client.crm.dto.PreLegalPartnerCrmDto;
import ru.yandex.market.pvz.client.crm.dto.PrePickupPointCrmDto;
import ru.yandex.market.pvz.client.model.approve.PreLegalPartnerApproveStatus;
import ru.yandex.market.pvz.client.model.approve.PrePickupPointApproveStatus;
import ru.yandex.market.pvz.core.domain.approve.MbiCabinetService;
import ru.yandex.market.pvz.core.domain.approve.crm_pre_pickup_point.CrmPrePickupPointParams;
import ru.yandex.market.pvz.core.domain.approve.crm_pre_pickup_point.CrmPrePickupPointQueryService;
import ru.yandex.market.pvz.core.domain.approve.pre_legal_partner.PreLegalPartnerCommandService;
import ru.yandex.market.pvz.core.domain.approve.pre_legal_partner.PreLegalPartnerParams;
import ru.yandex.market.pvz.core.domain.approve.pre_legal_partner.PreLegalPartnerQueryService;
import ru.yandex.market.pvz.core.domain.approve.startrek.partner_security.PartnerSecurityWorkflowResolution;
import ru.yandex.market.pvz.core.domain.approve.startrek.partner_security.handlers.PartnerSecurityAnyTransitionHandler;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.logbroker.crm.CrmLogbrokerEventMapper;
import ru.yandex.market.pvz.core.domain.logbroker.crm.consume.pre_legal_partner.PreLegalPartnerActiveEventHandler;
import ru.yandex.market.pvz.core.domain.logbroker.crm.consume.pre_pickup_point.CrmPrePickupPointApprovedEventHandler;
import ru.yandex.market.pvz.core.domain.logbroker.crm.consume.pre_pickup_point.CrmPrePickupPointAwaitingSecurityCheckEventHandler;
import ru.yandex.market.pvz.core.domain.logbroker.crm.consume.pre_pickup_point.CrmPrePickupPointLocationBookedEventHandler;
import ru.yandex.market.pvz.core.domain.logbroker.crm.produce.CrmLogbrokerEventPublisher;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestCrmPrePickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory;
import ru.yandex.market.pvz.internal.PvzIntTest;
import ru.yandex.market.tpl.common.startrek.ticket.StartrekTicket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.CRM_PVZ_SUPPORT_CARD;
import static ru.yandex.market.pvz.core.test.factory.TestCrmPrePickupPointFactory.CrmPrePickupPointTestParams;
import static ru.yandex.market.pvz.core.test.factory.TestCrmPrePickupPointFactory.CrmPrePickupPointTestParamsBuilder;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_CARD_COMPENSATION_RATE;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_CASH_COMPENSATION_RATE;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_TRANSMISSION_REWARD;

@PvzIntTest
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class CrmApproveFlowBranchesTest {

    private static final String SECURITY_TICKET_KEY = "PVZDD-123";
    private static final String SECURITY_TICKET_STATUS = "checking";
    private static final long DEFAULT_OWNER_UID = 42;

    private final TestableClock clock;
    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestPreLegalPartnerFactory preLegalPartnerFactory;
    private final TestCrmPrePickupPointFactory crmPrePickupPointFactory;

    private final CrmPrePickupPointApprovedEventHandler pickupPointApprovedEventHandler;
    private final CrmPrePickupPointAwaitingSecurityCheckEventHandler pickupPointAwaitingSecurityCheckEventHandler;
    private final CrmPrePickupPointLocationBookedEventHandler pickupPointLocationBookedEventHandler;
    private final PreLegalPartnerActiveEventHandler preLegalPartnerActiveEventHandler;

    private final PartnerSecurityAnyTransitionHandler partnerSecurityAnyTransitionHandler;

    private final PreLegalPartnerCommandService preLegalPartnerCommandService;
    private final CrmPrePickupPointQueryService crmPrePickupPointQueryService;
    private final PreLegalPartnerQueryService preLegalPartnerQueryService;
    private final CrmLogbrokerEventMapper crmLogbrokerEventMapper;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;

    @MockBean
    private MbiCabinetService mbiCabinetService;

    @MockBean
    private CrmLogbrokerEventPublisher crmLogbrokerEventPublisher;

    @BeforeEach
    void setup() {
        when(mbiCabinetService.createForPrePickupPoint(any(), any())).thenReturn(new SimpleShopRegistrationResponse() {{
            setOwnerId(DEFAULT_OWNER_UID);
        }});
    }

    /**
     * Партнёр создал ПВЗ, его зааппрувили, но дальше не двигаемся,
     * так как партнёр ещё не одобрен безопасниками
     */
    @Test
    void testApprovePickupPointWhilePartnerNotApprovedBySecurirty() {
        PreLegalPartnerParams partner = createPartnerLead(PreLegalPartnerApproveStatus.APPROVED);
        CrmPrePickupPointParams pvz = createPvzLead(partner, PrePickupPointApproveStatus.CHECKING);

        pickupPointApprovedEventHandler.handle(pvzDtoWithStatus(pvz, PrePickupPointApproveStatus.APPROVED));

        assertThat(getUpdated(partner).getApproveStatus()).isEqualTo(PreLegalPartnerApproveStatus.APPROVED);
        assertThat(getUpdated(pvz).getStatus()).isEqualTo(PrePickupPointApproveStatus.APPROVED);
        assertThat(getUpdated(pvz).getPickupPointId()).isNull();
    }

    /**
     * Партнёра одобрили безопасники, но дальше не двигаемся,
     * так как у него ещё нет ни одного ПВЗ
     */
    @Test
    void testApproveBySecurityPartnerWithoutPickupPoint() {
        PreLegalPartnerParams partner = createPartnerLead(PreLegalPartnerApproveStatus.APPROVED);

        partnerSecurityAnyTransitionHandler.handleTransition(fakeSecurityTicket());

        assertThat(getUpdated(partner).getApproveStatus()).isEqualTo(PreLegalPartnerApproveStatus.APPROVED_BY_SECURITY);
    }

    /**
     * Партнёра одобрили безопасники, но его ПВЗ находится ещё на одобрении.
     * Сразу переходим к подписанию оферты.
     */
    @Test
    void testApproveBySecurityPartnerWithoutApprovedPickupPoint() {
        PreLegalPartnerParams partner = createPartnerLead(PreLegalPartnerApproveStatus.APPROVED);
        CrmPrePickupPointParams pvz = createPvzLead(partner, PrePickupPointApproveStatus.CHECKING);

        partnerSecurityAnyTransitionHandler.handleTransition(fakeSecurityTicket());

        assertThat(getUpdated(partner).getApproveStatus()).isEqualTo(PreLegalPartnerApproveStatus.APPROVED_BY_SECURITY);
        assertThat(getUpdated(pvz).getStatus()).isEqualTo(PrePickupPointApproveStatus.CHECKING);
        assertThat(getUpdated(pvz).getPickupPointId()).isNull();
    }

    /**
     * Партнёр создал ПВЗ, его зааппрувили, безопасниками партнёр тоже одобрен,
     * проверяем, что идём по флоу дальше (в ожидание подписания оферты)
     */
    @Test
    void testApprovePickupPointForPartnerApprovedBySecurirty() {
        PreLegalPartnerParams partner = createPartnerLead(PreLegalPartnerApproveStatus.APPROVED_BY_SECURITY);
        CrmPrePickupPointParams pvz = createPvzLead(partner, PrePickupPointApproveStatus.CHECKING);

        pickupPointApprovedEventHandler.handle(pvzDtoWithStatus(pvz, PrePickupPointApproveStatus.APPROVED));

        assertThat(getUpdated(partner).getApproveStatus())
                .isEqualTo(PreLegalPartnerApproveStatus.OFFER_SIGNATURE_REQUIRED);
        assertThat(getUpdated(pvz).getStatus()).isEqualTo(PrePickupPointApproveStatus.APPROVED);
        assertThat(getUpdated(pvz).getPickupPointId()).isNull();
    }

    /**
     * Партнёра одобрили безопасники, зааппрувленный ПВЗ у него тоже уже есть,
     * проверяем, что идём по флоу дальше (аналогично верхнему, но в другом порядке)
     */
    @Test
    void testApproveBySecurirtyPartnerWithApprovedPickupPoint() {
        PreLegalPartnerParams partner = createPartnerLead(PreLegalPartnerApproveStatus.APPROVED);
        CrmPrePickupPointParams pvz = createPvzLead(partner, PrePickupPointApproveStatus.APPROVED);

        partnerSecurityAnyTransitionHandler.handleTransition(fakeSecurityTicket());

        assertThat(getUpdated(partner).getApproveStatus())
                .isEqualTo(PreLegalPartnerApproveStatus.OFFER_SIGNATURE_REQUIRED);
        assertThat(getUpdated(pvz).getStatus()).isEqualTo(PrePickupPointApproveStatus.APPROVED);
        assertThat(getUpdated(pvz).getPickupPointId()).isNull();
    }

    /**
     * Партнёр создал ПВЗ, он ожидает договора-аренду, безопасниками партнёр тоже одобрен,
     * проверяем, что партнёр попадает тоже в ожидание договора-аренды
     */
    @Test
    void testAwaitForLeasePickupPointForActivePartner() {
        PreLegalPartnerParams partner = createPartnerLead(PreLegalPartnerApproveStatus.APPROVED_BY_SECURITY);
        CrmPrePickupPointParams pvz = createPvzLead(partner, PrePickupPointApproveStatus.CHECKING);

        pickupPointAwaitingSecurityCheckEventHandler.handle(
                pvzDtoWithStatus(pvz, PrePickupPointApproveStatus.AWAITING_SECURITY_CHECK));

        assertThat(getUpdated(partner).getApproveStatus())
                .isEqualTo(PreLegalPartnerApproveStatus.LEASE_AGREEMENT_REQUIRED);
        assertThat(getUpdated(pvz).getStatus()).isEqualTo(PrePickupPointApproveStatus.LEASE_AGREEMENT_REQUIRED);
        assertThat(getUpdated(pvz).getLeaseAgreementRequiredSinceDate()).isEqualTo(LocalDate.now(clock));
        assertThat(getUpdated(pvz).getPickupPointId()).isNull();
    }

    /**
     * Уже активный Партнёр создал ПВЗ, он ожидает договора-аренду
     * проверяем, что ПВЗ попадает в ожидание договора-аренды
     */
    @Test
    void testAwaitForLeasePickupPointForPartnerApprovedBySecurirty() {
        PreLegalPartnerParams partner = createPartnerLead(PreLegalPartnerApproveStatus.ACTIVE, true);
        CrmPrePickupPointParams pvz = createPvzLead(partner, PrePickupPointApproveStatus.CHECKING);

        pickupPointAwaitingSecurityCheckEventHandler.handle(
                pvzDtoWithStatus(pvz, PrePickupPointApproveStatus.AWAITING_SECURITY_CHECK));

        assertThat(getUpdated(partner).getApproveStatus())
                .isEqualTo(PreLegalPartnerApproveStatus.ACTIVE);
        assertThat(getUpdated(pvz).getStatus()).isEqualTo(PrePickupPointApproveStatus.LEASE_AGREEMENT_REQUIRED);
        assertThat(getUpdated(pvz).getLeaseAgreementRequiredSinceDate()).isEqualTo(LocalDate.now(clock));
        assertThat(getUpdated(pvz).getPickupPointId()).isNull();
    }

    /**
     * Партнёра одобрили безопасники, ожидающий договора-аренду ПВЗ у него тоже уже есть,
     * проверяем, что партнёр попадает тоже в ожидание договора-аренды (аналогично верхнему, но в другом порядке)
     */
    @Test
    void testApproveBySecurirtyPartnerWithAwaitingForLeasePickupPoint() {
        PreLegalPartnerParams partner = createPartnerLead(PreLegalPartnerApproveStatus.APPROVED);
        CrmPrePickupPointParams pvz = createPvzLead(partner, PrePickupPointApproveStatus.AWAITING_SECURITY_CHECK);

        partnerSecurityAnyTransitionHandler.handleTransition(fakeSecurityTicket());

        assertThat(getUpdated(partner).getApproveStatus())
                .isEqualTo(PreLegalPartnerApproveStatus.LEASE_AGREEMENT_REQUIRED);
        assertThat(getUpdated(pvz).getStatus()).isEqualTo(PrePickupPointApproveStatus.LEASE_AGREEMENT_REQUIRED);
        assertThat(getUpdated(pvz).getLeaseAgreementRequiredSinceDate()).isEqualTo(LocalDate.now(clock));
        assertThat(getUpdated(pvz).getPickupPointId()).isNull();
    }

    /**
     * Проверяем, что попадаем в статус на подписание оферты и бронируем локацию
     * после предоставления договора-аренды, если оферта не подписана
     */
    @Test
    void testOfferSignatureRequestedAfterLeaseAgreement() {
        PreLegalPartnerParams partner = createPartnerLead(PreLegalPartnerApproveStatus.LEASE_AGREEMENT_REQUIRED);
        CrmPrePickupPointParams pvz = createPvzLead(partner, PrePickupPointApproveStatus.LEASE_AGREEMENT_REQUIRED);

        pickupPointLocationBookedEventHandler.handle(
                pvzDtoWithStatus(pvz, PrePickupPointApproveStatus.LOCATION_BOOKED));

        assertThat(getUpdated(partner).getApproveStatus())
                .isEqualTo(PreLegalPartnerApproveStatus.OFFER_SIGNATURE_REQUIRED);
        assertThat(getUpdated(pvz).getStatus()).isEqualTo(PrePickupPointApproveStatus.LOCATION_BOOKED);
        assertThat(getUpdated(pvz).getPickupPointId()).isNull();
    }

    /**
     * Проверяем, что скипаем подписание оферты и активируем ПВЗ (небренд),
     * если оферта уже была подписана
     */
    @Test
    void testActivatedAfterApprove() {
        configurationGlobalCommandService.setValue(CRM_PVZ_SUPPORT_CARD, true);
        pickupPointFactory.addFirstDeactivationReason();

        PreLegalPartnerParams partner = createPartnerLead(PreLegalPartnerApproveStatus.ACTIVE, true);
        CrmPrePickupPointParams pvz = createPvzLead(partner, PrePickupPointApproveStatus.CHECKING);

        pickupPointApprovedEventHandler.handle(pvzDtoWithStatus(pvz, PrePickupPointApproveStatus.APPROVED).toBuilder()
                .cashCompensation(DEFAULT_CASH_COMPENSATION_RATE)
                .cardCompensation(DEFAULT_CARD_COMPENSATION_RATE)
                .orderTransmissionReward(DEFAULT_TRANSMISSION_REWARD)
                .build());

        assertThat(getUpdated(partner).getApproveStatus()).isEqualTo(PreLegalPartnerApproveStatus.ACTIVE);
        assertThat(getUpdated(pvz).getStatus()).isEqualTo(PrePickupPointApproveStatus.APPROVED);
        assertThat(getUpdated(pvz).getPickupPointId()).isNotNull();

        verify(crmLogbrokerEventPublisher, times(1)).publish(isA(PickupPoint.class));
    }

    /**
     * Проверяем, что активируем аппрувнутый ПВЗ, ожидающий подписания оферты
     */
    @Test
    void testActivatedAfterOfferChecked() {
        configurationGlobalCommandService.setValue(CRM_PVZ_SUPPORT_CARD, true);
        pickupPointFactory.addFirstDeactivationReason();

        PreLegalPartnerParams partner = createPartnerLead(PreLegalPartnerApproveStatus.OFFER_CHECK_REQUIRED, false);
        CrmPrePickupPointParams pvz = createPvzLead(partner, PrePickupPointApproveStatus.CHECKING);

        pickupPointApprovedEventHandler.handle(pvzDtoWithStatus(pvz, PrePickupPointApproveStatus.APPROVED).toBuilder()
                .cashCompensation(DEFAULT_CASH_COMPENSATION_RATE)
                .cardCompensation(DEFAULT_CARD_COMPENSATION_RATE)
                .orderTransmissionReward(DEFAULT_TRANSMISSION_REWARD)
                .build());

        assertThat(getUpdated(pvz).getStatus()).isEqualTo(PrePickupPointApproveStatus.APPROVED);
        assertThat(getUpdated(pvz).getPickupPointId()).isNull();

        preLegalPartnerActiveEventHandler.handle(partnerDtoWithStatus(partner, PreLegalPartnerApproveStatus.ACTIVE));

        assertThat(getUpdated(partner).getApproveStatus()).isEqualTo(PreLegalPartnerApproveStatus.ACTIVE);
        assertThat(getUpdated(pvz).getStatus()).isEqualTo(PrePickupPointApproveStatus.APPROVED);
        assertThat(getUpdated(pvz).getPickupPointId()).isNotNull();

        verify(crmLogbrokerEventPublisher, times(1)).publish(isA(PickupPoint.class));
    }

    /**
     * Проверяем переход в статус проверки оферты после загрузки оферты
     */
    @Test
    void testOfferCheckRequiredAfterOfferSigned() {
        PreLegalPartnerParams partner = createPartnerLead(PreLegalPartnerApproveStatus.OFFER_SIGNATURE_REQUIRED, true);
        CrmPrePickupPointParams pvz = createPvzLead(partner, PrePickupPointApproveStatus.CHECKING);

        var result = preLegalPartnerCommandService.checkOffer(partner.getPartnerId());

        assertThat(result.getApproveStatus()).isEqualTo(PreLegalPartnerApproveStatus.OFFER_CHECK_REQUIRED);
    }

    private StartrekTicket fakeSecurityTicket() {
        StartrekTicket ticket = mock(StartrekTicket.class, Mockito.RETURNS_DEEP_STUBS);
        when(ticket.getKey()).thenReturn(SECURITY_TICKET_KEY);
        when(ticket.getStatusKey()).thenReturn(SECURITY_TICKET_STATUS);
        when(ticket.getResolution(PartnerSecurityWorkflowResolution.class))
                .thenReturn(Optional.of(PartnerSecurityWorkflowResolution.APPROVED));
        return ticket;
    }

    private PreLegalPartnerParams createPartnerLead(PreLegalPartnerApproveStatus status) {
        return createPartnerLead(status, false);
    }

    private PreLegalPartnerParams createPartnerLead(PreLegalPartnerApproveStatus status, boolean isOfferSigned) {
        var preLegalPartner = preLegalPartnerFactory.createPreLegalPartner();
        var legalPartner = legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .preLegalPartner(preLegalPartner)
                        .approvePreLegalPartner(false)
                        .build()
        );
        if (isOfferSigned) {
            legalPartnerFactory.forceApproveWithOffer(legalPartner.getId(), LocalDate.EPOCH);
        }
        preLegalPartner = preLegalPartnerFactory.forceChangeStatus(preLegalPartner.getId(), status);
        return preLegalPartnerCommandService.bindSecurityTicket(preLegalPartner.getId(), fakeSecurityTicket());
    }

    private CrmPrePickupPointParams createPvzLead(PreLegalPartnerParams partner, PrePickupPointApproveStatus status) {
        return crmPrePickupPointFactory.create(CrmPrePickupPointTestParamsBuilder.builder()
                .legalPartnerId(partner.getLegalPartnerId())
                .params(CrmPrePickupPointTestParams.builder()
                        .status(status)
                        .build())
                .build());
    }

    private PrePickupPointCrmDto pvzDtoWithStatus(
            CrmPrePickupPointParams params,
            PrePickupPointApproveStatus status
    ) {
        return crmLogbrokerEventMapper.map(params).toBuilder()
                .status(status)
                .build();
    }

    private PreLegalPartnerCrmDto partnerDtoWithStatus(
            PreLegalPartnerParams preLegalPartnerParams,
            PreLegalPartnerApproveStatus status
    ) {
        return crmLogbrokerEventMapper.map(preLegalPartnerParams).toBuilder()
                .approveStatus(status)
                .build();
    }

    private CrmPrePickupPointParams getUpdated(CrmPrePickupPointParams crmPrePickupPointParams) {
        return crmPrePickupPointQueryService.getById(crmPrePickupPointParams.getId());
    }

    private PreLegalPartnerParams getUpdated(PreLegalPartnerParams preLegalPartnerParams) {
        return preLegalPartnerQueryService.getById(preLegalPartnerParams.getId());
    }

}
