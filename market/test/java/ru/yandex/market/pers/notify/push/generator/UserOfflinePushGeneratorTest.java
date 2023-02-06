package ru.yandex.market.pers.notify.push.generator;

import freemarker.template.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.pers.notify.ems.persistence.NotificationEventService;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.pers.notify.push.PushReplacingConsumer;
import ru.yandex.market.pers.notify.templates.FTLoaderFactory;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         30.12.15
 */
public class UserOfflinePushGeneratorTest extends MarketMailerMockedDbTest {
	@Autowired
	private UserOfflinePushGenerator userOfflinePushGenerator;
	@Autowired
	private NotificationEventService notificationEventService;
	@Autowired
    private PushReplacingConsumer pushReplacingConsumer;

    @Autowired
	private Configuration configuration;
	@Autowired
	private FTLoaderFactory ftLoaderFactory;
	
	@BeforeEach
	public void init() throws Exception {
		configuration.setTemplateLoader(ftLoaderFactory.getNewTemplateLoader());
	}
	
	@Test
	public void testAll() throws Exception {
        List<NotificationEventSource> sourceList = userOfflinePushGenerator.generate().collect(Collectors.toList());
        System.out.println(sourceList);
        for (NotificationEventSource source : sourceList) {
            System.out.println(pushReplacingConsumer.processEvent(notificationEventService.addEvent(source)).getStatus());
        }
	}
}
