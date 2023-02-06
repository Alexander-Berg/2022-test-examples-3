package ru.yandex.market.vendors.analytics.tms.jobs.partner.beru;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.common.framework.user.UserInfo;
import ru.yandex.common.framework.user.UserInfoService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.utils.UserInfoMockUtils;
import ru.yandex.market.vendors.analytics.tms.FunctionalTest;
import ru.yandex.market.vendors.analytics.tms.jobs.partner.csv.ProcessMailExecutor;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * Функциональный тест для джобы {@link ProcessMailExecutor}.
 *
 * @author ogonek
 */
@DbUnitDataSet(before = "BeruFiredExecutorTest.before.csv")
public class YndxFiredExecutorTest extends FunctionalTest {

    @Autowired
    private YndxFiredExecutor yndxFiredExecutor;

    @Autowired
    protected RestTemplate staffRestTemplate;

    @MockBean(name = "userInfoService")
    private UserInfoService userInfoService;

    protected MockRestServiceServer mockRestServiceServer;

    @BeforeEach
    void resetMocks() {
        mockRestServiceServer = MockRestServiceServer.createServer(staffRestTemplate);
    }

    @Test
    @DbUnitDataSet(after = "BeruFiredExecutorTest.after.csv")
    void haveFiredUsersTest() {
        mock("yndx-ogonek", 1);
        mock("yndx-ratata", 2);
        var accountsRequestUrl = "https://staff-api.yandex-team.ru/v3/persons?"
                + "_limit=25&is_deleted=false&accounts.value=yndx-ogonek@yandex.ru,yndx-ratata@yandex.ru"
                + "&_fields=accounts.value";
        String accountsResponse = loadFromFile("accountsResponse.json");
        mockRestServiceServer.expect(ExpectedCount.once(), requestTo(accountsRequestUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(accountsResponse)
                );
        var emailsRequestUrl = "https://staff-api.yandex-team.ru/v3/persons?"
                + "_limit=25&is_deleted=false&emails.address=yndx-ogonek@yandex.ru,yndx-ratata@yandex.ru"
                + "&_fields=emails.address";
        String emailsResponse = loadFromFile("emailsResponse.json");
        mockRestServiceServer.expect(ExpectedCount.once(), requestTo(emailsRequestUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(emailsResponse)
                );
        yndxFiredExecutor.doJob(null);
    }

    private void mock(String userLogin, long userId) {
        UserInfo userInfo = UserInfoMockUtils.mockUserInfo(userLogin, userId);
        when(userInfoService.getUserInfo(userId)).thenReturn(userInfo);
    }

}
