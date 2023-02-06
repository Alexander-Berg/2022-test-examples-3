package ru.yandex.direct.web.entity.cashback;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.direct.common.util.HttpUtil;
import ru.yandex.direct.core.entity.cashback.CashbackConstants;
import ru.yandex.direct.core.testing.MockMvcCreator;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper;
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.direct.web.entity.cashback.CashbackController.CASHBACK_DETAILS_ENDPOINT;
import static ru.yandex.direct.web.entity.cashback.CashbackController.PERIOD_MONTHS_PARAM;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class CashbackControllerTest {
    private static final String CONTROLLER_URL_PATH = "/cashback/" + CASHBACK_DETAILS_ENDPOINT;

    @Autowired
    private CashbackController controller;

    @Autowired
    private MockMvcCreator mockMvcCreator;

    @Autowired
    private TestAuthHelper testAuthHelper;

    @Autowired
    private DirectWebAuthenticationSource directWebAuthenticationSource;

    @Autowired
    private Steps steps;

    private MockMvc mockMvc;

    @Before
    public void initTestData() {
        mockMvc = mockMvcCreator.setup(controller).build();

        UserInfo user = steps.userSteps().createDefaultUser();
        testAuthHelper.setOperatorAndSubjectUser(user.getUid());
        TestAuthHelper.setSecurityContextWithAuthentication(directWebAuthenticationSource.getAuthentication());
    }

    @Test
    public void getCashbackDetails_success() throws Exception {
        var requestBuilder = MockMvcRequestBuilders.get(CONTROLLER_URL_PATH)
                .param(PERIOD_MONTHS_PARAM, "12");

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpUtil.X_ACCEL_REDIRECT));
    }

    @Test
    public void getCashbackDetails_validationIssue() throws Exception {
        var requestBuilder = MockMvcRequestBuilders.get(CONTROLLER_URL_PATH)
                .param(PERIOD_MONTHS_PARAM, String.valueOf(CashbackConstants.DETALIZATION_MAX_LENGTH + 1));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest());
    }
}
