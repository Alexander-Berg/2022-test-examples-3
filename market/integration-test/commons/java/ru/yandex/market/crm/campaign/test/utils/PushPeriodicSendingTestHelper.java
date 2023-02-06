package ru.yandex.market.crm.campaign.test.utils;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.campaign.domain.grouping.campaign.Campaign;
import ru.yandex.market.crm.campaign.domain.periodic.Schedule;
import ru.yandex.market.crm.campaign.domain.segment.TargetAudience;
import ru.yandex.market.crm.campaign.domain.sending.PushPeriodicSending;
import ru.yandex.market.crm.campaign.domain.sending.conf.PushSendingConf;
import ru.yandex.market.crm.campaign.domain.sending.conf.PushSendingVariantConf;
import ru.yandex.market.crm.campaign.services.segments.SegmentService;
import ru.yandex.market.crm.campaign.services.sending.periodic.PushPeriodicSendingService;
import ru.yandex.market.crm.core.domain.messages.AndroidPushConf;
import ru.yandex.market.crm.core.domain.messages.IosPushConf;
import ru.yandex.market.crm.core.domain.mobile.MobileApplication;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.test.utils.SubscriptionTypes;
import ru.yandex.market.mcrm.db.Constants;

import static ru.yandex.market.crm.core.test.SegmentTestUtils.plusFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;

/**
 * @author apershukov
 */
@Component
public class PushPeriodicSendingTestHelper {

    public static PushSendingConf config(Segment segment) {
        PushSendingConf config = new PushSendingConf();
        config.setApplication(MobileApplication.MARKET_APP);
        config.setTarget(new TargetAudience(LinkingMode.NONE, segment.getId()));
        config.setVariants(List.of(variant(100)));
        config.setSubscriptionType(SubscriptionTypes.STORE_PUSH_GENERAL_ADVERTISING.getId());
        return config;
    }

    public static PushSendingConf config(Segment segment, String action) {
        var config = config(segment);
        for (var variant : config.getVariants()) {
            for (var platformConfig : variant.getPushConfigs().values()) {
                platformConfig.setAction(action);
            }
        }
        return config;
    }

    public static PushSendingVariantConf variant(int percent) {
        PushSendingVariantConf variant = new PushSendingVariantConf();
        variant.setId("variant_a");
        variant.setPercent(percent);
        var pushConfAndroid = new AndroidPushConf();
        pushConfAndroid.setTitle("Test push");
        pushConfAndroid.setText("You got a test push");
        pushConfAndroid.setIconBackground("#ffffff");
        var pushConfIos = new IosPushConf();
        pushConfIos.setTitle("Test push");
        pushConfIos.setText("You got a test push");
        variant.setPushConfigs(Map.of(
                pushConfAndroid.getPlatform(), pushConfAndroid,
                pushConfIos.getPlatform(), pushConfIos
        ));

        return variant;
    }

    private final CampaignTestHelper campaignTestHelper;
    private final PushPeriodicSendingService sendingService;
    private final SegmentService segmentService;

    public PushPeriodicSendingTestHelper(CampaignTestHelper campaignTestHelper,
                                         PushPeriodicSendingService sendingService,
                                         SegmentService segmentService) {
        this.campaignTestHelper = campaignTestHelper;
        this.sendingService = sendingService;
        this.segmentService = segmentService;
    }

    @Transactional(transactionManager = Constants.DEFAULT_TRANSACTION_MANAGER)
    public PushPeriodicSending addSending() {
        Campaign campaign = campaignTestHelper.prepareCampaign();

        Schedule schedule = new Schedule()
                .setTime(LocalTime.of(13, 30))
                .setDaysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.SATURDAY))
                .setActiveInterval(
                        new Schedule.DateTimeInterval()
                                .setBegin(LocalDateTime.now())
                                .setEnd(LocalDateTime.now().plusDays(7))
                );

        PushPeriodicSending sending = new PushPeriodicSending();
        sending.setKey("test_sending_" + IdGenerationUtils.dateTimeId());
        sending.setName("Test Sending");
        sending.setSchedule(schedule);

        return sendingService.addEntity(campaign.getId(), sending);
    }

    @Transactional(transactionManager = Constants.DEFAULT_TRANSACTION_MANAGER)
    public PushPeriodicSending prepareSending(Segment segment) {
        PushSendingConf config = config(segment);
        return prepareSending(config);
    }

    @Transactional(transactionManager = Constants.DEFAULT_TRANSACTION_MANAGER)
    public PushPeriodicSending prepareSending(Segment segment, String action) {
        PushSendingConf config = config(segment, action);
        return prepareSending(config);
    }

    @Transactional(transactionManager = Constants.DEFAULT_TRANSACTION_MANAGER)
    public PushPeriodicSending prepareSending(PushSendingConf config) {
        PushPeriodicSending sending = addSending();
        sending.setConfig(config);
        return sendingService.update(sending);
    }

    public PushSendingConf prepareConfig() {
        Segment segment = segmentService.addSegment(segment(plusFilter()));
        return config(segment);
    }
}
