package ru.yandex.calendar.logic.contact.ml;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.calendar.boot.CalendarContextConfiguration;
import ru.yandex.calendar.test.CalendarTestBase;
import ru.yandex.calendar.test.generic.CalendarSpringJUnit4ClassRunner;
import ru.yandex.calendar.test.generic.CalendarTestInitContextConfiguration;
import ru.yandex.calendar.test.generic.MeterRegistryStubTestConfiguration;
import ru.yandex.calendar.test.generic.TvmClientTestConfiguration;
import ru.yandex.calendar.tvm.TvmClient;
import ru.yandex.calendar.util.HttpClientConfiguration;
import ru.yandex.misc.email.Email;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(
        loader = AnnotationConfigContextLoader.class,
        classes = {
                CalendarContextConfiguration.class,
                TestMlConfiguration.class,
                TvmClientTestConfiguration.class
        }
)
@RunWith(CalendarSpringJUnit4ClassRunner.class)
public class MlTest extends CalendarTestBase {
    @Autowired
    private Ml ml;

    @Test
    @Ignore
    public void listsInfo() {
        Email email = new Email("meetings-notifiacations@yandex-team.ru");
        final MailLists actual = ml.listsInfo(Cf.arrayList(email), true);
        final MapF<Email, MailList> lists = actual.getLists();

        final ListF<Email> keys = lists.keys().sorted();
        final ListF<Email> correctedKeys = Cf.arrayList(
                new Email("meetings-notifiacations@yandex-team.ru")
        );
        assertThat(keys.size()).isEqualTo(1);
        assertThat(keys).isEqualTo(correctedKeys);

        final MailList mailList = lists.getOrThrow(keys.first());
        final ListF<Email> correctedSubscribers = Cf.arrayList(
                new Email("g-s-v@yandex-team.ru"),
                new Email("selivanov@yandex-team.ru")
        );
        final ListF<Email> subscribers = mailList.getSubscribers().map(Subscriber::getEmail);
        assertThat(subscribers.size()).isEqualTo(2);
        assertThat(subscribers).isEqualTo(correctedSubscribers);
    }
}

@Configuration
@Import({CalendarTestInitContextConfiguration.class,
        MeterRegistryStubTestConfiguration.class})
class TestMlConfiguration {
    @Autowired
    private TvmClient tvmClient;

    @Bean
    public Ml ml(MeterRegistry meterRegistry,
                 @Value("${yt.ml.url}") String mlUrl,
                 @Value("${yt.ml.tvm-id}") int mlTVMid) {
        final HttpClientConfiguration httpClientConfiguration = HttpClientConfiguration.forTest();
        return new Ml(mlUrl, httpClientConfiguration, meterRegistry, tvmClient, mlTVMid);
    }
}
