package ru.yandex.market.logshatter.spring;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.logshatter.reader.logbroker.MonitoringLagThreshold;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 17/08/2018
 */
public class LogshatterSpringLogbrokerConfigTest {

    @Test
    public void parseIdentToLagThreshold() {
        Map<String, MonitoringLagThreshold> actual = LogshatterSpringLogbrokerConfig.parseIdentToLagThreshold(
            "ident1:1:2,ident2:0.25:7.42"
        );

        Map<String, MonitoringLagThreshold> expected = new HashMap<>();
        expected.put("ident1", new MonitoringLagThreshold(1, 2));
        expected.put("ident2", new MonitoringLagThreshold(0.25, 7.42));

        Assert.assertEquals(expected, actual);

    }
}