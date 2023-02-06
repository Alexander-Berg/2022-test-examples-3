package ru.yandex.market.logistics.lom.converter.lgw;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.gateway.common.model.properties.ClientRequestMeta;
import ru.yandex.market.logistics.lom.AbstractTest;

@DisplayName("Тесты для методов формирования DTO ClientRequestMeta для LGW")
class LgwClientRequestMetaConverterTest extends AbstractTest {
    @Test
    @DisplayName("Проверка конвертации sequenceId в ClientRequestMeta для LGW")
    void testConvertSequenceIdToClientRequestMeta() {
        ClientRequestMeta actualClientRequestMeta =
            LgwClientRequestMetaConverter.convertSequenceIdToClientRequestMeta(123L);
        softly.assertThat(actualClientRequestMeta).isEqualTo(new ClientRequestMeta("123"));
    }
}
