package ru.yandex.market.crm.campaign.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.crm.campaign.yql.AsyncYqlService;
import ru.yandex.market.crm.campaign.yt.utils.YtRowMapper;
import ru.yandex.market.crm.chyt.services.ChytQueryExecutor;
import ru.yandex.market.crm.core.test.utils.CommunicationTables;
import ru.yandex.market.crm.core.yt.KeyValueStorage;
import ru.yandex.market.crm.core.yt.paths.CrmYtTables;
import ru.yandex.market.crm.core.yt.paths.YtFolders;
import ru.yandex.market.crm.yql.SyncYqlService;
import ru.yandex.market.crm.yt.YtAclConfiguration;
import ru.yandex.market.crm.yt.client.YtClient;
import ru.yandex.market.crm.yt.tx.TxRunner;
import ru.yandex.market.crm.yt.tx.TxRunnerStub;

import static org.mockito.Mockito.mock;

/**
 * @author apershukov
 */
@Configuration
@Import(YtAclConfiguration.class)
public class TestYtConfig {

    @Bean
    public YtClient ytClient() {
        return mock(YtClient.class);
    }

    @Bean
    public YtFolders ytFolders() {
        return new YtFolders(
                "//home/market/tests/crm",
                "//home/market/tmp",
                "//home/market/campaigns",
                "//home/market/tests/crm/external",
                "//home/market/push-sendings",
                "//home/market/market-promo/bunch_request",
                "//home/market/tests/crm/chyt_data"
        );
    }

    @Bean
    public CrmYtTables crmYtTables() {
        return mock(CrmYtTables.class);
    }

    @Bean
    public CommunicationTables communicationTables() {
        return new CommunicationTables(ytFolders());
    }

    @Bean
    public TxRunner txRunner() {
        return new TxRunnerStub();
    }

    @Bean
    public YtRowMapper ytRowMapper() {
        return mock(YtRowMapper.class);
    }

    @Bean
    public SyncYqlService yqlService() {
        return mock(SyncYqlService.class);
    }

    @Bean
    public AsyncYqlService asyncYqlService() {
        return mock(AsyncYqlService.class);
    }

    @Bean
    public KeyValueStorage keyValueStorage() {
        return mock(KeyValueStorage.class);
    }

    @Bean
    public ChytQueryExecutor chytQueryExecutor() {
        return mock(ChytQueryExecutor.class);
    }
}
