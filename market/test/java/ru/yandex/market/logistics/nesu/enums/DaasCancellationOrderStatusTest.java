package ru.yandex.market.logistics.nesu.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.lom.model.enums.CancellationOrderStatus;
import ru.yandex.market.logistics.nesu.AbstractTest;
import ru.yandex.market.logistics.nesu.dto.enums.DaasCancellationOrderStatus;

import static ru.yandex.market.logistics.nesu.dto.enums.DaasCancellationOrderStatus.UNKNOWN;
import static ru.yandex.market.logistics.nesu.dto.enums.DaasCancellationOrderStatus.createBasedOn;

public class DaasCancellationOrderStatusTest extends AbstractTest {

    @ParameterizedTest
    @EnumSource(CancellationOrderStatus.class)
    @DisplayName("Проверка, что все статусы из LOM замаплены")
    void allMapped(CancellationOrderStatus lomStatus) {
        DaasCancellationOrderStatus status = createBasedOn(lomStatus);

        if (lomStatus == CancellationOrderStatus.UNKNOWN) {
            softly.assertThat(status).isEqualTo(UNKNOWN);
        } else {
            softly.assertThat(status).isNotEqualTo(UNKNOWN);
        }
        softly.assertThat(status.getDescription()).endsWith(".");
    }
}
