package ru.yandex.market.health.jobs;

import org.assertj.core.api.SoftAssertions;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource("classpath:application-integration-test-cache.properties")
@ActiveProfiles("caching-pr")
public class CacheAndEvictionTest extends AbstractTest {

    @Test
    @Ignore
    public void cacheOkWaitForNotOkOnHangingSpecific() {
        prepareDataSet("classpath:db/9.xml", 0);
        String before = fireHangingJob("testJob9");
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(before).startsWith("0;");
            assertions.assertThat(before).contains("OK");
        });
        prepareDataSet("classpath:db/9.xml", -3);
        String after = fireHangingJob("testJob9");
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(after).startsWith("0;");
            assertions.assertThat(after).contains("OK");
        });
        waitCacheEviction();
        String afterEviction = fireHangingJob("testJob9");
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(afterEviction).startsWith("2;");
            assertions.assertThat(afterEviction).contains("Job testJob9 has not started");
        });
    }
}
