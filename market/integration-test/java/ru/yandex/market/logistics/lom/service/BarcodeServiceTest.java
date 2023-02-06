package ru.yandex.market.logistics.lom.service;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.enums.OrderStatus;
import ru.yandex.market.logistics.lom.entity.enums.PlatformClient;
import ru.yandex.market.logistics.lom.exception.http.InappropriateOrderStateException;
import ru.yandex.market.logistics.lom.service.barcode.BarcodeService;

@DisplayName("Генерация штрихкода заказа")
class BarcodeServiceTest extends AbstractContextualTest {
    private static final String NO_EXTERNAL_ID_DAAS_BARCODE = "LOinttest-1";

    @Autowired
    private BarcodeService barcodeService;

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Создание штрихкода по стратегии DAAS")
    void daasBarcode(String externalId, String expectedBarcode) {
        String actualBarcode = barcodeService.createOrUpdateBarcode(order().setExternalId(externalId));
        softly.assertThat(actualBarcode).isEqualTo(expectedBarcode);
    }

    @Nonnull
    private static Stream<Arguments> daasBarcode() {
        return Stream.of(
            Arguments.of(null, NO_EXTERNAL_ID_DAAS_BARCODE),
            Arguments.of("", NO_EXTERNAL_ID_DAAS_BARCODE),
            Arguments.of("ext1", "ext1-" + NO_EXTERNAL_ID_DAAS_BARCODE)

        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @EnumSource(value = PlatformClient.class, names = {"BERU", "DBS"})
    @DisplayName("Создание штрихкода по стратегии копирования externalId")
    void barcodeForBeru(PlatformClient platformClient) {
        Order order = order();
        order.setExternalId("checkouterOrderId");
        order.setPlatformClient(platformClient);
        softly.assertThat(barcodeService.createOrUpdateBarcode(order)).isEqualTo("checkouterOrderId");
    }

    @Test
    @DisplayName("Нельзя изменить баркод закоммиченного заказа")
    void existingBarcodeNotChangedForCommitedOrder() {
        Order order = order().setExternalId("ext1")
            .setStatus(OrderStatus.PROCESSING, clock)
            .setBarcode("LO-1");
        softly.assertThatThrownBy(() -> barcodeService.createOrUpdateBarcode(order))
            .hasMessage("Unable to create/update barcode for non-draft order=1")
            .isInstanceOf(InappropriateOrderStateException.class);
    }

    @Test
    @DisplayName("Можно изменить баркод черновика заказа")
    void existingBarcodeChangedForDraftOrder() {
        softly.assertThat(barcodeService.createOrUpdateBarcode(order().setExternalId("ext1").setBarcode("LO-1")))
            .isEqualTo("ext1-" + NO_EXTERNAL_ID_DAAS_BARCODE);
    }

    @Nonnull
    private Order order() {
        return new Order()
            .setStatus(OrderStatus.DRAFT, clock)
            .setPlatformClient(PlatformClient.YANDEX_DELIVERY)
            .setId(1L);
    }
}
