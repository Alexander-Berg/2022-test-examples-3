package ru.yandex.market.logistics.lom.service.internalVariable;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.InternalVariable;
import ru.yandex.market.logistics.lom.entity.enums.InternalVariableType;
import ru.yandex.market.logistics.lom.repository.InternalVariableRepository;
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;

@DisplayName("Сервис работы с internal variable")
class InternalVariableServiceImplTest extends AbstractContextualTest {

    @Autowired
    private InternalVariableService internalVariableService;

    @Autowired
    private InternalVariableRepository internalVariableRepository;

    @Test
    @JpaQueriesCount(5)
    @DisplayName("Получение значений internal variable кэшировано")
    void getValueCacheable() {
        String cachedValue = "enabled";
        internalVariableRepository.save(
            new InternalVariable()
                .setValue(cachedValue)
                .setType(InternalVariableType.SWITCHING_TO_ARNOLD_ENABLED)
        );
        softly.assertThat(internalVariableService.getValue(InternalVariableType.SWITCHING_TO_ARNOLD_ENABLED))
            .isEqualTo(Optional.of(cachedValue));

        internalVariableRepository.save(
            new InternalVariable()
                .setValue("disabled")
                .setType(InternalVariableType.SWITCHING_TO_ARNOLD_ENABLED)
        );
        for (int i = 0; i < 100; i++) {
            softly.assertThat(internalVariableService.getValue(InternalVariableType.SWITCHING_TO_ARNOLD_ENABLED))
                .isEqualTo(Optional.of(cachedValue));
        }
    }

    @Test
    @JpaQueriesCount(5)
    @DisplayName("Получение boolean значений internal variable для флагов кешированное")
    void getBooleanValueCacheable() {
        internalVariableRepository.save(
            new InternalVariable()
                .setValue("true")
                .setType(InternalVariableType.SWITCHING_TO_ARNOLD_ENABLED)
        );
        softly.assertThat(internalVariableService.getBooleanValue(
                InternalVariableType.SWITCHING_TO_ARNOLD_ENABLED
            ))
            .isTrue();

        internalVariableRepository.save(
            new InternalVariable()
                .setValue("false")
                .setType(InternalVariableType.SWITCHING_TO_ARNOLD_ENABLED)
        );
        for (int i = 0; i < 100; i++) {
            softly.assertThat(internalVariableService.getBooleanValue(
                    InternalVariableType.SWITCHING_TO_ARNOLD_ENABLED
                ))
                .isTrue();
        }
    }

    @Test
    @JpaQueriesCount(5)
    @DisplayName("Получение значений List<Long> internal variable кэшировано")
    void getLongValuesCacheable() {
        String cachedValue = "1,2";
        Set<Long> value = Set.of(1L, 2L);
        internalVariableRepository.save(
            new InternalVariable()
                .setValue(cachedValue)
                .setType(InternalVariableType.LOM_ORDER_CANCELLATION_ERROR_WITH_TECH_FAIL_EXCLUDE_PARTNERS)
        );
        Collection<Long> longValues = internalVariableService.getLongValues(
            InternalVariableType.LOM_ORDER_CANCELLATION_ERROR_WITH_TECH_FAIL_EXCLUDE_PARTNERS
        );
        softly.assertThat(longValues).isEqualTo(value);

        String newCachedValue = "1,2,3";
        internalVariableRepository.save(
            new InternalVariable()
                .setValue(newCachedValue)
                .setType(InternalVariableType.LOM_ORDER_CANCELLATION_ERROR_WITH_TECH_FAIL_EXCLUDE_PARTNERS)
        );
        for (int i = 0; i < 100; i++) {
            Collection<Long> newLongValues = internalVariableService.getLongValues(
                InternalVariableType.LOM_ORDER_CANCELLATION_ERROR_WITH_TECH_FAIL_EXCLUDE_PARTNERS
            );
            softly.assertThat(newLongValues).isEqualTo(value);
        }
    }
}
