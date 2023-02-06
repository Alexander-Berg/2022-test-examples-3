package ru.yandex.market.mbo.db;

import ru.yandex.common.util.db.IdGenerator;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author anmalysh
 * @since 1/26/2019
 */
public class IdGeneratorStub implements IdGenerator {
    private AtomicLong idGenerator = new AtomicLong(1);
    @Override
    public long getId() {
        return idGenerator.getAndIncrement();
    }
}
