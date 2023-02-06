package ru.yandex.market.partner.content.common.utils;

import org.junit.Test;
import ru.yandex.market.ir.http.ProtocolMessage;
import ru.yandex.market.partner.content.common.message.MessageInfo;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class MessageUtilsTest {

    @Test
    public void limitSkusForMessage() {
        assertThat(MessageUtils.limitSkusForMessage(new String[]{})).isEqualTo(new String[]{});
        assertThat(MessageUtils.limitSkusForMessage(skus(1))).isEqualTo(skus(1));
        String[] actualFor9 = MessageUtils.limitSkusForMessage(skus(9));
        assertThat(actualFor9).isEqualTo(skus(9));
        assertThat(actualFor9).isSorted();
        
        String[] twelve = skus(120);
        String[] actual = MessageUtils.limitSkusForMessage(twelve);
        assertThat(actual).hasSize(11);
        assertThat(actual[actual.length - 1]).isEqualTo("так же в 110 других");
    }

    @Test
    public void testMessageLevel() {
        try {
            Arrays.stream(MessageInfo.Level.values()).forEach(level -> {
                        ProtocolMessage.Message.Level pmLevel = ProtocolMessage.Message.Level.forNumber(level.ordinal() + 1);
                        assertThat(pmLevel.name()).isEqualTo(level.name());
                    }
            );
        } catch (Exception ex) {
            fail("MessageInfo.Level must correspond with ProtocolMessage.Message.Level " +
                    "in /arcadia/market/proto/content/ir");
        }
    }

    private String[] skus(int times) {
        String[] result = new String[times];
        Arrays.fill(result, "sku");
        return result;
    }
}