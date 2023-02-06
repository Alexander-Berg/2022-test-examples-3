package ru.yandex.market.pers.qa.admin;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.pers.qa.PersQACoreTest;
import ru.yandex.market.pers.qa.config.CoreConfig;
import ru.yandex.market.pers.security.PersUser;
import ru.yandex.market.pers.test.common.PersTestMocksHolder;

import static ru.yandex.market.pers.qa.admin.security.PersUserRole.ROLE_SUPERUSER;
import static ru.yandex.market.pers.qa.admin.security.PersUserRole.ROLE_VIEWER;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    AdminMockConfiguration.class,
    CoreConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource("classpath:/test-application.properties")
public class PersQaAdminTest extends PersQACoreTest {
    @Override
    protected void resetMocks() {
        PersTestMocksHolder.resetMocks();
    }

    @BeforeEach
    public void initSecurity() {
        SecurityContextHolder.getContext().setAuthentication(
            new AnonymousAuthenticationToken("GUEST",new PersUser("login", 123L, List.of()),
                AuthorityUtils.createAuthorityList(ROLE_SUPERUSER.getRole(), ROLE_VIEWER.getRole())));
    }
}
