package ru.yandex.market.crm.campaign.test.utils;

import java.util.Map;

import org.springframework.stereotype.Component;

import ru.yandex.market.crm.campaign.services.messages.MessageTemplatesService;
import ru.yandex.market.crm.core.domain.messages.AndroidPushConf;
import ru.yandex.market.crm.core.domain.messages.MessageTemplate;
import ru.yandex.market.crm.core.domain.messages.MessageTemplateType;
import ru.yandex.market.crm.core.domain.messages.PushMessageConf;
import ru.yandex.market.crm.core.domain.mobile.MobileApplication;

/**
 * @author apershukov
 */
@Component
public class PushTemplatesTestHelper {

    private final MessageTemplatesService templateService;

    public PushTemplatesTestHelper(MessageTemplatesService templateService) {
        this.templateService = templateService;
    }

    public MessageTemplate<PushMessageConf> prepare() {
        return prepare("Test push title");
    }

    public MessageTemplate<PushMessageConf> prepare(String title) {
        AndroidPushConf pushConf = new AndroidPushConf();
        pushConf.setTitle(title);
        pushConf.setText("Test push text");

        PushMessageConf config = new PushMessageConf();
        config.setApplication(MobileApplication.MARKET_APP);
        config.setPushConfigs(Map.of(pushConf.getPlatform(), pushConf));
        return prepare(config);
    }

    public MessageTemplate<PushMessageConf> prepare(PushMessageConf config) {
        var template = new MessageTemplate<PushMessageConf>();
        template.setType(MessageTemplateType.PUSH);
        template.setName("Test template");
        template.setConfig(config);

        return (MessageTemplate<PushMessageConf>) templateService.addTemplate(template);
    }
}
