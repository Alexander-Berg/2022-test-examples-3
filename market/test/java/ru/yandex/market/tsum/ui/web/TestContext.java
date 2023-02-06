package ru.yandex.market.tsum.ui.web;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.market.tsum.trace.TraceService;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 05.09.16
 */
@Configuration
public class TestContext {
    @Bean
    public TraceService traceService() {
        return Mockito.mock(TraceService.class);
    }
}
