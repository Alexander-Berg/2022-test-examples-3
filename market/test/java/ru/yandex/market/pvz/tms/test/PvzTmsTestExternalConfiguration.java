package ru.yandex.market.pvz.tms.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.market.tpl.common.calendar.YaCalendarService;
import ru.yandex.market.tpl.common.logbroker.config.LogbrokerTestExternalConfig;
import ru.yandex.market.tpl.common.mail.reader.MailReader;
import ru.yandex.market.tpl.common.yt.tables.download.YtDownloader;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

@Configuration
@Import({
        LogbrokerTestExternalConfig.class
})
public class PvzTmsTestExternalConfiguration {

    @Bean
    YaCalendarService yaCalendarService() {
        return mock(YaCalendarService.class);
    }

    @Bean
    Yt hahn() {
        return mock(Yt.class, RETURNS_DEEP_STUBS);
    }

    @Bean
    Yt arnold() {
        return mock(Yt.class, RETURNS_DEEP_STUBS);
    }

    @Bean
    YtDownloader hahnYtDownloader() {
        return mock(YtDownloader.class, RETURNS_DEEP_STUBS);
    }

    @Bean
    MailReader robotMarketTplMailReader() {
        return mock(MailReader.class, RETURNS_DEEP_STUBS);
    }

}
