package ru.yandex.market.pvz.core.domain.approve.pre_legal_partner;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.pvz.client.model.approve.PreLegalPartnerApproveStatus;
import ru.yandex.market.pvz.client.model.approve.PrePickupPointApproveStatus;
import ru.yandex.market.pvz.core.domain.approve.crm_pre_pickup_point.CrmPrePickupPointQueryService;
import ru.yandex.market.pvz.core.domain.logbroker.crm.produce.CrmLogbrokerEventPublisher;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestCrmPrePickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;
import ru.yandex.market.tpl.common.startrek.ticket.StartrekTicket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.DEFAULT_DATASOURCE_ID;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_COLLABORATION_FORM;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_DELEGATE_EMAIL;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_DELEGATE_NAME;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_DELEGATE_PHONE;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_FLOOR;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_HAS_SEPARATE_ENTRANCE;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_HAS_STREET_ENTRANCE;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_HAS_WINDOWS;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_LEGAL_FORM;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_LEGAL_TYPE;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_OGRN;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_ORGANISATION_NAME;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_PICKUP_POINT_ADDRESS;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_PICKUP_POINT_CEILING_HEIGHT;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_PICKUP_POINT_COMMENT;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_PICKUP_POINT_COUNT;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_PICKUP_POINT_LAT;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_PICKUP_POINT_LOCALITY;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_PICKUP_POINT_LON;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_PICKUP_POINT_PHOTO_URL;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_PICKUP_POINT_REGION;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_PICKUP_POINT_SQUARE;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_POLYGON_ID;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_REFUSAL_REASON;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_WANT_BRAND;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PreLegalPartnerCommandServiceTest {

    @MockBean
    private CrmLogbrokerEventPublisher crmLogbrokerEventPublisher;

    private final TestPreLegalPartnerFactory preLegalPartnerFactory;
    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestCrmPrePickupPointFactory crmPrePickupPointFactory;

    private final PreLegalPartnerQueryService preLegalPartnerQueryService;
    private final PreLegalPartnerCommandService preLegalPartnerCommandService;
    private final CrmPrePickupPointQueryService crmPrePickupPointQueryService;

    private StartrekTicket startrekTicket;

    @BeforeEach
    void setup() {
        startrekTicket = mock(StartrekTicket.class);
        when(startrekTicket.getKey()).thenReturn("TICKET-KEY");
        when(startrekTicket.getStatusKey()).thenReturn("status");
    }

    @Test
    void createPreLegalPartner() {
        PreLegalPartnerParams preLegalPartnerParams = preLegalPartnerFactory.createPreLegalPartner();
        Long id = preLegalPartnerParams.getId();
        String taxpayerNumber = preLegalPartnerParams.getTaxpayerNumber();
        long partnerId = preLegalPartnerParams.getPartnerId();
        long clientId = preLegalPartnerParams.getBalanceClientId();
        long uid = preLegalPartnerParams.getOwnerUid();

        PreLegalPartnerParams params = preLegalPartnerQueryService.getByPartnerId(partnerId);

        assertThat(params.getId()).isEqualTo(id);
        assertThat(params.getPartnerId()).isEqualTo(partnerId);
        assertThat(params.getMarketShopId()).isEqualTo(DEFAULT_DATASOURCE_ID);
        assertThat(params.getBalanceClientId()).isEqualTo(clientId);
        assertThat(params.getOwnerUid()).isEqualTo(uid);
        assertThat(params.getDelegateName()).isEqualTo(DEFAULT_DELEGATE_NAME);
        assertThat(params.getDelegateEmail()).isEqualTo(DEFAULT_DELEGATE_EMAIL);
        assertThat(params.getDelegatePhone()).isEqualTo(DEFAULT_DELEGATE_PHONE);
        assertThat(params.getLegalType()).isEqualTo(DEFAULT_LEGAL_TYPE);
        assertThat(params.getLegalForm()).isEqualTo(DEFAULT_LEGAL_FORM);
        assertThat(params.getOrganisationName()).isEqualTo(DEFAULT_ORGANISATION_NAME);
        assertThat(params.getTaxpayerNumber()).isEqualTo(taxpayerNumber);
        assertThat(params.getOgrn()).isEqualTo(DEFAULT_OGRN);
        assertThat(params.getCollaborationForm()).isEqualTo(DEFAULT_COLLABORATION_FORM);
        assertThat(params.isWantBrand()).isEqualTo(DEFAULT_WANT_BRAND);
        assertThat(params.getPickupPointCount()).isEqualTo(DEFAULT_PICKUP_POINT_COUNT);
        assertThat(params.getPickupPointRegion()).isEqualTo(DEFAULT_PICKUP_POINT_REGION);
        assertThat(params.getPickupPointLocality()).isEqualTo(DEFAULT_PICKUP_POINT_LOCALITY);
        assertThat(params.getPickupPointAddress()).isEqualTo(DEFAULT_PICKUP_POINT_ADDRESS);
        assertThat(params.getPickupPointLatitude()).isEqualTo(DEFAULT_PICKUP_POINT_LAT);
        assertThat(params.getPickupPointLongitude()).isEqualTo(DEFAULT_PICKUP_POINT_LON);
        assertThat(params.getPickupPointSquare()).isEqualTo(DEFAULT_PICKUP_POINT_SQUARE);
        assertThat(params.getPickupPointCeilingHeight()).isEqualTo(DEFAULT_PICKUP_POINT_CEILING_HEIGHT);
        assertThat(params.getPickupPointPhotoUrl()).isEqualTo(DEFAULT_PICKUP_POINT_PHOTO_URL);
        assertThat(params.getPickupPointComment()).isEqualTo(DEFAULT_PICKUP_POINT_COMMENT);
        assertThat(params.getPickupPointFloor()).isEqualTo(DEFAULT_FLOOR);
        assertThat(params.getPickupPointPolygonId()).isEqualTo(DEFAULT_POLYGON_ID);
        assertThat(params.getHasSeparateEntrance()).isEqualTo(DEFAULT_HAS_SEPARATE_ENTRANCE);
        assertThat(params.getHasStreetEntrance()).isEqualTo(DEFAULT_HAS_STREET_ENTRANCE);
        assertThat(params.getHasWindows()).isEqualTo(DEFAULT_HAS_WINDOWS);
        assertThat(params.getApproveStatus()).isEqualTo(PreLegalPartnerApproveStatus.CHECKING);

        verify(crmLogbrokerEventPublisher, times(1)).publish(isA(PreLegalPartnerParams.class));
    }

    @Test
    void approve() {
        var preLegalPartner = preLegalPartnerFactory.createPreLegalPartner();

        var approved = preLegalPartnerCommandService.approveFromCrm(preLegalPartner.getId());
        assertThat(approved.getApproveStatus()).isEqualTo(PreLegalPartnerApproveStatus.APPROVED);
        assertThat(approved.getRefusalReason()).isNull();

    }

    @Test
    void tryToApproveRejected() {
        var preLegalPartner = preLegalPartnerFactory.createRejectedPreLegalPartner();
        assertThatThrownBy(() -> preLegalPartnerCommandService.approveFromCrm(preLegalPartner.getId()));
    }

    @Test
    void tryToApproveApproved() {
        var preLegalPartner = preLegalPartnerFactory.createApprovedPreLegalPartner();
        var approved = preLegalPartnerCommandService.approveFromCrm(preLegalPartner.getId());
        assertThat(approved.getApproveStatus()).isEqualTo(PreLegalPartnerApproveStatus.APPROVED);
        assertThat(approved.getRefusalReason()).isNull();
    }

    @Test
    void tryToApproveNotExistentPreLegalPartner() {
        assertThatThrownBy(() -> preLegalPartnerCommandService.approveFromCrm(-1L))
                .isExactlyInstanceOf(TplEntityNotFoundException.class);
    }

    @Test
    void reject() {
        var preLegalPartner = preLegalPartnerFactory.createPreLegalPartner();

        var rejected = preLegalPartnerCommandService.rejectFromCrm(preLegalPartner.getId(), DEFAULT_REFUSAL_REASON);
        assertThat(rejected.getApproveStatus()).isEqualTo(PreLegalPartnerApproveStatus.REJECTED);
        assertThat(rejected.getRefusalReason()).isEqualTo(DEFAULT_REFUSAL_REASON);
    }

    @Test
    void tryToRejectApproved() {
        var preLegalPartner = preLegalPartnerFactory.createApprovedPreLegalPartner();
        assertThatThrownBy(() ->
                preLegalPartnerCommandService.rejectFromCrm(preLegalPartner.getId(), DEFAULT_REFUSAL_REASON));
    }

    @Test
    void offerReject() {
        var preLegalPartner = preLegalPartnerFactory.createPreLegalPartner();
        legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .preLegalPartner(preLegalPartner)
                        .approvePreLegalPartner(false)
                        .build()
        );
        preLegalPartnerFactory.bindSecurityTicket(preLegalPartner.getId());
        preLegalPartnerFactory.approveBySecurity(preLegalPartner.getId());
        preLegalPartnerFactory.offerSignatureRequired(preLegalPartner.getId());

        var offerRejected = preLegalPartnerCommandService.rejectOfferFromCrm(preLegalPartner.getId());

        assertThat(offerRejected.getApproveStatus()).isEqualTo(PreLegalPartnerApproveStatus.OFFER_SIGNATURE_REQUIRED);
    }

    @Test
    void tryToRejectRejected() {
        var preLegalPartner = preLegalPartnerFactory.createRejectedPreLegalPartner();
        var rejected = preLegalPartnerCommandService.rejectFromCrm(preLegalPartner.getId(), DEFAULT_REFUSAL_REASON);
        assertThat(rejected.getApproveStatus()).isEqualTo(PreLegalPartnerApproveStatus.REJECTED);
        assertThat(rejected.getRefusalReason()).isEqualTo(DEFAULT_REFUSAL_REASON);
    }

    @Test
    void tryToRejectNotExistentPreLegalPartner() {
        assertThatThrownBy(() -> preLegalPartnerCommandService.rejectFromCrm(-1L, DEFAULT_REFUSAL_REASON))
                .isExactlyInstanceOf(TplEntityNotFoundException.class);
    }

    @Test
    void rejectWithPrePickupPoints() {
        var preLegalPartner = preLegalPartnerFactory.createRejectedPreLegalPartner();
        var legalPartner = legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .preLegalPartner(preLegalPartner)
                        .approvePreLegalPartner(false)
                        .build()
        );
        var prePickupPoint = crmPrePickupPointFactory.create(
                TestCrmPrePickupPointFactory.CrmPrePickupPointTestParamsBuilder.builder()
                        .legalPartnerId(legalPartner.getId())
                        .build()
        );

        preLegalPartner = preLegalPartnerCommandService.rejectFromCrm(preLegalPartner.getId(), DEFAULT_REFUSAL_REASON);

        var rejectedPreLegalPartner = preLegalPartnerQueryService.getById(preLegalPartner.getId());
        assertThat(rejectedPreLegalPartner.getApproveStatus()).isEqualTo(PreLegalPartnerApproveStatus.REJECTED);
        assertThat(rejectedPreLegalPartner.getRefusalReason()).isEqualTo(DEFAULT_REFUSAL_REASON);

        var rejectedPrePickupPoint = crmPrePickupPointQueryService.getById(prePickupPoint.getId());
        assertThat(rejectedPrePickupPoint.getStatus()).isEqualTo(PrePickupPointApproveStatus.REJECTED);
    }

    @Test
    void testApproveBySecurityCrmFlow() {
        var preLegalPartner = preLegalPartnerFactory.createPreLegalPartner();
        preLegalPartner = preLegalPartnerCommandService.approveFromCrm(preLegalPartner.getId());
        preLegalPartner = preLegalPartnerCommandService.bindSecurityTicket(preLegalPartner.getId(), startrekTicket);
        preLegalPartner = preLegalPartnerCommandService.approveBySecurity(startrekTicket.getKey());
        assertThat(preLegalPartner.getApproveStatus()).isEqualTo(PreLegalPartnerApproveStatus.APPROVED_BY_SECURITY);
        assertThat(preLegalPartner.getSecurityApprovalTicket().getKey()).isEqualTo(startrekTicket.getKey());
        assertThat(preLegalPartner.getRefusalReason()).isNull();
    }

    @Test
    void testRejectBySecurityOnlyPrePartnerCrmFlow() {
        var preLegalPartner = preLegalPartnerFactory.createPreLegalPartner();
        preLegalPartner = preLegalPartnerCommandService.approveFromCrm(preLegalPartner.getId());
        preLegalPartner = preLegalPartnerCommandService.bindSecurityTicket(preLegalPartner.getId(), startrekTicket);
        preLegalPartner = preLegalPartnerCommandService.approveBySecurity(startrekTicket.getKey());
        assertThat(preLegalPartner.getApproveStatus()).isEqualTo(PreLegalPartnerApproveStatus.APPROVED_BY_SECURITY);
        assertThat(preLegalPartner.getSecurityApprovalTicket().getKey()).isEqualTo(startrekTicket.getKey());
        assertThat(preLegalPartner.getRefusalReason()).isNull();
    }

}
