package ru.yandex.market.tsum.pipelines.common.jobs.deploy;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.clients.yav.VaultClient;
import ru.yandex.market.tsum.clients.yp.YandexDeployClient;
import ru.yandex.market.tsum.clients.yp.YandexDeployClientTest;
import ru.yandex.market.tsum.core.environment.Environment;
import ru.yandex.market.tsum.pipe.engine.runtime.config.JobTesterConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobTester;
import ru.yandex.market.tsum.pipelines.common.resources.AbcServiceResource;
import ru.yandex.market.tsum.pipelines.common.resources.YandexDeployStage;
import ru.yandex.market.tsum.pipelines.sre.resources.ApplicationName;
import ru.yandex.market.tsum.pipelines.sre.resources.YandexDeployServiceSpec;
import ru.yandex.yp.YpInstance;
import ru.yandex.yp.YpRawClient;
import ru.yandex.yp.YpRawClientBuilder;

@Ignore("integration test")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {JobTesterConfig.class})
public class YandexDeployCreateServiceJobIntegrationTest extends TestCase {
    private static final String YP_TOKEN;
    private static final YpRawClient YP_RAW_CLIENT;
    private static final YandexDeployClient DEPLOY_CLIENT;
    private static final VaultClient VAULT_CLIENT;

    @Autowired
    private JobTester jobTester;

    static {
        // https://oauth.yandex-team.ru/authorize?response_type=token&client_id=f8446f826a6f4fd581bf0636849fdcd7
        YP_TOKEN = YandexDeployClientTest.getToken(".yp/token");
        YP_RAW_CLIENT = new YpRawClientBuilder(YpInstance.CROSS_DC, () -> YP_TOKEN)
            .setTimeout(20, TimeUnit.SECONDS)
            .setUseMasterDiscovery(false)
            .build();
        DEPLOY_CLIENT = new YandexDeployClient(YP_RAW_CLIENT);
        VAULT_CLIENT = new VaultClient("https://vault-api.passport.yandex.net", "");
    }

    @Test
    public void testModifyStage() throws Exception {
        jobTester.jobInstanceBuilder(YandexDeployCreateServiceJob.class)
            .withResources(new ApplicationName("test_modify_stage_2"))
            .withResource(new YandexDeployServiceSpec(
                null, null, "market-infra", null, null, null, null, null, Collections.singletonList(
                new YandexDeployStage("development_market_test_modify_stage_2", Environment.DEVELOPMENT)), null
            ))
            .withResource(new AbcServiceResource("marketinfra", -1))
            .withBean(DEPLOY_CLIENT)
            .withBean(VAULT_CLIENT)
            .create()
            .execute(null);
    }
}
