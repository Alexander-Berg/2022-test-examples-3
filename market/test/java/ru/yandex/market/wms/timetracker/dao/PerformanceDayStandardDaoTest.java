package ru.yandex.market.wms.timetracker.dao;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        PerformanceDayStandardDao.class
})
class PerformanceDayStandardDaoTest {

    @Autowired
    private PerformanceDayStandardDao performanceDayStandardDao;

    @Test
    void dayGoalsWhenSof() {
        final double result = performanceDayStandardDao.dayGoals("sof");
        Assertions.assertEquals(4048, result);
    }

    @Test
    void dayGoalsWhenRST() {
        final double result = performanceDayStandardDao.dayGoals("rst");
        Assertions.assertEquals(7700, result);
    }

    @Test
    void dayGoalsWhenDefault() {
        final double result = performanceDayStandardDao.dayGoals("def");
        Assertions.assertEquals(4048, result);
    }
}
