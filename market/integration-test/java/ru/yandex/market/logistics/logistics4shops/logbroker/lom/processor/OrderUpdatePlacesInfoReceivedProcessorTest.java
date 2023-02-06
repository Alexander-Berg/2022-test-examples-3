package ru.yandex.market.logistics.logistics4shops.logbroker.lom.processor;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.config.properties.FeatureProperties;
import ru.yandex.market.logistics.logistics4shops.logbroker.LomEventMessageHandler;
import ru.yandex.market.logistics.logistics4shops.utils.LomEventFactory;
import ru.yandex.market.logistics.logistics4shops.utils.logging.TskvLogRecord;

import static ru.yandex.market.logistics.logistics4shops.utils.logging.BackLogAssertions.logEqualsTo;

@ParametersAreNonnullByDefault
@DisplayName("Обработка статуса INFO_RECEIVED у заявки на изменение коробок заказа")
class OrderUpdatePlacesInfoReceivedProcessorTest extends AbstractIntegrationTest {

    private static final String CORRECT_DIFF_PATH = "logbroker/lom/event/updateplaces/diff/info_received_status.json";
    private static final String CORRECT_SNAPSHOT_PATH =
        "logbroker/lom/event/updateplaces/snapshot/info_received_snapshot.json";

    @Autowired
    private LomEventMessageHandler lomEventMessageHandler;

    @Test
    @DisplayName("Успешная обработка события, таска на обновление коробок в чекаутере создается")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/updateplaces/database/after/store_boxes_to_checkouter_task_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void success() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                CORRECT_DIFF_PATH,
                CORRECT_SNAPSHOT_PATH
            )
        ));
    }

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Не подходящие события")
    @ExpectedDatabase(
        value = "/jobs/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void notEligibleEvents(
        @SuppressWarnings("unused") String displayName,
        String diffFilePath,
        String snapshotFilePath
    ) {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                diffFilePath,
                snapshotFilePath
            )
        ));
    }

    @Nonnull
    private static Stream<Arguments> notEligibleEvents() {
        return Stream.of(
            Arguments.of(
                "Заказ не платформы BERU",
                CORRECT_DIFF_PATH,
                "logbroker/lom/event/updateplaces/snapshot/non_beru_snapshot.json"
            ),
            Arguments.of(
                "Нет заявок на изменение заказа в diff",
                "logbroker/lom/event/updateplaces/diff/no_update_places_request.json",
                CORRECT_SNAPSHOT_PATH
            ),
            Arguments.of(
                "Заявка на изменение заказа другого типа",
                "logbroker/lom/event/updateplaces/diff/added_another_change_order_request.json",
                "logbroker/lom/event/updateplaces/snapshot/another_change_order_request_snapshot.json"
            ),
            Arguments.of(
                "Заявка переходит в статус не INFO_RECEIVED",
                "logbroker/lom/event/updateplaces/diff/success_status.json",
                CORRECT_SNAPSHOT_PATH
            )
        );
    }

    @Test
    @DisplayName("Создание таски на сохранение коробок в чекаутере выключено")
    @ExpectedDatabase(
        value = "/jobs/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createStoreBoxesToCheckouterDisabled() {
        setupFeature(FeatureProperties::isCreateStoreBoxesToCheckouterTask, false);
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                CORRECT_DIFF_PATH,
                CORRECT_SNAPSHOT_PATH
            )
        ));

        assertLogs().anyMatch(logEqualsTo(
            TskvLogRecord.info("Skip creating STORE_BOXES_TO_CHECKOUTER task for order 33187394")
        ));
    }
}
