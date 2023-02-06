package ru.yandex.market.logistics.lom.admin.converter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.admin.enums.AdminCancellationOrderReason;
import ru.yandex.market.logistics.lom.model.enums.CancellationOrderReason;

public class AdminCancellationOrderReasonConverterTest extends AbstractTest {

    @DisplayName("Для всякой внутренней причины существует маппинг в админскую")
    @Test
    public void allInnerReasonsHaveMappingToAdminReasons() {
        CancellationOrderReason.valuesStream().forEach(reason -> {
            AdminCancellationOrderReason adminReason = AdminCancellationOrderReason.valueOf(reason.getName());
            softly.assertThat(adminReason).isNotNull();
            softly.assertThat(adminReason.getName()).isEqualTo(reason.getName());
        });

    }
}
