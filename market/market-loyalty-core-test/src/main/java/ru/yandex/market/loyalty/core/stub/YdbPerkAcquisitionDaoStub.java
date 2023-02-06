package ru.yandex.market.loyalty.core.stub;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.core.dao.ydb.PerkAcquisitionDao;
import ru.yandex.market.loyalty.core.model.perk.EmptyParams;
import ru.yandex.market.loyalty.core.model.perk.PerkAcquisition;

/**
 * @author : poluektov
 * date: 2020-04-21.
 */
public class YdbPerkAcquisitionDaoStub implements PerkAcquisitionDao, StubDao {

    private final List<PerkAcquisition<?>> savedPerks = new LinkedList<>();

    @Override
    public void upsertPerkAcquisitionWithEmptyOrderId(PerkAcquisition<?> perkAcquisition) {
        savedPerks.add(perkAcquisition);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<PerkAcquisition<EmptyParams>> getUserPerk(
            long uid, PerkType perkType
    ) {
        PerkAcquisition perk = savedPerks.stream()
                .filter(p -> p.getUid() == uid && p.getPerkType().equals(perkType))
                .findAny().orElse(null);
        if (perk == null) {
            return Collections.emptyList();
        } else {
            return Collections.singletonList(perk);
        }
    }

    @Override
    public void clear() {
        savedPerks.clear();
    }
}
