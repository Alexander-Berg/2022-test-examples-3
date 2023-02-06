package ru.yandex.direct.core.testing.repository;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ru.yandex.direct.core.entity.recommendation.model.RecommendationOnlineInfo;
import ru.yandex.direct.dbutil.QueryWithoutIndex;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jooqmapper.JooqMapperWithSupplier;

import static ru.yandex.direct.core.entity.recommendation.repository.RecommendationOnlineRepository.createMapper;
import static ru.yandex.direct.core.entity.recommendation.repository.RecommendationStatusRepository.NUMBER_OF_ROWS;
import static ru.yandex.direct.dbschema.ppc.Tables.RECOMMENDATIONS_ONLINE;

@Repository
@ParametersAreNonnullByDefault
@QueryWithoutIndex("Тестовый репозиторий")
public class TestRecommendationOnlineRepository {
    private final DslContextProvider dslContextProvider;
    private final JooqMapperWithSupplier<RecommendationOnlineInfo> mapper;

    @Autowired
    public TestRecommendationOnlineRepository(DslContextProvider dslContextProvider) {
        this.dslContextProvider = dslContextProvider;
        this.mapper = createMapper();
    }

    /**
     * Получить все рекомендации (для тестирования)
     *
     * @return {@link Collection} список рекомендаций
     */
    @Nonnull
    public Collection<RecommendationOnlineInfo> getAll(int shard) {
        return dslContextProvider.ppc(shard)
                .select(mapper.getFieldsToRead())
                .from(RECOMMENDATIONS_ONLINE)
                .fetch()
                .map(mapper::fromDb);
    }

    /**
     * Получить все рекомендации (для тестирования)
     *
     * @return {@link Collection} список рекомендаций
     */
    @Nonnull
    public Collection<RecommendationOnlineInfo> getOld(int shard, long timestamp) {
        return dslContextProvider.ppc(shard)
                .select(mapper.getFieldsToRead())
                .from(RECOMMENDATIONS_ONLINE)
                .where(RECOMMENDATIONS_ONLINE.TIMESTAMP
                        .le(timestamp))
                .limit(NUMBER_OF_ROWS)
                .fetch()
                .map(mapper::fromDb);
    }
}
