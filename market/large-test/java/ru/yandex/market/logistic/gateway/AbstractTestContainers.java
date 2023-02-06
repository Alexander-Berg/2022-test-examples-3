package ru.yandex.market.logistic.gateway;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import ru.yandex.market.logistic.gateway.config.IntegrationTestConfiguration;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = IntegrationTestConfiguration.class)
@TestPropertySource({"classpath:integration-test/application-integration-test.properties"})
@ContextConfiguration(initializers = AbstractTestContainers.AwsInitializer.class)
@TestExecutionListeners({
    DependencyInjectionTestExecutionListener.class,
    DbUnitTestExecutionListener.class,
    MockitoTestExecutionListener.class,
    ResetMocksTestExecutionListener.class
})
@ActiveProfiles(profiles = {
    "integration-test",
    "test-containers"
})
@DirtiesContext
@MockBean({
    TvmClientApi.class,
})
public abstract class AbstractTestContainers {

    public static final int SQS_PORT = 9324;
    public static final int S3_PORT = 4569;
    public static final String BUCKET_NAME = "delivery-dev-bucket";
    public static final String DEFAULT_REGION = "eu-west-1";

    @ClassRule
    public static GenericContainer sqsContainer =
        new GenericContainer("registry.yandex.net/market/delivery/elastic-mq-custom:0.0.1")
            .withEnv("HOSTNAME", DockerClientFactory.instance().dockerHostIpAddress())
            .withClasspathResourceMapping("elastic-mq.conf", "/var/lib/elasticmq/config.conf", BindMode.READ_ONLY)
            .withExposedPorts(SQS_PORT)
            .waitingFor(Wait.forHttp("/").forStatusCode(404));

    @ClassRule
    public static GenericContainer s3Container =
        new GenericContainer("registry.yandex.net/market/delivery/fake-s3:0.0.1")
            .withExposedPorts(S3_PORT)
            .waitingFor(Wait.forHttp("/").forStatusCode(200));

    protected static String getSqsConnectionString() {
        return "http://" + DockerClientFactory.instance().dockerHostIpAddress()
            + ":" + sqsContainer.getMappedPort(SQS_PORT);
    }

    private static String convertUriToHostIp(String uri) {
        try {
            return InetAddress.getByName(uri).getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException("Cannot get docker host ip");
        }
    }

    protected static String getS3ConnectionString() {
        return "http://" + convertUriToHostIp(DockerClientFactory.instance().dockerHostIpAddress())
            + ":" + s3Container.getMappedPort(S3_PORT);
    }

    @BeforeClass
    public static void prepareS3() {
        AmazonS3 s3 = AmazonS3ClientBuilder.standard()
            .withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration(getS3ConnectionString(), DEFAULT_REGION))
            .build();

        s3.createBucket(BUCKET_NAME);
    }

    @Ignore
    public static class AwsInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext,
                "lgw.aws.s3AccessKey=",
                "lgw.aws.s3SecretKey=",
                "lgw.aws.s3EndpointHost=" + getS3ConnectionString(),
                "lgw.aws.sqsAccessKey=",
                "lgw.aws.sqsSecretKey=",
                "lgw.aws.sqsEndpointHost=" + getSqsConnectionString(),
                "lgw.aws.s3BucketName=" + BUCKET_NAME,
                "lgw.aws.region=" + DEFAULT_REGION,
                "lgw.aws.proxyHost=" + DockerClientFactory.instance().dockerHostIpAddress(),
                "lgw.aws.proxyPort=" + sqsContainer.getMappedPort(SQS_PORT));
        }
    }
}
