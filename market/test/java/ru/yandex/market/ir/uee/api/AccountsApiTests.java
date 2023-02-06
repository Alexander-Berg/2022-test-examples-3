package ru.yandex.market.ir.uee.api;


import lombok.SneakyThrows;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.ir.uee.api.service.AccountService;
import ru.yandex.market.ir.uee.api.service.UserRequestService;
import ru.yandex.market.ir.uee.model.Account;
import ru.yandex.market.ir.uee.model.AccountLitePage;
import ru.yandex.market.ir.uee.model.AccountReq;
import ru.yandex.market.ir.uee.model.UserRequestType;
import ru.yandex.market.ir.uee.model.ValidationErrorResponse;
import ru.yandex.market.ir.uee.model.Violation;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.ir.uee.repository.AccountRepo.DEFAULT_ACCOUNT_LOGIN;

public class AccountsApiTests extends BaseTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    AccountService accountService;

    @SpyBean
    UserRequestService userRequestService;

    @Test
    public void createAccountTest() throws Exception {
        var accountReq = readResource("accountReq.json", AccountReq.class);
        MvcResult mvcResult = mockMvc.perform(
                post("/uee/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeValue(accountReq))
        ).andExpect(status().isOk())
                .andReturn();
        Account account = extractModelFromMvcResult(mvcResult, Account.class);

        assertThat(account.getId()).isNotNull();
        assertThat(account.getLogin()).isEqualTo(accountReq.getLogin());
        assertThat(account.getAcl()).usingElementComparatorIgnoringFields("role").containsAll(accountReq.getAcl());
        assertThat(account.getNotificationRecipients()).containsAll(accountReq.getNotificationRecipients());
        assertThat(account.getQuota()).isEqualTo(accountReq.getQuota());
        verify(userRequestService).createAccountUserRequest(any(), eq(UserRequestType.CREATE_ACCOUNT), any());
    }

    @Test
    @SneakyThrows
    public void createAccountDuplicateLogin() {
        var accountReq = readResource("accountReq.json", AccountReq.class);
        accountReq.setLogin(DEFAULT_ACCOUNT_LOGIN);

        MvcResult mvcResult = mockMvc.perform(
                post("/uee/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeValue(accountReq))
        ).andExpect(status().isBadRequest())
                .andReturn();
        var exception = extractModelFromMvcResult(mvcResult, ValidationErrorResponse.class);
        assertThat(exception.getViolations()).isNotEmpty();
        assertThat(exception.getViolations()).extracting(Violation::getFieldName).contains("login");
    }

    @Test
    public void getDefaultAccount() throws Exception {
        var accountRecordId = ensureDefaultAccountExistAndReturnId();

        MvcResult mvcResult = mockMvc.perform(
                get(format("/uee/api/accounts/%s", accountRecordId))
        ).andExpect(status().isOk())
                .andReturn();
        var account = extractModelFromMvcResult(mvcResult, Account.class);

        assertThat(account.getLogin()).isEqualTo(DEFAULT_ACCOUNT_LOGIN);
    }

    @Test
    public void searchAccountByLogin() throws Exception {
        var accountRecord = storeAccount();

        MvcResult mvcResult = mockMvc.perform(
                get(format("/uee/api/accounts?filterLogin=%s", accountRecord.getLogin()))
        ).andExpect(status().isOk())
                .andReturn();
        AccountLitePage accountLitePage = extractModelFromMvcResult(mvcResult, AccountLitePage.class);

        assertThat(accountLitePage.getTotalElements()).isEqualTo(1);
        assertThat(accountLitePage.getContent().get(0).getLogin()).isEqualTo(accountRecord.getLogin());
    }

    @Test
    public void searchEmptyPage() throws Exception {
        var accountRecord = storeAccount();

        MvcResult mvcResult = mockMvc.perform(
                get("/uee/api/accounts?offset=1000")
        ).andExpect(status().isOk())
                .andReturn();
        AccountLitePage accountLitePage = extractModelFromMvcResult(mvcResult, AccountLitePage.class);

        assertThat(accountLitePage.getTotalElements()).isEqualTo(2);
        assertThat(accountLitePage.getContent()).isEmpty();
    }


    @Test
    public void searchDefaultAccount() throws Exception {
        MvcResult mvcResult = mockMvc.perform(
                get("/uee/api/accounts"))
                .andExpect(status().isOk())
                .andReturn();
        AccountLitePage accountLitePage = extractModelFromMvcResult(mvcResult, AccountLitePage.class);

        assertThat(accountLitePage.getTotalElements()).isEqualTo(1);
        assertThat(accountLitePage.getContent().get(0).getLogin()).isEqualTo(DEFAULT_ACCOUNT_LOGIN);
    }

    @Test
    @SneakyThrows
    public void updateAccount() {
        var accountRecord = storeAccount();
        var accountReq = readResource("accountUpdateReq.json", AccountReq.class);

        MvcResult mvcResult = mockMvc.perform(
                put(format("/uee/api/accounts/%s", accountRecord.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeValue(accountReq))
        ).andExpect(status().isOk())
                .andReturn();
        Account account = extractModelFromMvcResult(mvcResult, Account.class);

        assertThat(account.getLogin()).isEqualTo(accountReq.getLogin());
        assertThat(account.getAcl()).usingElementComparatorIgnoringFields("role").containsAll(accountReq.getAcl());
        assertThat(account.getNotificationRecipients()).containsAll(accountReq.getNotificationRecipients());
        assertThat(account.getQuota()).isEqualTo(accountReq.getQuota());
        assertThat(account.getUserRunTtlDays()).isEqualTo(accountReq.getUserRunTtlDays());
        verify(userRequestService).createAccountUserRequest(any(), eq(UserRequestType.UPDATE_ACCOUNT), any());
    }

    @Test
    @SneakyThrows
    public void cleanAccountData() {
        var accountRecord = storeAccount();
        MvcResult mvcResult = mockMvc.perform(
                put(format("/uee/api/accounts/%s/clean", accountRecord.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk())
                .andReturn();

        verify(userRequestService).createCleanAccountDataUserRequest(eq(accountRecord.getId()), any());
    }

}
