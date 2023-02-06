package ru.yandex.market.crm.campaign.test.utils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.campaign.domain.grouping.campaign.Campaign;
import ru.yandex.market.crm.campaign.domain.segment.TargetAudience;
import ru.yandex.market.crm.campaign.domain.sending.PushPlainSending;
import ru.yandex.market.crm.campaign.domain.sending.conf.PushSendingConf;
import ru.yandex.market.crm.campaign.domain.sending.conf.PushSendingVariantConf;
import ru.yandex.market.crm.campaign.services.grouping.campaign.CampaignDAO;
import ru.yandex.market.crm.campaign.services.sending.PushPlainSendingService;
import ru.yandex.market.crm.core.domain.messages.AndroidPushConf;
import ru.yandex.market.crm.core.domain.messages.IosPushConf;
import ru.yandex.market.crm.core.domain.mobile.MobileApplication;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.test.utils.SubscriptionTypes;
import ru.yandex.market.mcrm.db.Constants;

/**
 * @author apershukov
 */
@Component
public class PushSendingTestHelper {

    public static PushSendingVariantConf variant(String id, int percent, String title) {
        PushSendingVariantConf variant = new PushSendingVariantConf();
        variant.setPercent(percent);

        var pushConfAndroid = new AndroidPushConf();
        pushConfAndroid.setTitle(title);
        pushConfAndroid.setText("Push text");
        pushConfAndroid.setIconBackground("#ffffff");

        var pushConfIos = new IosPushConf();
        pushConfIos.setTitle(title);
        pushConfIos.setText("Push text");

        variant.setPushConfigs(Map.of(
                pushConfAndroid.getPlatform(), pushConfAndroid,
                pushConfIos.getPlatform(), pushConfIos
        ));
        variant.setId(id);
        return variant;
    }

    public static PushSendingVariantConf variant(String id, int percent, String title, String action) {
        var variant = variant(id, percent, title);
        for (var conf : variant.getPushConfigs().values()) {
            conf.setAction(action);
        }
        return variant;
    }

    public static PushSendingVariantConf variant(String id, int percent) {
        return variant(id, percent, "Push Title");
    }

    public static PushSendingVariantConf variant() {
        return variant(VARIANT_ID, 100);
    }

    public static PushSendingConf config(Segment segment, PushSendingVariantConf... variants) {
        PushSendingConf config = new PushSendingConf();
        config.setApplication(MobileApplication.MARKET_APP);
        config.setVariants(Arrays.asList(variants));
        config.setTarget(new TargetAudience(LinkingMode.NONE, segment.getId()));
        config.setSubscriptionType(SubscriptionTypes.STORE_PUSH_GENERAL_ADVERTISING.getId());
        return config;
    }

    private static final String VARIANT_ID = "variant_a";

    @Inject
    private CampaignDAO campaignDAO;

    @Inject
    private PushPlainSendingService pushSendingService;

    @Transactional(transactionManager = Constants.DEFAULT_TRANSACTION_MANAGER)
    public PushPlainSending prepareSending(PushSendingConf config) {
        Campaign campaign = new Campaign();
        campaign.setName("Test Campaign");
        campaign = campaignDAO.insert(campaign);

        PushPlainSending sending = new PushPlainSending();
        sending.setName("Test sending");
        sending.setCampaignId(campaign.getId());
        sending.setId(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS) + "_" + UUID.randomUUID());

        pushSendingService.addSending(sending);

        sending.setConfig(config);
        pushSendingService.forceUpdate(sending.getId(), sending);

        return sending;
    }

    public PushPlainSending prepareSending(Segment segment) {
        var config = config(segment, variant());
        return prepareSending(config);
    }

    public void updateSending(PushPlainSending sending) {
        pushSendingService.forceUpdate(sending.getId(), sending);
    }
}
