package ru.yandex.direct.core.testing.repository;

import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ru.yandex.direct.core.entity.recommendation.model.RecommendationStatusInfo;
import ru.yandex.direct.dbutil.QueryWithoutIndex;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jooqmapper.JooqMapperWithSupplier;

import static ru.yandex.direct.core.entity.recommendation.repository.RecommendationStatusRepository.NUMBER_OF_ROWS;
import static ru.yandex.direct.core.entity.recommendation.repository.RecommendationStatusRepository.createMapper;
import static ru.yandex.direct.dbschema.ppc.Tables.RECOMMENDATIONS_STATUS;

@Repository
@ParametersAreNonnullByDefault
@QueryWithoutIndex("Тестовый репозиторий")
public class TestRecommendationStatusRepository {
    private final DslContextProvider dslContextProvider;
    private final JooqMapperWithSupplier<RecommendationStatusInfo> mapper;

    @Autowired
    public TestRecommendationStatusRepository(DslContextProvider dslContextProvider) {
        this.dslContextProvider = dslContextProvider;
        this.mapper = createMapper();
    }

    /**
     * Получить все статусы (для тестирования)
     *
     * @return {@link Set} список статусов
     */
    @Nonnull
    public Set<RecommendationStatusInfo> getAll(int shard) {
        return dslContextProvider.ppc(shard)
                .select(mapper.getFieldsToRead())
                .from(RECOMMENDATIONS_STATUS)
                .fetchSet(mapper::fromDb);
    }

    /**
     * Получить все статусы (для тестирования)
     *
     * @return {@link Set} список статусов
     */
    @Nonnull
    public Set<RecommendationStatusInfo> getOld(int shard, long timestamp) {
        return dslContextProvider.ppc(shard)
                .select(mapper.getFieldsToRead())
                .from(RECOMMENDATIONS_STATUS)
                .where(RECOMMENDATIONS_STATUS.TIMESTAMP
                        .le(timestamp))
                .limit(NUMBER_OF_ROWS)
                .fetchSet(mapper::fromDb);
    }
}
