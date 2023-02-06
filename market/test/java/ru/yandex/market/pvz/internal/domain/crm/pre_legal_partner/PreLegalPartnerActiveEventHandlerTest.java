package ru.yandex.market.pvz.internal.domain.crm.pre_legal_partner;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.client.crm.dto.PreLegalPartnerCrmDto;
import ru.yandex.market.pvz.client.model.approve.ApproveStatus;
import ru.yandex.market.pvz.client.model.approve.PreLegalPartnerApproveStatus;
import ru.yandex.market.pvz.core.domain.approve.pre_legal_partner.PreLegalPartnerParams;
import ru.yandex.market.pvz.core.domain.approve.pre_legal_partner.PreLegalPartnerQueryService;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerParams;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerQueryService;
import ru.yandex.market.pvz.core.domain.logbroker.crm.CrmLogbrokerEventMapper;
import ru.yandex.market.pvz.core.domain.logbroker.crm.consume.pre_legal_partner.PreLegalPartnerActiveEventHandler;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory;
import ru.yandex.market.pvz.internal.PvzIntTest;

import static org.assertj.core.api.Assertions.assertThat;

@PvzIntTest
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PreLegalPartnerActiveEventHandlerTest {

    private final TestableClock clock;

    private final TestPreLegalPartnerFactory preLegalPartnerFactory;
    private final TestLegalPartnerFactory legalPartnerFactory;

    private final CrmLogbrokerEventMapper crmLogbrokerEventMapper;
    private final PreLegalPartnerActiveEventHandler preLegalPartnerActiveEventHandler;
    private final LegalPartnerQueryService legalPartnerQueryService;
    private final PreLegalPartnerQueryService preLegalPartnerQueryService;

    @Test
    void statusTest() {
        var preLegalPartner = preLegalPartnerFactory.createPreLegalPartner();
        var legalPartner = legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .preLegalPartner(preLegalPartner)
                        .approvePreLegalPartner(false)
                        .build());
        preLegalPartnerFactory.bindSecurityTicket(preLegalPartner.getId());
        preLegalPartnerFactory.approveBySecurity(preLegalPartner.getId());
        preLegalPartnerFactory.offerSignatureRequired(preLegalPartner.getId());

        PreLegalPartnerCrmDto preLegalPartnerCrmDto = crmLogbrokerEventMapper.map(preLegalPartner);
        preLegalPartnerActiveEventHandler.handle(preLegalPartnerCrmDto);

        PreLegalPartnerParams actualPreLegalPartnerParams =
                preLegalPartnerQueryService.getById(preLegalPartnerCrmDto.getId());
        LegalPartnerParams actualLegalPartnerParams = legalPartnerQueryService.get(legalPartner.getId());

        assertThat(actualPreLegalPartnerParams.getApproveStatus()).isEqualTo(PreLegalPartnerApproveStatus.ACTIVE);
        assertThat(actualLegalPartnerParams.getOfferSignedSince()).isEqualTo(LocalDate.now(clock));
        assertThat(actualLegalPartnerParams.getApproveStatus()).isEqualTo(ApproveStatus.APPROVED);
    }
}
