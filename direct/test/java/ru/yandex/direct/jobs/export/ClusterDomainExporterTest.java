package ru.yandex.direct.jobs.export;

import java.util.Collections;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.jobs.configuration.DirectExportYtClustersParametersSource;
import ru.yandex.direct.jobs.configuration.JobsEssentialConfiguration;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        JobsEssentialConfiguration.class,
})
@ParametersAreNonnullByDefault
// если вдруг захотелось запустить, нужно закомментировать @Disabled
@Disabled("Для запуска вручную")
class ClusterDomainExporterTest {
    @Autowired
    private YtProvider ytProvider;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    private ClusterDomainExporter clusterDomainExporter;

    @BeforeEach
    void setUp() {
        clusterDomainExporter = new ClusterDomainExporter(
                ytProvider,
                ppcPropertiesSupport,
                new DirectExportYtClustersParametersSource(Collections.singletonList(
                        YtCluster.HAHN)))
        {
            @Override
            public String getParam() {
                return YtCluster.HAHN.name();
            }
        };
    }

    @Test
    void fire() throws Exception {
        clusterDomainExporter.execute();
    }
}
