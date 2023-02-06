package ru.yandex.direct.jobs.ppcdataexport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.env.EnvironmentCondition;
import ru.yandex.direct.jobs.configuration.DirectExportYtClustersParametersSource;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.jobs.configuration.PpcDataExportParameter;
import ru.yandex.direct.jobs.configuration.PpcDataExportParametersSource;
import ru.yandex.direct.jobs.ppcdataexport.model.PpcDataExportInfo;
import ru.yandex.direct.jobs.ppcdataexport.model.PpcDataExportInitInfo;
import ru.yandex.direct.juggler.check.model.NotificationRecipient;
import ru.yandex.direct.liveresource.LiveResourceFactory;
import ru.yandex.direct.ytwrapper.model.YtCluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.direct.jobs.configuration.JobsEssentialConfiguration.PPC_DATA_EXPORT_PATH;

@ExtendWith(SpringExtension.class)
@JobsTest
class PpcDataExportConfigsTest {

    private static final String ENVIRONMENT_CONDITION_PARAM = "environmentCondition";
    private static final String ENVIRONMENTS_PACKAGE = EnvironmentCondition.class.getPackageName();

    @Autowired
    private ResourcePatternResolver resourceResolver;

    @Autowired
    private DirectExportYtClustersParametersSource directExportYtClustersParametersSource;

    @Autowired
    private ApplicationContext applicationContext;

    private List<YtCluster> defaultYtClusters;

    @BeforeEach
    public void before() {
        defaultYtClusters = directExportYtClustersParametersSource.getAllParamValues();
    }

    @Test
    void checkIsConfigsValid() {
        List<PpcDataExportParameter> allParams = Objects.requireNonNull(getSource()).getAllParamValues();

        for (PpcDataExportParameter param : allParams) {
            PpcDataExportInfo exportInfo = PpcDataExportJob.getExportInfo(param);

            assertNotNull(exportInfo.getDeltaTime());
            assertNotNull(exportInfo.getYtCluster());
            assertNotNull(exportInfo.getJugglerTtl());
            assertNotNull(exportInfo.getYqlFilePath());

            assertTrue(exportInfo.getDestinationTableRelativePath() != null || exportInfo.getSolomonFlow() != null);

            assertTrue(
                    isChildOfEnvironmentCondition(param.getConfFilePath()),
                    String.format("Класс из %s должен быть наследником %s",
                            param.getConfFilePath(),
                            EnvironmentCondition.class.getCanonicalName()
                    )
            );

            checkNotificationFields(exportInfo);

            String content = LiveResourceFactory.get(exportInfo.getYqlFilePath()).getContent();
            assertThat(content).isNotEmpty();
        }
    }


    private boolean isChildOfEnvironmentCondition(String configPath) {
        Config config = getConfig(configPath);
        if (config.hasPath(ENVIRONMENT_CONDITION_PARAM)) {
            try {
                var clazz = Class.forName(
                        ENVIRONMENTS_PACKAGE
                                + "."
                                + config.getString(ENVIRONMENT_CONDITION_PARAM)
                );
                return EnvironmentCondition.class.isAssignableFrom(clazz)
                        && !clazz.isAssignableFrom(EnvironmentCondition.class);
            } catch (ClassNotFoundException e) {
                return false;
            }
        }

        return true;
    }

    private Config getConfig(String configPath) {
        String content = LiveResourceFactory.get(configPath).getContent();
        return ConfigFactory.parseString(content);
    }

    private static void checkNotificationFields(PpcDataExportInfo exportInfo) {
        List<String> notificationRecipients = exportInfo.getNotificationRecipients();
        if (notificationRecipients == null) {
            return;
        }

        assertNotNull(exportInfo.getNotificationMethods());
        for (String notificationRecipient : notificationRecipients) {
            NotificationRecipient.fromName(notificationRecipient);
        }
    }

    private List<PpcDataExportInitInfo> getPpcDataExportJobInitInfo() throws IOException {
        Resource[] resources = resourceResolver.getResources("classpath:" + PPC_DATA_EXPORT_PATH + "**/*.conf");

        List<PpcDataExportInitInfo> result = new ArrayList<>();
        for (Resource resource : resources) {
            String fullPath = resource.getURL().getPath();
            int classpathStartIndex = fullPath.lastIndexOf(PPC_DATA_EXPORT_PATH);
            String relativePath = "classpath:" + fullPath.substring(classpathStartIndex);

            Config config = ConfigFactory.parseString(
                    LiveResourceFactory.get(relativePath).getContent()
            );
            var ppcDataExportInitInfo = new PpcDataExportInitInfo(config, relativePath, defaultYtClusters);

            checkNeedScheduleBeanExist(ppcDataExportInitInfo);
            result.add(ppcDataExportInitInfo);
        }
        return result;
    }

    private PpcDataExportParametersSource getSource() {
        try {
            var initInfos = getPpcDataExportJobInitInfo();
            return PpcDataExportParametersSource.from(initInfos);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        return null;
    }

    private void checkNeedScheduleBeanExist(PpcDataExportInitInfo initInfo) {
        assertDoesNotThrow(
                () -> applicationContext.getBean(initInfo.getEnvironmentCondition()),
                String.format("Бин класса %s из %s не найден, либо создался с ошибкой",
                        initInfo.getEnvironmentCondition().getCanonicalName(),
                        initInfo.getConfFilePath()
                )
        );
    }
}
