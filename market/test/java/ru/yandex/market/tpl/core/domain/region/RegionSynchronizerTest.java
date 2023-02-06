package ru.yandex.market.tpl.core.domain.region;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.core.CoreTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author kukabara
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CoreTest
class RegionSynchronizerTest {

    private final RegionSynchronizer regionSynchronizer;
    private final RegionDao regionDao;

    @Test
    void synchronize() {
        regionSynchronizer.updateRegions();
        int regionsCountInDb = regionDao.getRegionsCountInDb();
        assertThat(regionsCountInDb).isGreaterThan(0);
    }
}
