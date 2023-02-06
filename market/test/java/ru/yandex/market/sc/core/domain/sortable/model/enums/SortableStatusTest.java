package ru.yandex.market.sc.core.domain.sortable.model.enums;

import com.google.common.base.Enums;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.les.sc.ScCargoUnitStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Конвертация SortableStatus в ScCargoUnitStatus LES юнит тест")
public class SortableStatusTest {
    @DisplayName("Проверяем конвертацию статусов в модель LES")
    @ParameterizedTest
    @EnumSource(SortableStatus.class)
    void checkSortableStatusConvertToScCargoUnitStatus(SortableStatus sortableStatus) {
        assertThat(Enums.getIfPresent(ScCargoUnitStatus.class, sortableStatus.name()).toJavaUtil()).isPresent();
    }
}
