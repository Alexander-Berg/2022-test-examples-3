package ru.yandex.direct.jobs.urlmonitoring;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.notification.NotificationService;
import ru.yandex.direct.core.entity.notification.container.Notification;
import ru.yandex.direct.core.entity.notification.container.UrlMonitoringEventNotification;
import ru.yandex.direct.core.entity.urlmonitoring.service.UrlMonitoringService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.solomon.SolomonPushClient;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.kikimr.persqueue.consumer.SyncConsumer;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerReadResponse;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.ppcproperty.model.PpcPropertyEnum.URL_MONITORING_ON_LB_AND_YT_ENABLED;
import static ru.yandex.direct.core.entity.ppcproperty.model.PpcPropertyEnum.URL_MONITORING_ON_LB_DRY_RUN;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;

@JobsTest
@ExtendWith(SpringExtension.class)
class UrlMonitoringEventReceivingJobIntegrationTest {

    private static final long EXPECTED_LAST_COOKIE = 123456789L;
    private static final String URL_MONITORING_INTEGRATION_TEST_DOMAIN = "urlmonitoringintegrationtest.ru";
    private static final String URL_MONITORING_INTEGRATION_TEST_DOMAIN_REVERSED = "ur.tsetnoitargetnignirotinomlru";
    private static final String URL_MONITORING_INTEGRATION_TEST_HREF =
            "http://" + URL_MONITORING_INTEGRATION_TEST_DOMAIN + "/blah";

    @Autowired
    Steps steps;
    @Autowired
    PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    private CampaignService campaignService;
    @Autowired
    private UserService userService;

    @Mock
    private NotificationService notificationService;
    @Mock
    private YtProvider ytProvider;
    @Mock
    private SolomonPushClient solomonPushClient;
    @Mock
    private Provider<SyncConsumer> syncConsumerProvider;

    private UrlMonitoringEventReceivingJob urlMonitoringEventReceivingJob;
    private UrlMonitoringService urlMonitoringService;

    private UserInfo userInfo;
    private Campaign campaign;
    private MessageData messageData;

    @BeforeEach
    void init() throws Exception {
        MockitoAnnotations.initMocks(this);

        urlMonitoringService =
                spy(new UrlMonitoringService(campaignService, userService, notificationService, ytProvider));

        initMockSyncConsumer(getEventJson("http://" + URL_MONITORING_INTEGRATION_TEST_DOMAIN, 0, 1, 0L));

        urlMonitoringEventReceivingJob =
                spy(new UrlMonitoringEventReceivingJob(ppcPropertiesSupport, this.syncConsumerProvider,
                        urlMonitoringService, solomonPushClient));

        ppcPropertiesSupport.set(URL_MONITORING_ON_LB_AND_YT_ENABLED.getName(), Boolean.toString(true));

        TextBannerInfo bannerInfo = steps.bannerSteps().createBanner(
                activeTextBanner().withDomain(URL_MONITORING_INTEGRATION_TEST_DOMAIN)
                        .withReverseDomain(URL_MONITORING_INTEGRATION_TEST_DOMAIN_REVERSED)
                        .withHref(URL_MONITORING_INTEGRATION_TEST_HREF),
                steps.campaignSteps().createCampaign(
                        TestCampaigns.activeTextCampaign(null, null)
                                .withStatusMetricaControl(true))
        );
        userInfo = bannerInfo.getClientInfo().getChiefUserInfo();
        ClientId clientId = userInfo.getClientInfo().getClientId();
        campaign = campaignService.getCampaigns(clientId, singleton(bannerInfo.getCampaignId())).get(0);
    }

    private String getEventJson(String url, Integer oldStatus, Integer newStatus, Long updatedTimestamp) {
        String eventPattern = "{\"old\": %s, \"new\": %s, \"updated\": %s, \"url\": \"%s\", \"sources\": [\"%s\"]}";
        return String.format(eventPattern, oldStatus, newStatus, updatedTimestamp, url, "direct");
    }

    private void initMockSyncConsumer(String jsonResponse) throws InterruptedException {
        messageData = mock(MessageData.class);
        mockMessage(jsonResponse);
        List<MessageData> batchMessageData = singletonList(messageData);
        MessageBatch messageBatch = mock(MessageBatch.class);
        when(messageBatch.getMessageData()).thenReturn(batchMessageData);
        ConsumerReadResponse readResponse = mock(ConsumerReadResponse.class);
        List<MessageBatch> messageBatches = singletonList(messageBatch);
        when(readResponse.getBatches()).thenReturn(messageBatches);
        when(readResponse.getCookie()).thenReturn(EXPECTED_LAST_COOKIE);
        SyncConsumer syncConsumer = mock(SyncConsumer.class);
        when(syncConsumer.read()).thenReturn(readResponse, (ConsumerReadResponse) null);
        when(this.syncConsumerProvider.get()).thenReturn(syncConsumer);
    }

    private void mockMessage(String jsonResponse) {
        byte[] requestBody = jsonResponse.getBytes(StandardCharsets.UTF_8);
        when(messageData.getDecompressedData()).thenReturn(requestBody);
    }

    @Test
    void exceptionFromNotificationServiceShouldNotPreventCommit() throws Exception {
        doThrow(new RuntimeException()).when(notificationService).addNotification(any());

        urlMonitoringEventReceivingJob.execute();

        verify(notificationService)
                .addNotification(getNotificationMatcher(userInfo.getUser(), campaign, singleton(
                        URL_MONITORING_INTEGRATION_TEST_DOMAIN)));
        verify(syncConsumerProvider.get()).commit(singletonList(EXPECTED_LAST_COOKIE));
    }

    @Test
    void dryRunEnablingPropertyShouldCauseLBCommitWithoutSendingNotifications() throws Exception {
        ppcPropertiesSupport.set(URL_MONITORING_ON_LB_DRY_RUN.getName(), Boolean.toString(true));

        urlMonitoringEventReceivingJob.execute();

        verify(notificationService, never()).addNotification(any());
        verify(syncConsumerProvider.get()).commit(singletonList(EXPECTED_LAST_COOKIE));
    }

    @Test
    void exceptionFromUrlMonitoringServiceShouldPreventCommit() throws Exception {
        doThrow(new RuntimeException())
                .when(urlMonitoringService)
                .notifyUsersOnDomainsStateChange(anyMap(), anyBoolean());
        try {
            urlMonitoringEventReceivingJob.execute();
        } catch (RuntimeException ignore) {
        }

        verify(syncConsumerProvider.get(), never()).commit(anyList());
    }

    @Test
    void invalidJsonMessageShouldNotPreventCommit() throws Exception {
        mockMessage("{\"old\":\"preved)\"}\n{\"sources\":null}");

        urlMonitoringEventReceivingJob.execute();

        verify(notificationService, never()).addNotification(any());
        verify(syncConsumerProvider.get()).commit(singletonList(EXPECTED_LAST_COOKIE));
    }

    private Notification getNotificationMatcher(User user, Campaign campaign, Set<String> domains) {
        return argThat(v -> beanDiffer(
                new UrlMonitoringEventNotification()
                        .withUid(user.getUid())
                        .withClientId(user.getClientId().asLong())
                        .withLogin(user.getLogin())
                        .withCampaigns(singletonList(campaign))
                        .withDomains(domains))
                .useCompareStrategy(onlyExpectedFields())
                .matches(v));
    }

}
