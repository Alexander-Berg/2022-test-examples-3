package ru.yandex.market.wms.auth.dao;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import ru.yandex.market.wms.auth.config.AuthIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MobileUserDaoTest extends AuthIntegrationTest {

    @Autowired
    private MobileUserDao mobileUserDao;

    @Test
    @DatabaseSetup(value = "/db/dao/mobile-user/insert/before.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(
            value = "/db/dao/mobile-user/insert/after.xml",
            connection = "enterpriseConnection",
            assertionMode = NON_STRICT
    )
    public void insert() {
        mobileUserDao.create("user1", "REC_OTGR_MULTI_NEW");
    }

    @Test
    @DatabaseSetup(value = "/db/dao/mobile-user/insert/before.xml", connection = "enterpriseConnection")
    public void insertDuplicate() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> mobileUserDao.create("user2", "REC_OTGR_MULTI_NEW"),
                "Expected duplicate key exception, but it didn't"
        );
    }

    @Test
    @DatabaseSetup(value = "/db/dao/mobile-user/insert/before.xml", connection = "enterpriseConnection")
    public void insertProfileNotExists() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> mobileUserDao.create("user1", "NOT-EXISTENT"),
                "Expected foreign key exception, but it didn't"
        );
    }

    @Test
    @DatabaseSetup(value = "/db/dao/mobile-user/delete/before.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(
            value = "/db/dao/mobile-user/delete/after.xml",
            connection = "enterpriseConnection",
            assertionMode = NON_STRICT
    )
    public void delete() {
        mobileUserDao.delete("user2");
    }

    @Test
    @DatabaseSetup(value = "/db/dao/mobile-user/delete/before.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(
            value = "/db/dao/mobile-user/delete/before.xml",
            connection = "enterpriseConnection",
            assertionMode = NON_STRICT
    )
    public void deleteNonExistent() {
        mobileUserDao.delete("user3");
    }
}
