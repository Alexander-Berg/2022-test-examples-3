package ru.yandex.market.logistics.management.service.combinator;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.repository.LogisticsPointRepository;
import ru.yandex.market.logistics.management.repository.PartnerRelationRepository;
import ru.yandex.market.logistics.management.repository.PartnerRepository;

@ParametersAreNonnullByDefault
@DatabaseSetup("/data/service/combinator/db/before/service_codes.xml")
public class SyncDataToLogisticsServicesTest extends AbstractContextualTest {

    @Autowired
    private PartnerRelationRepository partnerRelationRepository;

    @Autowired
    private LogisticsPointRepository logisticsPointRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/sync_partner_relation_enabled.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/sync_partner_relation_enabled_expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void syncPartnerRelationStatusWhenDisablePartnerRelation() {
        updatePartnerRelationEnabled(false);
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/sync_partner_relation_disabled.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/sync_partner_relation_disabled_expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void syncPartnerRelationStatusWhenEnablePartnerRelation() {
        updatePartnerRelationEnabled(true);
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/sync_partner_relation_disabled_partner_inactive.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/sync_partner_relation_disabled_partner_inactive_expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void serviceStatusRemainsUnchangedWhenEnablePartnerRelationAndPartnerInactive() {
        updatePartnerRelationEnabled(true);
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/sync_logistics_point_active.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/sync_logistics_point_active_expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void syncPointActiveWhenDisablePoint() {
        updateLogisticsPointActive(3000L, false);
        updateLogisticsPointActive(4000L, false);
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/sync_logistics_point_inactive.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/sync_logistics_point_inactive_expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void syncPointActiveWhenEnablePoint() {
        updateLogisticsPointActive(3000L, true);
        updateLogisticsPointActive(4000L, true);
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/sync_logistics_point_inactive_partner_inactive.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/sync_logistics_point_inactive_partner_inactive_expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void serviceStatusRemainsUnchangedWhenEnablePointAndPartnerInactive() {
        updateLogisticsPointActive(3000L, true);
        updateLogisticsPointActive(4000L, true);
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/sync_partner_active.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/sync_partner_active_expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void syncPartnerStatusWhenDisablePartner() {
        updatePartnerStatus(3000L, PartnerStatus.INACTIVE);
        updatePartnerStatus(4000L, PartnerStatus.INACTIVE);
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/sync_partner_inactive.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/sync_partner_inactive_expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void syncPartnerStatusWhenEnablePartner() {
        updatePartnerStatus(3000L, PartnerStatus.ACTIVE);
        updatePartnerStatus(4000L, PartnerStatus.ACTIVE);
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/sync_partner_inactive_entities_disabled.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/sync_partner_inactive_entities_disabled_expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void serviceStatusRemainsUnchangedWhenEnablePartnerAndEntitiesInactive() {
        updatePartnerStatus(3000L, PartnerStatus.ACTIVE);
        updatePartnerStatus(4000L, PartnerStatus.ACTIVE);
    }

    private void updatePartnerRelationEnabled(boolean enabled) {
        partnerRelationRepository.findById(1L)
            .ifPresent(pr -> {
                pr.setEnabled(enabled);
                partnerRelationRepository.save(pr);
            });
    }

    private void updateLogisticsPointActive(long id, boolean active) {
        logisticsPointRepository.findById(id)
            .ifPresent(lp -> {
                lp.setActive(active);
                logisticsPointRepository.save(lp);
            });
    }

    private void updatePartnerStatus(long id, PartnerStatus status) {
        partnerRepository.findById(id)
            .ifPresent(p -> {
                p.setStatus(status);
                partnerRepository.save(p);
            });
    }
}
