package ru.yandex.market.logistics.management.service.graph;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.util.CleanDatabase;

import static com.github.springtestdbunit.annotation.DatabaseOperation.INSERT;
import static com.github.springtestdbunit.annotation.DatabaseOperation.UPDATE;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@CleanDatabase
@DatabaseSetup({
    "/data/service/graph/edge/before/service_codes.xml",
    "/data/service/graph/edge/before/platform_client.xml",
    "/data/service/graph/edge/before/partners.xml",
    "/data/service/graph/edge/before/logistic_points.xml"
})
@DatabaseSetup(value = "/data/service/graph/edge/before/partner_relations.xml", type = INSERT)
class EdgeServiceTest extends AbstractContextualTest {

    @Autowired
    private EdgeService edgeService;

    @Test
    @DisplayName("Не удаляем ничего, для выключенного склада СД")
    @DatabaseSetup("/data/service/graph/edge/before/dr_sc_ds_created.xml")
    @ExpectedDatabase(
        value = "/data/service/graph/edge/before/dr_sc_ds_created.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void noCleanupEdges() {
        edgeService.cleanup();
    }

    @Test
    @DisplayName("Не удаляем ничего для возвратных перемещений")
    @DatabaseSetup("/data/service/graph/edge/before/returns.xml")
    @ExpectedDatabase(
        value = "/data/service/graph/edge/before/returns.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void noCleanupEdgesForReturns() {
        edgeService.cleanup();
    }

    @Test
    @DisplayName("Не удаляем ничего для зафриженных еджей")
    @DatabaseSetup("/data/service/graph/edge/before/frozen_edges.xml")
    @ExpectedDatabase(
        value = "/data/service/graph/edge/before/frozen_edges.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void noCleanupFrozenEdges() {
        edgeService.cleanup();
    }

    @Test
    @DisplayName("Не удаляем ничего, если зафрижены рёбра у сегмента перемещения")
    @DatabaseSetup("/data/service/graph/edge/before/frozen_edges_on_movement.xml")
    @ExpectedDatabase(
        value = "/data/service/graph/edge/before/frozen_edges_on_movement.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void noCleanupFrozenEdgesOnMovement() {
        edgeService.cleanup();
    }

    @Test
    @DisplayName("Удаляем ребра от и к выключенному складу")
    @DatabaseSetup("/data/service/graph/edge/before/dr_sc_ds_created.xml")
    @DatabaseSetup(value = "/data/service/graph/edge/before/disable-sc-warehouse.xml", type = UPDATE)
    @ExpectedDatabase(
        value = "/data/service/graph/edge/after/edges_without_sc_inbound.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void cleanupScEdges() {
        edgeService.cleanup();
    }
}
