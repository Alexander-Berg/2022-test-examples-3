package ru.yandex.market.logistics.lom;

import org.springframework.context.annotation.Import;

import ru.yandex.market.logistics.lom.configuration.YdbConfigurationMock;

@Import(YdbConfigurationMock.class)
public class AbstractContextualTest extends AbstractContextualCommonTest {
}
