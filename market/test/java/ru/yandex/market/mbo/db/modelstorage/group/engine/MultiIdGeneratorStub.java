package ru.yandex.market.mbo.db.modelstorage.group.engine;

import ru.yandex.common.util.db.MultiIdGenerator;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * @author s-ermakov
 */
public class MultiIdGeneratorStub implements MultiIdGenerator {

    private final AtomicLong atomicLong;

    public MultiIdGeneratorStub(long initialValue) {
        this.atomicLong = new AtomicLong(initialValue);
    }

    public MultiIdGeneratorStub(AtomicLong atomicLong) {
        this.atomicLong = atomicLong;
    }

    public long getLastId() {
        return atomicLong.get();
    }

    public void setStartId(long id) {
        atomicLong.set(id);
    }

    @Override
    public List<Long> getIds(int count) {
        return LongStream.range(0, count).map(i -> getId()).boxed().collect(Collectors.toList());
    }

    @Override
    public long getId() {
        return atomicLong.addAndGet(1);
    }
}
