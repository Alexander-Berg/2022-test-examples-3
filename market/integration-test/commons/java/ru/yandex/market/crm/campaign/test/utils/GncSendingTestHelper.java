package ru.yandex.market.crm.campaign.test.utils;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.campaign.domain.grouping.campaign.Campaign;
import ru.yandex.market.crm.campaign.domain.segment.TargetAudience;
import ru.yandex.market.crm.campaign.domain.sending.GncPlainSending;
import ru.yandex.market.crm.campaign.domain.sending.conf.GncSendingConf;
import ru.yandex.market.crm.campaign.domain.sending.conf.GncSendingVariantConf;
import ru.yandex.market.crm.campaign.services.grouping.campaign.CampaignDAO;
import ru.yandex.market.crm.campaign.services.sending.GncSendingService;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.mcrm.db.Constants;

/**
 * @author vtarasoff
 * @since 21.09.2020
 */
@Service
public class GncSendingTestHelper {
    private final GncSendingService gncSendingService;
    private final CampaignDAO campaignDAO;

    public GncSendingTestHelper(GncSendingService gncSendingService, CampaignDAO campaignDAO) {
        this.gncSendingService = gncSendingService;
        this.campaignDAO = campaignDAO;
    }

    @Transactional(transactionManager = Constants.DEFAULT_TRANSACTION_MANAGER)
    public GncPlainSending prepareSending(Segment segment) {
        GncPlainSending sending = new GncPlainSending();
        sending.setId(IdGenerationUtils.randomId());
        sending.setName("Gnc " + sending.getId());
        sending.setCampaignId(prepareCampaign().getId());

        gncSendingService.addSending(sending);

        sending.setConfig(prepareConfig(segment));

        gncSendingService.forceUpdate(sending.getId(), sending);

        return sending;
    }

    private GncSendingConf prepareConfig(Segment segment) {
        GncSendingVariantConf variant = new GncSendingVariantConf();
        variant.setId("variant_a");
        variant.setPercent(100);

        GncSendingConf config = new GncSendingConf();
        config.setTarget(new TargetAudience(LinkingMode.NONE, segment.getId()));
        config.setVariants(List.of(variant));
        config.setSubscriptionType(0L);

        return config;
    }

    private Campaign prepareCampaign() {
        Campaign campaign = new Campaign();
        campaign.setName("Campaign");

        campaign = campaignDAO.insert(campaign);

        return campaign;
    }
}
