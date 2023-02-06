package ru.yandex.market.logistics.lom.service.les;

import java.time.Instant;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.les.OrderDamagedEvent;
import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.boxbot.CodeEvent;
import ru.yandex.market.logistics.les.dto.AddressDto;
import ru.yandex.market.logistics.les.dto.LocationDto;
import ru.yandex.market.logistics.les.dto.TplAddressChangedSource;
import ru.yandex.market.logistics.les.tpl.TplAddressChangedEvent;
import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@DisplayName("Тест обработки сообщения из LES")
class LesSqsConsumerTest extends AbstractContextualTest {

    @Autowired
    private LesConsumer lesConsumer;

    @Test
    @DisplayName("Event без подходящего processor")
    @ExpectedDatabase(
        value = "/service/les/after/empty_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processUnknownEvent() {
        Event event = new Event(
            "lom",
            "event_id_3",
            Instant.now().toEpochMilli(),
            "test_type",
            new OrderDamagedEvent("123"),
            "Тест"
        );
        lesConsumer.processEvent("messageId", event);
    }

    @Test
    @DisplayName("Обработка ивента с кодом постамата")
    @ExpectedDatabase(
        value = "/service/les/after/save_locker_code_task.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processTrueEventTest() {
        var event = new Event(
            "lom",
            "event_id_3",
            Instant.now().toEpochMilli(),
            CodeEvent.EVENT_TYPE_NEW_CODE,
            new CodeEvent("1", "test"),
            "Тест"
        );
        lesConsumer.processEvent("messageId", event);
    }

    @Test
    @DisplayName("Обработка события об изменении адреса в TPL")
    @DatabaseSetup("/service/les/save_tpl_address/before/order.xml")
    @ExpectedDatabase(
        value = "/service/les/save_tpl_address/after/save_tpl_address_task_created.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processTplAddressChangedEvent() {
        var event = new Event(
            "courier",
            "event_id_4",
            Instant.now().toEpochMilli(),
            TplAddressChangedEvent.EVENT_TYPE,
            new TplAddressChangedEvent(
                "1",
                createAddress(),
                null,
                TplAddressChangedSource.SYSTEM,
                "Новый комментарий"
            ),
            "Тест"
        );
        lesConsumer.processEvent("messageId", event);
    }

    @Nonnull
    private AddressDto createAddress() {
        return new AddressDto(
            "Какой-то адрес целиком",
            new LocationDto(
                "Россия",
                "Москва",
                "Москва и Московская область",
                null,
                null,
                null,
                "Новая улица",
                "Новый дом",
                null,
                "Новый корпус",
                "Новая квартира",
                "12345",
                "Новый подъезд",
                1,
                null,
                55.018803f,
                82.933952f,
                12345,
                "Новый домофон"
            )
        );
    }
}
