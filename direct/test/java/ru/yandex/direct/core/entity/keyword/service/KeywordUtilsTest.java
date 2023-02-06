package ru.yandex.direct.core.entity.keyword.service;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnitParamsRunner.class)
public class KeywordUtilsTest {

    Object[] keywords() {
        return new Object[][]{
                {"word", "word"},
                {"---autotargeting word", "word"},
                {null, null},
                {"word ---autotargeting", "word ---autotargeting"},
                //Если во фразе содержится только ---autotargeting, то это признак автотаргетинга на всю группу
                {" ---autotargeting      ", "---autotargeting"}
        };
    }

    @Test
    @Parameters(method = "keywords")
    public void phraseWithoutAutotargetingPrefix(String inputKeyword, String expectedKeyword) {
        assertThat(KeywordUtils.phraseWithoutAutotargetingPrefix(inputKeyword)).isEqualTo(expectedKeyword);
    }

    Object[] hasPrefix() {
        return new Object[][] {
                {"word", false},
                {"---autotargeting word", true},
                {null, false},
                {"one ---autotargeting two", false},
                {"---autotargeting  ", false}
        };
    }

    @Test
    @Parameters(method = "hasPrefix")
    public void hasAutotargetingPrefix(String inputKeyword, boolean result) {
        assertThat(KeywordUtils.hasAutotargetingPrefix(inputKeyword)).isEqualTo(result);
    }
}
