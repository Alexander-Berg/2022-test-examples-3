package ru.yandex.market.crm.campaign.services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.hubspot.jinjava.Jinjava;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.owasp.html.Htmls;

import ru.yandex.market.crm.campaign.services.messages.MessageTemplatesService;
import ru.yandex.market.crm.campaign.test.AbstractServiceMediumTest;
import ru.yandex.market.crm.campaign.test.utils.BlockTemplateTestHelper;
import ru.yandex.market.crm.campaign.test.utils.EmailTemplatesTestHelper;
import ru.yandex.market.crm.campaign.test.utils.JinjavaSender;
import ru.yandex.market.crm.core.domain.messages.EmailMessageConf;
import ru.yandex.market.crm.core.domain.messages.MessageTemplate;
import ru.yandex.market.crm.core.domain.sending.conf.InfoBlockConf;
import ru.yandex.market.crm.core.services.external.yandexsender.CampaignResult;
import ru.yandex.market.crm.core.test.utils.YaSenderHelper;
import ru.yandex.market.mcrm.utils.Maps;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author zloddey
 */
public class EmailTemplateRenderingTest extends AbstractServiceMediumTest {

    private final Jinjava jinjava = new JinjavaSender();
    @Inject
    private MessageTemplatesService service;
    @Inject
    private EmailTemplatesTestHelper emailTemplatesTestHelper;
    @Inject
    private BlockTemplateTestHelper blockTemplateTestHelper;
    @Inject
    private YaSenderHelper yaSenderHelper;

    @Test
    public void renderHref() {
        InfoBlockConf infoBlock = blockTemplateTestHelper.prepareInfoBlock(getResource("href-input.html"));
        var template = emailTemplatesTestHelper.prepareEmailTemplate("subj", infoBlock);
        String templateBody = publishTemplate(template);

        Map<String, Object> context = map(
                "data", map(
                        "blocks", list(map("id", infoBlock.getId())),
                        "utm_campaign", "test"
                ),
                "url", "https://pokupki.market.yandex.ru",
                "url_fragment", "https://pokupki.market.yandex.ru#fragment",
                "model", "zemlecherpalka9000",
                "sku", "76287462727424"
        );
        String renderedTemplate = jinjava.render(templateBody, context);

        List<String> extractedUrls = new ArrayList<>();
        Htmls.processUrls(renderedTemplate, s -> {
            extractedUrls.add(s);
            return s;
        });
        assertEquals(list(
                withUtm("https://yandex.ru"),
                withUtm("https://pokupki.market.yandex.ru/product/zemlecherpalka9000/76287462727424"),
                withUtm("https://pokupki.market.yandex.ru"),
                // Добавление utm-меток к ссылке с фрагментом - баг, который мы как раз хотим поправить в LILUCRM-3404
                // А пока исправление не готово, мы просто фиксируем текущее (неправильное!) поведение
                withUtm("https://pokupki.market.yandex.ru#fragment")),
                extractedUrls);
    }

    private String withUtm(String s) {
        return s + "?utm_campaign=test&utm_source=&utm_medium=&utm_referrer=&eh=&ecid=&clid=";
    }

    private String publishTemplate(MessageTemplate<EmailMessageConf> template) {
        List<String> receivedBody = new ArrayList<>();
        yaSenderHelper.onCreateTxCampaign((campaign) -> {
            receivedBody.add(campaign.getLetterBody());
            CampaignResult response = new CampaignResult();
            response.setId(12312312L);
            response.setSlug("whataslug");
            return response;
        });
        service.publish(template.getId());
        assertEquals(1, receivedBody.size());
        return receivedBody.get(0);
    }

    private Map<String, Object> map(Object... keysAndValues) {
        return Maps.of(keysAndValues);
    }

    private List<Object> list(Object... items) {
        return List.of(items);
    }

    private String getResource(String path) {
        try {
            return IOUtils.toString(
                    getClass().getResourceAsStream(path),
                    StandardCharsets.UTF_8
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
