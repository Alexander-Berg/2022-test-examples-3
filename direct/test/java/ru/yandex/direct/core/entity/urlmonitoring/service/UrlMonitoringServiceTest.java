package ru.yandex.direct.core.entity.urlmonitoring.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignForNotifyUrlMonitoring;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.notification.NotificationService;
import ru.yandex.direct.core.entity.notification.container.Notification;
import ru.yandex.direct.core.entity.notification.container.UrlMonitoringEventNotification;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.BannerSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.ytwrapper.client.YtProvider;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class UrlMonitoringServiceTest {

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignService campaignService;
    @Autowired
    private UserService userService;

    private NotificationService notificationService;
    @Mock
    private YtProvider ytProvider;

    private UrlMonitoringService urlMonitoringService;

    @Before
    public void setUp() {
        notificationService = mock(NotificationService.class);
        urlMonitoringService = new UrlMonitoringService(campaignService, userService, notificationService,
                ytProvider);
    }

    @Test
    public void testUrlMonitoringNotifications() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        String domain1 = "www.omsk.urlmonitoringservicetest.com";
        String reverseDomain1 = "moc.tsetecivresgnirotinomlru.ksmo.www";
        String domain2 = "www.orel.urlmonitoringservicetest.com";
        String reverseDomain2 = "moc.tsetecivresgnirotinomlru.lero.www";
        CampaignInfo campaign1 = steps.campaignSteps().createCampaign(
                TestCampaigns.activeTextCampaign(null, null)
                        .withStatusMetricaControl(true),
                clientInfo);
        CampaignInfo campaign2 = steps.campaignSteps().createCampaign(
                TestCampaigns.activeTextCampaign(null, null)
                        .withStatusMetricaControl(true),
                clientInfo);
        CampaignInfo campaignWithUnmatchedHref = steps.campaignSteps().createCampaign(
                TestCampaigns.activeTextCampaign(null, null)
                        .withStatusMetricaControl(true),
                clientInfo);
        CampaignInfo campaignWithoutMonitoring = steps.campaignSteps().createCampaign(
                TestCampaigns.activeTextCampaign(null, null)
                        .withStatusMetricaControl(false),
                clientInfo);
        BannerSteps bannerSteps = steps.bannerSteps();
        bannerSteps.createBanner(activeTextBanner().withDomain(domain1).withReverseDomain(reverseDomain1).withHref(
                "Https://" + domain1), campaign1);
        bannerSteps.createBanner(activeTextBanner().withDomain(domain2).withReverseDomain(reverseDomain2).withHref(
                "httPs://" + domain2), campaign2);
        bannerSteps.createBanner(activeTextBanner().withDomain(domain2).withReverseDomain(reverseDomain2).withHref(
                "https://" + domain2), campaignWithUnmatchedHref);
        bannerSteps.createBanner(activeTextBanner().withDomain(domain2).withReverseDomain(reverseDomain2).withHref(
                "httPs://" + domain2), campaignWithoutMonitoring);

        String state = "dead";
        Set<Pair<String, String>> protocolDomainPairs = new HashSet<>(
                Arrays.asList(
                        Pair.of("Https", domain1),
                        Pair.of("httPs", domain2)
                )
        );
        ImmutableMap<String, Set<Pair<String, String>>> domainsByState = ImmutableMap.of(state, protocolDomainPairs);

        urlMonitoringService.notifyUsersOnDomainsStateChange(domainsByState, false);

        ClientId clientId = clientInfo.getClientId();
        List<CampaignForNotifyUrlMonitoring> campaigns =
                mapList(campaignService.getCampaigns(clientId,
                        Arrays.asList(campaign1.getCampaignId(), campaign2.getCampaignId())),
                        CampaignForNotifyUrlMonitoring.class::cast);

        Set<String> domains = protocolDomainPairs.stream().map(Pair::getRight).collect(Collectors.toUnmodifiableSet());
        verify(notificationService)
                .addNotification(getNotificationMatcher(clientInfo.getChiefUserInfo().getUser(), campaigns, state,
                        domains));
    }

    private Notification getNotificationMatcher(User user,
                                                List<CampaignForNotifyUrlMonitoring> campaigns, String state,
                                                Set<String> domains) {
        return argThat(v -> beanDiffer(
                new UrlMonitoringEventNotification()
                        .withUid(user.getUid())
                        .withClientId(user.getClientId().asLong())
                        .withLogin(user.getLogin())
                        .withState(state)
                        .withCampaigns(campaigns)
                        .withDomains(domains))
                .useCompareStrategy(onlyExpectedFields())
                .matches(v));
    }

}
