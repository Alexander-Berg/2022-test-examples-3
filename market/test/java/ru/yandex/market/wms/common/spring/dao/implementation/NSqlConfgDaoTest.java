package ru.yandex.market.wms.common.spring.dao.implementation;

import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider;
import ru.yandex.market.wms.shared.libs.configproperties.dao.NSqlConfigDao;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class NSqlConfgDaoTest extends IntegrationTest {

    @MockBean
    @Autowired
    SecurityDataProvider userProvider;

    @Autowired
    NSqlConfigDao nSqlConfigDao;

    @Test
    @DatabaseSetup(value = "/db/dao/nsqlconfig/without-user/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/dao/nsqlconfig/without-user/after.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void updateValuesByKeysWithoutUserInSecurityContext() {
        Mockito.when(userProvider.getUser())
                .thenThrow(new IllegalStateException("There is no user in security context"));
        nSqlConfigDao.updateValuesByKeys(Map.of("TEST", "2"));
    }

    @Test
    @DatabaseSetup(value = "/db/dao/nsqlconfig/with-user/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/dao/nsqlconfig/with-user/after.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void updateValuesByKeysWithUserInSecurityContext() {
        Mockito.when(userProvider.getUser())
                .thenReturn("NEW_TEST_USER");
        nSqlConfigDao.updateValuesByKeys(Map.of("TEST", "2"));
    }

    @Test
    @DatabaseSetup(value = "/db/dao/nsqlconfig/with-null-user/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/dao/nsqlconfig/with-null-user/after.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void updateValuesByKeysWithNullUserInSecurityContext() {
        Mockito.when(userProvider.getUser())
                .thenReturn(null);
        nSqlConfigDao.updateValuesByKeys(Map.of("TEST", "2"));
    }
}
