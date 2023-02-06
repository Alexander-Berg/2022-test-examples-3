package ru.yandex.market.core.tmessage.service.impl;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Сервис таргетированных переменных, заботится только об эскейпинге переменных, которые он вставляет в шаблон.
 * Остальной эскейпинг производит TelegramBotContentProvider по правилам версии транспорта
 */
public class TargetMessageTemplateProcessorTest {
    @Test
    @DisplayName("Тестирует замену нескольких переменных в шаблоне и переиспользование процессора")
    public void testProcessTemplateWithSeveralVars() {
        var templateProcessor = new TargetMessageTemplateProcessor();
        var text = "Ша\\*бло\\*н, в кот\\_ором\\_ есть {{SHOP_ID}} и ещё {{CMP}}, а также {{PRODUCT_ID}}";
        assertThat(templateProcessor.prepareEscapedTextForRecipient(text, Map.of(
                "SHOP_ID", 111,
                "CMP", "22_2",
                "PRODUCT_ID", 333
        ))).isEqualTo("Ша\\*бло\\*н, в кот\\_ором\\_ есть 111 и ещё 22\\_2, а также 333");
        assertThat(templateProcessor.prepareEscapedTextForRecipient(text, Map.of(
                "SHOP_ID", 444,
                "CMP", 555,
                "PRODUCT_ID", 666
        ))).isEqualTo("Ша\\*бло\\*н, в кот\\_ором\\_ есть 444 и ещё 555, а также 666");
    }

    @Test
    @DisplayName("Тестирует замену нескольких переменных в шаблоне c использование спец символов")
    public void testProcessTemplateWithSeveralVarsAndSpecialChars() {
        var templateProcessor = new TargetMessageTemplateProcessor();
        var text = "Шаблон, в котором есть '*{{SHOP_ID}}**' и ещё \"{{CMP}}\", а также \"{{PRODUCT_ID}}!\"";
        assertThat(templateProcessor.prepareEscapedTextForRecipient(text, Map.of(
                "SHOP_ID", 555,
                "CMP", 666,
                "PRODUCT_ID", 777
        ))).isEqualTo("Шаблон, в котором есть '*555**' и ещё \"666\", а также \"777!\"");

    }

    @Test
    @DisplayName("Тестирует кейс, когда значения переменных содерждат спец. символы Markdown")
    public void testProcessTemplateWithSeveralVarsAndSpecialAndMarkdownCharsInVars() {
        var templateProcessor = new TargetMessageTemplateProcessor();
        var text = "Шаблон, в котором есть '*{{SHOP_ID}}**' и ещё \"{{CMP}}\", а также \"{{PRODUCT_ID}}!\", в " +
                "котором Markdown, а тут " +
                "{{PRODUCT_ID_2}} несколько";
        assertThat(templateProcessor.prepareEscapedTextForRecipient(text, Map.of(
                "SHOP_ID", 555,
                "CMP", 666,
                "PRODUCT_ID", "vendor_bids_csv@yandex.ru",
                "PRODUCT_ID_2", "Текст *жирный* и _италик_, а тут сивол {}[]}}(a) + and # -! =|."
        ))).isEqualTo(
                "Шаблон, в котором есть '*555**' и ещё \"666\", а также \"vendor\\_bids\\_csv@yandex.ru!\", в котором" +
                        " Markdown, а тут Текст \\*жирный\\* и \\_италик\\_, а тут сивол {}\\[]}}(a) + and # -! =|. " +
                        "несколько");
    }
}
