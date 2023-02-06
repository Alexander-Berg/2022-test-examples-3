package ru.yandex.direct.core.testing.repository;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.BIDS_BASE;

public class TestBidsRepository {
    private DslContextProvider dslContextProvider;

    @Autowired
    public TestBidsRepository(DslContextProvider dslContextProvider) {
        this.dslContextProvider = dslContextProvider;
    }

    /**
     * Удаляет условие показа по ID
     */
    public void deleteBid(int shard, Long id) {
        dslContextProvider.ppc(shard)
                .delete(BIDS_BASE)
                .where(BIDS_BASE.BID_ID.eq(id))
                .execute();
    }

}
