package ru.yandex.market.wms.receiving.unit.converter;

import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.model.enums.FillingStatus;
import ru.yandex.market.wms.common.model.enums.ReceivingContainerType;
import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.receiving.model.converter.ReceivingFillingStatusConverter;

public class ReceivingFillingStatusConverterTest extends BaseTest {

    @Test
    void convertMeasureContainerTypeToMeasureFillingStatus() {
        assertions.assertThat(ReceivingFillingStatusConverter.convert(ReceivingContainerType.MEASURE))
                .isEqualTo(FillingStatus.MEASURE);
    }

    @Test
    void convertUnknownContainerTypeToOtherFillingStatus() {
        assertions.assertThat(ReceivingFillingStatusConverter.convert(ReceivingContainerType.UNKNOWN))
                .isEqualTo(FillingStatus.OTHER);
    }

    @Test
    void convertNullContainerTypeToOtherFillingStatus() {
        assertions.assertThat(ReceivingFillingStatusConverter.convert(null))
                .isEqualTo(FillingStatus.OTHER);
    }

    @Test
    void convertAnyContainerTypeToReceivingFillingStatus() {
        assertions.assertThat(ReceivingFillingStatusConverter.convert(ReceivingContainerType.EXPENSIVE))
                .isEqualTo(FillingStatus.RECEIVING);
    }
}
