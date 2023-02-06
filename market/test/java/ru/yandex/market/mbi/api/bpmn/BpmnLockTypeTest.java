package ru.yandex.market.mbi.api.bpmn;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.id.IdsUtils;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.mbi.api.client.entity.bpmn.BpmnLockType;

/**
 * Тесты для {@link BpmnLockType}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class BpmnLockTypeTest {

    @Test
    @DisplayName("У всех типов bpmn-блокировок уникальный id")
    void lockTypesHaveUniqueIds() {
        Set<String> uniqueIds = IdsUtils.toIdsSet(List.of(BpmnLockType.values()));

        Assertions.assertEquals(BpmnLockType.values().length, uniqueIds.size());
    }

    @Test
    @DisplayName("Все bpmn-блокировки привязаны к разным параметрам")
    void lockTypesHaveUniqueParamTypes() {
        Set<ParamType> uniqueParamTypes = Arrays.stream(BpmnLockType.values())
                .map(BpmnLockType::getLockParam)
                .collect(Collectors.toSet());

        Assertions.assertEquals(BpmnLockType.values().length, uniqueParamTypes.size());
    }

    @Test
    @DisplayName("Все параметры bpmn-блокировок - системные")
    void lockTypesHaveSystemParamTypes() {
        for (BpmnLockType lockType : BpmnLockType.values()) {
            Assertions.assertTrue(lockType.getLockParam().isSystem());
        }
    }
}
