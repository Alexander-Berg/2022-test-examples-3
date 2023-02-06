package ru.yandex.market.tpl.core.test;

import java.time.Clock;
import java.util.HashSet;
import java.util.Set;

import lombok.Getter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.core.CleanupAfterEachV2Extension;
import ru.yandex.market.tpl.core.CleanupCachesAfterEachExtension;
import ru.yandex.market.tpl.core.CoreTestV2;

@CoreTestV2
@Getter
@ExtendWith({CleanupAfterEachV2Extension.class, CleanupCachesAfterEachExtension.class})
public abstract class TplAbstractTest {

    @Autowired
    private Clock clock;

    private final Set<Object> removingEntities = new HashSet<>();

    /**
     * Добавлен, чтобы можно было удалить конкретные сущности, не зацепив тестовые данные, которые приходят
     * с разных LB скриптов.
     * Напр,  PickupPoint приходят при старте теста с LB-скриптами + мы добавляем их при создании Order
     * Order - чистятся каскадно, но PickupPoint остаются,  что может мешать исполнению других стестов.
     *
     * @param entity
     */
    protected void clearAfterTest(Object entity) {
        this.removingEntities.add(entity);
    }


    @BeforeEach
    void beforeEach() {
        ClockUtil.reset(clock);
    }

    @AfterEach
    void afterEach() {
        ClockUtil.reset(clock);
    }

    protected Clock getClock() {
        return clock;
    }
}
