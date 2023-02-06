package ru.yandex.market.mbi.bpmn.process;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.mbi.bpmn.model.enums.ProcessType;


class ProcessTypeTest {

    @Test
    void testProcessTypeConverting() {
        for (var apiProcessType : ru.yandex.market.mbi.bpmn.model.ProcessType.values()) {
            Assertions.assertDoesNotThrow(() -> ProcessType.from(apiProcessType));
        }
    }
}
