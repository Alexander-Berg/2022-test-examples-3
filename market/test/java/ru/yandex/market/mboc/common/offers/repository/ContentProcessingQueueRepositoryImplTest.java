package ru.yandex.market.mboc.common.offers.repository;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import ru.yandex.market.mboc.common.contentprocessing.to.model.BusinessOfferGroupId;
import ru.yandex.market.mboc.common.contentprocessing.to.model.ContentProcessingOffer;
import ru.yandex.market.mboc.common.contentprocessing.to.repository.ContentProcessingQueueRepository;
import ru.yandex.market.mboc.common.contentprocessing.to.repository.ContentProcessingQueueRepositoryImpl;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.mboc.common.contentprocessing.to.repository.ContentProcessingQueueRepository.ContentProcessingQueueStats.SLA_HOURS;
import static ru.yandex.market.mboc.common.utils.DateTimeUtils.dateTimeNow;
import static ru.yandex.market.mboc.common.utils.DateTimeUtils.instantNow;

public class ContentProcessingQueueRepositoryImplTest extends BaseDbTestClass {
    @Autowired
    ContentProcessingQueueRepository repository;

    @Test
    public void findAllGroupsRead() {
        var now = instantNow();
        var longAgo = now.minus(Duration.ofHours(2));
        repository.insertBatch(
            new ContentProcessingOffer(1, "offer1", 1, longAgo),
            new ContentProcessingOffer(2, "offer2", 1, longAgo),
            new ContentProcessingOffer(3, "offer3", 2, longAgo),
            new ContentProcessingOffer(4, "offer4", 2, now),
            new ContentProcessingOffer(5, "offer5", 3, longAgo),
            new ContentProcessingOffer(6, "offer6", 4, longAgo),
            new ContentProcessingOffer(7, "offer7", null, longAgo),
            new ContentProcessingOffer(8, "offer8", 4, now)
        );
        jdbcTemplate.update(
            "UPDATE mbo_category.datacamp_offers_content_processing " +
                "   SET inserted = now() - INTERVAL  '6 HOURS' " +
                " WHERE group_id = 4");
        var ready = repository.findGroupsReady();
        Assertions.assertThat(ready).containsExactlyInAnyOrder(
            new BusinessOfferGroupId(1, 1),
            new BusinessOfferGroupId(2, 1),
            new BusinessOfferGroupId(3, 2),
            new BusinessOfferGroupId(5, 3),
            new BusinessOfferGroupId(6, 4),
            new BusinessOfferGroupId(8, 4)
        );
    }

    @Test
    public void findAllReadyByGroupId() {
        var now = instantNow();
        var longAgo = instantNow().minus(Duration.ofHours(7));
        repository.insertBatch(
            new ContentProcessingOffer(1, "offer1", 1, now),
            new ContentProcessingOffer(1, "offer2", 1, now),
            new ContentProcessingOffer(3, "offer3", 1, now),
            new ContentProcessingOffer(4, "offer4", 1, now),
            new ContentProcessingOffer(4, "offer5", 1, now),
            new ContentProcessingOffer(4, "offer6", null, longAgo),
            new ContentProcessingOffer(5, "offer7", null, now)
        );
        assertEquals("all offers inserted correctly", 7, repository.findAll().size());
        var offers = repository.findAllByGroupId(new BusinessOfferGroupId(1, 1));
        assertEquals(
            Set.of("offer1", "offer2"),
            offers.stream()
                .map(ContentProcessingOffer::getShopSku)
                .collect(Collectors.toSet())
        );
        assertTrue(offers.stream().allMatch(
            o -> o.getGroupId() == 1
        ));
        Set<String> offers3 = new HashSet<>();
        repository.readAllWithoutGroupId(batch -> {
                offers3.addAll(
                    batch.stream()
                        .map(ContentProcessingOffer::getShopSku)
                        .collect(Collectors.toSet())
                );
                assertTrue(batch.stream().noneMatch(o -> o.getGroupId() != null));
            },
            10, 10000);
        assertEquals(Set.of("offer6", "offer7"), offers3);
    }

    @Test
    public void deleteWithExactChangedTime() {
        var changed1 = instantNow().minus(1, ChronoUnit.HOURS);
        var changed2 = instantNow().minus(5, ChronoUnit.HOURS);
        var changed3 = instantNow().minus(1, ChronoUnit.MICROS);
        var changed4 = instantNow().plus(1, ChronoUnit.MICROS);

        var offer1 = new ContentProcessingOffer(1, "offer1", null, changed1);
        var offer2 = new ContentProcessingOffer(1, "offer2", 1, changed2);
        var offer3 = new ContentProcessingOffer(3, "offer3", null, changed3);
        var offer4 = new ContentProcessingOffer(4, "offer4", 1, changed4);
        repository.insertBatch(offer1, offer2, offer3, offer4);

        Assertions.assertThat(repository.findAll()).containsExactlyInAnyOrder(offer1, offer2, offer3, offer4);

        repository.deleteChangedBeforeByBusinessSkuKeys(
            List.of(offer3.getKey(), offer4.getKey()), changed3);
        Assertions.assertThat(repository.findAll()).containsExactlyInAnyOrder(offer1, offer2, offer4);

        repository.deleteChangedBeforeByBusinessSkuKeys(
            List.of(offer1.getKey(), offer2.getKey(), offer4.getKey()), offer4.getChanged());
        Assertions.assertThat(repository.findAll()).isEmpty();
    }

    @Test
    public void collectsStats() {
        var now = instantNow();
        var longAgo = instantNow().minus(Duration.ofHours(7));
        repository.insertBatch(
            new ContentProcessingOffer(1, "offer1", 1, now),
            new ContentProcessingOffer(1, "offer2", 1, now),
            new ContentProcessingOffer(3, "offer3", 1, now),
            new ContentProcessingOffer(4, "offer4", null, longAgo),
            new ContentProcessingOffer(5, "offer5", null, now)
        );

        var params = new MapSqlParameterSource()
            .addValue("insertedOverSla1", dateTimeNow().minus(SLA_HOURS, ChronoUnit.HOURS))
            .addValue("insertedOverSla2", dateTimeNow().minus(SLA_HOURS + 1, ChronoUnit.HOURS));
        namedParameterJdbcTemplate.update("update " + ContentProcessingQueueRepositoryImpl.TABLE_NAME
            + " set inserted = :insertedOverSla1 where business_id = 4 and shop_sku = 'offer4'", params);
        namedParameterJdbcTemplate.update("update " + ContentProcessingQueueRepositoryImpl.TABLE_NAME
            + " set inserted = :insertedOverSla2 where business_id = 5 and shop_sku = 'offer5'", params);

        var stats = repository.collectStats();

        Assertions.assertThat(stats.getInQueueCount()).isEqualTo(5);
        Assertions.assertThat(stats.getOldestInQueueSec()).isEqualTo((SLA_HOURS + 1) * 3600);
        Assertions.assertThat(stats.getOverSlaCount()).isEqualTo(2);
    }
}
