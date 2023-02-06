package ru.yandex.market.crm.campaign.test.utils;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.springframework.stereotype.Component;

import ru.yandex.market.crm.campaign.domain.grouping.campaign.Campaign;
import ru.yandex.market.crm.campaign.domain.periodic.Schedule;
import ru.yandex.market.crm.campaign.domain.periodic.Schedule.DateTimeInterval;
import ru.yandex.market.crm.campaign.domain.segment.TargetAudience;
import ru.yandex.market.crm.campaign.domain.sending.EmailPeriodicSending;
import ru.yandex.market.crm.campaign.domain.sending.conf.EmailSendingConf;
import ru.yandex.market.crm.campaign.domain.sending.conf.EmailSendingVariantConf;
import ru.yandex.market.crm.campaign.services.grouping.campaign.CampaignDAO;
import ru.yandex.market.crm.campaign.services.segments.SegmentService;
import ru.yandex.market.crm.campaign.services.sending.periodic.EmailPeriodicSendingService;
import ru.yandex.market.crm.core.domain.references.SendingReturnAddress;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;

import static ru.yandex.market.crm.campaign.test.utils.EmailSendingConfigUtils.variant;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.passportGender;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;
import static ru.yandex.market.crm.core.test.utils.SubscriptionTypes.ADVERTISING;

/**
 * @author apershukov
 */
@Component
public class EmailPeriodicSendingTestHelper {

    public static final String DEFAULT_VARIANT = "variant_a";
    public static final String DEFAULT_SUBJECT = "Default Subject";

    private final SegmentService segmentService;
    private final EmailPeriodicSendingService sendingService;
    private final CampaignDAO campaignDAO;
    private final BlockTemplateTestHelper blockTemplateTestHelper;
    private final AtomicInteger preparedCounter;

    public EmailPeriodicSendingTestHelper(SegmentService segmentService,
                                          EmailPeriodicSendingService sendingService,
                                          CampaignDAO campaignDAO,
                                          BlockTemplateTestHelper blockTemplateTestHelper) {
        this.segmentService = segmentService;
        this.sendingService = sendingService;
        this.campaignDAO = campaignDAO;
        this.blockTemplateTestHelper = blockTemplateTestHelper;
        this.preparedCounter = new AtomicInteger(0);
    }

    @Nonnull
    public EmailPeriodicSending prepareSending() {
        Campaign campaign = prepareCampaign();
        return prepareSending(campaign, c -> {});
    }

    @Nonnull
    public EmailPeriodicSending prepareSending(Campaign campaign) {
        return prepareSending(campaign, c -> {});
    }

    @Nonnull
    public EmailPeriodicSending prepareSending(Consumer<EmailPeriodicSending> customizer) {
        Campaign campaign = prepareCampaign();
        return prepareSending(campaign, customizer);
    }

    @Nonnull
    public EmailPeriodicSending prepareSending(Campaign campaign, Consumer<EmailPeriodicSending> customizer) {
        Segment segment = segmentService.addSegment(segment(
                passportGender("m")
        ));

        return prepareSending(campaign, segment, customizer);
    }

    @Nonnull
    public EmailPeriodicSending prepareSending(Campaign campaign,
                                               Segment segment,
                                               Consumer<EmailPeriodicSending> customizer) {
        Schedule schedule = new Schedule()
                .setTime(LocalTime.of(13, 30))
                .setDaysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.SATURDAY))
                .setActiveInterval(
                        new DateTimeInterval()
                                .setBegin(LocalDateTime.now())
                                .setEnd(LocalDateTime.now().plusDays(7))
                );

        EmailPeriodicSending sending = new EmailPeriodicSending();

        String key = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString() + preparedCounter.incrementAndGet();
        sending.setKey(key);

        sending.setName("Test sending");
        sending.setSchedule(schedule);

        sending = sendingService.addEntity(campaign.getId(), sending);

        SendingReturnAddress address = new SendingReturnAddress();
        address.setId(UUID.randomUUID().toString());
        address.setEmail("adv@market.yandex.ru");
        address.setName("Test return address");

        EmailSendingConf config = new EmailSendingConf();
        config.setTarget(new TargetAudience(LinkingMode.NONE, segment.getId()));
        config.setSubscriptionType(ADVERTISING.getId());
        config.setFrom(address);
        config.setReplyTo(address);

        EmailSendingVariantConf variant = variant(
                DEFAULT_VARIANT,
                100,
                blockTemplateTestHelper.prepareMessageTemplate(),
                DEFAULT_SUBJECT,
                blockTemplateTestHelper.prepareCreativeBlock()
        );

        config.setVariants(List.of(variant));

        sending.setConfig(config);

        customizer.accept(sending);

        return sendingService.update(sending);
    }

    @Nonnull
    public Campaign prepareCampaign() {
        Campaign campaign = new Campaign();
        campaign.setName("Test campaign");
        campaign = campaignDAO.insert(campaign);
        return campaign;
    }
}
