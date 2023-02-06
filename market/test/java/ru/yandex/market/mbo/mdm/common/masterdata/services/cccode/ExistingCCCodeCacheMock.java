package ru.yandex.market.mbo.mdm.common.masterdata.services.cccode;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExistingCCCodeCacheMock implements ExistingCCCodeCache {

    private static final HashSet<String> set = new HashSet<>();

    @Override
    public Set<String> get() {
        return set;
    }

    public void add(String... values) {
        set.addAll(List.of(values));
    }

    public void addAll(Collection<String> values) {
        set.addAll(values);
    }

    public void deleteAll() {
        set.clear();
    }

    @Override
    public void invalidate() {
        deleteAll();
    }
}
