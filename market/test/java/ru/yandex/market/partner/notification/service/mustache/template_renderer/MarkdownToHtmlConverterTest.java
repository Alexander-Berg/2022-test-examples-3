package ru.yandex.market.partner.notification.service.mustache.template_renderer;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class MarkdownToHtmlConverterTest {

    MarkdownToHtmlConverter converter;

    @BeforeEach
    void setUp() {
        converter = new MarkdownToHtmlConverter();
    }

    @Test
    public void messageConversionText() {
        converter = new MarkdownToHtmlConverter();

        assertThat(
                converter.convert(
                        "# Заголовок\n" +
                                "Параграф\n" +
                                "\n" +
                                "Тестовое сообщение для магазина *Магазин*:\n" +
                                " * элемент списка 1\n" +
                                " * элемент списка 2\n" +
                                "\n" +
                                "Подпись"
                ),
                equalTo("<h1>Заголовок</h1>\n" +
                        "Параграф\n" +
                        "\n" +
                        "Тестовое сообщение для магазина <em>Магазин</em>:\n" +
                        "<ul>\n" +
                        "<li>элемент списка 1</li>\n" +
                        "<li>элемент списка 2</li>\n" +
                        "</ul>\n" +
                        "Подпись\n\n"
                )
        );
    }

    private static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("Заголовок",
                        "# Заголовок\n",
                        "<h1>Заголовок</h1>\n"
                ),
                Arguments.of(
                        "Нумерованный список",
                        "1. Первый элемент списка.\n" +
                                "2. Второй элемент списка.\n",
                        "<ol>\n" +
                                "<li>Первый элемент списка.</li>\n" +
                                "<li>Второй элемент списка.</li>\n" +
                                "</ol>\n"
                ),
                Arguments.of(
                        "Маркированный список",
                        "- Первый элемент списка.\n" +
                                "- Второй элемент списка.\n",
                        "<ul>\n" +
                                "<li>Первый элемент списка.</li>\n" +
                                "<li>Второй элемент списка.</li>\n" +
                                "</ul>\n"
                )
        );
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("data")
    public void test(String description, String baseContent, String expectedContent) {
        converter = new MarkdownToHtmlConverter();

        assertThat(converter.convert(baseContent), equalTo(expectedContent));
    }
}
