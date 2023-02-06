package ru.yandex.market.forecastint.service.yt.loader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.forecastint.AbstractFunctionalTest;
import ru.yandex.market.forecastint.config.yt.YqlDataService;
import ru.yandex.market.forecastint.config.yt.YtFactory;
import ru.yandex.market.forecastint.service.yt.loader.service.YtDynamicTablesForecastServiceImpl;
import ru.yandex.market.yql_query_service.service.QueryService;

public class YtDynamicTableLoaderTest extends AbstractFunctionalTest {

    @Autowired
    private YtFactory ytFactory;

    @Autowired
    private QueryService queryService;

    @Test
    public void test() {
        final YqlDataService yqlDataService = Mockito.mock(YqlDataService.class);
        Mockito.doNothing().when(yqlDataService).executeQuery(Mockito.anyString());
        final YtDynamicTableLoader loader = new YtDynamicTableLoader(
                new YtDynamicTablesForecastServiceImpl(ytFactory, yqlDataService, queryService)
        );
        Assertions.assertDoesNotThrow(loader::load);
    }
}
