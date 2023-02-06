package ru.yandex.market.mbo.mdm.common.masterdata.repository;

import java.time.Instant;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.BusinessLockKey;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.BusinessLockStatus;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;

public class BusinessLockStatusRepositoryImplTest
    extends MdmGenericMapperRepositoryTestBase<BusinessLockStatusRepository, BusinessLockStatus, BusinessLockKey> {

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    TransactionTemplate transactionTemplate;
    @Autowired
    BusinessLockStatusRepository repository;

    @Override
    @Before
    public void setup() {
        random = TestDataUtils.defaultRandom(12312);
        repository = new BusinessLockStatusRepositoryImpl(jdbcTemplate, transactionTemplate);
    }

    @Override
    protected BusinessLockStatus randomRecord() {
        BusinessLockStatus obj = random.nextObject(BusinessLockStatus.class);
        return obj;
    }

    @Override
    protected Function<BusinessLockStatus, BusinessLockKey> getIdSupplier() {
        return BusinessLockStatus::getKey;
    }

    @Override
    protected String[] getFieldsToIgnore() {
        return new String[]{};
    }

    @Override
    protected List<BiConsumer<Integer, BusinessLockStatus>> getUpdaters() {
        return List.of(
            (i, record) -> {
                record.setUpdatedTs(Instant.now());
            },
            (i, record) -> {
                record.setStatus(BusinessLockStatus.Status.LOCKED);
                record.setUpdatedTs(Instant.now());
            }
        );
    }

    @Test
    public void checkLockingById() {
        BusinessLockKey key1 = new BusinessLockKey(1L, 2L);
        repository.lockBusiness(key1);

        List<BusinessLockStatus> result = repository.findLocked(List.of(key1));
        Assertions.assertThat(result.get(0).getStatus()).isEqualTo(BusinessLockStatus.Status.LOCKED);
    }

    @Test
    public void checkUnlockingById() {
        BusinessLockKey key1 = new BusinessLockKey(1L, 2L);
        repository.lockBusiness(key1);
        repository.unlockBusiness(key1);

        List<BusinessLockStatus> result = repository.findUnlocked(List.of(key1));
        Assertions.assertThat(result.get(0).getStatus()).isEqualTo(BusinessLockStatus.Status.UNLOCKED);
    }

    @Test
    public void whenFindAllLockedShouldReturnCorrectRows() {
        var row1 = random.nextObject(BusinessLockStatus.class);
        var row2 = random.nextObject(BusinessLockStatus.class);
        var row3 = random.nextObject(BusinessLockStatus.class);
        row1.setStatus(BusinessLockStatus.Status.LOCKED);
        row2.setStatus(BusinessLockStatus.Status.LOCKED);
        row3.setStatus(BusinessLockStatus.Status.UNLOCKED);
        repository.insertOrUpdateAll(List.of(row1, row2, row3));

        var result = repository.findAllLocked();
        Assertions.assertThat(result).hasSize(2);
        Assertions.assertThat(result).containsExactlyInAnyOrder(row1, row2);
    }
 }
