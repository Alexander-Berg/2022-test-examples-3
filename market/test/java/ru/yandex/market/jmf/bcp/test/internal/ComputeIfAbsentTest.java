package ru.yandex.market.jmf.bcp.test.internal;

import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.metadata.Fqn;

public interface ComputeIfAbsentTest extends Entity {
    Fqn FQN = Fqn.of("computeIfAbsentTest");
}
