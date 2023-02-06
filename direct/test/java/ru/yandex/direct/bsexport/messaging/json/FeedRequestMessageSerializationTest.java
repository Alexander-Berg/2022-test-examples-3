package ru.yandex.direct.bsexport.messaging.json;

import com.google.protobuf.Any;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.bsexport.messaging.BaseSerializationTest;
import ru.yandex.direct.bsexport.model.FeedRequestMessage;
import ru.yandex.direct.bsexport.model.MessageType;
import ru.yandex.direct.bsexport.model.Order;
import ru.yandex.direct.bsexport.testing.data.TestLogrbokerDebugInfo;
import ru.yandex.direct.bsexport.testing.data.TestOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.bsexport.testing.Util.getCompactJsonFromClasspath;

class FeedRequestMessageSerializationTest extends BaseSerializationTest {

    @Test
    void orderMessageSerialization() {
        Order order = TestOrder.textWithUpdateInfo1Full;
        FeedRequestMessage message = FeedRequestMessage.newBuilder()
                .setDebugInfo(TestLogrbokerDebugInfo.ppcdev3Std1)
                .setFullExportFlag(0)
                .setIterId(555393281)
                .setUuid("2652C122-1787-11EA-AA70-D0D5A8E9E60C")
                .setActualInDirectDbAt(1575567977)
                .setLevel(MessageType.ORDER)
                .setData(Any.pack(order))
                .setCid(order.getEID())
                .build();

        serialize(message);

        String expected = getCompactJsonFromClasspath("json/feed_request_order_1.json");
        assertThat(json).isEqualTo(expected);
    }
}
