package ru.yandex.market.pvz.internal.domain.legal_partner;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.client.billing.dto.BillingLegalPartnerDto;
import ru.yandex.market.pvz.core.domain.delivery_service.DeliveryService;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerRepository;
import ru.yandex.market.pvz.core.domain.order.OrderRepository;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.pvz.core.domain.pickup_point.changelog.PickupPointChangeLogRepository;
import ru.yandex.market.pvz.core.domain.returns.ReturnRequestRepository;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestDeliveryServiceFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.internal.PvzIntTest;

import static org.springframework.test.util.AssertionErrors.assertEquals;

@PvzIntTest
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LegalPartnerBillingServiceTest {
    private final Clock clock;
    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestDeliveryServiceFactory deliveryServiceFactory;
    private final LegalPartnerBillingService legalPartnerBillingService;
    private final LegalPartnerRepository legalPartnerRepository;
    private final PickupPointRepository pickupPointRepository;
    private final PickupPointChangeLogRepository pickupPointChangeLogRepository;
    private final OrderRepository orderRepository;
    private final ReturnRequestRepository returnRequestRepository;

    @Test
    void getAll() {
        returnRequestRepository.deleteAll();
        orderRepository.deleteAll();
        pickupPointChangeLogRepository.deleteAll();
        pickupPointRepository.deleteAll();
        legalPartnerRepository.deleteAll();
        DeliveryService deliveryService = deliveryServiceFactory.createDeliveryService();

        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .deliveryService(deliveryService)
                        .params(TestLegalPartnerFactory.LegalPartnerTestParams.builder()
                                .agreementNo("Номер договора")
                                .build())
                        .build());

        List<BillingLegalPartnerDto> legalPartners = legalPartnerBillingService.getAll();

        assertEquals("legalPartners", List.of(BillingLegalPartnerDto.builder()
                        .legalPartnerId(legalPartner.getId())
                        .deliveryServiceId(deliveryService.getId())
                        .balanceClientId(legalPartner.getBalanceClientId())
                        .virtualAccountNumber(legalPartner.getVirtualAccountNumber())
                        .contractNumber(legalPartner.getAgreementNo())
                        .name(legalPartner.getOrganization().getName())
                        .ogrn(legalPartner.getOrganization().getOgrn())
                        .legalType(legalPartner.getOrganization().getLegalType().name())
                        .legalForm(legalPartner.getOrganization().getLegalForm().name())
                        .kpp(legalPartner.getOrganization().getKpp())
                        .inn(legalPartner.getOrganization().getTaxpayerNumber())
                        .campaignId(legalPartner.getPartnerId())
                        .offerDate(LocalDate.now(clock))
                        .build()),
                legalPartners);
    }
}
