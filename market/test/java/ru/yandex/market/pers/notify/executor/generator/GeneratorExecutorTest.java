package ru.yandex.market.pers.notify.executor.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.market.pers.notify.ems.generator.NotificationEventSourceGenerator;
import ru.yandex.market.pers.notify.ems.persistence.NotificationEventService;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.NotificationTransportType;
import ru.yandex.market.pers.notify.model.Uid;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.pers.notify.model.push.PushTemplateType;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         28.04.16
 */
public class GeneratorExecutorTest extends MarketMailerMockedDbTest {
    @Autowired
    private NotificationEventService notificationEventService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private GeneratorExecutor generatorExecutor;

    @BeforeEach
    public void setUp() {
        notificationEventService = spy(notificationEventService);

        generatorExecutor = new GeneratorExecutor();
        generatorExecutor.setNotificationEventService(notificationEventService);
    }

    @Test
    public void doJob() {
        doReturn(true).when(notificationEventService).populateByMobileAppInfo(any());

        int uid = RND.nextInt(100_000);
        String email = UUID.randomUUID() + "@yandex.ru";
        String uuid = UUID.randomUUID().toString();
        List<NotificationEventSource> sources = new ArrayList<>();
        int count = RND.nextInt(1000) + 100;
        List<NotificationSubtype> mailNotificationSubtypes = Arrays.stream(NotificationSubtype.values())
            .filter(s -> s.getTransportType() == NotificationTransportType.MAIL)
            .collect(Collectors.toList());

        List<PushTemplateType> pushTemplateTypes = new ArrayList<>(Arrays.asList(PushTemplateType.values()));

        for (int i = 0; i < count; i++) {
            NotificationSubtype subtype = mailNotificationSubtypes.get(RND.nextInt(mailNotificationSubtypes.size()));
            sources.add(NotificationEventSource.fromIdentity(new Uid((long) uid), email, subtype).setUuid(uuid).build());
        }

        for (int i = 0; i < count; i++) {
            PushTemplateType subtype = pushTemplateTypes.get(RND.nextInt(pushTemplateTypes.size()));
            sources.add(NotificationEventSource.push((long) uid, uuid, null, subtype).build());
        }

        long differentSubtypes = sources.stream()
            .map(NotificationEventSource::getNotificationSubtype)
            .distinct()
            .count();

        generatorExecutor.setGenerator(new NotificationEventSourceGenerator() {
            @Override
            public Stream<NotificationEventSource> generate() {
                return sources.stream();
            }
        });

        generatorExecutor.doRealJob(null);

        assertEquals(differentSubtypes, (int)
            Optional.ofNullable(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM EVENT_SOURCE WHERE EMAIL = ?", Integer.class, email)).orElse(0) +
            Optional.ofNullable(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM EVENT_SOURCE WHERE EMAIL = ?", Integer.class, uuid)).orElse(0));
    }
}
