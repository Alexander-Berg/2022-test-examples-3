package ru.yandex.market.wms.common.spring.dao;

import java.math.BigDecimal;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.wms.common.pojo.Dimensions;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.implementation.PackDaoImpl;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class PacksDaoImplTest extends IntegrationTest {

    @Autowired
    private PackDaoImpl packDao;

    @Autowired
    @Qualifier("enterprisePackDao")
    private PackDaoImpl enterprisePackDao;

    @Test
    @DatabaseSetup(value = "/db/dao/pack/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/db/dao/pack/before.xml", assertionMode = NON_STRICT, connection = "wmwhseConnection")
    public void checkExistenceWhenExists() {
        boolean exists = packDao.checkExistence("PACK");
        assertions.assertThat(exists).isTrue();
    }

    @Test
    @DatabaseSetup(value = "/db/dao/pack/before.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/db/dao/pack/before.xml", assertionMode = NON_STRICT,
            connection = "enterpriseConnection")
    public void checkExistenceWhenNotExists() {
        boolean exists = enterprisePackDao.checkExistence("PACK10");
        assertions.assertThat(exists).isFalse();
    }

    @Test
    @DatabaseSetup(value = "/db/dao/pack/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/db/dao/pack/after-copy.xml",
            assertionMode = NON_STRICT_UNORDERED, connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/pack/before.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/db/dao/pack/after-copy.xml",
            assertionMode = NON_STRICT_UNORDERED, connection = "enterpriseConnection")
    public void copyFromStdPack() {
        Dimensions dimensions = new Dimensions.DimensionsBuilder()
                .length(BigDecimal.valueOf(5.82))
                .width(BigDecimal.valueOf(3.16))
                .height(BigDecimal.valueOf(2.11))
                .build();
        packDao.copyFromStdPack("PACK3", dimensions);
        enterprisePackDao.copyFromStdPack("PACK3", dimensions);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/pack/before.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/db/dao/pack/after-update.xml", assertionMode = NON_STRICT,
            connection = "enterpriseConnection")
    public void updatePack() {
        Dimensions dimensions = new Dimensions.DimensionsBuilder()
                .length(BigDecimal.valueOf(5.82))
                .width(BigDecimal.valueOf(3.16))
                .height(BigDecimal.valueOf(2.11))
                .build();
        enterprisePackDao.updatePack("PACK", dimensions, "user");
    }
}
