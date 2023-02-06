package ru.yandex.market.logistics.lrm.converter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.lrm.LrmTest;
import ru.yandex.market.logistics.lrm.api.model.LomSegmentStatus;

@DisplayName("Конвертация статусов LOM")
class LomStatusConverterTest extends LrmTest {

    private final LomStatusConverter lomStatusConverter = new LomStatusConverter();

    @ParameterizedTest
    @EnumSource(LomSegmentStatus.class)
    void convertToSegmentStatus(LomSegmentStatus status) {
        softly.assertThat(lomStatusConverter.convert(status)).isNotNull();
    }
}
