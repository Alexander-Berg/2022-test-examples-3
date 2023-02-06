package ru.yandex.market.logistics.logistics4shops.queue.processor.checkouter;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yoomoney.tech.dbqueue.api.TaskExecutionResult;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.config.properties.FeatureProperties;
import ru.yandex.market.logistics.logistics4shops.factory.CheckouterFactory;
import ru.yandex.market.logistics.logistics4shops.model.exception.ResourceNotFoundException;
import ru.yandex.market.logistics.logistics4shops.queue.payload.checkouter.StoreBoxesToCheckouterPayload;

import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.logistics.logistics4shops.factory.CheckouterFactory.CHECKOUTER_ORDER_ID;
import static ru.yandex.market.logistics.logistics4shops.factory.CheckouterFactory.CHECKOUTER_PARCEL_ID;
import static ru.yandex.market.logistics.logistics4shops.factory.CheckouterFactory.CHECKOUTER_SHOP_ID;
import static ru.yandex.market.logistics.logistics4shops.factory.CheckouterFactory.SYSTEM_USER;

@DisplayName("Сохранение коробок в чекаутере")
class StoreBoxesToCheckouterProcessorTest extends AbstractIntegrationTest {

    @Autowired
    private StoreBoxesToCheckouterProcessor processor;

    @Autowired
    private CheckouterAPI checkouterAPI;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(checkouterAPI);
    }

    @Test
    @DisplayName("Заказ не найден в чекаутере")
    void orderNotFound() throws Exception {
        try (
            var mockGet = checkouterFactory.mockGetOrderNotFound(SYSTEM_USER, CHECKOUTER_ORDER_ID)
        ) {
            softly.assertThatThrownBy(this::runProcessor)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Failed to find [ORDER] with id [123456]");
        }
    }

    @ParameterizedTest
    @MethodSource
    @DisplayName("Пустые коробки в задаче")
    void emptyBoxes(List<StoreBoxesToCheckouterPayload.OrderBox> emptyBoxes) {
        softly.assertThat(runProcessor(emptyBoxes))
            .isEqualTo(TaskExecutionResult.finish());
    }

    @Nonnull
    private static Stream<Arguments> emptyBoxes() {
        return Stream.<List<?>>of(List.of(), null).map(Arguments::of);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("Успешное сохранение")
    void success(boolean saveEnabled) throws Exception {
        if (saveEnabled) {
            setupFeature(FeatureProperties::getOrderIdThresholdForSavingBoxesInDb, 100500L);
        }
        try (
            var mockGet = checkouterFactory.mockGetOrder(
                SYSTEM_USER,
                CHECKOUTER_ORDER_ID,
                CheckouterFactory.buildOrder(CHECKOUTER_ORDER_ID, CHECKOUTER_SHOP_ID)
            );
            var mockPutBoxes = checkouterFactory.mockPutOrderBoxes(
                SYSTEM_USER,
                CHECKOUTER_ORDER_ID,
                CHECKOUTER_PARCEL_ID,
                getExpectedParcelBoxes(saveEnabled)
            )
        ) {
            softly.assertThat(runProcessor())
                .isEqualTo(TaskExecutionResult.finish());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("Успешно не обновляем коробки, если такие же уже сохранены")
    void dontSaveWhenBoxesEqual(boolean saveEnabled) throws Exception {
        if (saveEnabled) {
            setupFeature(FeatureProperties::getOrderIdThresholdForSavingBoxesInDb, 100500L);
        }
        try (
            var mockGet = checkouterFactory.mockGetOrder(
                SYSTEM_USER,
                CHECKOUTER_ORDER_ID,
                CheckouterFactory.buildOrderWithBoxes(
                    CHECKOUTER_ORDER_ID,
                    CHECKOUTER_SHOP_ID,
                    getExpectedParcelBoxes(true)
                )
            )
        ) {
            softly.assertThat(runProcessor())
                .isEqualTo(TaskExecutionResult.finish());
        }
    }

    @Nonnull
    private List<ParcelBox> getExpectedParcelBoxes(boolean setBoxIds) {
        ParcelBox box1 = new ParcelBox();
        box1.setFulfilmentId("box-id-with-dimensions");
        box1.setWeight(1234L);
        box1.setWidth(1L);
        box1.setHeight(2L);
        box1.setDepth(3L);
        ParcelBox box2 = new ParcelBox();
        box2.setFulfilmentId("box-id-without-dimensions");
        if (setBoxIds) {
            box1.setId(1L);
            box2.setId(2L);
        }
        return List.of(box1, box2);
    }

    @Nonnull
    private TaskExecutionResult runProcessor() {
        return runProcessor(List.of(
            StoreBoxesToCheckouterPayload.OrderBox.builder()
                .fulfilmentId("box-id-without-dimensions")
                .build(),
            StoreBoxesToCheckouterPayload.OrderBox.builder()
                .fulfilmentId("box-id-with-dimensions")
                .weight(1234L)
                .width(1L)
                .height(2L)
                .depth(3L)
                .build()
        ));
    }

    @Nonnull
    private TaskExecutionResult runProcessor(List<StoreBoxesToCheckouterPayload.OrderBox> boxes) {
        return processor.execute(
            StoreBoxesToCheckouterPayload.builder()
                .orderId(CHECKOUTER_ORDER_ID)
                .boxes(boxes)
                .build()
        );
    }
}
