package ru.yandex.market.logistics.lrm.les;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.les.dto.PvzReturnStatusType;
import ru.yandex.market.logistics.lrm.LrmTest;
import ru.yandex.market.logistics.lrm.converter.les.PvzReturnStatusesConverter;

@DisplayName("Конвертация PvzReturnStatusType из LES")
class PvzReturnStatusesConverterTest extends LrmTest {

    private final PvzReturnStatusesConverter converter = new PvzReturnStatusesConverter();

    @DisplayName("В ReturnSegmentStatus из LRM")
    @ParameterizedTest
    @EnumSource(PvzReturnStatusType.class)
    void toReturnSegmentStatus(PvzReturnStatusType status) {
        softly.assertThatCode(() -> converter.convertStatus(status))
            .doesNotThrowAnyException();
    }

}
