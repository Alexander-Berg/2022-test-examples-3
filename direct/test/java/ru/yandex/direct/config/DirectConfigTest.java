package ru.yandex.direct.config;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

public class DirectConfigTest {
    private DirectConfig cfg;

    @Before
    public void setUp() throws Exception {
        Config config = DirectConfigFactory
                .readConfigFromFile(new ClassPathResource("sample-config.conf").getFile().getAbsolutePath());
        this.cfg = new DirectConfig(config);
    }

    @Test
    public void asMapWorks() {
        Assertions.assertThat(cfg.getBranch("sendmail").asMap())
                .isEqualTo(ImmutableMap.of(
                        "enabled", "true",
                        "redirect_address", "direct-dev-letters@yandex-team.ru",
                        "num", "12"));
    }
}
