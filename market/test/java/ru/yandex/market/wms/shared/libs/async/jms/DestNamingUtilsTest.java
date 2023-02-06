package ru.yandex.market.wms.shared.libs.async.jms;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DestNamingUtilsTest {
    @Test
    public void shouldSuccessGetTemplate() {
        final String actualTemplateName = DestNamingUtils.toTemplateName("test-message-queue");

        Assertions.assertEquals("{mq}_{wrh}_test-message-queue", actualTemplateName);
    }

    @Test
    public void shouldSuccessGetShortQueueNameTemplate() {
        final String actualShortName = DestNamingUtils.removePrefix("{mq}_{wrh}_test-message-queue");

        Assertions.assertEquals("test-message-queue", actualShortName);
    }

    @Test
    public void shouldSuccessGetShortQueueNameReplaced() {
        final String actualShortName = DestNamingUtils.removePrefix("rmq_123_test-message-queue");

        Assertions.assertEquals("test-message-queue", actualShortName);
    }

    @Test
    public void shouldSuccessAddFirstParamToQueueName() {
        final String actualDestination = DestNamingUtils.addParam("{mq}_{wrh}_test-message-queue",
                "param1", "value1"
        );

        Assertions.assertEquals("{mq}_{wrh}_test-message-queue?param1=value1", actualDestination);
    }

    @Test
    public void shouldSuccessAddSecondParamToQueueName() {
        final String actualDestination = DestNamingUtils.addParam(
                "{mq}_{wrh}_test-message-queue?param1=value1",
                "param2", "value2"
        );

        Assertions.assertEquals("{mq}_{wrh}_test-message-queue?param1=value1;param2=value2",
                actualDestination);
    }

    @Test
    public void shouldSuccessExtractParamsFromQueueName() {
        final Map<String, String> actualParams = DestNamingUtils.extractParams(
                "{mq}_{wrh}_test-message-queue?param1=value1;param2=value2"
        );

        Assertions.assertEquals(Map.ofEntries(
                Map.entry("param1", "value1"),
                Map.entry("param2", "value2")
        ), actualParams);
    }

    @Test
    public void shouldSuccessRemoveParamsFromQueueName() {
        final String actualQueueName = DestNamingUtils.removeParams(
                "{mq}_{wrh}_test-message-queue?param1=value1;param2=value2"
        );

        Assertions.assertEquals("{mq}_{wrh}_test-message-queue", actualQueueName);
    }

    @Test
    public void shouldSuccessExtractParamsToDestParams() {
        DestParams actualParams = DestNamingUtils.extractParamsToDestParams(
                "{mq}_{wrh}_test-message-queue?param1=value1;param2=value2"
        );

        Assertions.assertEquals("value1", actualParams.getParam("param1"));
        Assertions.assertEquals("value2", actualParams.getParam("param2"));
    }
}
