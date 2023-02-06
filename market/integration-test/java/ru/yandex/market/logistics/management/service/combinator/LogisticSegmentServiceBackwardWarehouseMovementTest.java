package ru.yandex.market.logistics.management.service.combinator;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;

import static com.github.springtestdbunit.annotation.DatabaseOperation.INSERT;
import static com.github.springtestdbunit.annotation.DatabaseOperation.UPDATE;

@DisplayName("Создание и обновление BMV сегментов между BWH")
@DatabaseSetup({
    "/data/service/combinator/db/before/regions.xml",
    "/data/service/combinator/db/before/service_codes.xml",
    "/data/service/combinator/db/before/platform_client.xml",
    "/data/service/combinator/db/before/partner_external_param_type.xml",
    "/data/service/combinator/db/before/logistic_segments_services_meta_keys.xml",
    "/data/service/combinator/db/before/bwh_bmv/common.xml",
})
public class LogisticSegmentServiceBackwardWarehouseMovementTest extends AbstractContextualAspectValidationTest {

    @Autowired
    private LogisticSegmentService logisticSegmentService;

    @Test
    @DisplayName("Новая связка")
    @DatabaseSetup("/data/service/combinator/db/before/bwh_bmv/sc_relation.xml")
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/bwh_bmv/new_sc_relation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void newSortingCenterRelation() {
        logisticSegmentService.buildPartnerRelationBackwardSegments();
    }

    @Test
    @DisplayName("Неактивная связка без сегментов")
    @DatabaseSetup("/data/service/combinator/db/before/bwh_bmv/sc_relation.xml")
    @DatabaseSetup(value = "/data/service/combinator/db/before/bwh_bmv/inactive_sc_relation.xml", type = UPDATE)
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/bwh_bmv/inactive_sc_relation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void inactiveRelationWithoutSegment() {
        logisticSegmentService.buildPartnerRelationBackwardSegments();
    }

    @Test
    @DisplayName("Неактивная связка, существующий сегмент")
    @DatabaseSetup("/data/service/combinator/db/before/bwh_bmv/sc_relation.xml")
    @DatabaseSetup(value = "/data/service/combinator/db/before/bwh_bmv/inactive_sc_relation.xml", type = UPDATE)
    @DatabaseSetup(value = "/data/service/combinator/db/before/bwh_bmv/sc_relation_segments.xml", type = INSERT)
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/bwh_bmv/inactive_sc_relation_existing_segment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void inactiveRelationExistingSegment() {
        logisticSegmentService.buildPartnerRelationBackwardSegments();
    }

    @Test
    @DisplayName("Связка с дропоффом с возвратным СЦ")
    @DatabaseSetup(value = "/data/service/combinator/db/before/bwh_bmv/dropoff_return_sc.xml", type = INSERT)
    @DatabaseSetup("/data/service/combinator/db/before/bwh_bmv/dropoff_relation.xml")
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/bwh_bmv/new_dropoff_relation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void dropoffWithReturnSortingCenterRelation() {
        logisticSegmentService.buildPartnerRelationBackwardSegments();
    }
}
