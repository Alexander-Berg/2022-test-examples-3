package ru.yandex.direct.grid.processing.util.findandreplace;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.grid.model.findandreplace.ChangeMode;
import ru.yandex.direct.grid.model.findandreplace.FindAndReplaceParams;
import ru.yandex.direct.grid.model.findandreplace.ReplaceRule;
import ru.yandex.direct.grid.model.findandreplace.SearchOptions;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.processing.util.InputTestDataUtils.searchOptions;

@RunWith(Parameterized.class)
public class ReplaceRuleTest {

    private ReplaceRule replaceRule;

    @Parameterized.Parameter
    public String text;

    @Parameterized.Parameter(1)
    public String searchText;

    @Parameterized.Parameter(2)
    public String changeText;

    @Parameterized.Parameter(3)
    public ChangeMode changeMode;

    @Parameterized.Parameter(4)
    public SearchOptions searchOptions;

    @Parameterized.Parameter(5)
    public String expectedText;

    @Parameterized.Parameters(name = "text={0}, searchText={1}, changeText={2}, expectedText = {5}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                //check AlwaysReplaceRule
                {
                        "новый парк -официальный -сайт", "", "отель", ChangeMode.REPLACE, searchOptions(false, false),
                        "отель"
                },
                {
                        "новый парк -официальный -сайт", "", "отель", ChangeMode.PREFIX, searchOptions(false, false),
                        "отель" + "новый парк -официальный -сайт"
                },
                {
                        "новый парк -официальный -сайт", "", "отель", ChangeMode.POSTFIX, searchOptions(false, false),
                        "новый парк -официальный -сайт" + "отель"
                },
                {
                        "новый парк -официальный -сайт", "", "searchOptions not matter", ChangeMode.REPLACE,
                        searchOptions(true, true),
                        "searchOptions not matter"
                },

                //check PatternReplaceRule
                {
                        "новый парк -официальный -сайт", "новый парк", "отель", ChangeMode.REPLACE,
                        searchOptions(false, false),
                        "отель -официальный -сайт"
                },
                {
                        "t->' ' новый\tпарк -официальный -сайт", "новый парк", "отель", ChangeMode.REPLACE,
                        searchOptions(false, false),
                        "t->' ' отель -официальный -сайт"
                },
                {
                        "n->' ' новый\nпарк -официальный -сайт", "новый парк", "отель", ChangeMode.REPLACE,
                        searchOptions(false, false),
                        "n->' ' отель -официальный -сайт"
                },
                {
                        "n->t новый\nпарк -официальный -сайт", "новый\tпарк", "отель", ChangeMode.REPLACE,
                        searchOptions(false, false),
                        "n->t отель -официальный -сайт"
                },
                {
                        "t->n новый\tпарк -официальный -сайт", "новый\nпарк", "отель", ChangeMode.REPLACE,
                        searchOptions(false, false),
                        "t->n отель -официальный -сайт"
                },
                {
                        "NBSP->' ' новый парк -официальный -сайт", "новый парк", "отель", ChangeMode.REPLACE,
                        searchOptions(false, false),
                        "NBSP->' ' отель -официальный -сайт"
                },
                {
                        "NBSP->NBSP новый парк -официальный -сайт", "новый парк", "отель", ChangeMode.REPLACE,
                        searchOptions(false, false),
                        "NBSP->NBSP отель -официальный -сайт"
                },
                {
                        "NBSP->THSP новый парк -официальный -сайт", "новый парк", "отель", ChangeMode.REPLACE,
                        searchOptions(false, false),
                        "NBSP->THSP отель -официальный -сайт"
                },
                {
                        "THSP->' ' новый парк -официальный -сайт", "новый парк", "отель", ChangeMode.REPLACE,
                        searchOptions(false, false),
                        "THSP->' ' отель -официальный -сайт"
                },
                {
                        "новый парк -официальный -сайт", "парк", "отелЬ", ChangeMode.PREFIX,
                        searchOptions(false, false),
                        "новый отелЬпарк -официальный -сайт"
                },
                {
                        "новый парк -официальный -сайт", "парк", "Отель", ChangeMode.POSTFIX,
                        searchOptions(false, false),
                        "новый паркОтель -официальный -сайт"
                },
                //check matchCase
                {
                        "новый парк -официальный -сайт", "Новый", "Старый", ChangeMode.REPLACE,
                        searchOptions(false, false),
                        "Старый парк -официальный -сайт"
                },
                {
                        "новый парк -официальный -сайт", "Новый", "Старый", ChangeMode.REPLACE,
                        searchOptions(true, false),
                        null
                },
                //check onlyWholeWords
                {
                        "новый парк Новый -официальный -НОВЫЙ", "Новый", "СТАРЫЙ", ChangeMode.REPLACE,
                        searchOptions(false, true),
                        "СТАРЫЙ парк СТАРЫЙ -официальный -СТАРЫЙ"
                },
                {
                        "new парк New -официальный -NEW", "New", "Old", ChangeMode.REPLACE, searchOptions(false, true),
                        "Old парк Old -официальный -Old"
                },
                {
                        "новый парк Новый -официальный -НОВЫЙ", "Новый", "СТАРЫЙ", ChangeMode.REPLACE,
                        searchOptions(true, true),
                        "новый парк СТАРЫЙ -официальный -НОВЫЙ"
                },
                {
                        "паркИ парк Ипарк", "парк", "отель_", ChangeMode.PREFIX, searchOptions(false, true),
                        "паркИ отель_парк Ипарк"
                },
                {
                        "паркИ парк Ипарк", "парк", "_отель", ChangeMode.POSTFIX, searchOptions(false, true),
                        "паркИ парк_отель Ипарк"
                },
                //check escapeSpecialSymbols
                {
                        "фраза со спец символами !+\"[]()| ", "[]", " Квадратные скобки ", ChangeMode.REPLACE,
                        searchOptions(false, false),
                        "фраза со спец символами !+\" Квадратные скобки ()| "
                },
                {
                        "фраза не должна меняться", ".*", "", ChangeMode.REPLACE, searchOptions(false, false),
                        null
                },
        });
    }

    @Before
    public void initTestData() {
        FindAndReplaceParams findAndReplaceParams = new FindAndReplaceParams() {
            @Override
            public String getSearchText() {
                return searchText;
            }

            @Override
            public String getChangeText() {
                return changeText;
            }

            @Override
            public ChangeMode getChangeMode() {
                return changeMode;
            }

            @Override
            public Set getFields() {
                return null;
            }

            @Override
            public SearchOptions getSearchOptions() {
                return searchOptions;
            }
        };

        replaceRule = ReplaceRuleHelper.getReplaceRule(findAndReplaceParams);
    }


    @Test
    public void checkReplaceRule() {
        assertThat(replaceRule.apply(text))
                .isEqualTo(expectedText);
    }

}
