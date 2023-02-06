package ru.yandex.market.pvz.tms.executor.pickup_point;

import java.time.LocalDate;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import ru.yandex.market.mbi.api.client.entity.shops.SimpleShopRegistrationResponse;
import ru.yandex.market.pvz.client.model.approve.PreLegalPartnerApproveStatus;
import ru.yandex.market.pvz.client.model.approve.PrePickupPointApproveStatus;
import ru.yandex.market.pvz.core.domain.approve.MbiCabinetService;
import ru.yandex.market.pvz.core.domain.approve.crm_pre_pickup_point.CrmPrePickupPointParams;
import ru.yandex.market.pvz.core.domain.approve.crm_pre_pickup_point.CrmPrePickupPointQueryService;
import ru.yandex.market.pvz.core.domain.approve.pre_legal_partner.PreLegalPartnerCommandService;
import ru.yandex.market.pvz.core.domain.approve.pre_legal_partner.PreLegalPartnerParams;
import ru.yandex.market.pvz.core.domain.approve.startrek.partner_security.PartnerSecurityWorkflowResolution;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestCrmPrePickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory;
import ru.yandex.market.tpl.common.startrek.ticket.StartrekTicket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TransactionlessEmbeddedDbTest
@Import({ActivatePendingPickupPointsWithoutEventExecutor.class})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ActivatePendingPickupPointsWithoutEventExecutorTest {

    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestPreLegalPartnerFactory preLegalPartnerFactory;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestCrmPrePickupPointFactory crmPrePickupPointFactory;

    private final PreLegalPartnerCommandService preLegalPartnerCommandService;
    private final CrmPrePickupPointQueryService crmPrePickupPointQueryService;

    private final ActivatePendingPickupPointsWithoutEventExecutor executor;


    @MockBean
    private MbiCabinetService mbiCabinetService;

    @BeforeEach
    void setup() {
        pickupPointFactory.addFirstDeactivationReason();
        when(mbiCabinetService.createForPrePickupPoint(any(), any())).thenReturn(new SimpleShopRegistrationResponse() {{
            setOwnerId(1);
        }});
    }

    @Test
    void testActivated() {
        PreLegalPartnerParams partner = createPartnerWithLead(PreLegalPartnerApproveStatus.ACTIVE, true);
        CrmPrePickupPointParams pvz = createPvzLead(partner, PrePickupPointApproveStatus.APPROVED);

        assertThat(getUpdated(pvz).getPickupPointId()).isNull();
        executor.doRealJob(null);
        assertThat(getUpdated(pvz).getPickupPointId()).isNotNull();
    }

    @Test
    void testNotActivatedIfInvalidPickupPointStatus() {
        PreLegalPartnerParams partner = createPartnerWithLead(PreLegalPartnerApproveStatus.ACTIVE, true);
        CrmPrePickupPointParams pvz = createPvzLead(partner, PrePickupPointApproveStatus.CHECKING);

        assertThat(getUpdated(pvz).getPickupPointId()).isNull();
        executor.doRealJob(null);
        assertThat(getUpdated(pvz).getPickupPointId()).isNull();
    }

    @Test
    void testNotActivatedIfInvalidPartnerStatus() {
        PreLegalPartnerParams partner = createPartnerWithLead(PreLegalPartnerApproveStatus.OFFER_SIGNATURE_REQUIRED,
                false);
        CrmPrePickupPointParams pvz = createPvzLead(partner, PrePickupPointApproveStatus.APPROVED);

        assertThat(getUpdated(pvz).getPickupPointId()).isNull();
        executor.doRealJob(null);
        assertThat(getUpdated(pvz).getPickupPointId()).isNull();
    }

    private PreLegalPartnerParams createPartnerWithLead(PreLegalPartnerApproveStatus status, boolean isOfferSigned) {
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
        return crmPrePickupPointFactory.create(TestCrmPrePickupPointFactory.CrmPrePickupPointTestParamsBuilder.builder()
                .legalPartnerId(partner.getLegalPartnerId())
                .params(TestCrmPrePickupPointFactory.CrmPrePickupPointTestParams.builder()
                        .status(status)
                        .build())
                .build());
    }

    private StartrekTicket fakeSecurityTicket() {
        StartrekTicket ticket = mock(StartrekTicket.class, Mockito.RETURNS_DEEP_STUBS);
        when(ticket.getKey()).thenReturn("TICKET-123");
        when(ticket.getStatusKey()).thenReturn("checking");
        when(ticket.getResolution(PartnerSecurityWorkflowResolution.class))
                .thenReturn(Optional.of(PartnerSecurityWorkflowResolution.APPROVED));
        return ticket;
    }

    private CrmPrePickupPointParams getUpdated(CrmPrePickupPointParams crmPrePickupPointParams) {
        return crmPrePickupPointQueryService.getById(crmPrePickupPointParams.getId());
    }

}
