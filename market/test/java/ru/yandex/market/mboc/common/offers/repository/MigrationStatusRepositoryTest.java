package ru.yandex.market.mboc.common.offers.repository;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MigrationStatus;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

public class MigrationStatusRepositoryTest extends BaseDbTestClass {

    @Autowired
    private MigrationStatusRepository migrationStatusRepository;

    @Test
    public void getActiveMigrationsByTargetNullTest() {
        MigrationStatus migrationStatus = migrationStatusRepository.getNotFinishedMigrationByTarget(123);
        Assertions.assertThat(migrationStatus).isNull();
    }
}
