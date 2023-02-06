package ru.yandex.direct.core.testing.repository;

import java.util.Collection;

import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.tables.Deals.DEALS;
import static ru.yandex.direct.dbschema.ppcdict.tables.DealsAdfox.DEALS_ADFOX;

public class TestDealRepository {
    private final DslContextProvider dslContextProvider;

    public TestDealRepository(DslContextProvider dslContextProvider) {
        this.dslContextProvider = dslContextProvider;
    }

    /**
     * Удаление сделок из {@code ppcdict}
     */
    public int deleteDealsPpcDict(Collection<Long> dealIds) {
        return dslContextProvider.ppcdict()
                .deleteFrom(DEALS_ADFOX)
                .where(DEALS_ADFOX.DEAL_ID.in(dealIds))
                .execute();
    }

    /**
     * Удаление сделок из {@code ppc}
     */
    public int deleteDealsPpc(int shard, Collection<Long> dealIds) {
        return dslContextProvider.ppc(shard)
                .deleteFrom(DEALS)
                .where(DEALS.DEAL_ID.in(dealIds))
                .execute();
    }

}
