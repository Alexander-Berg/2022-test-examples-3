package ru.yandex.market.logistics.lom.service.order.route;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.yandex.ydb.table.query.DataQueryResult;
import com.yandex.ydb.table.result.ResultSetReader;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.AbstractContextualYdbTest;
import ru.yandex.market.logistics.lom.entity.combinator.embedded.OrderCombinedRouteHistory;
import ru.yandex.market.logistics.lom.repository.ydb.OrderCombinedRouteHistoryYdbRepository;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateStatusHistoryTableDescription;
import ru.yandex.market.logistics.lom.repository.ydb.description.OrderCombinedRouteHistoryTableDescription;
import ru.yandex.market.logistics.lom.service.order.history.route.OrderCombinedRouteHistoryService;
import ru.yandex.market.logistics.lom.utils.UuidGenerator;
import ru.yandex.market.ydb.integration.YdbTableDescription;
import ru.yandex.market.ydb.integration.YdbTemplate;
import ru.yandex.market.ydb.integration.query.QFrom;
import ru.yandex.market.ydb.integration.query.QSelect;
import ru.yandex.market.ydb.integration.query.YdbSelect;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@ParametersAreNonnullByDefault
@DatabaseSetup("/service/order/route/before/prepare_orders.xml")
@DisplayName("Класс с общими методами для работы с историей маршрутов заказов")
abstract class AbstractOrderCombinedRouteHistoryTest extends AbstractContextualYdbTest {

    @Autowired
    protected UuidGenerator uuidGenerator;

    @Autowired
    protected TestableClock clock;

    @Autowired
    protected OrderCombinedRouteHistoryService orderCombinedRouteHistoryService;

    @Autowired
    protected OrderCombinedRouteHistoryYdbRepository ydbRepository;

    @Autowired
    protected TransactionTemplate transactionTemplate;

    protected static final UUID MOCKED_UUID = UUID.fromString("e11c5e64-3694-40c9-b9b4-126efedaa098");

    @Autowired
    protected OrderCombinedRouteHistoryTableDescription routeHistoryTableDescription;

    @Autowired
    private BusinessProcessStateStatusHistoryTableDescription businessProcessStateStatusHistoryTable;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2021-09-21T12:30:00Z"), DateTimeUtils.MOSCOW_ZONE);
        doReturn(MOCKED_UUID).when(uuidGenerator).randomUuid();
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(uuidGenerator, ydbRepository);
    }

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(routeHistoryTableDescription, businessProcessStateStatusHistoryTable);
    }

    @Nonnull
    @SneakyThrows
    protected JsonNode combinedRoute() {
        return objectMapper.readTree(extractFileContent("service/order/route/combined_route.json"));
    }

    @Nonnull
    protected OrderCombinedRouteHistory expectedRouteHistory(@Nullable Long orderId) {
        return new OrderCombinedRouteHistory()
            .setOrderId(orderId)
            .setCreated(clock.instant())
            .setRouteUuid(MOCKED_UUID);
    }

    protected void verifyUidsInYdb(List<UUID> expectedUuids) {
        List<UUID> actualUuids = ydbTemplate.selectList(
            YdbSelect.select(
                QSelect.of(routeHistoryTableDescription.fields())
                    .from(QFrom.table(routeHistoryTableDescription))
                    .select()
            )
                .toQuery(),
            YdbTemplate.DEFAULT_READ,
            this::convertToUuids
        );

        softly.assertThat(actualUuids).isEqualTo(expectedUuids);
    }

    @Nonnull
    protected List<UUID> convertToUuids(DataQueryResult queryResult) {
        if (queryResult.isEmpty()) {
            return List.of();
        }
        ResultSetReader resultSetReader = queryResult.getResultSet(0);
        List<UUID> uuids = new ArrayList<>();
        while (resultSetReader.next()) {
            String uuid = resultSetReader
                .getColumn(routeHistoryTableDescription.getRouteUuid().alias())
                .getString(StandardCharsets.UTF_8);
            uuids.add(UUID.fromString(uuid));
        }

        return uuids;
    }
}
