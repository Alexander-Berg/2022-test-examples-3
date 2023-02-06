package ru.yandex.direct.web.entity.smsauth.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.bolts.collection.Option; // IGNORE-BAD-STYLE DIRECT-100909
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.security.DirectAuthentication;
import ru.yandex.direct.core.smsauth.service.SmsAuthStorageService;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.configuration.mock.auth.DirectWebAuthenticationSourceMock;
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource;
import ru.yandex.direct.web.entity.smsauth.model.getsmsphone.GetSmsPhoneResponse;
import ru.yandex.direct.web.entity.smsauth.model.getsmsphone.GetSmsPhoneResult;
import ru.yandex.direct.web.entity.smsauth.service.SmsAuthControllerService;
import ru.yandex.direct.web.entity.smsauth.service.SmsAuthService;
import ru.yandex.direct.web.entity.smsauth.service.SmsAuthValidationService;
import ru.yandex.direct.web.validation.kernel.ValidationResultConversionService;
import ru.yandex.direct.yasms.YaSmsClient;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxPhone;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.bolts.collection.Option.empty; // IGNORE-BAD-STYLE DIRECT-100909

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SmsAuthControllerTest {

    private static final String EXPECTED_CONTROLLER_MAPPING = "/sms_auth/get_sms_phone";
    private static final Long USER_PHONE_ID = 1234L;
    private static final String USER_PHONE = "+7927*****86";

    @Autowired
    private Steps steps;

    @Autowired
    private DirectWebAuthenticationSource authenticationSource;

    @Autowired
    private SmsAuthValidationService smsAuthValidationService;

    @Autowired
    private ValidationResultConversionService validationResultConversionService;

    private MockMvc mockMvc;

    private User operator;

    @Before
    public void before() {
        operator = steps.userSteps().createDefaultUser().getUser();

        UserService userService = mock(UserService.class);
        SmsAuthStorageService smsAuthStorageService = mock(SmsAuthStorageService.class);
        YaSmsClient yaSmsClient = mock(YaSmsClient.class);

        SmsAuthService smsAuthService = new SmsAuthService(userService, smsAuthStorageService, yaSmsClient);
        SmsAuthControllerService smsAuthControllerService = new SmsAuthControllerService(smsAuthService,
                authenticationSource, smsAuthValidationService, validationResultConversionService);
        SmsAuthController smsAuthController = new SmsAuthController(smsAuthControllerService);

        BlackboxPhone blackboxPhone = new BlackboxPhone(USER_PHONE_ID, empty(), empty(), empty(), empty(),
                Option.of(USER_PHONE), empty(), empty(), empty(), empty());
        doReturn(blackboxPhone).when(userService).getSmsBlackboxPhone(operator.getUid());

        mockMvc = MockMvcBuilders.standaloneSetup(smsAuthController).build();
    }

    @Test
    public void getSmsPhone_Success() throws Exception {
        setAuthData(operator);

        GetSmsPhoneResponse actualResponse = callGetSmsPhone();
        GetSmsPhoneResponse expectedResponse = new GetSmsPhoneResponse(new GetSmsPhoneResult(USER_PHONE, USER_PHONE_ID));

        assertThat(actualResponse, beanDiffer(expectedResponse));
    }

    private void setAuthData(User operator) {
        DirectWebAuthenticationSourceMock authSource = (DirectWebAuthenticationSourceMock) authenticationSource;
        authSource.withOperator(new User().withUid(operator.getUid()));

        SecurityContextHolder.getContext()
                .setAuthentication(new DirectAuthentication(operator, operator));
    }

    private GetSmsPhoneResponse callGetSmsPhone() throws Exception {
        String response = mockMvc
                .perform(get(EXPECTED_CONTROLLER_MAPPING))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonUtils.fromJson(response, GetSmsPhoneResponse.class);
    }
}
