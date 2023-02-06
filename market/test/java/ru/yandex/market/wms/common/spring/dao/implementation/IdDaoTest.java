package ru.yandex.market.wms.common.spring.dao.implementation;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

class IdDaoTest extends IntegrationTest {

    @Autowired
    private IdDao idDao;

    @Test
    @DatabaseSetup("/db/dao/id/before.xml")
    @ExpectedDatabase(value = "/db/dao/id/after.xml", assertionMode = NON_STRICT)
    void updateIsFake() {
        idDao.updateIsFake(List.of("PLT123"), false);
    }

}
