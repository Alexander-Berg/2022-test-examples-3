package ru.yandex.market.tsum.pipelines.sre.jobs;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import ru.yandex.market.tsum.pipelines.sre.resources.balancer.BalancerInfo;

import static org.junit.Assert.assertEquals;

public class GenerateAndPreCommitBalancerConfigsJobTest {
    private final GenerateAndPreCommitBalancerConfigsJob job = new GenerateAndPreCommitBalancerConfigsJob();

    private HashMap<String, Object> getConfigParams(BalancerInfo balancerInfo) {
        return job.getTemplateParams(balancerInfo);
    }

    private void assertTemplate(String resultConfigPath, String config) throws Exception {
        assertEquals(
            IOUtils.toString(
                Objects.requireNonNull(this.getClass().getResourceAsStream(resultConfigPath)), StandardCharsets.UTF_8
            ),
            config
        );
    }

    @Test
    public void yaBalancerHttpConfig() throws Exception {
        BalancerInfo balancerInfo = BalancerPipelineTestFactory.getBalancerInfoHttp();
        assertTemplate(
            "/balancerPipeline/fslbConfigHttp.yaml",
            job.renderTemplate(
                GenerateAndPreCommitBalancerConfigsJob.YABALANCER_CONFIG_TEMPLATE_PATH,
                getConfigParams(balancerInfo)
            )
        );
    }

    @Test
    public void yaBalancerHttpsConfig() throws Exception {
        BalancerInfo balancerInfo = BalancerPipelineTestFactory.getBalancerInfoHttps();
        assertTemplate(
            "/balancerPipeline/fslbConfigHttps.yaml",
            job.renderTemplate(
                GenerateAndPreCommitBalancerConfigsJob.YABALANCER_CONFIG_TEMPLATE_PATH,
                getConfigParams(balancerInfo)
            )
        );
    }

    @Test
    public void testHaproxyConfig() throws Exception {
        BalancerInfo balancerInfo = BalancerPipelineTestFactory.getBalancerInfoHttp();
        assertTemplate(
            "/balancerPipeline/haproxyConfig.yaml",
            job.renderTemplate(
                GenerateAndPreCommitBalancerConfigsJob.HAPROXY_CONFIG_TEMPLATE_PATH,
                getConfigParams(balancerInfo)
            )
        );
    }

    @Test
    public void testNginxHttpConfig() throws Exception {
        BalancerInfo balancerInfo = BalancerPipelineTestFactory.getBalancerInfoHttp();
        assertTemplate(
            "/balancerPipeline/nginxHttpConfig.yaml",
            job.renderTemplate(
                GenerateAndPreCommitBalancerConfigsJob.NGINX_CONFIG_TEMPLATE_PATH,
                getConfigParams(balancerInfo)
            )
        );
    }

    @Test
    public void testNginxHttpsConfig() throws Exception {
        BalancerInfo balancerInfo = BalancerPipelineTestFactory.getBalancerInfoHttps();
        balancerInfo.setRedirectToHttps(false);
        assertTemplate(
            "/balancerPipeline/nginxHttpsConfig.yaml",
            job.renderTemplate(
                GenerateAndPreCommitBalancerConfigsJob.NGINX_CONFIG_TEMPLATE_PATH,
                getConfigParams(balancerInfo)
            )
        );
    }

    @Test
    public void testNginxHttpsRedirectConfig() throws Exception {
        BalancerInfo balancerInfo = BalancerPipelineTestFactory.getBalancerInfoHttps();
        balancerInfo.setRedirectToHttps(true);
        assertTemplate(
            "/balancerPipeline/nginxHttpsRedirectConfig.yaml",
            job.renderTemplate(
                GenerateAndPreCommitBalancerConfigsJob.NGINX_CONFIG_TEMPLATE_PATH,
                getConfigParams(balancerInfo)
            )
        );
    }

    @Test
    public void testHaproxyGrpcConfig() throws Exception {
        BalancerInfo balancerInfo = BalancerPipelineTestFactory.getBalancerInfoGrpc();
        assertTemplate(
            "/balancerPipeline/haproxyGrpcConfig.yaml",
            job.renderTemplate(
                GenerateAndPreCommitBalancerConfigsJob.HAPROXY_CONFIG_TEMPLATE_PATH,
                getConfigParams(balancerInfo)
            )
        );
    }

    @Test
    public void testHaproxyGrpcSslConfig() throws Exception {
        BalancerInfo balancerInfo = BalancerPipelineTestFactory.getBalancerInfoGrpcSsl();
        assertTemplate(
            "/balancerPipeline/haproxyGrpcSslConfig.yaml",
            job.renderTemplate(
                GenerateAndPreCommitBalancerConfigsJob.HAPROXY_CONFIG_TEMPLATE_PATH,
                getConfigParams(balancerInfo)
            )
        );
    }

    @Test
    public void testHaproxyConfigForYp() throws Exception {
        BalancerInfo balancerInfo = BalancerPipelineTestFactory.getBalancerInfoHttp();
        balancerInfo.setTypeOfBackends(BalancerInfo.TypeOfBackends.YP_ENDPOINT);
        balancerInfo.setRealServers(Collections.singletonList("my_stage.my_deploy_unit"));
        assertTemplate(
            "/balancerPipeline/haproxyConfigForYp.yaml",
            job.renderTemplate(
                GenerateAndPreCommitBalancerConfigsJob.HAPROXY_CONFIG_TEMPLATE_PATH,
                getConfigParams(balancerInfo)
            )
        );
    }
}
