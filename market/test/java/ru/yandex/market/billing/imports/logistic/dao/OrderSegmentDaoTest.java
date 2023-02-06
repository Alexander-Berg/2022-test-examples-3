package ru.yandex.market.billing.imports.logistic.dao;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;

class OrderSegmentDaoTest {

    private static final List<String> TIME_STRINGS = List.of(
            "2022-02-03T18:55:07+03:00",
            "2022-02-03T18:55:07.3+03:00",
            "2022-02-03T18:55:07.38+03:00",
            "2022-02-03T18:55:07.383+03:00",
            "2022-02-03T18:55:07.384+03:00",
            "2022-02-03T18:55:07.3845+03:00",
            "2022-02-03T18:55:07.38456+03:00",
            "2022-02-03T18:55:07.384567+03:00"
    );

    @Test
    public void testDateTimeMappers() {
        Set<Timestamp> results = new HashSet<>();
        TIME_STRINGS.forEach(
                s -> results.add(OrderSegmentDao.toTimestamp(s))
        );
        MatcherAssert.assertThat("Number of timeStamps must be 1", results, hasSize(1));
    }

}
