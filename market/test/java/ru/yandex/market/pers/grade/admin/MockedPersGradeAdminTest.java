package ru.yandex.market.pers.grade.admin;

import java.util.Arrays;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.common.framework.user.UserInfoService;
import ru.yandex.market.pers.grade.core.MockedTest;
import ru.yandex.market.pers.security.PersUser;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Import(PersGradeAdminMockConfig.class)
@TestPropertySource({
    "classpath:/test-application.properties",
    "classpath:/test-custom-application.properties"
})
public abstract class MockedPersGradeAdminTest extends MockedTest {
    public static final long FAKE_MODERATOR_ID = 5437653;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("blackBoxUserService")
    private UserInfoService blackBoxUserService;

    protected MockMvc mvc;



    @Before
    public synchronized void initMocks() {
        super.initMocks();
        PersGradeAdminMockConfig.initBlackBoxUserServiceMock(blackBoxUserService);

        mvc = MockMvcBuilders
            .webAppContextSetup(context)
            .build();
    }

    @Before
    public void intSecurityContext() {
        SecurityContext context = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);

        when(context.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(
            new PersUser("login", FAKE_MODERATOR_ID, Arrays.asList("ROLE_ADMINISTRATOR")));

        SecurityContextHolder.setContext(context);
    }

}
