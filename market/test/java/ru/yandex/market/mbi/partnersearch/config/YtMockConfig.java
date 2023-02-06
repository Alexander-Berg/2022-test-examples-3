package ru.yandex.market.mbi.partnersearch.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.mbi.partnersearch.data.yt.PartnerSummaryService;
import ru.yandex.market.yt.util.reader.YtTemplate;

@Configuration
public class YtMockConfig {
    @Bean
    @Primary
    public YtTemplate dataImportYtTemplate() {
        return Mockito.mock(YtTemplate.class);
    }

    @Bean
    @Primary
    public PartnerSummaryService partnerSummaryService() {
        return Mockito.mock(PartnerSummaryService.class);
    }
}
