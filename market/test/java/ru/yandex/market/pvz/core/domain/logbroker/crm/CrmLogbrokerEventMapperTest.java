package ru.yandex.market.pvz.core.domain.logbroker.crm;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.client.crm.dto.LegalPartnerCrmDto;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;

import static org.assertj.core.api.Assertions.assertThat;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class CrmLogbrokerEventMapperTest {

    private final TestLegalPartnerFactory legalPartnerFactory;
    private final CrmLogbrokerEventMapper mapper;

    @Test
    void testMapLegalPartner() {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        LegalPartnerCrmDto dto = mapper.map(legalPartner);

        assertThat(dto.getDelegate().getDelegateName())
                .isEqualTo(legalPartner.getDelegate().getDelegateName())
                .isNotBlank();

        assertThat(dto.getAccountant().getAccountantName())
                .isEqualTo(legalPartner.getAccountant().getAccountantName())
                .isNotBlank();

        assertThat(dto.getCommissioner().getCommissionerName())
                .isEqualTo(legalPartner.getCommissioner().getCommissionerName())
                .isNotBlank();
    }

}
