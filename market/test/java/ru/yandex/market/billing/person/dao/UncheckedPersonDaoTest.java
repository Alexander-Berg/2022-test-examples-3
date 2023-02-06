package ru.yandex.market.billing.person.dao;

import java.time.LocalDateTime;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
class UncheckedPersonDaoTest extends FunctionalTest {

    @Autowired
    private UncheckedPersonDao uncheckedPersonDao;

    @Test
    @DbUnitDataSet(before = "UncheckedPersonDao.getUncheckedPersons.csv")
    void getUncheckedPersons() {
        var uncheckedPersons = uncheckedPersonDao.getUncheckedPersons();

        assertThat(uncheckedPersons).containsExactly(
                Map.entry(1L, LocalDateTime.parse("2021-10-20T21:19:00")),
                Map.entry(3L, LocalDateTime.parse("2021-10-20T22:33:00"))
        );
    }
}
