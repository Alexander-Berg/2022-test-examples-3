package ru.yandex.market.tpl.internal;

import org.springframework.context.annotation.Import;

import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.internal.config.TestTplIntConfiguration;
import ru.yandex.market.tpl.internal.config.TplIntSpringConfiguration;

@Import({TestTplIntConfiguration.class, TplIntSpringConfiguration.class})
public class TplIntAbstractTest extends TplAbstractTest {
}
