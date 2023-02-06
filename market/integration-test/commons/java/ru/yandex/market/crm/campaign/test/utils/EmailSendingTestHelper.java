package ru.yandex.market.crm.campaign.test.utils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;

import javax.annotation.Nonnull;

import com.google.common.base.Strings;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.campaign.domain.grouping.campaign.Campaign;
import ru.yandex.market.crm.campaign.domain.segment.TargetAudience;
import ru.yandex.market.crm.campaign.domain.sending.EmailPlainSending;
import ru.yandex.market.crm.campaign.domain.sending.SendingStage;
import ru.yandex.market.crm.campaign.domain.sending.conf.EmailSendingConf;
import ru.yandex.market.crm.campaign.domain.sending.conf.EmailSendingVariantConf;
import ru.yandex.market.crm.campaign.domain.workflow.StageStatus;
import ru.yandex.market.crm.campaign.services.segments.SegmentService;
import ru.yandex.market.crm.campaign.services.sending.EmailDefaultConfigGenerator;
import ru.yandex.market.crm.campaign.services.sending.EmailPlainSendingService;
import ru.yandex.market.crm.campaign.services.sending.EmailSendingDAO;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.domain.sending.conf.BlockConf;
import ru.yandex.market.crm.core.domain.sending.conf.ModelBlockConf;
import ru.yandex.market.crm.core.domain.templates.BlockTemplate;
import ru.yandex.market.crm.core.domain.templates.TemplateType;
import ru.yandex.market.mcrm.db.Constants;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static ru.yandex.market.crm.campaign.test.utils.EmailSendingConfigUtils.variant;

/**
 * @author apershukov
 */
@Component
public class EmailSendingTestHelper {

    private final CampaignTestHelper campaignTestHelper;
    private final EmailSendingDAO emailSendingDAO;
    private final BlockTemplateTestHelper blockTemplateTestHelper;
    private final SegmentService segmentService;
    private final EmailSendingDAO sendingDAO;
    private final EmailPlainSendingService emailSendingService;
    private final EmailDefaultConfigGenerator emailDefaultConfigGenerator;

    public EmailSendingTestHelper(CampaignTestHelper campaignTestHelper,
                                  EmailSendingDAO emailSendingDAO,
                                  BlockTemplateTestHelper blockTemplateTestHelper,
                                  SegmentService segmentService,
                                  EmailSendingDAO sendingDAO,
                                  EmailPlainSendingService emailSendingService,
                                  EmailDefaultConfigGenerator emailDefaultConfigGenerator) {
        this.campaignTestHelper = campaignTestHelper;
        this.emailSendingDAO = emailSendingDAO;
        this.blockTemplateTestHelper = blockTemplateTestHelper;
        this.segmentService = segmentService;
        this.sendingDAO = sendingDAO;
        this.emailSendingService = emailSendingService;
        this.emailDefaultConfigGenerator = emailDefaultConfigGenerator;
    }

    @Nonnull
    public EmailPlainSending prepareSending(Segment segment,
                                            LinkingMode linkingMode,
                                            BlockConf blockConf) {
        String templateId = blockTemplateTestHelper.prepareMessageTemplate();

        return prepareSending(
                segment,
                linkingMode,
                variant("variant_a", 100, templateId, blockConf)
        );
    }

    @Nonnull
    @Transactional(transactionManager = Constants.DEFAULT_TRANSACTION_MANAGER)
    public EmailPlainSending prepareSending(Segment segment,
                                            LinkingMode linkingMode,
                                            EmailSendingVariantConf... variants) {
        segment = segmentService.addSegment(segment);
        return prepareSending(prepareConfig(segment, linkingMode, variants));
    }

    public EmailSendingConf prepareConfig(Segment segment,
                                          LinkingMode linkingMode,
                                          EmailSendingVariantConf... variants) {
        EmailSendingConf config = emailDefaultConfigGenerator.generate();
        config.setSubscriptionType(2L);
        config.setTarget(new TargetAudience(linkingMode, segment.getId()));
        config.setVariants(Arrays.asList(variants));
        config.setGlobalControlEnabled(false);

        return config;
    }

    @Nonnull
    @Transactional(transactionManager = Constants.DEFAULT_TRANSACTION_MANAGER)
    public EmailPlainSending prepareSending(EmailSendingConf config) {
        Campaign campaign = campaignTestHelper.prepareCampaign();

        EmailPlainSending sending = new EmailPlainSending();
        sending.setCampaignId(campaign.getId());
        sending.setId(IdGenerationUtils.dateTimeId());
        sending.setName("sending");

        emailSendingService.addSending(sending);

        sending.setConfig(config);
        updateSending(sending);

        sending.setStageAndStatus(SendingStage.GENERATE, StageStatus.FINISHED);
        sendingDAO.updateSendingStates(sending.getId(), sending);
        return sending;
    }

    @Transactional(transactionManager = Constants.DEFAULT_TRANSACTION_MANAGER)
    public EmailPlainSending saveSending() {
        BlockTemplate blockTemplate = blockTemplateTestHelper.saveBlockTemplate(
                "Image template",
                TemplateType.MODEL,
                "models.html"
        );

        BlockTemplate bodyTemplate = blockTemplateTestHelper.saveBlockTemplate(
                "Body",
                TemplateType.FOOTER,
                "body.html"
        );

        Campaign campaign = campaignTestHelper.prepareCampaign();

        ModelBlockConf blockConf = new ModelBlockConf();
        blockConf.setId("banner_block");
        blockConf.setTemplate(blockTemplate.getId());

        EmailSendingVariantConf variant = new EmailSendingVariantConf();
        variant.setId("variant_a");
        variant.setTemplate(bodyTemplate.getId());
        variant.setBlocks(Collections.singletonList(blockConf));
        variant.setPercent(100);

        EmailSendingConf config = new EmailSendingConf();
        config.setSubscriptionType(2L);
        config.setVariants(Collections.singletonList(variant));

        EmailPlainSending emailSending = new EmailPlainSending();
        emailSending.setId(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString());
        emailSending.setCampaignId(campaign.getId());
        emailSending.setName("Test Sending");
        emailSending.setConfig(config);

        emailSendingDAO.createSending(emailSending);

        return emailSending;
    }

    public void updateSending(EmailPlainSending sending) {
        emailSendingService.forceUpdate(sending.getId(), sending);
    }

    public void waitGenerated(String sendingId) throws InterruptedException {
        long startTime = System.currentTimeMillis();

        while (true) {
            if (System.currentTimeMillis() - startTime > 900_000) {
                fail("Generation wait timeout");
            }

            EmailPlainSending sending = sendingDAO.getSending(sendingId);

            assertEquals("Sending is not generating", SendingStage.GENERATE, sending.getStage());
            assertTrue(sending.getMessage(), Strings.isNullOrEmpty(sending.getMessage()));

            StageStatus status = sending.getStageStatus();

            assertNotEquals("Generation is failed", StageStatus.ERROR, status);

            if (status == StageStatus.FINISHED) {
                return;
            }

            Thread.sleep(1_000);
        }
    }
}
