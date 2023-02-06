package ru.yandex.autotests.market.billing.backend.core.dao.shops_web.repository;

import java.time.LocalDateTime;

import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import ru.yandex.autotests.market.billing.backend.core.dao.entities.feed.FeedLogWithSessionCompositeKey;
import ru.yandex.autotests.market.billing.backend.core.dao.entities.feed.FeedLogWithSessionEntity;
import ru.yandex.autotests.market.indexer.backend.core.dao.entities.generation.IndexerType;

public interface TestFeedLogAndDeltaRawRepository extends Repository<FeedLogWithSessionEntity, FeedLogWithSessionCompositeKey>,
        BaseFeedLogRepository {

    default void generateSample(@Param("leftBound") LocalDateTime leftBound,
                                @Param("rightBound") LocalDateTime rightBound,
                                @Param("samplesCount") int samplesCount) {
        generateSample(leftBound, rightBound, IndexerType.PLANESHIFT.getId(), samplesCount);
    }
}
