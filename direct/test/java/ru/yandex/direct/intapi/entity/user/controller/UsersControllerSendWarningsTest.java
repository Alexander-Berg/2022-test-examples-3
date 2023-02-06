package ru.yandex.direct.intapi.entity.user.controller;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.direct.core.entity.eventlog.model.EventLog;
import ru.yandex.direct.core.entity.eventlog.model.EventLogType;
import ru.yandex.direct.core.entity.eventlog.repository.EventLogRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.dbutil.QueryWithoutIndex;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.sender.YandexSenderClient;
import ru.yandex.direct.sender.YandexSenderException;
import ru.yandex.direct.sender.YandexSenderTemplateParams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.direct.dbschema.ppc.Tables.SMS_QUEUE;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UsersControllerSendWarningsTest {

    @Autowired
    private UsersController controller;
    @Autowired
    private Steps steps;
    @Autowired
    private YandexSenderClient senderClient;
    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private EventLogRepository eventLogRepository;


    private MockMvc mockMvc;
    private User user;
    private Campaign campaign;
    private String desiredEmail;
    private ClientInfo clientInfo;

    private static final String CUSTOM_LOGIN = "petrov";
    private static final String CUSTOM_EMAIL = "petrov@petrov.pe";
    private static final String CAMP_SLUG = "CVG38K14-BIZ1";

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        user = clientInfo.getChiefUserInfo().getUser();
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        campaign = campaignInfo.getCampaign().withClientId(clientInfo.getClientId().asLong()).withUid(user.getId());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        reset(senderClient);
    }

    void setupSenderClient() {
        doReturn(false).when(senderClient).sendTemplate(any(), any());
        doReturn(true).when(senderClient).sendTemplate(argThat(this::isValidTemplateParams), any());
    }

    @Test
    public void sendWarning_successful() throws Exception {
        setupSenderClient();
        desiredEmail = user.getEmail();
        String jsonAnswer = doRequest(
                "[{\"uid\":" + user.getUid() +
                        ", \"cid\":" + campaign.getId() +
                        ", \"template_data\": {\"login\":\"" + CUSTOM_LOGIN + "\"}}]");
        Set<Long> smsCampaignIds = getSmsQueueCampaignIdToText(clientInfo, user.getUid());
        List<EventLog> campaignEventLogEntries = eventLogRepository.getEventLogsByClientIdAndCampaignId(
                clientInfo.getShard(),
                campaign.getClientId(),
                campaign.getId());

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(jsonAnswer).isEqualTo("{\"" + campaign.getId() + "\":true}");
        soft.assertThat(smsCampaignIds)
                .as("SMS отправлено")
                .isEqualTo(Collections.singleton(campaign.getId()));
        soft.assertThat(campaignEventLogEntries.size())
                .as("Есть запись в эвентлоге")
                .isEqualTo(1);
        if (!campaignEventLogEntries.isEmpty()) {
            EventLog eventLog = campaignEventLogEntries.get(0);
            soft.assertThat(eventLog.getType() == EventLogType.MONEY_OUT);
            soft.assertThat(eventLog.getParams() == null);
        }
        soft.assertAll();
    }

    @Test
    public void sendWarningWithCustomEmail_successful() throws Exception {
        setupSenderClient();
        desiredEmail = CUSTOM_EMAIL;
        String jsonAnswer = doRequest(
                "[{\"uid\":" + user.getUid() +
                        ", \"cid\":" + campaign.getId() +
                        ", \"custom_email\": \"" + CUSTOM_EMAIL +
                        "\", \"template_data\": {\"login\":\"" + CUSTOM_LOGIN + "\"}}]");
        Set<Long> smsCampaignIds = getSmsQueueCampaignIdToText(clientInfo, user.getUid());
        List<EventLog> campaignEventLogEntries = eventLogRepository.getEventLogsByClientIdAndCampaignId(
                clientInfo.getShard(),
                campaign.getClientId(),
                campaign.getId());

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(jsonAnswer).isEqualTo("{\"" + campaign.getId() + "\":true}");
        soft.assertThat(smsCampaignIds)
                .as("SMS отправлено")
                .isEqualTo(Collections.singleton(campaign.getId()));
        soft.assertThat(campaignEventLogEntries.size())
                .as("Есть запись в эвентлоге")
                .isEqualTo(1);
        if (!campaignEventLogEntries.isEmpty()) {
            EventLog eventLog = campaignEventLogEntries.get(0);
            soft.assertThat(eventLog.getType() == EventLogType.MONEY_OUT);
            soft.assertThat(eventLog.getParams() == null);
        }
        soft.assertAll();
    }

    @Test
    public void sendWarningWithCustomEmail_fail() throws Exception {
        setupSenderClient();
        desiredEmail = CUSTOM_EMAIL;
        final String wrongEmail = CUSTOM_LOGIN + "1";
        String jsonAnswer = doRequest(
                "[{\"uid\":" + user.getUid() +
                        ", \"cid\":" + campaign.getId() +
                        ", \"custom_email\": \"" + CUSTOM_EMAIL +
                        "\", \"template_data\": {\"login\":\"" + wrongEmail + "\"}}]");
        Set<Long> smsCampaignIds = getSmsQueueCampaignIdToText(clientInfo, user.getUid());
        List<EventLog> campaignEventLogEntries = eventLogRepository.getEventLogsByClientIdAndCampaignId(
                clientInfo.getShard(),
                campaign.getClientId(),
                campaign.getId());

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(jsonAnswer).isEqualTo("{\"" + campaign.getId() + "\":true}");
        soft.assertThat(smsCampaignIds)
                .as("SMS отправлено")
                .isEqualTo(Collections.singleton(campaign.getId()));
        soft.assertThat(campaignEventLogEntries.size())
                .as("Есть запись в эвентлоге")
                .isEqualTo(1);
        if (!campaignEventLogEntries.isEmpty()) {
            EventLog eventLog = campaignEventLogEntries.get(0);
            soft.assertThat(eventLog.getType() == EventLogType.MONEY_OUT);
            soft.assertThat(eventLog.getParams() == null);
        }
        soft.assertAll();
    }

    @Test
    public void sendWarningWithCustomEmail_fail_throw() throws Exception {
        when(senderClient.sendTemplate(any(), any())).thenThrow(new YandexSenderException("Fail to send"));
        desiredEmail = CUSTOM_EMAIL;
        String jsonAnswer = doRequest(
                "[{\"uid\":" + user.getUid() +
                        ", \"cid\":" + campaign.getId() +
                        ", \"custom_email\": \"" + CUSTOM_EMAIL +
                        "\", \"template_data\": {\"login\":\"" + CUSTOM_LOGIN + "\"}}]");
        Set<Long> smsCampaignIds = getSmsQueueCampaignIdToText(clientInfo, user.getUid());
        List<EventLog> campaignEventLogEntries = eventLogRepository.getEventLogsByClientIdAndCampaignId(
                clientInfo.getShard(),
                campaign.getClientId(),
                campaign.getId());

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(jsonAnswer).isEqualTo("{\"" + campaign.getId() + "\":false}");
        soft.assertThat(smsCampaignIds)
                .as("SMS не отправлено")
                .isEmpty();
        soft.assertThat(campaignEventLogEntries.size())
                .as("Нет записи в эвентлоге")
                .isEqualTo(0);
        soft.assertAll();
    }
    private String doRequest(String content) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.post("/users/send_money_out_warning")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(content);
        ResultActions perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk());
        return perform.andReturn().getResponse().getContentAsString();
    }

    private boolean isValidTemplateParams(YandexSenderTemplateParams params) {
        assertThat(params.getToEmail()).as("email is correct").isEqualTo(desiredEmail);
        assertThat(params.getCampaignSlug()).as("slug is correct").isEqualTo(CAMP_SLUG);
        return CUSTOM_LOGIN.equals(params.getArgs().get("login"));
    }

    @QueryWithoutIndex("Только для тестов")
    private Set<Long> getSmsQueueCampaignIdToText(ClientInfo clientInfo, Long uid) {
        return dslContextProvider.ppc(clientInfo.getShard())
                .select(SMS_QUEUE.CID, SMS_QUEUE.SMS_TEXT)
                .from(SMS_QUEUE)
                .where(SMS_QUEUE.UID.eq(uid))
                .fetchSet(SMS_QUEUE.CID);
    }
}
