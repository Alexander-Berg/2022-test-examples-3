package ru.yandex.direct.core.testing.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.yandex.direct.dbutil.QueryWithoutIndex;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.CONTENT_PROMOTION;

@Component
public class TestContentPromotionRepository {

    private final DslContextProvider dslContextProvider;

    @Autowired
    public TestContentPromotionRepository(DslContextProvider dslContextProvider) {
        this.dslContextProvider = dslContextProvider;
    }

    @QueryWithoutIndex("Чистка таблицы в юниттестах")
    public void removeAllContentPromotions(int shard) {
        dslContextProvider.ppc(shard)
                .deleteFrom(CONTENT_PROMOTION)
                .execute();
    }
}
