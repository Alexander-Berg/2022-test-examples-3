package ru.yandex.direct.grid.core.configuration;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.direct.communication.config.CommunicationConfiguration;
import ru.yandex.direct.core.testing.configuration.CoreTestingConfiguration;
import ru.yandex.direct.grid.core.entity.offer.repository.GridOfferYtRepository;
import ru.yandex.direct.grid.core.util.yt.YtClusterFreshnessLoader;

@Configuration
@Import({GridCoreConfiguration.class, CoreTestingConfiguration.class, CommunicationConfiguration.class})
public class GridCoreTestingConfiguration {
    @MockBean
    public YtClusterFreshnessLoader ytClusterFreshnessLoader;

    @SpyBean
    public GridOfferYtRepository gridOfferYtRepository;
}
