package ru.yandex.market.wms.common.spring.enums;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ContainerIdTypeTest {

    @ParameterizedTest
    @ValueSource(strings = {"VS", "TM"})
    void isToteReturnsTrueWhenContainerIsTote(ContainerIdType containerIdType) {
        Assertions.assertTrue(containerIdType.isTote());
    }

    @ParameterizedTest
    @ValueSource(strings = {"VS", "TM", "BL", "BM"})
    void isReceivingConveyableReturnsTrueWhenContainerIsReceivingConveyable(ContainerIdType containerIdType) {
        Assertions.assertTrue(containerIdType.isReceivingConveyable());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PLT", "CART", "L", "CDR", "RCP", "CONT", "AN", "DROP", "P", "FLPB", "UNKNOWN", "BL", "BM"})
    void isToteReturnsFalseWhenContainerIsNotTote(ContainerIdType containerIdType) {
        Assertions.assertFalse(containerIdType.isTote());
    }

    @ParameterizedTest
    @ValueSource(strings = {"BL", "BM"})
    void isConveyableCartonContainerReturnsTrueWhenContainerIsConveyableCarton(ContainerIdType containerIdType) {
        Assertions.assertTrue(containerIdType.isConveyableCartonContainer());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PLT", "CART", "L", "CDR", "RCP", "CONT", "AN", "DROP", "P", "FLPB", "UNKNOWN", "VS", "TM"})
    void isConveyableCartonContainerReturnsFalseWhenContainerIsConveyableCarton(ContainerIdType containerIdType) {
        Assertions.assertFalse(containerIdType.isConveyableCartonContainer());
    }
}
