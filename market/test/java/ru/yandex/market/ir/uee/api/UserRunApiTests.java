package ru.yandex.market.ir.uee.api;

import lombok.SneakyThrows;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.ir.uee.repository.ResourceRepo;
import ru.yandex.market.ir.uee.api.service.UserRequestService;
import ru.yandex.market.ir.uee.api.service.UserRunService;
import ru.yandex.market.ir.uee.jooq.generated.tables.records.UserRunRecord;
import ru.yandex.market.ir.uee.model.UserRequestType;
import ru.yandex.market.ir.uee.model.UserRun;
import ru.yandex.market.ir.uee.model.UserRunLitePage;
import ru.yandex.market.ir.uee.model.UserRunReq;
import ru.yandex.market.ir.uee.model.UserRunState;
import ru.yandex.market.ir.uee.model.UserRunType;
import ru.yandex.market.ir.uee.model.UserRunUpdateReq;
import ru.yandex.market.ir.uee.model.ValidationErrorResponse;
import ru.yandex.market.ir.uee.model.Violation;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class UserRunApiTests extends BaseTest {

    private static final String SOME_CUSTOM_YT_POOL = "some-custom-yt-pool";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRunService userRunService;

    @SpyBean
    UserRequestService userRequestService;

    @Autowired
    ResourceRepo resourceRepo;

    @Test
    public void createUserRunTest() throws Exception {
        UserRunReq userRunReq = readResource("userRunReq.json", UserRunReq.class);
        Integer accountId = ensureDefaultAccountExistAndReturnId();
        userRunReq.setAccountId(accountId);
        grantAccessToYtPool(accountId, DEFAULT_YT_POOL);
        MvcResult mvcResult = mockMvc.perform(
                post("/uee/api/userRuns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeValue(userRunReq))
        ).andExpect(status().isOk())
                .andReturn();
        UserRun userRun = extractModelFromMvcResult(mvcResult, UserRun.class);

        assertThat(userRun.getAccountId()).isEqualTo(userRunReq.getAccountId());
        assertThat(userRun.getAccountName()).isNotEmpty();
        assertThat(userRun.getAuthor()).isNotEmpty();
        assertThat(userRun.getFieldMappings()).isEqualTo(userRunReq.getFieldMappings());
        assertThat(userRun.getNotificationRecipients()).isEqualTo(userRunReq.getNotificationRecipients());
        assertThat(userRun.getInput()).isEqualTo(userRunReq.getInput());
        assertThat(userRun.getState()).isEqualTo(UserRunState.CREATED);
        assertThat(userRun.getUserRunType()).isEqualTo(UserRunType.SM);
        assertThat(userRun.getYtPool()).isEqualTo(DEFAULT_YT_POOL);

        verify(userRequestService).createUserRunUserRequest(any(), eq(UserRequestType.CREATE_USER_RUN), any());
    }

    @Test
    public void createUserRunFailedNoRequiredFiled() throws Exception {
        UserRunReq userRunReq = readResource("userRunReqNoRequiredField.json", UserRunReq.class);
        Integer accountId = ensureDefaultAccountExistAndReturnId();
        userRunReq.setAccountId(accountId);
        grantAccessToYtPool(accountId, DEFAULT_YT_POOL);
        MvcResult mvcResult = mockMvc.perform(
                post("/uee/api/userRuns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeValue(userRunReq))
        ).andExpect(status().isBadRequest())
                .andReturn();
        var error = extractModelFromMvcResult(mvcResult, ValidationErrorResponse.class);
        assertThat(error.getViolations()).extracting(Violation::getFieldName).contains("fieldMappings.title");
    }

    @Test
    public void createUserRunFailedIfNoAccessToCustomYtPool() throws Exception {
        UserRunReq userRunReq = readResource("userRunReq.json", UserRunReq.class);
        Integer accountId = ensureDefaultAccountExistAndReturnId();
        userRunReq.setAccountId(accountId);
        grantAccessToYtPool(accountId, DEFAULT_YT_POOL);
        userRunReq.setYtPool(SOME_CUSTOM_YT_POOL);
        MvcResult mvcResult = mockMvc.perform(
                post("/uee/api/userRuns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeValue(userRunReq))
        ).andExpect(status().isNotFound())
                .andReturn();
        var error = extractModelFromMvcResult(mvcResult, ValidationErrorResponse.class);
        assertThat(error.getErrorMessage()).contains(SOME_CUSTOM_YT_POOL);
    }

    @Test
    public void createUserRunFailedIfNoAccessToDefaultYtPool() throws Exception {
        UserRunReq userRunReq = readResource("userRunReq.json", UserRunReq.class);
        Integer accountId = ensureDefaultAccountExistAndReturnId();
        userRunReq.setAccountId(accountId);
        grantAccessToYtPool(accountId, SOME_CUSTOM_YT_POOL);
        MvcResult mvcResult = mockMvc.perform(
                post("/uee/api/userRuns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeValue(userRunReq))
        ).andExpect(status().isNotFound())
                .andReturn();
        var error = extractModelFromMvcResult(mvcResult, ValidationErrorResponse.class);
        assertThat(error.getErrorMessage()).contains(DEFAULT_YT_POOL);
    }

    @Test
    public void createUserRunWithCustomYtPool() throws Exception {
        UserRunReq userRunReq = readResource("userRunReq.json", UserRunReq.class);
        Integer accountId = ensureDefaultAccountExistAndReturnId();
        userRunReq.setAccountId(accountId);
        grantAccessToYtPool(accountId, SOME_CUSTOM_YT_POOL);
        userRunReq.setYtPool(SOME_CUSTOM_YT_POOL);

        MvcResult mvcResult = mockMvc.perform(
                post("/uee/api/userRuns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeValue(userRunReq))
        ).andExpect(status().isOk())
                .andReturn();
        UserRun userRun = extractModelFromMvcResult(mvcResult, UserRun.class);

        assertThat(userRun.getYtPool()).isEqualTo(SOME_CUSTOM_YT_POOL);

        verify(userRequestService).createUserRunUserRequest(any(), eq(UserRequestType.CREATE_USER_RUN), any());
    }

    @Test
    public void createUserRunWithInputUniqueAndCheckRetry() throws Exception {
        UserRunReq userRunReq = readResource("userRunReq.json", UserRunReq.class);
        Integer accountId = ensureDefaultAccountExistAndReturnId();
        userRunReq.setAccountId(accountId);
        userRunReq.setInputUniqueId("b1c6c4cc-d65d-4f15-bbae-e0df878ae924");
        grantAccessToYtPool(accountId, DEFAULT_YT_POOL);

        MvcResult mvcResult = mockMvc.perform(
                post("/uee/api/userRuns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeValue(userRunReq))
        ).andExpect(status().isOk())
                .andReturn();
        UserRun userRun = extractModelFromMvcResult(mvcResult, UserRun.class);

        mvcResult = mockMvc.perform(
                        post("/uee/api/userRuns")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(writeValue(userRunReq))
                ).andExpect(status().isOk())
                .andReturn();

        UserRun userRunRetry = extractModelFromMvcResult(mvcResult, UserRun.class);

        userRunReq.setInputUniqueId(null);
        mvcResult = mockMvc.perform(
                        post("/uee/api/userRuns")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(writeValue(userRunReq))
                ).andExpect(status().isOk())
                .andReturn();

        UserRun userRunWOInputUnique = extractModelFromMvcResult(mvcResult, UserRun.class);

        assertThat(userRun.getId()).isEqualTo(userRunRetry.getId());
        assertThat(userRun.getId()).isNotEqualTo(userRunWOInputUnique.getId());

        verify(userRequestService, times(2))
                .createUserRunUserRequest(any(), eq(UserRequestType.CREATE_USER_RUN), any());
    }

    @Test
    @SneakyThrows
    public void updateUserRunTest() {
        var userRunRecord = storeUserRun(r -> r.setAccountId(ensureDefaultAccountExistAndReturnId()));
        var userRunUpdateReq = readResource("userRunUpdateReq.json", UserRunUpdateReq.class);

        MvcResult mvcResult = mockMvc.perform(
                put("/uee/api/userRuns/" + userRunRecord.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeValue(userRunUpdateReq))
        ).andExpect(status().isOk())
                .andReturn();
        UserRun userRun = extractModelFromMvcResult(mvcResult, UserRun.class);

        assertThat(userRun.getName()).isEqualTo(userRunUpdateReq.getName());
        assertThat(userRun.getNotificationRecipients()).containsAll(userRunUpdateReq.getNotificationRecipients());
        verify(userRequestService).createUserRunUserRequest(any(), eq(UserRequestType.UPDATE_USER_RUN), any());
    }

    @SneakyThrows
    @Test
    public void pauseUserRunTest() {
        var userRunRecord = storeUserRun(r -> r.setAccountId(ensureDefaultAccountExistAndReturnId()));

        mockMvc .perform(put("/uee/api/userRuns/" + userRunRecord.getId() + "/pause"))
                .andExpect(status().isOk()) // user run can be paused in CREATED state; verify repository operations
                .andReturn();
    }

    @SneakyThrows
    @Test
    public void resumeUserRunTest() {
        var userRunRecord = storeUserRun(r -> r.setAccountId(ensureDefaultAccountExistAndReturnId()));

        mockMvc .perform(put("/uee/api/userRuns/" + userRunRecord.getId() + "/resume"))
                .andExpect(status().isBadRequest()) // user run cannot be resumed in CREATED state
                .andReturn();
    }

    @SneakyThrows
    @Test
    public void recoverUserRunTest() {
        var userRunRecord = storeUserRun(r -> r.setAccountId(ensureDefaultAccountExistAndReturnId()));

        mockMvc .perform(put("/uee/api/userRuns/" + userRunRecord.getId() + "/recover"))
                .andExpect(status().isBadRequest()) // user run cannot be recovered in CREATED state
                .andReturn();
    }

    @SneakyThrows
    @Test
    public void cancelUserRunTest() {
        var userRunRecord = storeUserRun(r -> r.setAccountId(ensureDefaultAccountExistAndReturnId()));

        mockMvc .perform(put("/uee/api/userRuns/" + userRunRecord.getId() + "/stop"))
                .andExpect(status().isOk()) // user run can be cancelled in CREATED state; verify repository operations
                .andReturn();
    }

    @SneakyThrows
    @Test
    public void searchUserRunsByStateTest() {
        UserRunRecord firstTask = storeUserRuns();

        MvcResult mvcResult = mockMvc.perform(
                get(format("/uee/api/userRuns?filterState=%s", UserRunState.CREATED))
        ).andExpect(status().isOk())
                .andReturn();

        var userRunLitePage = extractModelFromMvcResult(mvcResult, UserRunLitePage.class);

        assertThat(userRunLitePage.getTotalElements()).isEqualTo(2);
        assertThat(userRunLitePage.getContent().get(0).getId()).isEqualTo(firstTask.getId());
    }

    @SneakyThrows
    @Test
    public void searchUserRunsByAccountTest() {
        storeUserRuns();

        MvcResult mvcResult = mockMvc.perform(
                get(format("/uee/api/userRuns?filterAccountLogin=%s", "acc1"))
        ).andExpect(status().isOk())
                .andReturn();

        var userRunLitePage = extractModelFromMvcResult(mvcResult, UserRunLitePage.class);

        assertThat(userRunLitePage.getTotalElements()).isEqualTo(2);
    }

    private UserRunRecord storeUserRuns() {
        var firstAccount = storeAccount(r -> r.setLogin("acc1"));
        var secondAccount = storeAccountWithoutAcl(r -> r.setLogin("acc2"));
        var thirdAccount = storeAccount(r -> r.setLogin("acc3"));

        var firstUserRun = storeUserRun(r -> r.setAccountId(firstAccount.getId()));
        var secondUserRun = storeUserRun(r -> r.setAccountId(secondAccount.getId()));
        var cancelledUserRun = storeUserRun(
                r -> r.setAccountId(firstAccount.getId()),
                r -> r.setState(UserRunState.CANCELLED.toString())
        );
        var runningUserRUn = storeUserRun(
                r -> r.setAccountId(thirdAccount.getId()),
                r -> r.setState(UserRunState.RUNNING.toString())
        );
        return secondUserRun;
    }
}
