package ru.yandex.market.logistics.lom.jobs.processor.les;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.lom.OrderArrivedPickupPointEvent;
import ru.yandex.market.logistics.lom.jobs.model.LesOrderArrivedPickupPointEventPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static org.mockito.Mockito.verify;

@DisplayName("Тест на отправку ивента о прибытии заказа в ПВЗ или Постамат в LES")
@DatabaseSetup("/jobs/processor/les_export/before/order_pvz.xml")
class ExportOrderArrivedPickupPointProcessorTest extends AbstractExportLesEventProcessorTest {
    @Autowired
    private ExportOrderArrivedPickupPointProcessor processor;

    @Test
    @DisplayName("Отправка ивента о прибытии заказа в ПВЗ или Постамат в LES")
    void success() {
        test(new OrderArrivedPickupPointEvent(
            1L,
            "LO1",
            "+79999999999",
            "1234",
            FIXED_TIME,
            "улица Тест, 11",
            213
        ));
    }

    @Test
    @DisplayName("Отправка ивента о прибытии заказа в ПВЗ или Постамат в LES - null в поле house")
    @DatabaseSetup(
        value = "/jobs/processor/les_export/before/order_pickup_address_house_null.xml",
        type = DatabaseOperation.UPDATE
    )
    void ignoreNullHouse() {
        test(new OrderArrivedPickupPointEvent(
            1L,
            "LO1",
            "+79999999999",
            "1234",
            FIXED_TIME,
            "улица Тест, 12",
            213
        ));
    }

    @Test
    @DisplayName("Отправка ивента о прибытии заказа в ПВЗ или Постамат в LES - null в поле street")
    @DatabaseSetup(
        value = "/jobs/processor/les_export/before/order_pickup_address_street_null.xml",
        type = DatabaseOperation.UPDATE
    )
    void notFailOnNullStreet() {
        test(new OrderArrivedPickupPointEvent(
            1L,
            "LO1",
            "+79999999999",
            "1234",
            FIXED_TIME,
            "поселение Цветочное, 15",
            213
        ));
    }

    @Test
    @DisplayName("Отправка ивента о прибытии заказа в ПВЗ или Постамат в LES - null в полях street и locality")
    @DatabaseSetup(
        value = "/jobs/processor/les_export/before/order_pickup_address_street_and_locality_null.xml",
        type = DatabaseOperation.UPDATE
    )
    void failOnNullStreetAndLocality() {
        softly.assertThatThrownBy(() -> processor.processPayload(getPayload()))
            .hasMessage("Invalid pickup point address");
    }

    private void test(OrderArrivedPickupPointEvent expectedPayload) {
        ProcessingResult result = processor.processPayload(getPayload());
        softly.assertThat(result).isEqualTo(ProcessingResult.success());

        Event event = new Event(
            "lom",
            "100",
            FIXED_TIME.toEpochMilli(),
            OrderArrivedPickupPointEvent.EVENT_TYPE,
            expectedPayload,
            ""
        );

        verify(lesProducer).send(event, "lom_out");
    }

    private LesOrderArrivedPickupPointEventPayload getPayload() {
        return PayloadFactory.lesOrderArrivedPickupPointEventPayload(
            100,
            1,
            FIXED_TIME,
            "1",
            1
        );
    }
}
