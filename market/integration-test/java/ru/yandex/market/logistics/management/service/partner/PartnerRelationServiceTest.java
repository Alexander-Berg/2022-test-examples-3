package ru.yandex.market.logistics.management.service.partner;

import java.util.HashSet;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.service.client.PartnerRelationService;

class PartnerRelationServiceTest extends AbstractContextualTest {

    @Autowired
    private PartnerRelationService partnerRelationService;

    @Test
    @DatabaseSetup("/data/service/partner/before/relations_active.xml")
    @ExpectedDatabase(
        value = "/data/service/partner/before/relations_inactive.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void deactivateByIds() {
        Set<Long> idSet = new HashSet<>();
        idSet.add(1L);
        idSet.add(2L);
        partnerRelationService.deactivateRelations(PartnerRelationFilter.newBuilder().ids(idSet).build());
    }

    @Test
    @DatabaseSetup("/data/service/partner/before/relations_inactive.xml")
    @ExpectedDatabase(
        value = "/data/service/partner/before/relations_active.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void activateByIds() {
        Set<Long> idSet = new HashSet<>();
        idSet.add(1L);
        idSet.add(2L);
        partnerRelationService.activateRelations(PartnerRelationFilter.newBuilder().ids(idSet).build());
    }

}
