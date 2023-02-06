package ru.yandex.market.notifier;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.notifier.application.AbstractServicesTestBase;
import ru.yandex.market.notifier.core.TemplateResolver;
import ru.yandex.market.notifier.entity.ChannelType;
import ru.yandex.market.notifier.entity.DeliveryChannel;
import ru.yandex.market.notifier.entity.Notification;
import ru.yandex.market.notifier.xml.DocumentValuesExtractor;
import ru.yandex.market.notifier.xml.XmlSourceDocument;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author kukabara
 */
public class TemplateResolverTest extends AbstractServicesTestBase {

    @Autowired
    private ResourceLoadUtil resourceLoadUtil;
    @Autowired
    private TemplateResolver templateResolver;

    @Test
    public void testTemplateResolving() throws Exception {
        List<Notification> notifications = resourceLoadUtil.getSampleNotifications();
        for (Notification note : notifications) {
            for (DeliveryChannel dc : note.getDeliveryChannels()) {
                String noteType = note.getType();
                String noteData = note.getData();

                ChannelType channelType = dc.getType();
                if (ChannelType.EMAIL == channelType) {
                    continue;
                }
                DocumentValuesExtractor dve = templateResolver.getValueExtractor(channelType, noteType);
                assertNotNull(dve);
                Map<String, String> result;
                if (noteData != null && noteData.trim().length() > 0) {
                    result = dve.extract(new XmlSourceDocument(noteData.trim()));
                } else {
                    result = dve.extract(null);
                }
                assertNotNull(result);
                System.out.println(result);

            }

        }
    }
}
