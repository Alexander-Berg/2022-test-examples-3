package ru.yandex.direct.jobs.ppcdataexport;

import javax.annotation.ParametersAreNonnullByDefault;

import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.jobs.configuration.JobsEssentialConfiguration;
import ru.yandex.direct.jobs.configuration.PpcDataExportParametersSource;
import ru.yandex.direct.jobs.ppcdataexport.model.PpcDataExportInitInfo;
import ru.yandex.direct.solomon.SolomonPushClient;
import ru.yandex.direct.ytcomponents.repository.YtClusterFreshnessRepository;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;

import static java.util.Collections.singletonList;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        JobsEssentialConfiguration.class,
})
@ParametersAreNonnullByDefault
// если вдруг захотелось запустить, нужно закомментировать @Disabled
@Disabled("Для запуска вручную")
public class PpcDataExportManualTest {
    @Autowired
    private YtProvider ytProvider;

    @Autowired
    private YtClusterFreshnessRepository ytClusterFreshnessRepository;
    private PpcDataExportJob ppcDataExportJob;
    private SolomonPushClient solomonPushClient;

    @BeforeEach
    void setUp() {

        String confFilePath = "classpath:/export/ppcdataexport/pythia/campaigns_brand_survey.conf";
        PpcDataExportInitInfo ppcDataExportInitInfo =
                new PpcDataExportInitInfo(ConfigFactory.empty(), confFilePath, singletonList(YtCluster.HAHN));

        PpcDataExportParametersSource parametersSource =
                PpcDataExportParametersSource.from(singletonList(ppcDataExportInitInfo));

        ppcDataExportJob = new PpcDataExportJob(
                parametersSource,
                ytProvider,
                ytClusterFreshnessRepository,
                solomonPushClient,
                (event, timeout) -> {
                }) {
            @Override
            public String getParam() {
                return YtCluster.HAHN.name() + "---" + confFilePath;
            }
        };
    }

    @Test
    void fire() throws Exception {
        ppcDataExportJob.execute();
    }
}
