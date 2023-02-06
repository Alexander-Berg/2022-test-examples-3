package ru.yandex.market.tsum.exp3;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.experiments3.client.Exp3MatcherCallException;
import ru.yandex.market.experiments3.client.Experiments3Client;
import ru.yandex.market.experiments3.client.generated.model.ResponseItem;
import ru.yandex.market.tsum.exp3configs.Exp3Configs;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    Exp3TestBeansConfig.class
})
public class Exp3MatcherTest {
    @Autowired
    private Experiments3Client experiments3Client;

    @Test
    public void getConfigsTest() throws Exp3MatcherCallException {
        List<ResponseItem> items = experiments3Client.getConfigs("tsum");
        Assert.assertEquals(3, items.size());
        Assert.assertEquals("1", items.get(0).getName());
    }

    @Test
    public void getConfigByNameTest() throws Exp3MatcherCallException, InvalidProtocolBufferException,
        JsonProcessingException {
        Exp3Configs.Settings testConfig = experiments3Client.getConfig("tsum", "release_launch_blocker",
            Exp3Configs.Settings.class);
        Assert.assertEquals("market_dev_exp", testConfig.getProjectsList().get(0));
    }
}
