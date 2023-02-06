package ru.yandex.market.clab.common.service.goodtype;

import ru.yandex.market.clab.common.service.ConcurrentModificationException;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.GoodType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 14.01.2019
 */
public class GoodTypeRepositoryStub implements GoodTypeRepository {
    private final AtomicLong idGenerator = new AtomicLong();
    private final List<GoodType> goodTypes = new ArrayList<>();

    public List<GoodType> getTypesInternal() {
        return goodTypes;
    }

    @Override
    public List<GoodType> getTypes() {
        return Collections.unmodifiableList(goodTypes);
    }

    @Override
    public GoodType getById(Long id) {
        return goodTypes.stream()
            .filter(g -> g.getId().equals(id))
            .findFirst().orElse(null);
    }

    @Override
    public GoodType saveType(GoodType type) {
        if (type.getId() == null) {
            type.setId(idGenerator.incrementAndGet());
        } else {
            if (!goodTypes.removeIf(t -> t.getId().equals(type.getId()))) {
                throw new ConcurrentModificationException();
            }
        }
        goodTypes.add(type);
        return type;
    }

    @Override
    public void removeType(long goodTypeId) {
        goodTypes.removeIf(t -> t.getId().equals(goodTypeId));
    }
}
