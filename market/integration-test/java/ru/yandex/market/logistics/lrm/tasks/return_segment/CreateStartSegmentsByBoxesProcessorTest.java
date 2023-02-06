package ru.yandex.market.logistics.lrm.tasks.return_segment;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.config.properties.FeatureProperties;
import ru.yandex.market.logistics.lrm.queue.payload.ReturnCreateStartSegmentsByBoxesPayload;
import ru.yandex.market.logistics.lrm.queue.processor.CreateStartSegmentsByBoxesProcessor;

@DisplayName("Создание стартовых сегментов по коробкам")
class CreateStartSegmentsByBoxesProcessorTest extends AbstractIntegrationTest {

    @Autowired
    private CreateStartSegmentsByBoxesProcessor processor;

    @Autowired
    private FeatureProperties featureProperties;

    @AfterEach
    void tearDown() {
        featureProperties.setEnableCourierFashionFbyReturnFlow(false);
    }

    @Test
    @DisplayName("Успех, старый штрихкод")
    @DatabaseSetup("/database/tasks/return-segment/create-by-boxes/before/full.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/create-by-boxes/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successWithOldBarcode() {
        process();
    }

    @Test
    @DisplayName("Успех, новый штрихкод, курьерский возврат")
    @DatabaseSetup("/database/tasks/return-segment/create-by-boxes/before/full_courier.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/create-by-boxes/after/success_courier.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successWithNewBarcodeCourier() {
        process();
    }

    @Test
    @DisplayName("Успех, новый штрихкод, курьерский возврат с сегментом")
    @DatabaseSetup("/database/tasks/return-segment/create-by-boxes/before/full_courier.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/create-by-boxes/after/success_courier_segment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successNewClientFlowWithNewBarcodeCourierSegment() {
        featureProperties.setEnableCourierFashionFbyReturnFlow(true);
        process();
    }

    @Test
    @DisplayName("Успех, fashion fbs возврат в ПВЗ")
    @DatabaseSetup("/database/tasks/return-segment/create-by-boxes/before/full_fashion_fbs_pickup.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/create-by-boxes/after/success_pickup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successFashionFbsFlowPickup() {
        process();
    }

    @Test
    @DisplayName("Успех, fashion fby возврат в ПВЗ")
    @DatabaseSetup("/database/tasks/return-segment/create-by-boxes/before/full_fashion_ff_pickup.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/create-by-boxes/after/success_pickup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successNewFashionFbyFlowPickup() {
        process();
    }

    @Test
    @DisplayName("Успех, новый штрихкод FBS")
    @DatabaseSetup("/database/tasks/return-segment/create-by-boxes/before/full_new_client_flow_fbs.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/create-by-boxes/after/success_new_client_flow_fbs.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successNewClientFlowWithNewBarcodeFbs() {
        process();
    }

    @Test
    @DisplayName("Успех, новый штрихкод FF")
    @DatabaseSetup("/database/tasks/return-segment/create-by-boxes/before/full_new_client_flow_ff.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/create-by-boxes/after/success_new_client_flow_ff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successNewClientFlowWithNewBarcodeFf() {
        process();
    }

    @Test
    @DisplayName("Успех, клиентский возврат курьером")
    @DatabaseSetup("/database/tasks/return-segment/create-by-boxes/before/client_courier_return.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/create-by-boxes/after/success_client_courier_segment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void clientCourierReturn() {
        process();
    }

    private void process() {
        processor.execute(
            ReturnCreateStartSegmentsByBoxesPayload.builder()
                .returnId(1L)
                .sortingCenters(List.of())
                .build()
        );
    }
}
