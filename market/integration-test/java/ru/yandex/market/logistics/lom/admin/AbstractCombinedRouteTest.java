package ru.yandex.market.logistics.lom.admin;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualYdbTest;
import ru.yandex.market.logistics.lom.entity.combinator.embedded.CombinedRoute;
import ru.yandex.market.logistics.lom.repository.ydb.OrderCombinedRouteHistoryYdbRepository;
import ru.yandex.market.logistics.lom.repository.ydb.description.OrderCombinedRouteHistoryTableDescription;
import ru.yandex.market.logistics.lom.utils.ydb.converter.OrderCombinedRouteHistoryYdbConverter;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

public abstract class AbstractCombinedRouteTest extends AbstractContextualYdbTest {
    protected static final UUID EXISTING_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    protected OrderCombinedRouteHistoryTableDescription routeHistoryTable;

    @Autowired
    protected OrderCombinedRouteHistoryYdbConverter routeHistoryConverter;

    @Autowired
    protected OrderCombinedRouteHistoryYdbRepository newRepository;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(newRepository);
    }

    @Override
    @Nonnull
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(routeHistoryTable);
    }

    @Nonnull
    protected CombinedRoute combinedRoute(long orderId, UUID routeUuid) {
        return combinedRoute(orderId, routeUuid, "controller/admin/combined/source_route_3.json");
    }

    @Nonnull
    @SneakyThrows
    protected CombinedRoute combinedRoute(long orderId, UUID routeUuid, String routeFilePath) {
        return new CombinedRoute()
            .setOrderId(orderId)
            .setRouteUuid(routeUuid)
            .setSourceRoute(objectMapper.readTree(extractFileContent(routeFilePath)));
    }
}
