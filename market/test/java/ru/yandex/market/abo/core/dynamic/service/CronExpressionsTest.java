package ru.yandex.market.abo.core.dynamic.service;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.quartz.CronExpression;

import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author komarovns
 * @date 24.08.18
 */
public abstract class CronExpressionsTest extends EmptyTest {

    @Test
    public void validCron() {
        getCronExpressions().forEach(expr ->
                assertTrue(CronExpression.isValidExpression(expr), "invalid expression " + expr));
    }

    public abstract List<String> getCronExpressions();
}