package ru.yandex.market.pvz.core.domain.logbroker.crm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.logbroker.crm.produce.CrmLogbrokerEventPublisher;
import ru.yandex.market.pvz.core.test.EmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;

@Slf4j
@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class CrmLogbrokerEventPublisherTest {

    private final TestLegalPartnerFactory legalPartnerFactory;
    private final CrmLogbrokerEventPublisher eventPublisher;

    @Test
    void test() {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        eventPublisher.publish(legalPartner);
    }

}
