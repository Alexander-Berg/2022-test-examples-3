package ru.yandex.market.delivery.transport_manager.service.event.lom;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.logistics.lom.model.dto.EventDto;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
public class OrderEventConsumerTest extends AbstractContextualTest {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OrderEventConsumer orderEventConsumer;

    @Nonnull
    private static Stream<Arguments> eventsToProcess() {
        return Stream.of(
            Arguments.of(
                "У партнера перемещения изменился externalId заказа",
                "service/event/logbroker/externalIdChanged/diff.json",
                "service/event/logbroker/externalIdChanged/snapshot.json"
            ),
            Arguments.of(
                "У заказа добавился сегмент путевого листа",
                "service/event/logbroker/addSegment/diff.json",
                "service/event/logbroker/addSegment/snapshot.json"
            ),
            Arguments.of(
                "Заказ отменен",
                "service/event/logbroker/cancelOrder/diff.json",
                "service/event/logbroker/cancelOrder/snapshot.json"
            ),
            Arguments.of(
                "Пришел DAAS заказ",
                "service/event/logbroker/daasExternalIdChanged/diff.json",
                "service/event/logbroker/daasExternalIdChanged/snapshot.json"
            ),
            Arguments.of(
                "Пришло изменение маршрута заказа",
                "service/event/logbroker/routeUuidChanged/diff.json",
                "service/event/logbroker/routeUuidChanged/snapshot.json"
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("eventsToProcess")
    @ExpectedDatabase(value = "/repository/order_event/after/any_order_event.xml", assertionMode = NON_STRICT_UNORDERED)
    void persistEvents(String caseName, String diffPath, String snapshotPath) {
        orderEventConsumer.accept(List.of(extractEvent(diffPath, snapshotPath)));
    }

    @Nonnull
    private static Stream<Arguments> eventsToSkip() {
        return Stream.of(
            Arguments.of(
                "Событие fake заказа",
                "service/event/logbroker/fake/diff.json",
                "service/event/logbroker/fake/snapshot.json"
            ),
            Arguments.of(
                "Событие заказа из неподдерживаемой платформы",
                "service/event/logbroker/notSupportedPlatform/diff.json",
                "service/event/logbroker/notSupportedPlatform/snapshot.json"
            ),
            Arguments.of(
                "Не влияющее на перемещения событие",
                "service/event/logbroker/draft/diff.json",
                "service/event/logbroker/draft/snapshot.json"
            ),
            Arguments.of(
                "Отмена невалидного заказа",
                "service/event/logbroker/cancelInvalidOrder/diff.json",
                "service/event/logbroker/cancelInvalidOrder/snapshot.json"
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("eventsToSkip")
    @DatabaseSetup(value = "/repository/task/no_tasks.xml", connection = "dbUnitDatabaseConnectionDbQueue")
    void skipEvent(String caseName, String diffPath, String snapshotPath) {
        orderEventConsumer.accept(List.of(extractEvent(diffPath, snapshotPath)));
    }

    @Nonnull
    private EventDto extractEvent(String diffPath, String snapshotPath) {
        try {
            return new EventDto()
                .setLogbrokerId(5001L)
                .setDiff(objectMapper.readTree(extractFileContent(diffPath)))
                .setSnapshot(objectMapper.readTree(extractFileContent(snapshotPath)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
