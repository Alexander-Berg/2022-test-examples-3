package ru.yandex.direct.core.testing.repository;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_BUTTONS;

@Repository
public class TestBannerButtonsRepository {

    private final DslContextProvider dslContextProvider;

    @Autowired
    public TestBannerButtonsRepository(DslContextProvider dslContextProvider) {
        this.dslContextProvider = dslContextProvider;
    }

    public Set<Long> getBannersWithButton(int shard, List<Long> bannerId) {
        return dslContextProvider.ppc(shard)
                .select(BANNER_BUTTONS.BID)
                .from(BANNER_BUTTONS)
                .where(BANNER_BUTTONS.BID.in(bannerId))
                .fetchSet(BANNER_BUTTONS.BID);
    }

}
