package ru.yandex.market.pers.notify.comparison;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         22.11.16
 */
public class MemCachedComparisonServiceTest extends ComparisonServiceTest {
    @Autowired
    @Qualifier("memCachedComparisonService")
    private void setComparisonService(ComparisonService comparisonService) {
        this.comparisonService = comparisonService;
    }
}
