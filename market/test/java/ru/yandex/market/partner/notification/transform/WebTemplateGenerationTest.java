package ru.yandex.market.partner.notification.transform;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.notification.simple.model.type.NotificationTransport;
import ru.yandex.market.partner.notification.transform.common.BasicTemplateGenerationTest;
import ru.yandex.market.partner.notification.transform.common.TemplateId;


/**
 * Тесты по генерации шаблонов для транспорта {@link NotificationTransport#MBI_WEB_UI}.
 *
 * @author Vladislav Bauer
 */
class WebTemplateGenerationTest extends BasicTemplateGenerationTest {

    WebTemplateGenerationTest() {
        super("ui");
    }

    @Disabled
    @ParameterizedTest(name = "{0}")
    @MethodSource("ru.yandex.market.partner.notification.transform.common.BasicTemplateGenerationTest#data")
    void testTemplateGenerationWeb(TemplateId templateId) throws Exception {
        //TODO move from mbi-core and enable after transports implementation
    }
}
