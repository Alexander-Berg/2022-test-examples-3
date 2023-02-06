package ru.yandex.market.logistics.management.controller.admin.partnerRelation;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.PartnerRelation;
import ru.yandex.market.logistics.management.repository.PartnerRelationRepository;

abstract class BaseAdminPartnerRelationTest extends AbstractContextualTest {
    @Autowired
    protected PartnerRelationRepository partnerRelationRepository;

    protected void assertReturnPartner(Optional<PartnerRelation> partnerRelationOptional) {
        softly.assertThat(partnerRelationOptional)
            .as("Asserting that the partner relation exists and is valid")
            .hasValueSatisfying(
                partnerRelation -> softly.assertThat(Optional.ofNullable(partnerRelation.getReturnPartner()))
                    .as("Asserting that the return partner exists and is valid")
                    .hasValueSatisfying(partner -> softly.assertThat(partner.getId()).isEqualTo(3L))
            );
    }
}
