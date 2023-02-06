package ru.yandex.direct.jobs.campaign;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.campaign.model.CampaignSource;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.eventlog.service.EventLogService;
import ru.yandex.direct.core.entity.notification.NotificationService;
import ru.yandex.direct.core.entity.notification.container.CampFinishedMailNotification;
import ru.yandex.direct.core.entity.notification.container.Notification;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.jobs.configuration.JobsTest;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;

/**
 * Тесты на джобу {@link CampFinishedByDateWarningSender}
 */
@JobsTest
@ExtendWith(SpringExtension.class)
class CampFinishedByDateWarningSenderTest {
    private static final CampaignType LEGAL_CAMPAIGN_TYPE = CampaignType.TEXT;
    private static final CampaignType ILLEGAL_CAMPAIGN_TYPE = CampaignType.WALLET;

    private CampFinishedByDateWarningSender job;

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private UserService userService;

    @Mock
    private EventLogService eventLogService;

    @Mock
    private NotificationService notificationService;

    private CampaignInfo campaignInfo;
    private User user;

    static Iterable<Object[]> parameters() {
        final List<Integer> legalWarnDays = CampFinishedByDateWarningSender.WARN_WHEN_FINISHED_AFTER_DAYS;
        final int illegalWarnDay = legalWarnDays.stream().max(Comparator.naturalOrder()).get() + 1;
        // should not send notification for illegal campaign type
        final Stream<Object[]> illegalCampTypeCases = legalWarnDays.stream()
                .map(day -> new Object[]{day, ILLEGAL_CAMPAIGN_TYPE, false});
        final StreamEx<Object[]> legalCampTypeCases = StreamEx.of(legalWarnDays)
                // should send notification if a day is in legal warn days list
                .map(day -> new Object[]{day, LEGAL_CAMPAIGN_TYPE, true})
                // one case with illegal warn day
                .append(new Object[]{illegalWarnDay, LEGAL_CAMPAIGN_TYPE, false});
        return Stream.concat(legalCampTypeCases, illegalCampTypeCases).collect(toList());
    }

    void prepareJob(int daysBefore, CampaignType campaignType) {
        MockitoAnnotations.initMocks(this);

        campaignInfo = steps.campaignSteps()
                .createCampaign(activeTextCampaign(null, null)
                        .withType(campaignType)
                        .withFinishTime(LocalDate.now().minusDays(daysBefore + 1))
                        .withEmail("")
                        .withSource(CampaignSource.DIRECT));
        user = campaignInfo.getClientInfo().getChiefUserInfo().getUser();

        job = new CampFinishedByDateWarningSender(campaignInfo.getShard(), campaignService, eventLogService,
                notificationService, userService);
    }


    @ParameterizedTest(name = "Campaign of type {1} ended {0} days before today. Should send notification? - {2}")
    @MethodSource("parameters")
    void shouldSendNotificationOnFinishedCampaign(
            int daysBefore, CampaignType campaignType,
            boolean shouldSendNotification) {
        prepareJob(daysBefore, campaignType);

        assertThatCode(() -> job.execute())
                .doesNotThrowAnyException();

        final Campaign campaign = campaignInfo.getCampaign();
        if (shouldSendNotification) {
            verify(eventLogService)
                    .addCampFinishedEventLog(campaign.getId(), campaign.getFinishTime(), campaign.getClientId());
            verify(notificationService)
                    .addNotification(getNotificationMatcher(user, campaign));
        } else {
            verify(eventLogService, never())
                    .addCampFinishedEventLog(campaign.getId(), campaign.getFinishTime(), campaign.getClientId());
            verify(notificationService, never())
                    .addNotification(getNotificationMatcher(user, campaign));
        }
    }

    private Notification getNotificationMatcher(User user, Campaign campaign) {
        return argThat(v -> beanDiffer(
                new CampFinishedMailNotification()
                        .withCampaignId(campaign.getId())
                        .withCampaignName(campaign.getName())
                        .withAgencyUid(campaign.getAgencyUid())
                        .withClientId(campaign.getClientId())
                        .withClientUserId(user.getUid())
                        .withClientLogin(user.getLogin())
                        .withClientEmail(user.getEmail())
                        .withClientFullName(user.getFio())
                        .withClientLang(user.getLang().getLangString())
                        .withFinishDate(campaign.getFinishTime()))
                .useCompareStrategy(onlyExpectedFields())
                .matches(v));
    }

}
