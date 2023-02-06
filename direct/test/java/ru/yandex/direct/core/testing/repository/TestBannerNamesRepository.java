package ru.yandex.direct.core.testing.repository;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_NAMES;

@Repository
public class TestBannerNamesRepository {

    private final DslContextProvider dslContextProvider;

    @Autowired
    public TestBannerNamesRepository(DslContextProvider dslContextProvider) {
        this.dslContextProvider = dslContextProvider;
    }

    public Set<Long> getBannersWithName(int shard, List<Long> bannerId) {
        return dslContextProvider.ppc(shard)
                .select(BANNER_NAMES.BID)
                .from(BANNER_NAMES)
                .where(BANNER_NAMES.BID.in(bannerId))
                .fetchSet(BANNER_NAMES.BID);
    }

}
