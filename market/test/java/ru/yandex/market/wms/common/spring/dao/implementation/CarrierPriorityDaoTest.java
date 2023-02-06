package ru.yandex.market.wms.common.spring.dao.implementation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.CarrierPriority;

class CarrierPriorityDaoTest extends IntegrationTest {

    @Autowired
    private CarrierPriorityDao carrierPriorityDao;

    @Test
    @DatabaseSetup("/db/dao/carrier-priority/find/1.xml")
    void findEmpty() {
        Optional<CarrierPriority> result = carrierPriorityDao.find("CARRIER");
        assertions.assertThat(result).isEmpty();
    }

    @Test
    @DatabaseSetup("/db/dao/carrier-priority/find/2.xml")
    void findExists() {
        String carrier = "CARRIER";
        Optional<CarrierPriority> result = carrierPriorityDao.find(carrier);
        assertions.assertThat(result).isPresent();
        assertions.assertThat(result.get().getPriority()).isEqualTo(2);
        assertions.assertThat(result.get().getCarrierCode()).isEqualTo(carrier);
    }

    @Test
    @DatabaseSetup("/db/dao/carrier-priority/insert/before.xml")
    @ExpectedDatabase(value = "/db/dao/carrier-priority/insert/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void insert() {
        String carrier = "CARRIER";
        carrierPriorityDao.insert(carrier, 2, "TEST");
    }

    @Test
    @DatabaseSetup("/db/dao/carrier-priority/find-batch/1.xml")
    void findBatchEmpty() {
        Map<String, Integer> result = carrierPriorityDao.findByCarrierCodes(List.of("CARRIER"));
        assertions.assertThat(result).isEmpty();
    }

    @Test
    @DatabaseSetup("/db/dao/carrier-priority/find-batch/2.xml")
    void findBatchExists() {
        String carrier = "CARRIER";
        Map<String, Integer> result = carrierPriorityDao.findByCarrierCodes(List.of(carrier));
        assertions.assertThat(result.size()).isOne();
        assertions.assertThat(result.get(carrier)).isEqualTo(2);
    }
}
