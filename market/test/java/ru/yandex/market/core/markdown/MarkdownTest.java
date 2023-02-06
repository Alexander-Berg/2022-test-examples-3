package ru.yandex.market.core.markdown;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.core.markdown.template.FontMarkdownTemplate;
import ru.yandex.market.core.markdown.template.PlainUrlMarkdownTemplate;

/**
 * Тесты для {@link Markdown}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class MarkdownTest {

    private static Stream<Arguments> dataPlainUrlMarkdownTemplate() {
        return Stream.of(
                Arguments.of(
                        "Есть замена без аттрибутов тега",
                        "текст [ссылка](http://url.ru) еще текст",
                        "текст ссылка (http://url.ru) еще текст"
                ),
                Arguments.of(
                        "Есть замена с аттрибутами тега",
                        "текст [ссылка|{\"target\": \"_blank\"}](http://url.ru) еще текст",
                        "текст ссылка (http://url.ru) еще текст"
                ),
                Arguments.of(
                        "Нет замены. Только скобки",
                        "текст [не ссылка] еще текст",
                        "текст [не ссылка] еще текст"
                ),
                Arguments.of(
                        "Нет замены. Просто url",
                        "текст (не ссылка) еще текст",
                        "текст (не ссылка) еще текст"
                ),
                Arguments.of(
                        "Bold текст",
                        "some text was **formatted** here",
                        "some text was formatted here"
                ),
                Arguments.of(
                        "Italic текст",
                        "some text was *formatted* here",
                        "some text was formatted here"
                ),
                Arguments.of(
                        "Bold italic текст",
                        "some text was ***formatted*** here",
                        "some text was formatted here"
                )
        );
    }

    private static Markdown createMarkdown() {
        final DefaultMarkdownFactory factory = DefaultMarkdownFactory.builder()
                .addTemplate(new PlainUrlMarkdownTemplate())
                .addTemplate(FontMarkdownTemplate.BOLD_ITALIC_TEMPLATE)
                .addTemplate(FontMarkdownTemplate.BOLD_TEMPLATE)
                .addTemplate(FontMarkdownTemplate.ITALIC_TEMPLATE)
                .build();
        return factory.create();
    }

    @ParameterizedTest
    @MethodSource("dataPlainUrlMarkdownTemplate")
    void testPlainUrlMarkdownTemplate(final String name, final String from, final String expected) {
        final Markdown markdown = createMarkdown();
        final String actual = markdown.process(from);
        Assertions.assertEquals(expected, actual);
    }
}
