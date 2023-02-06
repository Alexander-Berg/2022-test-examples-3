package ru.yandex.market.logshatter.config;

import org.junit.Test;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 24.05.2019
 */
public class ConfigurationServiceTest {

    @Test(expected = IllegalStateException.class)
    public void checkTableNameFailsOnWrongTable() {
        ConfigurationService.checkTableName("torrent-client-perf");
    }

    @Test
    public void checkTableNameSuccessOnGoodTable() {
        ConfigurationService.checkTableName("torrent_client_perf");
        ConfigurationService.checkTableName("market.torrent_client_perf");
    }
}