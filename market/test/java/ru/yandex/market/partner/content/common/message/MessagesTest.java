package ru.yandex.market.partner.content.common.message;

import com.google.common.collect.ImmutableMap;
import io.qameta.allure.Issue;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.ir.http.ProtocolMessage;
import ru.yandex.market.partner.content.common.utils.MessageUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/*
 * For test in IDEA: File | Settings | Build, Execution, Deployment | Compiler | Java Compiler
 * Add to "Additional command line parameters:"
 * -parameters
 */
@Issue("MARKETIR-7056")
public class MessagesTest {
    @Test
    public void testExcelWrongFileFormat() {
        final MessageInfo message = Messages.get().excelWrongFileFormat("message");
        assertEquals(message.getParams(),
            ImmutableMap.of("message", "message")
        );
    }

    @Test
    public void testDefaultErrorLevel() {
        final MessageInfo message = Messages.get().excelWrongFileFormat("message");
        assertEquals(MessageInfo.Level.ERROR, message.getLevel());
    }

    @Test
    public void testSetWarningLevel() {
        final MessageInfo message = Messages.get(MessageInfo.Level.WARNING).excelWrongFileFormat("message");
        assertEquals(MessageInfo.Level.WARNING, message.getLevel());
    }

    @Test
    public void testUseAnnotationLevel() {
        final MessageInfo message = Messages.get().modelCreatedMessage("message", 1L, 1L);
        assertEquals(MessageInfo.Level.INFO, message.getLevel());
    }

    @Test
    public void testOverrideAnnotationLevel() {
        final MessageInfo message = Messages.get(MessageInfo.Level.WARNING).modelCreatedMessage("message", 1L, 1L);
        assertEquals(MessageInfo.Level.WARNING, message.getLevel());
    }

    @Test
    public void testEverythingCanCompile() {
        Messages.Holder.methodToAnnotation.values()
            .forEach(template -> MessageInfo.tryCompile(template.message()));
    }

    @Test
    public void testNoDuplicates() {
        List<Map.Entry<String, Long>> duplicates = Messages.Holder.methodToAnnotation.values().stream()
            .map(Messages.MessageTemplate::code)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet().stream()
            .filter(e -> e.getValue() > 1)
            .collect(Collectors.toList());

        Assertions.assertThat(duplicates).isEmpty();
    }

    @Test
    public void testRecreate() {
        final MessageInfo message = Messages.get().excelWrongFileFormat("message");
        final MessageInfo recreatedMessageInfo = Messages.get().recreateMessageInfo(
            message.getCode(), message.getParams(), message.getLevel()
        );
        assertEquals(message.getParams(), recreatedMessageInfo.getParams());
        assertEquals(message.getCode(), recreatedMessageInfo.getCode());
        assertEquals(message.getMessageTemplate(), recreatedMessageInfo.getMessageTemplate());
        assertEquals(message.getLevel(), recreatedMessageInfo.getLevel());
    }

    @Test
    public void testRecreateWarning() {
        final MessageInfo message = Messages.get(MessageInfo.Level.WARNING).excelWrongFileFormat("message");
        final MessageInfo recreatedMessageInfo = Messages.get().recreateMessageInfo(
            message.getCode(), message.getParams(), message.getLevel()
        );
        assertEquals(message.getParams(), recreatedMessageInfo.getParams());
        assertEquals(message.getCode(), recreatedMessageInfo.getCode());
        assertEquals(message.getMessageTemplate(), recreatedMessageInfo.getMessageTemplate());
        assertEquals(message.getLevel(), recreatedMessageInfo.getLevel());
    }

    @Test
    public void testRecreateWithNullLevelParamUseDefault() {
        final MessageInfo message = Messages.get(MessageInfo.Level.WARNING).excelWrongFileFormat("message");
        final MessageInfo recreatedMessageInfo = Messages.get().recreateMessageInfo(
            message.getCode(), message.getParams(), null
        );
        assertEquals(message.getParams(), recreatedMessageInfo.getParams());
        assertEquals(message.getCode(), recreatedMessageInfo.getCode());
        assertEquals(message.getMessageTemplate(), recreatedMessageInfo.getMessageTemplate());
        assertEquals(MessageInfo.Level.ERROR, recreatedMessageInfo.getLevel());
    }

    @Test
    public void testRecreateOfCustomLevelWithNullLevelParamUseCustom() {
        final MessageInfo message = Messages.get(MessageInfo.Level.WARNING).excelWrongFileFormat("message");
        final MessageInfo recreatedMessageInfo = Messages.get(MessageInfo.Level.INFO).recreateMessageInfo(
            message.getCode(), message.getParams(), null
        );
        assertEquals(message.getParams(), recreatedMessageInfo.getParams());
        assertEquals(message.getCode(), recreatedMessageInfo.getCode());
        assertEquals(message.getMessageTemplate(), recreatedMessageInfo.getMessageTemplate());
        assertEquals(MessageInfo.Level.INFO, recreatedMessageInfo.getLevel());
    }

    @Test
    public void testRecreateOfCustomLevelWithLevelParamUseLevelParam() {
        final MessageInfo message = Messages.get(MessageInfo.Level.WARNING).excelWrongFileFormat("message");
        final MessageInfo recreatedMessageInfo = Messages.get(MessageInfo.Level.INFO).recreateMessageInfo(
            message.getCode(), message.getParams(), MessageInfo.Level.CRITICAL
        );
        assertEquals(message.getParams(), recreatedMessageInfo.getParams());
        assertEquals(message.getCode(), recreatedMessageInfo.getCode());
        assertEquals(message.getMessageTemplate(), recreatedMessageInfo.getMessageTemplate());
        assertEquals(MessageInfo.Level.CRITICAL, recreatedMessageInfo.getLevel());
    }

    @Test
    public void testMessageToProto() {
        String paramName = "paramName";
        String paramValue = "paramValue";
        String shopSKU = "shopSKU";

        final MessageInfo message = Messages.get().skuWrongParam(shopSKU, paramName, paramValue);

        ProtocolMessage.Message proto = MessageUtils.convertToProto(message);

        assertEquals("ir.partner_content.error.sku.wrong_param", proto.getCode());
        assertEquals("Неверно заполнен параметр {{paramName}} в shop_sku {{shopSKU}}. " +
            "Предоставленное неверное значение: {{paramValue}}.", proto.getTemplate());
        assertEquals("{\"shopSKU\":\"" + shopSKU + "\"," +
                "\"paramName\":\"" + paramName + "\"," +
                "\"paramValue\":\"" + paramValue + "\"}",
            proto.getParams()
        );
    }

    @Test
    @Issue("MARKETIR-7382")
    public void testPictureInvalid() {
        final MessageInfo message = Messages.get().pictureInvalid(
            "http://абракадабра.рф/qwerty", false, true, true, false, "AAAAA"
        );
        final String stringMessage = message.toString();
        assertEquals("С изображением http://абракадабра.рф/qwerty, представленным в AAAAA, обнаружены проблемы:\n" +
                "изображение имеет не верный размер,\n" +
                "изображение НЕ на белом фоне",
            stringMessage
        );
    }

    @Test
    public void testDifferentModelNames() {
        String defaultError = Messages.get().differentModelNames(
            Collections.singletonList("partner sku"),
            "partner model",
            Collections.singletonList("market model")).toString();
        assertEquals(
            "Shop_sku partner sku,  имя 'partner model' не соответствует имени маркетной модели 'market model', .",
            defaultError
        );

        String multipleMarketNames = Messages.get().differentModelNames(
            Collections.singletonList("partner sku"),
            "partner model",
            Arrays.asList("market model 1", "market model 2")).toString();
        assertEquals(
            "Shop_sku partner sku,  имя 'partner model' не соответствует имени маркетной модели 'market model 1'," +
                " 'market model 2', .",
            multipleMarketNames
        );
    }

    @Test
    @Issue("MARKETIR-9772")
    public void gcParamContainsUrl() {
        final MessageInfo param = Messages.get().gcParamContainsUrl(
            "12345", "param", -2l
        );
        assertEquals("ir.partner_content.goodcontent.validation.text.param_contains_url", param.getCode());
        assertNotNull(param.getParameterId());
        assertEquals(-2L, param.getParameterId().longValue());
    }

    @Test
    @Issue("MARKETIR-9772")
    public void checkDefaultNull() {
        final MessageInfo message = Messages.get().gcCwImageWatermark("shopSku", 1, "url", null);
        final Map<String, Object> params = new HashMap<>(message.getParams());
        params.remove("paramId");
        final MessageInfo recreateMessage = Messages.get().recreateMessageInfo(
            message.getCode(), params, message.getLevel()
        );
        assertEquals(message, recreateMessage);
    }
}
