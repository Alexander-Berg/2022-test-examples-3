package ru.yandex.market.loyalty.core.stub;

import ru.yandex.market.loyalty.core.dao.ydb.PersonalPromoPerksDao;

import java.util.HashSet;
import java.util.Set;


public class YdbPersonalPromoPerksStub implements PersonalPromoPerksDao, StubDao {

    private final Set<String> savedPerks = new HashSet<>();

    @Override
    public void clear() {
        savedPerks.clear();
    }

    @Override
    public Set<String> getPersonalPerks(long uid) {
        return savedPerks;
    }

    @Override
    public void upsertPersonalPerks(long uid, Set<String> personalPerks) {
        savedPerks.addAll(personalPerks);
    }
}
