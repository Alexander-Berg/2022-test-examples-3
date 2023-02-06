package ru.yandex.market.wms.common.spring.dao;

import java.util.stream.IntStream;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.enums.CounterName;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.implementation.CounterDao;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.assertj.core.api.Assertions.assertThat;

class CounterDaoTest extends IntegrationTest {

    @Autowired
    private CounterDao dao;

    @Test
    @ExpectedDatabase(value = "/db/dao/counter/expected.xml", assertionMode = NON_STRICT_UNORDERED)
    void getNextCounterValues() {
        int[] keys = dao.getNextCounterValues(CounterName.ITRN_KEY, 33).toArray();
        assertThat(keys).isEqualTo(IntStream.rangeClosed(301, 333).toArray());
    }

    @Test
    @ExpectedDatabase(value = "/db/dao/counter/expected-new.xml", assertionMode = NON_STRICT_UNORDERED)
    void getNextCounterValuesWhenCounterIsNew() {
        runInNewTx(() -> deleteCounter(CounterName.ITRN_KEY));
        int[] keys = dao.getNextCounterValues(CounterName.ITRN_KEY, 33).toArray();
        assertThat(keys).isEqualTo(IntStream.rangeClosed(1, 33).toArray());
    }

    private void deleteCounter(CounterName counterName) {
        jdbc.update("delete FROM wmwhse1.NCOUNTER WHERE KEYNAME = ?", counterName.getKey());
    }
}
