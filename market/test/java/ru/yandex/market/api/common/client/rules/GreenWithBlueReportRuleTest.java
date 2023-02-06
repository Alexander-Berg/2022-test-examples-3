package ru.yandex.market.api.common.client.rules;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.integration.ContainerTestBase;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;

@WithContext
public class GreenWithBlueReportRuleTest extends ContainerTestBase {

    @Inject
    private GreenWithBlueReportRule rule;

    @Test
    public void testDefaultIsFalse() {
        Assert.assertFalse(rule.test());
    }

    @Test
    public void testFalse() {
        ContextHolder.update(ctx -> ctx.setGreenWithBlueReport(false));
        Assert.assertFalse(rule.test());
    }

    @Test
    public void testTrue() {
        ContextHolder.update(ctx -> ctx.setGreenWithBlueReport(true));
        Assert.assertTrue(rule.test());
    }

}