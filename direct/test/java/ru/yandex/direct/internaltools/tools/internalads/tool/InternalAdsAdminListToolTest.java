package ru.yandex.direct.internaltools.tools.internalads.tool;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.tools.internalads.model.InternalAdsBlockUserParameter;
import ru.yandex.direct.internaltools.tools.internalads.model.InternalToolsInternalAdsPrivilegedUser;
import ru.yandex.direct.rbac.RbacRole;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class InternalAdsAdminListToolTest {
    private static final RbacRole RBAC_ROLE = RbacRole.INTERNAL_AD_ADMIN;

    @Autowired
    private Steps steps;

    @Autowired
    private InternalAdsBlockUserTool tool;

    @Autowired
    private UserService userService;

    private ClientInfo clientInfo;
    private User user;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClientWithRole(RBAC_ROLE);
        user = userService.getUser(clientInfo.getUid());
    }

    @Test
    public void testGetMassData() {
        List<InternalToolsInternalAdsPrivilegedUser> toolResult = tool.getMassData(new InternalAdsBlockUserParameter()
                .withClientId(clientInfo.getClientId().asLong()));
        assertThat(toolResult).isEqualTo(singletonList(
                new InternalToolsInternalAdsPrivilegedUser()
                        .withClientId(clientInfo.getClientId().asLong())
                        .withLogin(user.getLogin())
                        .withFio(user.getFio())
                        .withBlocked(user.getStatusBlocked())));
    }
}
