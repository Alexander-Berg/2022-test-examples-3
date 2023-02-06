package ru.yandex.market.pers.tms.ugc.stat;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.tms.MockedPersTmsTest;

public class GradeStatsServiceTest extends MockedPersTmsTest {

    @Autowired
    private GradeStatsService gradeStatsService;

    @Test
    public void test() {
        gradeStatsService.logStats();
    }

}