package ru.yandex.market.wms.common.model.enums;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.spring.BaseTest;

class LocStatusTest extends BaseTest {
    @Test
    void ofSingleTypeCisQuar() {
        assertions.assertThat(LocStatus.of(InventoryHoldStatus.CIS_QUAR)).isEqualTo(LocStatus.OK);
    }

    @Test
    void ofSingleTypeOK() {
        assertions.assertThat(LocStatus.of(InventoryHoldStatus.OK)).isEqualTo(LocStatus.OK);
    }

    @Test
    void ofSingleTypeNone() {
        assertions.assertThat(LocStatus.of(InventoryHoldStatus.NONE)).isEqualTo(LocStatus.OK);
    }

    @Test
    void ofSingleTypeDamage() {
        assertions.assertThat(LocStatus.of(InventoryHoldStatus.DAMAGE)).isEqualTo(LocStatus.HOLD);
    }

    @Test
    void ofMultipleEmptySet() {
        assertions.assertThat(LocStatus.of(Collections.emptySet())).isEqualTo(LocStatus.OK);
    }

    @Test
    void ofMultipleOK() {
        assertions.assertThat(LocStatus.of(Set.of(InventoryHoldStatus.OK, InventoryHoldStatus.CIS_QUAR)))
                .isEqualTo(LocStatus.OK);
    }

    @Test
    void ofMultipleHold() {
        assertions.assertThat(
                LocStatus.of(Set.of(InventoryHoldStatus.OK, InventoryHoldStatus.CIS_QUAR, InventoryHoldStatus.DAMAGE)))
                .isEqualTo(LocStatus.HOLD);
    }
}
