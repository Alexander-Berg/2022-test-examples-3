package ru.yandex.market.api.partner.controllers.order.model.view.json;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import ru.yandex.market.api.partner.controllers.order.model.Delivery;
import ru.yandex.market.api.partner.view.json.Names;
import ru.yandex.market.checkout.common.json.JsonWriter;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DeliveryJsonSerializerTest {


    static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of("123", 124L, "123", 123L),
                Arguments.of(null, 124L, null, 124L),
                Arguments.of("123d", 124L, "123d", 124L)
        );
    }

    @ParameterizedTest
    @MethodSource("args")
    void serializeOutletCodesTest(
            String outletCode,
            Long outletId,
            String expectedOutletCode,
            Long expectedOutletId
    ) throws IOException {
        Delivery delivery = new Delivery();
        delivery.setOutletCode(outletCode);
        delivery.setOutletId(outletId);

        JsonWriter jsonWriter = mock(JsonWriter.class);
        new DeliveryJsonSerializer().serialize(delivery, jsonWriter);

        ArgumentCaptor<String> writerAttrNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> writerAttrLongValueCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> writerAttrStrValueCaptor = ArgumentCaptor.forClass(String.class);

        verify(jsonWriter, atLeastOnce()).setAttribute(writerAttrNameCaptor.capture(), writerAttrLongValueCaptor.capture());
        verify(jsonWriter, atLeastOnce()).setAttribute(writerAttrNameCaptor.capture(), writerAttrStrValueCaptor.capture());

        List<String> capturedNames = writerAttrNameCaptor.getAllValues();
        List<Long> capturedLongValues = writerAttrLongValueCaptor.getAllValues();
        List<String> capturedStrValues = writerAttrStrValueCaptor.getAllValues();

        for (int i = 0; i < capturedNames.size(); i++) {
            if (capturedNames.get(i).equals(Names.Delivery.OUTLET_ID)) {
                Assertions.assertEquals(expectedOutletId, capturedLongValues.get(i));
            }
            if (capturedNames.get(i).equals(Names.Delivery.OUTLET_CODE)) {
                Assertions.assertEquals(expectedOutletCode, capturedStrValues.get(i - capturedLongValues.size()));
            }
        }
    }

}
