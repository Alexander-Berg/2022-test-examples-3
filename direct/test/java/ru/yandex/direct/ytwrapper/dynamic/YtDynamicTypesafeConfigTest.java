package ru.yandex.direct.ytwrapper.dynamic;

import java.time.Duration;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;

public class YtDynamicTypesafeConfigTest {

    private YtDynamicTypesafeConfig ytDynamicConfig;

    @Before
    public void setUp() {
        Config ytConfig = ConfigFactory.parseResources("ru/yandex/direct/ytwrapper/yt_test.conf").getConfig("yt");
        ytDynamicConfig = new YtDynamicTypesafeConfig(ytConfig);
    }

    @Test
    public void configParamsRead_success() {
        SoftAssertions.assertSoftly(
                softly -> {
                    softly.assertThat(ytDynamicConfig.defaultSelectRowsTimeout())
                            .describedAs("default-select-rows-timeout")
                            .isEqualTo(Duration.ofSeconds(60));
                    softly.assertThat(ytDynamicConfig.globalTimeout())
                            .describedAs("global-timeout")
                            .isEqualTo(Duration.ofSeconds(90));
                    softly.assertThat(ytDynamicConfig.failoverTimeout())
                            .describedAs("failover-timeout")
                            .isEqualTo(Duration.ofSeconds(30));
                    softly.assertThat(ytDynamicConfig.inputRowsLimit())
                            .describedAs("input-rows-limit")
                            .isEqualTo(1_000_000_000L);
                    softly.assertThat(ytDynamicConfig.outputRowsLimit())
                            .describedAs("output-rows-limit")
                            .isEqualTo(10_000_000_000L);
                }
        );
    }
}
