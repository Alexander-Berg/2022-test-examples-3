package ru.yandex.market.adv.shop.integration.checkouter.datasync.database.repository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.shop.integration.AbstractShopIntegrationTest;
import ru.yandex.market.adv.shop.integration.checkouter.datasync.database.entity.SyncInfoEntity;
import ru.yandex.market.common.test.db.DbUnitDataSet;

@DisplayName("Тесты на репозиторий SyncInfoRepository")
class SyncInfoRepositoryTest extends AbstractShopIntegrationTest {

    @Autowired
    private SyncInfoRepository repository;

    @DisplayName("Успешно получили информацию о синхронизации по имени таблицы")
    @DbUnitDataSet(
            before = "SyncInfoRepositoryTest/csv/syncInfoFind_existTable_syncInfoEntity.before.csv"
    )
    @Test
    void syncInfoFind_existTable_syncInfoEntity() {
        Assertions.assertThat(repository.findByTableName("market_order"))
                .isPresent()
                .get()
                .isEqualTo(
                        new SyncInfoEntity(
                                "market_order",
                                LocalDateTime.parse("2022-05-23T16:00:00")
                                        .atZone(ZoneOffset.systemDefault())
                                        .toInstant(),
                                123L
                        )
                );
    }

    @DisplayName("Успешно обновили информацию о синхронизации по имени таблицы")
    @DbUnitDataSet(
            before = "SyncInfoRepositoryTest/csv/syncInfoUpsert_existTable_success.before.csv",
            after = "SyncInfoRepositoryTest/csv/syncInfoUpsert_existTable_success.after.csv"
    )
    @Test
    void syncInfoUpsert_existTable_success() {
        repository.upsert(
                "market_order",
                LocalDateTime.parse("2022-05-23T16:00:00")
                        .atZone(ZoneOffset.systemDefault())
                        .toInstant(),
                456
        );
    }
}
