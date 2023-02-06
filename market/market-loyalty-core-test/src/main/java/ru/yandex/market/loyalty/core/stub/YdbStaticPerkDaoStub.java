package ru.yandex.market.loyalty.core.stub;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.loyalty.api.model.perk.StaticPerkStatus;
import ru.yandex.market.loyalty.core.dao.ydb.StaticPerkDao;
import ru.yandex.market.loyalty.core.dao.ydb.model.StaticPerk;

@AllArgsConstructor
public class YdbStaticPerkDaoStub implements StaticPerkDao, StubDao {
    private final Map<Pair<Long, String>, StaticPerk> table = new HashMap<>();

    private final Clock clock;

    @Override
    public void clear() {
        table.clear();
    }

    @Override
    public void upsert(long uid, String perkName, StaticPerkStatus status) {
        table.put(Pair.of(uid, perkName), new StaticPerk(uid, perkName, status, clock.instant()));
    }

    @Override
    public void updateStatus(long uid, String perkName, StaticPerkStatus status) {
        var key = Pair.of(uid, perkName);
        Optional.ofNullable(table.get(key))
                .ifPresent(p -> table.put(key, StaticPerk.builder()
                        .uid(p.getUid())
                        .perkName(p.getPerkName())
                        .status(status)
                        .build()
                ));
    }

    @Override
    public Set<StaticPerk> selectByUid(long uid) {
        return table.entrySet().stream()
                .filter(kv -> kv.getKey().getFirst() == uid)
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<StaticPerk> selectByUidAndPerkName(long uid, String perkName) {
        return Optional.ofNullable(table.get(Pair.of(uid, perkName)));
    }

    @Override
    public boolean userHasStaticPerk(long uid, String perkName) {
        return selectByUidAndPerkName(uid, perkName).isPresent();
    }

    @Override
    public boolean userHasStaticPerkWithStatus(long uid, String perkName, StaticPerkStatus status) {
        return selectByUidAndPerkName(uid, perkName)
                .map(perk -> Objects.equals(perk.getStatus(), status))
                .orElse(false);
    }

    @Override
    public Set<StaticPerk> selectByPerkNameAndStatus(String perkName, StaticPerkStatus status) {
        return table.values().stream()
                .filter(perk -> perk.getPerkName().equals(perkName) && perk.getStatus().equals(status))
                .collect(Collectors.toSet());
    }

    public Map<Pair<Long, String>, StaticPerk> getTable() {
        return table;
    }
}
