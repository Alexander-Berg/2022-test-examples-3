package ru.yandex.direct.core.testing.repository;


import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.addition.callout.model.Callout;
import ru.yandex.direct.core.entity.addition.callout.model.CalloutDeleted;
import ru.yandex.direct.core.entity.addition.callout.model.CalloutsStatusModerate;
import ru.yandex.direct.core.entity.addition.callout.repository.CalloutRepository;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.ADDITIONS_ITEM_CALLOUTS;
import static ru.yandex.direct.utils.FunctionalUtils.filterList;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@ParametersAreNonnullByDefault
public class TestCalloutRepository extends CalloutRepository {

    private final DslContextProvider dslContextProvider;

    @Autowired
    public TestCalloutRepository(ShardHelper shardHelper, DslContextProvider dslContextProvider) {
        super(shardHelper, dslContextProvider);
        this.dslContextProvider = dslContextProvider;
    }

    /**
     * Обновить статус модерации для кампаний
     *
     * @param shard          Шард
     * @param calloutIds     Список ID уточнений
     * @param statusModerate Новый статус
     */
    public void updateStatusModerate(int shard, Collection<Long> calloutIds, CalloutsStatusModerate statusModerate) {
        if (calloutIds.isEmpty()) {
            return;
        }
        dslContextProvider.ppc(shard)
                .update(ADDITIONS_ITEM_CALLOUTS)
                .set(ADDITIONS_ITEM_CALLOUTS.STATUS_MODERATE, CalloutsStatusModerate.toSource(statusModerate))
                .set(ADDITIONS_ITEM_CALLOUTS.LAST_CHANGE, ADDITIONS_ITEM_CALLOUTS.LAST_CHANGE)
                .where(ADDITIONS_ITEM_CALLOUTS.ADDITIONS_ITEM_ID.in(calloutIds))
                .execute();
    }

    public Map<Pair<Long, BigInteger>, CalloutDeleted> getExistingCalloutsMap(int shard, List<Callout> callouts) {
        callouts.forEach(callout -> callout.withHash(calcHash(callout)));

        Map<Pair<Long, BigInteger>, CalloutDeleted> existCalloutsMap =
                getExistingCallouts(shard, callouts);
        callouts.forEach(callout -> {
            CalloutDeleted existCallout = existCalloutsMap.get(Pair.of(callout.getClientId(), callout.getHash()));
            callout.withId(existCallout != null ? existCallout.getId() : null);
        });

        return existCalloutsMap;
    }

    public int insertCallouts(int shard, List<Callout> callouts, Map<Pair<Long, BigInteger>, CalloutDeleted> existCalloutsMap) {
        List<Callout> newCallouts = filterList(callouts, sl -> sl.getId() == null);

        Set<Long> existCalloutIdsDeleted = callouts.stream()
                .filter(callout -> callout.getId() != null
                        && existCalloutsMap.get(Pair.of(callout.getClientId(), callout.getHash())).getDeleted())
                .map(Callout::getId)
                .collect(Collectors.toSet());

        setDeleted(shard, existCalloutIdsDeleted, Boolean.FALSE);

        return addToAdditionItemCalloutsTable(shard, newCallouts);
    }

    public List<Long> getCalloutIds(int shard, int inserted, List<Callout> callouts, List<Callout> newCallouts) {
        if (inserted == newCallouts.size()) {
            return mapList(callouts, Callout::getId);
        } else {
            return mapList(getExistingCallouts(shard, callouts).values(), CalloutDeleted::getId);
        }
    }
}
