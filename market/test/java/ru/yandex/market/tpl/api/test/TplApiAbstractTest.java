package ru.yandex.market.tpl.api.test;

import org.springframework.context.annotation.Import;

import ru.yandex.market.tpl.api.confg.TestTplApiConfiguration;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

@Import(TestTplApiConfiguration.class)
public class TplApiAbstractTest extends TplAbstractTest {
}
