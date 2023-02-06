package ru.yandex.direct.grid.processing.service.showcondition.keywords;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.keyword.model.KeywordWithMinuses;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;

import static org.assertj.core.api.Assertions.assertThat;

@GridProcessingTest
@RunWith(Parameterized.class)
public class AdvqQueryForRefineTest {

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private RefineKeywordService refineKeywordService;

    @Parameterized.Parameter
    public String phrase;
    @Parameterized.Parameter(1)
    public List<String> minusWords;
    @Parameterized.Parameter(2)
    public String advqQuery;

    @Parameterized.Parameters(name = "{0} && -{1} => {2}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
                {
                        "красное платье",
                        Collections.singletonList("красное"),
                        "красное платье"
                },
                {
                        "красное платье",
                        Arrays.asList("купить платье", "ушанка"),
                        "красное платье -купить платье -ушанка"
                },
                {
                        "красное платье",
                        Arrays.asList("купить", "платье"),
                        "красное платье -купить"
                },
                {
                        "!красное платье",
                        Arrays.asList("!красное платье", "!красное", "платье", "красное", "зеленое", "красное яблоко"),
                        "!красное платье -красное яблоко -зеленое"
                },
                {
                        "!где +еще купить красное платье",
                        Arrays.asList(
                                "!где",
                                "+еще",
                                "+еще купить красное платье",
                                "красное",
                                "зеленое",
                                "красное яблоко"),
                        "!где +еще купить красное платье -красное яблоко -зеленое"
                },
                {
                        "слово -минус",
                        Collections.singletonList("плюс"),
                        "слово -минус -плюс"
                },
                {
                        "слово минус +на",
                        Arrays.asList("плюс", "на"),
                        "слово минус +на -плюс"
                },
                {
                        "слово минус +на",
                        Arrays.asList("плюс", "на", "+на"),
                        "слово минус +на -плюс"
                },
                {
                        "слово минус +на",
                        Arrays.asList("[плюс]", "[на]"),
                        "слово минус +на -плюс"
                },
                {
                        "слово минус +на",
                        Arrays.asList("белая вода", "\"котик\""),
                        "слово минус +на -\"котик\" -белая вода"
                },
                {
                        "пена морская",
                        Arrays.asList("мыльная пена", "\"котик\""),
                        "пена морская -\"котик\" -мыльная пена"
                },
                {
                        "пена морская",
                        Arrays.asList("[мыльная пена]", "\"котик\""),
                        "пена морская -\"котик\" -[мыльная пена]"
                },
                {
                        "цветы в горшках недорого",
                        Collections.singletonList("цветы недорого"),
                        "цветы в горшках недорого"
                },
                {
                        "цветы !в горшках недорого",
                        Collections.singletonList("цветы +в горшках"),
                        "цветы !в горшках недорого"
                },
                {
                        "!купить цветы +в горшках",
                        Collections.singletonList("!купить +в горшках"),
                        "!купить цветы +в горшках"
                },
                {
                        "купить цветы +в горшках",
                        Collections.singletonList("купить в горшках"),
                        "купить цветы +в горшках"
                },
                {
                        "билеты [Москва Париж]",
                        Collections.singletonList("[Москва] Париж"),
                        "билеты [Москва Париж]"
                }
        });
    }

    @Test
    public void testFormatAdvqQuery() {
        KeywordWithMinuses advqKeyword = refineKeywordService.buildKeywordToAdvq(phrase, minusWords);
        assertThat(advqKeyword.toString()).isEqualTo(advqQuery);
    }
}
