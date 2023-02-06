package ru.yandex.market.wms.common.spring.dao;

import java.time.ZoneId;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.implementation.UserDAO;
import ru.yandex.market.wms.common.spring.dto.UserInfoDto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DatabaseSetup(
        value = {"/db/dao/e-sso-user/data.xml", "/db/dao/e-user/data.xml", "/db/dao/user-pref-instance/timezones.xml"},
        connection = "scprdd1DboConnection")
public class UserDaoTest extends IntegrationTest {

    @Autowired
    private UserDAO userDao;

    @Test
    public void getUserInfo() {
        UserInfoDto dto = userDao.getUserInfo("ad3");
        assertions.assertThat(dto.getFullName()).isEqualTo("A D 3");
    }

    @Test
    public void getUserInfoNotExisting() {
        UserInfoDto dto = userDao.getUserInfo("some not exisiting");
        assertions.assertThat(dto.getFullName()).isEqualTo(null);
    }

    @Test
    public void getUserInfoNullLoginId() {
        UserInfoDto dto = userDao.getUserInfo(null);
        assertions.assertThat(dto.getFullName()).isEqualTo(null);
    }

    @Test
    public void getUserTimezone() {
        Optional<String> maybeTimezone = userDao.getUserTimezone("ad1");
        assertTrue(maybeTimezone.isPresent());
        assertNotNull(ZoneId.of(maybeTimezone.get()));
    }

    @Test
    public void getUserTimezoneTwoRows() {
        Optional<String> maybeTimezone = userDao.getUserTimezone("ad2");
        assertTrue(maybeTimezone.isPresent());
    }

    @Test
    public void getUserTimezoneNotFound() {
        Optional<String> maybeTimezone = userDao.getUserTimezone("ad3");
        assertFalse(maybeTimezone.isPresent());
    }

}
