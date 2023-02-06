package ru.yandex.calendar.logic.user;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.boot.CalendarContextConfiguration;
import ru.yandex.calendar.test.CalendarTestBase;
import ru.yandex.calendar.test.generic.CalendarSpringJUnit4ClassRunner;
import ru.yandex.calendar.test.generic.CalendarTestInitContextConfiguration;
import ru.yandex.calendar.test.generic.MeterRegistryStubTestConfiguration;
import ru.yandex.calendar.test.generic.TvmClientTestConfiguration;
import ru.yandex.calendar.util.HttpClientConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(
        loader = AnnotationConfigContextLoader.class,
        classes = {
                CalendarContextConfiguration.class,
                TestAvatarManagerConfiguration.class,
                TvmClientTestConfiguration.class
        }
)
@RunWith(CalendarSpringJUnit4ClassRunner.class)
public class AvatarManagerTest extends CalendarTestBase {
    @Autowired
    private AvatarManager avatarManager;

    @org.junit.Test
    public void getAvatarSafe() {
        final Option<Avatar> existedAvatar = avatarManager.getAvatarSafe("volozh");
        assertThat(existedAvatar.isPresent()).isTrue();
    }
}


@Configuration
@Import({CalendarTestInitContextConfiguration.class,
        MeterRegistryStubTestConfiguration.class})
class TestAvatarManagerConfiguration {
    @Bean
    public AvatarManager avatarManager(MeterRegistry meterRegistry,
                                       @Value("${yt.center.url}") String centerUrl) {
        final HttpClientConfiguration httpClientConfiguration = HttpClientConfiguration.forTest();
        return new AvatarManager(centerUrl, httpClientConfiguration, meterRegistry);
    }
}
