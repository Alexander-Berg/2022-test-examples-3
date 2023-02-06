package ru.yandex.direct.core.entity.campaign.repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.OptimizingCampaignRequest;
import ru.yandex.direct.core.entity.campaign.model.OptimizingCampaignRequestNotificationData;
import ru.yandex.direct.core.entity.campaign.model.OptimizingReqType;
import ru.yandex.direct.core.entity.campaign.model.OptimizingRequestStatus;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.time.LocalDateTime.now;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.dbschema.ppc.Tables.OPTIMIZING_CAMPAIGN_REQUESTS;

@CoreTest
@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class OptimizingCampaignRequestRepositoryTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    /**
     * Через сколько дней после готовности "Первой помощи" отправляем уведомление
     */
    private static final List<Duration> OPTIMIZE_NOTIFICATION_INTERVALS =
            ImmutableList.of(Duration.ofDays(3), Duration.ofDays(14));

    private int shard;
    private long campaignId;
    private User user;

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private OptimizingCampaignRequestRepository repository;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.PERFORMANCE},
                {CampaignType.MOBILE_CONTENT},
                {CampaignType.MCBANNER},
                {CampaignType.DYNAMIC}
        });
    }

    @Before
    public void before() {
        CampaignInfo campaignInfo = steps.campaignSteps().createDefaultCampaignByCampaignType(campaignType);
        user = campaignInfo.getClientInfo().getChiefUserInfo().getUser();
        campaignId = campaignInfo.getCampaignId();
        shard = campaignInfo.getShard();
    }

    private void addOptimizingCampaignRequests(@Nullable LocalDateTime readyTime, OptimizingReqType optimizingReqType) {
        OptimizingCampaignRequest optimizingCampaignRequest = new OptimizingCampaignRequest()
                .withRequestId(shardHelper.generateOptCampRequestIds(1).get(0))
                .withCampaignId(campaignId)
                //Отнимаем секунду, чтобы было строго меньше now()
                .withReadyTime(readyTime == null ? null : readyTime.minusSeconds(1))
                .withBannersCount(1)
                .withReqType(optimizingReqType)
                .withStatus(OptimizingRequestStatus.READY)
                .withIsAutomatic(false)
                .withIsSupport(false)
                .withCreateTime(now());

        repository.addRequests(shard, List.of(optimizingCampaignRequest));
    }

    private void setCreateTimeForOptimizingCampaignRequests(LocalDateTime createTime) {
        dslContextProvider.ppc(shard)
                .update(OPTIMIZING_CAMPAIGN_REQUESTS)
                //Отнимаем секунду, чтобы было строго меньше now()
                .set(OPTIMIZING_CAMPAIGN_REQUESTS.CREATE_TIME, createTime.minusSeconds(1))
                .where(OPTIMIZING_CAMPAIGN_REQUESTS.CID.eq(campaignId))
                .execute();
    }

    @Nullable
    private OptimizingCampaignRequestNotificationData getNotificationData(Long campaignId) {
        return repository.getNotificationsData(shard, OPTIMIZE_NOTIFICATION_INTERVALS)
                .stream()
                .filter(n -> campaignId.equals(n.getCampaignId()))
                .findFirst()
                .orElse(null);
    }


    @Test
    public void checkGetEmptyNotificationsData_whenInDbNotRequestsReadyToSendNotification() {
        int daysToGo = 5; //Отправляем уведомление только если прошло 3 или 14 дней
        LocalDateTime readyTime = now().minusDays(daysToGo);
        addOptimizingCampaignRequests(readyTime, OptimizingReqType.FIRSTAID);

        OptimizingCampaignRequestNotificationData notificationData = getNotificationData(campaignId);

        assertThat("не должны получить параметры уведомления", notificationData, nullValue());
    }

    @Test
    public void checkGetNotificationsDataParams() {
        int daysToGo = 3; //Отправляем уведомление если прошло 3 дня
        LocalDateTime readyTime = now().minusDays(daysToGo);
        OptimizingReqType reqType = OptimizingReqType.FIRSTAID;
        addOptimizingCampaignRequests(readyTime, reqType);

        OptimizingCampaignRequestNotificationData notificationData = getNotificationData(campaignId);

        OptimizingCampaignRequestNotificationData expectedNotificationData = new OptimizingCampaignRequest()
                .withCampaignId(campaignId)
                .withUid(user.getUid())
                .withReqType(reqType)
                .withDaysToGo(daysToGo)
                .withFio(user.getFio())
                .withEmail(user.getEmail());
        assertThat("параметры уведомления соответствуют ожиданиям",
                notificationData, beanDiffer(expectedNotificationData));
    }

    @Test
    public void checkGetNotificationsData_whenReadyTimeIsNull() {
        int daysToGo = 14;  //Отправляем уведомление если прошло 14 дней
        LocalDateTime createTime = now().minusDays(daysToGo);
        OptimizingReqType reqType = OptimizingReqType.SECONDAID;
        addOptimizingCampaignRequests(null, reqType);
        setCreateTimeForOptimizingCampaignRequests(createTime);

        OptimizingCampaignRequestNotificationData notificationData = getNotificationData(campaignId);

        OptimizingCampaignRequestNotificationData expectedNotificationData = new OptimizingCampaignRequest()
                .withCampaignId(campaignId)
                .withUid(user.getUid())
                .withReqType(reqType)
                .withDaysToGo(daysToGo)
                .withFio(user.getFio())
                .withEmail(user.getEmail());
        assertThat("параметры уведомления соответствуют ожиданиям",
                notificationData, beanDiffer(expectedNotificationData));
    }
}
