package ru.yandex.market.replenishment.autoorder.api.security;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.replenishment.autoorder.config.security.tvm.TvmProperties;


@ExtendWith(SpringExtension.class)
@RunWith(SpringRunner.class)
@EnableConfigurationProperties(value = TvmProperties.class)
@TestPropertySource(locations = "classpath:tvm-test.properties")
public class TvmPropertiesTest {

    @Autowired
    private TvmProperties tvmProperties;

    @Test
    public void testSourceProperties() {
        Assert.assertEquals(Set.of(1L, 2L, 3L, 4L, 5L, 6L), tvmProperties.getValidTvmSourceIdValues());
        Assert.assertEquals(Set.of(10L, 20L, 30L, 40L, 50L, 60L), tvmProperties.getValidTvmDestIdValues());
    }
}
