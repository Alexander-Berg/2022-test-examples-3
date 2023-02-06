package ru.yandex.market.core.testing;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;

import static ru.yandex.market.core.testing.TestingStatus.CANCELED;
import static ru.yandex.market.core.testing.TestingStatus.DISABLED;
import static ru.yandex.market.core.testing.TestingStatus.FAILED;
import static ru.yandex.market.core.testing.TestingStatus.INITED;
import static ru.yandex.market.core.testing.TestingStatus.PASSED;

/**
 * Содержит полную информацию о модерации для магазина для всех програм.
 *
 * @author Vadim Lyalin
 */
public class FullTestingState implements Serializable {
    private static final EnumSet<TestingStatus> NOT_IN_TESTING_STATUSES =
            EnumSet.of(INITED, FAILED, CANCELED, PASSED, DISABLED);
    private final Collection<TestingState> testingStates;

    public FullTestingState(Collection<TestingState> testingStates) {
        this.testingStates = Collections.unmodifiableCollection(Objects.requireNonNull(testingStates));
        // Записи должны относиться к одному магазину
        if (testingStates.stream().map(TestingState::getDatasourceId).distinct().count() > 1) {
            throw new IllegalArgumentException("Testing states correspond to different shops");
        }
    }

    /**
     * Возвращаем "минимальный" тип для проверок. Нужен для обратной совместимости. По-хорошему надо переделать код,
     * где это используется.
     */
    public Optional<TestingType> getTestingType() {
        return testingStates.stream().map(TestingState::getTestingType).min(TestingType::compareTo);
    }

    /**
     * Возвращаем "максимальный" тип для проверок. Нужен для обратной совместимости. По-хорошему надо переделать код,
     * где это используется.
     */
    public Optional<TestingStatus> getTestingStatus() {
        return testingStates.stream().map(TestingState::getStatus).max(TestingStatus::compareTo);
    }

    public Collection<TestingState> getTestingStates() {
        return testingStates;
    }

    public boolean isInTesting() {
        return !testingStates.isEmpty()
                && !testingStates.stream().anyMatch(state -> NOT_IN_TESTING_STATUSES.contains(state.getStatus()));
    }
}
