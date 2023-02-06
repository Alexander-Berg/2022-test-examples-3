package ru.yandex.direct.core.testing.repository;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_LOGOS;

@Repository
public class TestBannerLogosRepository {

    private final DslContextProvider dslContextProvider;

    @Autowired
    public TestBannerLogosRepository(DslContextProvider dslContextProvider) {
        this.dslContextProvider = dslContextProvider;
    }

    public Set<Long> getBannersWithLogo(int shard, List<Long> bannerId) {
        return dslContextProvider.ppc(shard)
                .select(BANNER_LOGOS.BID)
                .from(BANNER_LOGOS)
                .where(BANNER_LOGOS.BID.in(bannerId))
                .fetchSet(BANNER_LOGOS.BID);
    }

}
