package ru.yandex.market.crm.triggers.test.helpers;

import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;

import ru.yandex.market.crm.core.domain.messages.CampaignInfo;
import ru.yandex.market.crm.core.domain.messages.EmailMessageConf;
import ru.yandex.market.crm.core.domain.messages.MessageTemplate;
import ru.yandex.market.crm.core.domain.messages.MessageTemplateState;
import ru.yandex.market.crm.core.domain.messages.MessageTemplateType;
import ru.yandex.market.crm.core.domain.sending.conf.BannerBlockConf;
import ru.yandex.market.crm.core.services.messages.MessageTemplatesDAO;
import ru.yandex.market.crm.core.services.messages.TemplateSendrLinkDAO;
import ru.yandex.market.crm.mapreduce.domain.ImageLink;

/**
 * @author apershukov
 */
@Component
public class EmailMessageTemplatesTestHelper {

    public static final String SENDER_CAMPAIGN_SLUG = "sender_campaign_slug";

    private final MessageTemplatesDAO messageTemplatesDAO;
    private final TemplateSendrLinkDAO templateSendrLinkDAO;

    public EmailMessageTemplatesTestHelper(MessageTemplatesDAO messageTemplatesDAO,
                                           TemplateSendrLinkDAO templateSendrLinkDAO) {
        this.messageTemplatesDAO = messageTemplatesDAO;
        this.templateSendrLinkDAO = templateSendrLinkDAO;
    }

    public String prepareMessageTemplate() {
        return prepareMessageTemplate(emailMessageConf -> {});
    }

    public String prepareMessageTemplate(Consumer<EmailMessageConf> messageConfCustomizer) {
        BannerBlockConf block = new BannerBlockConf();
        block.setId("creative");
        block.setBanners(
                Collections.singletonList(
                        new ImageLink("https://yandex.ru/image", "", "")
                )
        );

        EmailMessageConf config = new EmailMessageConf();
        config.setTemplate("default");
        config.setBlocks(Collections.singletonList(block));
        messageConfCustomizer.accept(config);

        var template = new MessageTemplate<EmailMessageConf>();
        template.setType(MessageTemplateType.EMAIL);
        template.setId(UUID.randomUUID().toString());
        template.setName("Test template");
        template.setVersion(1);
        template.setKey(UUID.randomUUID().toString());
        template.setState(MessageTemplateState.PUBLISHED);
        template.setConfig(config);

        messageTemplatesDAO.save(template);

        CampaignInfo campaign = new CampaignInfo();
        campaign.setId(123L);
        campaign.setSlug(SENDER_CAMPAIGN_SLUG);
        campaign.setSenderAccount("market");
        templateSendrLinkDAO.add(template.getId(), campaign);

        return template.getId();
    }
}
