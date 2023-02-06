package ru.yandex.market.wms.common.spring.service.unit;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.service.UserService;

@DatabaseSetup(value = "/db/dao/e-sso-user/data.xml", connection = "scprdd1DboConnection")
public class UserServiceTest extends IntegrationTest {

    @Autowired
    UserService userService;

    @Test
    public void getUserInfo() {
        assertions.assertThat(userService.getUserInfo("ad3").getFullName()).isEqualTo("A D 3");
    }

    @Test
    public void getUserInfoByNull() {
        assertions.assertThat(userService.getUserInfo(null).getFullName()).isEqualTo(null);
    }
}
