package ru.yandex.market.vendors.analytics.core.utils.lock;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.vendors.analytics.core.FunctionalTest;
import ru.yandex.market.vendors.analytics.core.jpa.entity.lock.LockEntity;
import ru.yandex.market.vendors.analytics.core.jpa.repository.LockRepository;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author antipov93.
 * @date 11.06.19.
 */
public class AnalyticsLockTest extends FunctionalTest {

    @Autowired
    private LockRepository lockRepository;

    @Test
    @DisplayName("Значения из енума и из csv-файла совпадают")
    void allEnumsHasEntryInDB() {
        Set<AnalyticsLock> locksNamesFormEnum = Stream.of(AnalyticsLock.values()).collect(toSet());
        Set<AnalyticsLock> locksNamesFormCsv = lockRepository.findAll().stream()
                .map(LockEntity::getName)
                .collect(toSet());
        assertEquals(locksNamesFormEnum, locksNamesFormCsv);
    }
}
