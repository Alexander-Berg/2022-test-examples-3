package ru.yandex.market.tsum.core.clients.idx;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.clients.idx.RecentGeneration;
import ru.yandex.market.tsum.core.TsumDebugRuntimeConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TsumDebugRuntimeConfig.class})
public class RecentGenerationTest {
    private RecentGeneration recentGeneration;

    @Before
    public void setUp() {
        recentGeneration = RecentGeneration.builder()
            .withName("foo")
            .build();
    }

    @Test
    public void builderWorks() {
        Assert.assertEquals("foo", recentGeneration.getName());
    }

    @Test
    public void builderFromInstanceWorks() {
        RecentGeneration newGeneration = RecentGeneration.builder(recentGeneration)
            .build();

        Assert.assertEquals("foo", recentGeneration.getName());
    }
}