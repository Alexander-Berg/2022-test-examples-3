package ru.yandex.direct.jobs.configuration;

import com.yandex.ydb.table.TableClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.direct.bannerstorage.client.BannerStorageClient;
import ru.yandex.direct.bannerstorage.client.DummyBannerStorageClient;
import ru.yandex.direct.common.jetty.JettyLauncher;
import ru.yandex.direct.core.testing.configuration.CoreTestingConfiguration;
import ru.yandex.direct.core.testing.configuration.UacYdbTestingConfiguration;

import static ru.yandex.direct.common.configuration.MonitoringHttpServerConfiguration.MONITORING_HTTP_SERVER_BEAN_NAME;
import static ru.yandex.direct.configuration.YdbConfiguration.HOURGLASS_YDB_TABLE_CLIENT_BEAN;

@Configuration
@Import({JobsConfiguration.class, CoreTestingConfiguration.class, UacYdbTestingConfiguration.class})
public class JobsTestingConfiguration {
    @MockBean(name = HOURGLASS_YDB_TABLE_CLIENT_BEAN)
    public TableClient tableClient;

    @Bean
    public BannerStorageClient bannerStorageClient() {
        return new DummyBannerStorageClient();
    }

    @MockBean(name = MONITORING_HTTP_SERVER_BEAN_NAME)
    public JettyLauncher adminServlet;
}
