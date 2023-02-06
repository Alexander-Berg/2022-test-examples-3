package ru.yandex.market.tsum.pipelines.common.jobs.health;

import java.io.IOException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.tsum.core.TestResourceLoader.getTestResourceAsString;
import static ru.yandex.market.tsum.pipelines.common.jobs.health.HealthMonitoringComponent.CLICKPHITE;
import static ru.yandex.market.tsum.pipelines.common.jobs.health.HealthMonitoringComponent.LOGSHATTER;
import static ru.yandex.market.tsum.pipelines.common.jobs.health.HealthMonitoringConfigFactory.createConfig;
import static ru.yandex.market.tsum.pipelines.common.jobs.health.HealthMonitoringConfigFactory.createConfigVersion;

public class HealthMonitoringConfigFactoryTest {

    static final String TEST_APPLICATION_NAME = "applicationName";
    static final String TEST_PROJECT_NAME = "market-projectName";
    static final String TEST_ABC_GROUP = "abcGroup";
    static final String TEST_TAG = "tag";
    static final String TEST_JOB_URL = "job_url";
    static final String CONFIG_EXPECTED_FILE = "expected_config.json";
    static final String LOGSHTTAER_EXPECTED_FILE = "expected_logshatter_config.json";
    static final String CLICKPHITE_EXPECTED_FILE = "expected_clickphite_config.json";
    static final String CLICKPHITE_ID = "click_id";
    static final String LOGSHATTER_ID = "log_id";

    @Test
    public void verifyNewConfigFormattedWithApplicationInfo() throws IOException {
        HealthMonitoringConfigFactory.ApplicationInfo applicationInfo = getApplicationInfo();
        String newVersionContent = createConfig(applicationInfo, TEST_ABC_GROUP, "tag", "job_url", "confId");
        assertContentNotContainsPlaceholders(newVersionContent);
        assertContentContainsAbcGroup(newVersionContent);
        assertContentContainsTag(newVersionContent);
        assertContentContainsJobUrl(newVersionContent);
        assertExpectedContent(newVersionContent, CONFIG_EXPECTED_FILE);
    }

    @Test
    public void verifyLogshatterConfigFormattedWithApplicationInfo() throws IOException {
        HealthMonitoringConfigFactory.ApplicationInfo applicationInfo = getApplicationInfo();
        String logshatterConfig = createConfigVersion(applicationInfo, TEST_ABC_GROUP, LOGSHATTER, LOGSHATTER_ID);
        assertContentNotContainsPlaceholders(logshatterConfig);
        assertContentContainsApplicationName(logshatterConfig);
        assertExpectedContent(logshatterConfig, LOGSHTTAER_EXPECTED_FILE);
    }

    @Test
    public void verifyClickphiteConfigFormattedWithApplicationInfoAndAbcGroup() throws IOException {
        HealthMonitoringConfigFactory.ApplicationInfo applicationInfo = getApplicationInfo();
        String clickphiteConfig = createConfigVersion(applicationInfo, TEST_ABC_GROUP, CLICKPHITE, CLICKPHITE_ID);
        assertContentNotContainsPlaceholders(clickphiteConfig);
        assertContentContainsApplicationName(clickphiteConfig);
        assertContentContainsProjectNameWithDefaultPrefix(clickphiteConfig);
        assertContentContainsAbcGroup(clickphiteConfig);
        assertExpectedContent(clickphiteConfig, CLICKPHITE_EXPECTED_FILE);
    }

    static HealthMonitoringConfigFactory.ApplicationInfo getApplicationInfo() {
        return new HealthMonitoringConfigFactory.ApplicationInfo(TEST_APPLICATION_NAME, TEST_PROJECT_NAME);
    }

    static void assertContentContainsProjectNameWithDefaultPrefix(String content) {
        assertTrue(content.contains(TEST_PROJECT_NAME));
    }

    static void assertContentNotContainsPlaceholders(String content) {
        assertFalse(content.contains("${application}"));
    }

    static void assertContentContainsTag(String content) {
        assertTrue(content.contains(TEST_TAG));
    }

    static void assertContentContainsJobUrl(String content) {
        assertTrue(content.contains(TEST_JOB_URL));
    }

    static void assertContentContainsApplicationName(String content) {
        assertTrue(content.contains(TEST_APPLICATION_NAME));
    }

    static void assertContentContainsAbcGroup(String content) {
        assertTrue(content.contains(TEST_ABC_GROUP));
    }

    static void assertExpectedContent(String content, String file) throws IOException {
        String expectedContent = getTestResourceAsString(HealthMonitoringConfigFactory.class, file);
        assertEquals(expectedContent, content);
    }
}
