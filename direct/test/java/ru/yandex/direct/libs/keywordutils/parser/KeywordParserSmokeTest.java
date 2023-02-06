package ru.yandex.direct.libs.keywordutils.parser;

import org.junit.Test;

import ru.yandex.advq.query.ast.Expression;
import ru.yandex.advq.query.ast.Intersection;
import ru.yandex.advq.query.ast.SquareBrackets;
import ru.yandex.advq.query.ast.Word;
import ru.yandex.advq.query.ast.WordKind;
import ru.yandex.direct.libs.keywordutils.model.Keyword;
import ru.yandex.direct.libs.keywordutils.model.KeywordWithMinuses;
import ru.yandex.direct.libs.keywordutils.model.SingleKeyword;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Smoke-тест на то, что парсинг парсит. По факту сейчас {@link KeywordParser} использует ADVQ'шный парсер.
 * Парсер используется в тестах на включение фраз и нормализацию.
 */
public class KeywordParserSmokeTest {

    @Test
    public void buildExpression_success() {
        String query = "привет [+новый !мир]";
        Expression actual = KeywordParser.buildExpression(query);

        Expression expected = new Intersection(asList(
                new Word(WordKind.RAW, "привет"),
                new SquareBrackets(asList(
                        new Word(WordKind.PLUS, "новый"),
                        new Word(WordKind.FIXED, "мир")
                ))
        ));
        assertThat("ADVQ'шный Expression успешно создан по заданной фразе", actual, beanDiffer(expected));
    }

    @Test
    public void parseWithMinuses_withUnion_success() {
        String query = "aaa ( xxx | yyy ) bbb";
        KeywordWithMinuses actual = KeywordParser.parseWithMinuses(query);

        KeywordWithMinuses expected = new KeywordWithMinuses(new Keyword(asList(
                new SingleKeyword(new Word(WordKind.RAW, "aaa")),
                new SingleKeyword(new Word(WordKind.RAW, "xxx")),
                new SingleKeyword(new Word(WordKind.RAW, "yyy")),
                new SingleKeyword(new Word(WordKind.RAW, "bbb"))
        )));
        assertThat("Expression успешно создан", actual, beanDiffer(expected));
    }

    @Test
    public void buildExpressionWithQuote1_success() {
        String query = "xxx 'yyy";
        Expression actual = KeywordParser.buildExpression(query);

        Expression expected = new Intersection(asList(
                new Word(WordKind.RAW, "xxx"),
                new Word(WordKind.RAW, "yyy")
        ));
        assertThat("Expression успешно создан", actual, beanDiffer(expected));
    }

    @Test
    public void buildExpressionWithQuote2_success() {
        String query = "''xxx yyy";
        Expression actual = KeywordParser.buildExpression(query);

        Expression expected = new Intersection(asList(
                new Word(WordKind.RAW, "xxx"),
                new Word(WordKind.RAW, "yyy")
        ));
        assertThat("Expression успешно создан", actual, beanDiffer(expected));
    }

    @Test
    public void buildExpressionWithQuote3_success() {
        String query = "x'xx yyy'";
        Expression actual = KeywordParser.buildExpression(query);

        Expression expected = new Intersection(asList(
                new Word(WordKind.RAW, "x'xx"),
                new Word(WordKind.RAW, "yyy'")
        ));
        assertThat("Expression успешно создан", actual, beanDiffer(expected));
    }

    @Test
    public void buildExpressionWithQuote4_success() {
        String query = "xxx\t'yyy\n'";
        Expression actual = KeywordParser.buildExpression(query);

        Expression expected = new Intersection(asList(
                new Word(WordKind.RAW, "xxx"),
                new Word(WordKind.RAW, "yyy")
        ));
        assertThat("Expression успешно создан", actual, beanDiffer(expected));
    }

    @Test
    public void buildExpressionWithQuote5_success() {
        String query = "'' xxx yyy";
        Expression actual = KeywordParser.buildExpression(query);

        Expression expected = new Intersection(asList(
                new Word(WordKind.RAW, "xxx"),
                new Word(WordKind.RAW, "yyy")
        ));
        assertThat("Expression успешно создан", actual, beanDiffer(expected));
    }

    @Test
    public void buildExpressionWithQuote6_success() {
        String query = "'' xxx '' gfdg ''yyy";
        Expression actual = KeywordParser.buildExpression(query);

        Expression expected = new Intersection(asList(
                new Word(WordKind.RAW, "xxx"),
                new Word(WordKind.RAW, "gfdg"),
                new Word(WordKind.RAW, "yyy")
        ));
        assertThat("Expression успешно создан", actual, beanDiffer(expected));
    }

    @Test
    public void serializeCreatedKeyword_success() {
        String input = "привет на [о !дивный +новый мир] арбуз";
        Expression expression = KeywordParser.buildExpression(input);

        Keyword keyword = Keyword.from(expression);
        String serialized = keyword.toString();

        assertThat("Сериализованное представление фразы равно исходному", serialized, is(input));
    }

}
