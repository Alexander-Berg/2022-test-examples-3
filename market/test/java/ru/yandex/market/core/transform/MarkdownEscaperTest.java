package ru.yandex.market.core.transform;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.notification.service.provider.content.markdown.MarkdownEscaper;
import ru.yandex.market.notification.telegram.bot.model.dto.ParseMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MarkdownEscaperTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("ru.yandex.market.core.transform.MarkdownEscaperTest#dataToEscapeUnderlineAndStarsV2")
    public void testEscapingUnderlineAndStars(String expected, String text) {
        assertEquals(expected, MarkdownEscaper.newInstance(ParseMode.MarkdownV2).escapeEntity(text));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("ru.yandex.market.core.transform.MarkdownEscaperTest#dataWithUnderlineAndStarsV2")
    public void testEscapingSpecialCharsExceptUnderlineAndStars(String expected, String text) {
        assertEquals(expected, MarkdownEscaper.newInstance(ParseMode.MarkdownV2).escapeText(text));
    }

    /**
     * Эмуляция того как работает TelegramBotContentProvider:
     * - отдельно эскейпит переменные и в самом конце;
     * - затем эскейпятся остальные символы.
     *
     * @param expected ожидание
     * @param text     оригинальный текст
     * @param italics  сущности, которые ставятся в оригинальный текст, например первая заменит {italic0}, вторая {
     *                 italic1} и тд
     * @param bolds    замены для {bold0}, {bold1} и тд
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("ru.yandex.market.core.transform.MarkdownEscaperTest#complexDataV2")
    public void testTwoPassEscapingV2(MarkdownEscaper markdownEscaper,
                                      String expected,
                                      String text,
                                      String[] italics,
                                      String[] bolds) {
        testTwoPassEscaping(markdownEscaper, expected, text, italics, bolds);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("ru.yandex.market.core.transform.MarkdownEscaperTest#complexDataV1")
    public void testTwoPassEscapingV1(MarkdownEscaper markdownEscaper,
                                      String expected,
                                      String text,
                                      String[] italics,
                                      String[] bolds) {
        testTwoPassEscaping(markdownEscaper, expected, text, italics, bolds);
    }

    private void testTwoPassEscaping(MarkdownEscaper markdownEscaper, String expected, String text, String[] italics,
                                     String[] bolds) {
        text = replaceEntities(markdownEscaper, "italic", "_", text, italics);
        text = replaceEntities(markdownEscaper, "bold", "*", text, bolds);
        text = markdownEscaper.escapeText(text);
        assertEquals(expected, text);
    }

    private String replaceEntities(MarkdownEscaper markdownEscaper, String entityName, String entityChar, String text
            , String[] entities) {
        for (int i = 0; i < entities.length; i++) {
            String escaped = String.format("%s%s%s", entityChar, markdownEscaper.escapeEntity(entities[i]), entityChar
            );
            text = text.replace("{" + entityName + i + "}", escaped);
        }
        return text;
    }

    private static Stream<Arguments> dataToEscapeUnderlineAndStarsV2() {
        return Stream.of(
                Arguments.of(
                        // возможная проблема - двойной эскейпинг
                        "test \\_italic\\_ test2 \\_itl\\\\_ic\\_ bo\\*ld\\* but b\\*o\\\\*ld\\* -\\_|",
                        "test _italic_ test2 _itl\\_ic_ bo*ld* but b*o\\*ld* -_|")
        );
    }

    private static Stream<Arguments> dataWithUnderlineAndStarsV2() {
        return Stream.of(
                Arguments.of(
                        "test _italic_ test2 _itl\\_ic_ bo*ld* but b*o\\*ld* \\-_\\|",
                        "test _italic_ test2 _itl\\_ic_ bo*ld* but b*o\\*ld* -_|"),
                Arguments.of(
                        "[в Excel\\-шаблоне](https://yandex.ru/support/marketplace/assortment/files/excel-fby-fbs.html)",
                        "[в Excel-шаблоне](https://yandex.ru/support/marketplace/assortment/files/excel-fby-fbs.html)"),
                Arguments.of(
                        "my _italic_ was t_e\\+st * how\\,\\| anV\\]ER\\[E\\.D [чер\\.ез API](https://yandex)",
                        "my _italic_ was t_e+st * how,| anV]ER[E.D [чер.ез API](https://yandex)")
        );
    }

    private static Stream<Arguments> complexDataV2() {
        return Stream.of(
                Arguments.of(
                        MarkdownEscaper.newInstance(ParseMode.MarkdownV2),
                        // произошла замена _ и * внутри сущностей,
                        // но если в шаблонах есть _ и * то экранироваться не будут.
                        "m_\\_itali\\+c ent\\_ity\\__ _italic_ was t_e\\+st * h\\-_\\| \\-_\\| ow\\,\\| " +
                                "*\\*bo\\*l\\_d\\** anV\\]ER\\[E\\.D [чер\\.ез API](https://yandex)[в " +
                                "Excel\\-шаблоне](https://yandex.ru/support/marketplace/assortment/files/excel-fby-fbs.html)",

                        "m{italic0} _italic_ was t_e+st * h-_| -_| ow,| {bold0} anV]ER[E.D [чер.ез API]" +
                                "(https://yandex)[в Excel-шаблоне](https://yandex" +
                                ".ru/support/marketplace/assortment/files/excel-fby-fbs.html)",

                        new String[]{"_itali+c ent_ity_"},
                        new String[]{"*bo*l_d*"}
                )
        );
    }

    private static Stream<Arguments> complexDataV1() {
        return Stream.of(
                Arguments.of(
                        MarkdownEscaper.newInstance(ParseMode.Markdown),
                        // для V1 вы экранировали только символы Markdown Entities
                        "m_\\_itali+c ent\\_ity\\__ _italic_ wa`s` t_e+st * h-_| -_| ow,| *\\*bo\\*l\\_d\\** anV]ER[E.D" +
                                " [чер.ез API](https://yandex)[в Excel-шаблоне](https://yandex" +
                                ".ru/support/marketplace/assortment/files/excel-fby-fbs.html)",

                        "m{italic0} _italic_ wa`s` t_e+st * h-_| -_| ow,| {bold0} anV]ER[E.D [чер.ез API]" +
                                "(https://yandex)[в Excel-шаблоне](https://yandex" +
                                ".ru/support/marketplace/assortment/files/excel-fby-fbs.html)",

                        new String[]{"_itali+c ent_ity_"},
                        new String[]{"*bo*l_d*"}
                )
        );
    }
}
