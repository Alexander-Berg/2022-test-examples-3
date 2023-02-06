package ru.yandex.direct.core.entity.adgroup.service;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MinusKeywordPreparingToolTest {

    @Autowired
    private MinusKeywordPreparingTool minusKeywordPreparingTool;

    @Test
    public void preprocess_WorksFine() {
        List<String> inputMinusKeywords = asList(
                "вакуумный конь ",
                " [Вакуумному на коню] в !зубы не смотрят",
                "не смотреть [вакуумный на конь] !зубы в  ",
                "составные слова санкт-петербург !по-русски");
        List<String> expectedPreparedMinusKeywords = asList(
                "вакуумный конь",
                "[Вакуумному на коню] !в !зубы !не смотрят",
                "!не смотреть [вакуумный на конь] !зубы !в",
                "составные слова санкт петербург !по !русски");
        List<String> actualPreparedMinusKeywords =
                minusKeywordPreparingTool.preprocess(inputMinusKeywords);
        assertThat("список подготовленных к сохранению минус-фраз соответствует ожидаемому",
                actualPreparedMinusKeywords, beanDiffer(expectedPreparedMinusKeywords));
    }

    @Test
    public void removeDuplicatesAndSort_WorksFine() {
        List<String> inputMinusKeywords = asList(
                "вакуумный конь",
                "[вакуумному на коню] !в !зубы !не смотрят",
                "!не смотреть [вакуумный на конь] !зубы !в");
        List<String> expectedPreparedMinusKeywords = asList(
                "[вакуумному на коню] !в !зубы !не смотрят",
                "вакуумный конь");
        List<String> actualPreparedMinusKeywords =
                minusKeywordPreparingTool.removeDuplicatesAndSort(inputMinusKeywords);
        assertThat("список подготовленных к сохранению минус-фраз соответствует ожидаемому",
                actualPreparedMinusKeywords, beanDiffer(expectedPreparedMinusKeywords));
    }

    @Test
    public void fullPrepareForSaving_WorksFine() {
        List<String> inputMinusKeywords = asList(
                " [Вакуумному на коню] в !зубы не смотрят",
                "не смотреть [вакуумный на конь] !зубы в  ",
                "вакуумный конь ");
        List<String> expectedPreparedMinusKeywords = asList(
                "[Вакуумному на коню] !в !зубы !не смотрят",
                "вакуумный конь");
        List<String> actualPreparedMinusKeywords =
                minusKeywordPreparingTool.fullPrepareForSaving(inputMinusKeywords);
        assertThat("список подготовленных к сохранению минус-фраз соответствует ожидаемому",
                actualPreparedMinusKeywords, beanDiffer(expectedPreparedMinusKeywords));
    }

    @Test
    public void mergePrivateAndLibrary_WorksFine() {
        List<String> privateMinusKeywords = asList("i678", "bbb1", "ggg6");
        List<List<String>> libraryMinusKeywords = asList(singletonList("abksdf2"), asList("ggg6", "xz22", "d100500"));
        List<String> result =
                minusKeywordPreparingTool.mergePrivateAndLibrary(privateMinusKeywords, libraryMinusKeywords);

        List<String> expected = asList("abksdf2", "bbb1", "d100500", "ggg6", "i678", "xz22");
        assertThat(result, contains(expected.toArray()));
    }

    @Test
    public void mergePrivateAndLibrary_EmptyPrivate_WorksFine() {
        List<String> privateMinusKeywords = emptyList();
        List<List<String>> libraryMinusKeywords = asList(singletonList("abksdf2"), asList("ggg6", "xz22", "d100500"));
        List<String> result =
                minusKeywordPreparingTool.mergePrivateAndLibrary(privateMinusKeywords, libraryMinusKeywords);

        List<String> expected = asList("abksdf2", "d100500", "ggg6", "xz22");
        assertThat(result, contains(expected.toArray()));
    }

    @Test
    public void mergePrivateAndLibrary_EmptyLibrary_WorksFine() {
        List<String> privateMinusKeywords = asList("i678", "bbb1", "ggg6");
        List<List<String>> libraryMinusKeywords = emptyList();
        List<String> result =
                minusKeywordPreparingTool.mergePrivateAndLibrary(privateMinusKeywords, libraryMinusKeywords);

        List<String> expected = asList("bbb1", "ggg6", "i678");
        assertThat(result, contains(expected.toArray()));
    }
}
