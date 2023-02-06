package ru.yandex.market.delivery.mdbapp.components.service;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.mdbapp.AbstractMediumContextualTest;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.InternalVariable;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.InternalVariableType;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.InternalVariableRepository;

@DisplayName("Сервис работы с internal variable")
class InternalVariableServiceTest extends AbstractMediumContextualTest {

    @Autowired
    private InternalVariableService internalVariableService;

    @Autowired
    private InternalVariableRepository internalVariableRepository;

    @Test
    @DisplayName("Получение значений internal variable кэшировано")
    void getValueCacheable() {
        String cachedValue = "enabled";
        internalVariableRepository.save(
            new InternalVariable()
                .setValue(cachedValue)
                .setType(InternalVariableType.CHECKOUTER_LOGBROKER_BATCH_SIZE)
        );
        softly.assertThat(internalVariableService.getValue(InternalVariableType.CHECKOUTER_LOGBROKER_BATCH_SIZE))
            .isEqualTo(Optional.of(cachedValue));

        internalVariableRepository.save(
            new InternalVariable()
                .setValue("disabled")
                .setType(InternalVariableType.CHECKOUTER_LOGBROKER_BATCH_SIZE)
        );
        for (int i = 0; i < 100; i++) {
            softly.assertThat(internalVariableService.getValue(InternalVariableType.CHECKOUTER_LOGBROKER_BATCH_SIZE))
                .isEqualTo(Optional.of(cachedValue));
        }
    }

    @Test
    @DisplayName("Получение boolean значений internal variable для флагов кешированное")
    void getBooleanValueCacheable() {
        internalVariableRepository.save(
            new InternalVariable()
                .setValue("true")
                .setType(InternalVariableType.CHECKOUTER_LOGBROKER_BATCH_SIZE)
        );
        softly.assertThat(internalVariableService.getBooleanValue(
                InternalVariableType.CHECKOUTER_LOGBROKER_BATCH_SIZE
            ))
            .isTrue();

        internalVariableRepository.save(
            new InternalVariable()
                .setValue("false")
                .setType(InternalVariableType.CHECKOUTER_LOGBROKER_BATCH_SIZE)
        );
        for (int i = 0; i < 100; i++) {
            softly.assertThat(internalVariableService.getBooleanValue(
                    InternalVariableType.CHECKOUTER_LOGBROKER_BATCH_SIZE
                ))
                .isTrue();
        }
    }
}
