package ru.yandex.market.abo.core.autoorder.callcenter.model;

import javax.persistence.OptimisticLockException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.autoorder.callcenter.repo.AutoOrderPhoneRepo;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author komarovns
 * @date 19.02.2020
 */
class AutoOrderCallCenterOptimisticLockTest extends EmptyTest {
    private static final String PHONE_NUM = "";

    @Autowired
    AutoOrderPhoneRepo autoOrderPhoneRepo;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    @Qualifier("jpaPgTransactionManager")
    PlatformTransactionManager transactionManager;

    @AfterEach
    public void tearDown() {
        entityManager.clear();
        autoOrderPhoneRepo.deleteAll();
        flushAndCommit();
    }

    @Test
    public void modificationTimeTest() {
        var entity = new AutoOrderPhone(PHONE_NUM, AutoOrderPhoneStatus.FREE, AutoOrderPhoneBanStatus.NOT_BANNED);
        autoOrderPhoneRepo.save(entity);
        flushAndCommit();
        var saveTime = entity.getModificationTime();

        entity = autoOrderPhoneRepo.findByIdOrNull(PHONE_NUM);
        entity.setStatus(AutoOrderPhoneStatus.BOUND);
        autoOrderPhoneRepo.save(entity);
        flushAndCommit();
        var modificationTime = entity.getModificationTime();
        assertTrue(modificationTime.isAfter(saveTime));
    }

    @Test
    public void optimisticLockTest() {
        var transactionTemplate = new TransactionTemplate(
                transactionManager,
                new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW)
        );

        var newEntity = new AutoOrderPhone(PHONE_NUM, AutoOrderPhoneStatus.FREE, AutoOrderPhoneBanStatus.NOT_BANNED);
        autoOrderPhoneRepo.save(newEntity);
        flushAndCommit();

        var entity = autoOrderPhoneRepo.findByIdOrNull(PHONE_NUM);

        transactionTemplate.executeWithoutResult(__ -> {
            var sameEntity = autoOrderPhoneRepo.findByIdOrNull(PHONE_NUM);
            sameEntity.setStatus(AutoOrderPhoneStatus.BOUND);
            autoOrderPhoneRepo.save(sameEntity);
            flushAndCommit();
        });

        entity.setStatus(AutoOrderPhoneStatus.BOUND);
        assertThrows(OptimisticLockException.class, () -> {
            autoOrderPhoneRepo.save(entity);
            flushAndCommit();
        });
    }

    private void flushAndCommit() {
        flushAndClear();
        jdbcTemplate.update("COMMIT");
    }
}
