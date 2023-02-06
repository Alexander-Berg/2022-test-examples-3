package ru.yandex.market.mboc.common.masterdata.parsing.validator;

import java.util.Optional;
import java.util.stream.IntStream;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import ru.yandex.market.mboc.common.utils.ErrorInfo;

/**
 * @author dmserebr
 * @date 28/02/2019
 */
@SuppressWarnings("checkstyle:magicNumber")
public class CommentValidatorTest {
    private static final String TEST_HEADER_TITLE = "TestHeader";

    private CommentValidator commentValidator = new CommentValidator();

    private static String generateLongString(int length) {
        StringBuilder longStringBuilder = new StringBuilder();
        IntStream.range(0, length).forEach(i -> longStringBuilder.append(i % 10));
        return longStringBuilder.toString();
    }

    @Test
    public void testLength() {
        SoftAssertions.assertSoftly(softly -> {
            Optional<ErrorInfo> nullResult = commentValidator.validate(TEST_HEADER_TITLE, null);
            softly.assertThat(nullResult).isNotPresent();

            Optional<ErrorInfo> emptyResult = commentValidator.validate(TEST_HEADER_TITLE, "");
            softly.assertThat(emptyResult).isNotPresent();

            Optional<ErrorInfo> normalResult = commentValidator.validate(
                TEST_HEADER_TITLE, "Тестовый комментарий"
            );
            softly.assertThat(normalResult).isNotPresent();

            Optional<ErrorInfo> tooLongResult = commentValidator.validate(
                TEST_HEADER_TITLE, generateLongString(300)
            );
            assertErrorInfoOpt(softly, "Содержимое столбца TestHeader имеет длину в 300 символов, " +
                "допустимо максимум 250", tooLongResult);

            Optional<ErrorInfo> notTooLongResult = commentValidator.validate(
                TEST_HEADER_TITLE, generateLongString(250)
            );
            softly.assertThat(notTooLongResult).isNotPresent();
        });
    }

    @Test
    public void testAllowedCharacters() {
        SoftAssertions.assertSoftly(softly -> {
            Optional<ErrorInfo> normalResult = commentValidator.validate(TEST_HEADER_TITLE,
                "Тестовый комментарий ёЁ .,;()-–—?!'\"«»&%/");
            softly.assertThat(normalResult).isNotPresent();

            Optional<ErrorInfo> degreeResult = commentValidator.validate(TEST_HEADER_TITLE,
                "срок годности - 5 суток при температуре от 10 до 15 °C, 10 суток при температуре до 10 °C");
            softly.assertThat(degreeResult).isNotPresent();

            Optional<ErrorInfo> chineseResult = commentValidator.validate(TEST_HEADER_TITLE, "汉字");
            assertErrorInfoOpt(softly, "Содержимое столбца TestHeader содержит недопустимые символы. " +
                "Допустимы только русские буквы, латинские буквы, " +
                "цифры, пробелы и следующие знаки: .,;()-?!'\"«»&%/°", chineseResult);

            Optional<ErrorInfo> numberResult = commentValidator.validate(TEST_HEADER_TITLE, "№1 в");
            softly.assertThat(numberResult).isNotPresent();
        });
    }

    private void assertErrorInfoOpt(SoftAssertions softly, String expected, Optional<ErrorInfo> actual) {
        softly.assertThat(actual).isPresent();
        softly.assertThat(actual.get().toString()).isEqualTo(expected);
    }
}
