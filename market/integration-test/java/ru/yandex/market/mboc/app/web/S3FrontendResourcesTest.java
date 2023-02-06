package ru.yandex.market.mboc.app.web;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.assertj.core.api.Assertions;
import org.gaul.s3proxy.junit.S3ProxyRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.mboc.app.BaseWebIntegrationTestClass;
import ru.yandex.market.mboc.common.utils.S3FrontendResources;
import ru.yandex.market.mboc.common.utils.S3FrontendVersions;
import ru.yandex.market.mboc.common.utils.TestFieldReplacer;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * @author yuramalinov
 * @created 18.06.2020
 */
@SuppressWarnings("UnstableApiUsage")
public class S3FrontendResourcesTest extends BaseWebIntegrationTestClass {
    private static final String TEST_BUCKET = "test-bucket";

    @Rule
    public S3ProxyRule s3Proxy = S3ProxyRule.builder()
        .withCredentials("access", "secret")
        .build();

    @Autowired
    private S3FrontendResources frontendResources;

    private AmazonS3 s3Client;
    private TestFieldReplacer replacer;

    @Before
    public void setup() {
        s3Client = AmazonS3ClientBuilder
            .standard()
            .withCredentials(new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(s3Proxy.getAccessKey(), s3Proxy.getSecretKey())))
            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                s3Proxy.getUri().toString(), Regions.US_EAST_1.getName()))
            .build();

        s3Client.createBucket(TEST_BUCKET);
        frontendResources.clearCaches();

        replacer = new TestFieldReplacer();
        replacer.replace(frontendResources, "amazonS3", s3Client);
        replacer.replace(frontendResources, "bucketName", TEST_BUCKET);
        replacer.replace(frontendResources, "enabled", true);
    }

    @After
    public void restoreBeans() {
        replacer.restore();
    }

    @Test
    public void testReadExistingVersioned() throws Exception {
        mvc.perform(get("/index.html"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("text/html"))
            .andExpect(content().string("Hey there! Test HTML index.\n"));
    }

    @Test
    public void testReadVersioned() throws Exception {
        s3Client.putObject(TEST_BUCKET, "current_version", "test-version");
        s3Client.putObject(TEST_BUCKET, "versions/test-version/index.html", "test index");

        mvc.perform(get("/index.html"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("text/html"))
            .andExpect(content().string("test index"));
    }

    @Test
    public void testReadStaticResource() throws Exception {
        s3Client.putObject(TEST_BUCKET, "static/something.js", "some.js");

        mvc.perform(get("/static/something.js"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/javascript"))
            .andExpect(content().string("some.js"));
    }

    @Test
    public void test404OnNonExisting() throws Exception {
        mvc.perform(get("/static/something.js"))
            .andExpect(status().isNotFound());
    }

    @Test
    public void testForceVersion() throws Exception{
        s3Client.putObject(TEST_BUCKET, "versions/v1/index.html", "v1 index");
        s3Client.putObject(TEST_BUCKET, "versions/v2/index.html", "v2 index");
        s3Client.putObject(TEST_BUCKET, "current_version", "v1");

        mvc.perform(get("/index.html"))
            .andExpect(status().isOk())
            .andExpect(content().string("v1 index"));

        frontendResources.forceVersion("v2");
        frontendResources.clearCaches();

        mvc.perform(get("/index.html"))
            .andExpect(status().isOk())
            .andExpect(content().string("v2 index"));

        frontendResources.clearForceVersion();
        frontendResources.clearCaches();

        mvc.perform(get("/index.html"))
            .andExpect(status().isOk())
            .andExpect(content().string("v1 index"));
    }

    @Ignore("К сожалению, S3Proxy не умеет в ListObjectsV2, так что тест не рабочий.")
    @Test
    public void testVersions() throws Exception {
        s3Client.putObject(TEST_BUCKET, "versions/v1/index.html", "v1 index");
        s3Client.putObject(TEST_BUCKET, "versions/v2/index.html", "v2 index");
        s3Client.putObject(TEST_BUCKET, "current_version", "v2");
        s3Client.putObject(TEST_BUCKET, "force_version", "v1");

        S3FrontendVersions versions = frontendResources.getVersions(null);
        Assertions.assertThat(versions.getVersions()).containsExactlyInAnyOrder("v1", "v2");
        Assertions.assertThat(versions.getCurrentVersion()).isEqualTo("v2");
        Assertions.assertThat(versions.getForceVersion()).isEqualTo("v1");
    }

    private MockHttpServletRequestBuilder get(String s) {
        return MockMvcRequestBuilders.get(s).header("Authorization", "test");
    }
}
