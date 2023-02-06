package ru.yandex.market.partner.notification.transform;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.partner.notification.transform.common.BasicTemplateGenerationTest;
import ru.yandex.market.partner.notification.transform.common.TemplateId;

public class MobilePushTemplateGenerationTest extends BasicTemplateGenerationTest {

    MobilePushTemplateGenerationTest() {
        super("mobile_push");
    }

    @Disabled
    @ParameterizedTest(name = "{0}")
    @MethodSource("ru.yandex.market.partner.notification.transform.common.BasicTemplateGenerationTest#data")
    void testTemplateGenerationMobilePush(TemplateId templateId) {
        //TODO move from mbi-core and enable after transports implementation
    }

}
