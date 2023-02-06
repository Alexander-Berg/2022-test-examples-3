package ru.yandex.direct.utils;

import java.util.Arrays;
import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.utils.StringUtils.areBracketsBalanced;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class StringUtilsAreBracketsBalancedTest {
    private static final char OPENING_BRACKET = '(';
    private static final char CLOSING_BRACKET = ')';
    private final String testString;
    private final boolean expectStringToBeBalanced;

    public StringUtilsAreBracketsBalancedTest(String testString, boolean expectStringToBeBalanced) {
        this.testString = testString;
        this.expectStringToBeBalanced = expectStringToBeBalanced;
    }

    @Parameterized.Parameters(name = "<{0}> -> {1}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"(", false},
                {")", false},
                {"(()", false},
                {"(продать (выгодно)", false},
                {")продать(", false},
                {"(word)", true},
                {" ( word ) ", true},
                {"word", true},
                {"\"(word)\"", true},
                {" \" (word) \" ", true},
                {"купить (в магазине)", true},
                {"на(конь)", true},
                {"(конь)на", true},
                {"купить(в)магазине(на)диване(сегодня)", true},
                {"(купить в Москве) (недорого у метро)", true},
                {"()", true},
                {"текст ()на русском", true},
                {"(()())", true},
                {"(текст (с) (вложенными) скобками)", true},
                {"((два))", true},
        });
    }

    @Test
    public void test() throws Exception {
        assertEquals("скобки считаются сбалансированными: " + expectStringToBeBalanced,
                areBracketsBalanced(testString, OPENING_BRACKET, CLOSING_BRACKET),
                expectStringToBeBalanced);
    }
}
