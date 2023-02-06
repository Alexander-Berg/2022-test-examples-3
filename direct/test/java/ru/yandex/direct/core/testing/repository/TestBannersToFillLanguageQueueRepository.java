package ru.yandex.direct.core.testing.repository;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.stereotype.Repository;

import ru.yandex.direct.core.entity.banner.repository.BannersToFillLanguageQueueRepository;
import ru.yandex.direct.dbutil.QueryWithoutIndex;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS_TO_FILL_LANGUAGE_QUEUE;

/**
 * Тестовый репозиторий для работы с таблицей ppc.banners_to_fill_language_queue
 */
@Repository
@ParametersAreNonnullByDefault
public class TestBannersToFillLanguageQueueRepository extends BannersToFillLanguageQueueRepository {

    private final DslContextProvider dslContextProvider;

    public TestBannersToFillLanguageQueueRepository(DslContextProvider dslContextProvider) {
        super(dslContextProvider);

        this.dslContextProvider = dslContextProvider;
    }

    /**
     * Возвращает количество объектов в очереди.
     *
     * @param shard шард
     * @return количество объектов в очереди
     */
    @QueryWithoutIndex("Подсчёт числа строк для тестов")
    public int countItems(int shard) {
        return dslContextProvider.ppc(shard)
                .selectCount()
                .from(BANNERS_TO_FILL_LANGUAGE_QUEUE)
                .fetch()
                .get(0)
                .getValue(0, Integer.class);
    }
}
