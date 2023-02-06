package ru.yandex.direct.jobs.export;

import java.util.Collections;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.jobs.configuration.DirectExportYtClustersParametersSource;
import ru.yandex.direct.jobs.configuration.JobsEssentialConfiguration;
import ru.yandex.direct.jobs.verifications.BillingOrderQualityYqlRunner;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;

import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        JobsEssentialConfiguration.class,
})
@ParametersAreNonnullByDefault
// если вдруг захотелось запустить, нужно закомментировать @Disabled
@Disabled("Для запуска вручную")
class BillingOrderDomainsExporterTest {

    @Autowired
    private YtProvider ytProvider;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Mock
    private BillingOrderQualityYqlRunner billingOrderQualityYqlRunner;

    private BillingOrderDomainsExporter billingOrderDomainsExporter;

    @BeforeEach
    void setUp() {
        initMocks(this);
        billingOrderDomainsExporter = new BillingOrderDomainsExporter(
                ytProvider,
                ppcPropertiesSupport,
                new DirectExportYtClustersParametersSource(Collections.singletonList(
                        YtCluster.HAHN)),
                billingOrderQualityYqlRunner
        ) {
            @Override
            public String getParam() {
                return YtCluster.HAHN.name();
            }
        };
    }

    @Test
    void fire() throws Exception {
        billingOrderDomainsExporter.execute();
    }
}
