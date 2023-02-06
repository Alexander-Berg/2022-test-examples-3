package ru.yandex.market.crm.campaign.test.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import ru.yandex.market.crm.campaign.services.messages.MessageTemplatesService;
import ru.yandex.market.crm.campaign.services.templates.BlockTemplateService;
import ru.yandex.market.crm.core.domain.messages.EmailMessageConf;
import ru.yandex.market.crm.core.domain.messages.MessageTemplate;
import ru.yandex.market.crm.core.domain.messages.MessageTemplateState;
import ru.yandex.market.crm.core.domain.messages.MessageTemplateType;
import ru.yandex.market.crm.core.domain.sending.conf.BannerBlockConf;
import ru.yandex.market.crm.core.domain.sending.conf.BlockConf;
import ru.yandex.market.crm.core.domain.templates.BlockTemplate;
import ru.yandex.market.crm.core.domain.templates.TemplateType;
import ru.yandex.market.crm.core.services.messages.MessageTemplatesDAO;
import ru.yandex.market.crm.mapreduce.domain.ImageLink;

/**
 * @author apershukov
 */
@Component
public class EmailTemplatesTestHelper {

    @Inject
    private MessageTemplatesService templateService;

    @Inject
    private BlockTemplateService blockTemplateService;

    @Inject
    private MessageTemplatesDAO messageTemplatesDAO;

    public MessageTemplate<EmailMessageConf> prepareEmailTemplate(String subject, BlockConf... blocks) {
        BlockTemplate messageTemplate = prepareMessageTemplate();

        EmailMessageConf config = new EmailMessageConf();
        config.setTemplate(messageTemplate.getId());
        config.setBlocks(Arrays.asList(blocks));
        config.setSubject(subject);

        return prepareEmailTemplate(config);
    }

    public MessageTemplate<EmailMessageConf> prepareEmailTemplate(EmailMessageConf config) {
        var template = new MessageTemplate<EmailMessageConf>();
        template.setType(MessageTemplateType.EMAIL);
        template.setName("Test template");
        template.setConfig(config);

        return (MessageTemplate<EmailMessageConf>) templateService.addTemplate(template);
    }

    public BlockTemplate prepareMessageTemplate() {
        BlockTemplate messageTemplate = new BlockTemplate();
        messageTemplate.setType(TemplateType.HEAD);
        messageTemplate.setName("Test message template");
        messageTemplate.setBody("<div>${renderBlocks('''''')}</div>");
        messageTemplate = blockTemplateService.saveTemplate(messageTemplate);
        return messageTemplate;
    }

    public MessageTemplate<EmailMessageConf> prepareEmailTemplate() {
        return prepareEmailTemplate("Subject", prepareBannerBlock(block -> {
        }));
    }

    public BannerBlockConf prepareBannerBlock(@Nonnull Consumer<BannerBlockConf> customiser) {
        BlockTemplate blockTemplate = new BlockTemplate();
        blockTemplate.setType(TemplateType.BANNER);
        blockTemplate.setName("Test template");
        blockTemplate.setBody(
                """
                        <div>
                            <a href=${data.banners[0].link}>
                                <img src="${data.banners[0].img}"/>
                            </a>
                            <p>${data.text}</p>
                        </div>"""
        );
        blockTemplate = blockTemplateService.saveTemplate(blockTemplate);

        BannerBlockConf bannerBlock = new BannerBlockConf();
        bannerBlock.setId("creative");
        bannerBlock.setTemplate(blockTemplate.getId());
        bannerBlock.setBanners(Collections.singletonList(
                new ImageLink(
                        "http://market.yandex.ru/product/111",
                        "http://market.yandex.ru/images/qwerty",
                        ""
                )
        ));

        customiser.accept(bannerBlock);

        return bannerBlock;
    }

    public BannerBlockConf prepareBannerBlock() {
        return prepareBannerBlock(x -> {
        });
    }

    public MessageTemplate<EmailMessageConf> createNewVersion(MessageTemplate<EmailMessageConf> template) {
        template.setState(MessageTemplateState.PUBLISHED);
        messageTemplatesDAO.update(template);
        return (MessageTemplate<EmailMessageConf>) templateService.update(template.getKey(), template);
    }
}
