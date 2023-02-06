package ru.yandex.market.wms.common.spring.dao.implementation;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.DropToCarrier;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;

class DropToCarrierDaoTest extends IntegrationTest {

    private static final String USER = "TEST";

    @Autowired
    protected DropToCarrierDao dropToCarrierDao;

    @Test
    @DatabaseSetup(value = "/db/dao/droptocarrier/data.xml")
    @ExpectedDatabase(value = "/db/dao/droptocarrier/after-delete-insert.xml", assertionMode = NON_STRICT_UNORDERED)
    void selectDeleteThenInsert() {
        List<DropToCarrier> dropToCarriers = dropToCarrierDao.findByCarrierCode("107");
        assertThat(dropToCarriers, Matchers.not(emptyIterable()));
        assertThat(
                dropToCarriers,
                Matchers.hasSize(2));
        dropToCarriers.forEach(it -> dropToCarrierDao.deleteDropToCarrierRecord(it.getDropId()));
        dropToCarrierDao.insert("PLT188", "112", USER);
    }
}
