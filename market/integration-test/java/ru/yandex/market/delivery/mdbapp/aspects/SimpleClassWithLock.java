package ru.yandex.market.delivery.mdbapp.aspects;

import org.springframework.stereotype.Component;

import ru.yandex.market.delivery.mdbapp.components.curator.annotations.Locked;
import ru.yandex.market.delivery.mdbapp.components.curator.managers.LockManager;

@Component
public class SimpleClassWithLock {

    private final SimpleService simpleService;

    public SimpleClassWithLock(SimpleService simpleService) {
        this.simpleService = simpleService;
    }

    @Locked(LockManager.Lock.DEFAULT)
    public void openLock() {
        simpleService.handle();
    }

    @Locked(LockManager.Lock.DELIVERY_TARIFF)
    public void closedLock() {
        simpleService.handle();
    }
}
