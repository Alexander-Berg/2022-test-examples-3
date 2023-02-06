package ru.yandex.market.mbo.user;

import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.yandex.market.mbo.db.UserAgentService;
import ru.yandex.market.mbo.db.pg.BasePgTestClass;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class UserAgentServiceTest extends BasePgTestClass {

    private static final String USER_AGENT = "testUserAgent";
    private static final String EMPTY_USER_AGENT = "";
    private static final String TEST_DESCRIPTION = "Test description";

    private UserAgentService userAgentService;

    @Inject
    private DataSource siteCatalogPgDb;

    @Before
    public void setUp() throws Exception {
        userAgentService = new UserAgentService(new NamedParameterJdbcTemplate(new JdbcTemplate(siteCatalogPgDb)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addEmptyUserAgent() {
        userAgentService.addUserAgent(EMPTY_USER_AGENT);
    }

    @Test
    public void testGetAllUserAgentsEmpty() {
        Map<String, Boolean> userAgentMap = userAgentService.getAllowedUserAgentMap();
        assertTrue(userAgentMap.isEmpty());
    }

    @Test
    public void testGetAllUserAgents() {
        userAgentService.addUserAgent(USER_AGENT);
        Map<String, Boolean> userAgentMap = userAgentService.getAllowedUserAgentMap();
        assertThat(userAgentMap).hasSize(1);
        assertThat(userAgentMap.get(USER_AGENT)).isEqualTo(true);
    }

    @Test
    public void addAndCheckAllowedUserAgent() {
        userAgentService.addUserAgent(USER_AGENT, TEST_DESCRIPTION);
        assertTrue(userAgentService.isAllowed(USER_AGENT));
    }

    @Test
    public void addAndCheckNotAllowedUserAgent() {
        userAgentService.addUserAgent(USER_AGENT, false);
        assertFalse(userAgentService.isAllowed(USER_AGENT));
    }

    @Test
    public void addAndCheckNotAllowedUnknownUserAgent() {
        assertFalse(userAgentService.isAllowed(USER_AGENT));
    }

    @Test
    public void addUserAgentChangeAllowedAndCheck() {
        userAgentService.addUserAgent(USER_AGENT);
        assertTrue(userAgentService.isAllowed(USER_AGENT));
        userAgentService.setAllowed(USER_AGENT, false);
        assertFalse(userAgentService.isAllowed(USER_AGENT));
    }
}
