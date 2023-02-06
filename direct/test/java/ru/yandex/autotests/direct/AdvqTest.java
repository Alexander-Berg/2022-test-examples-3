package ru.yandex.autotests.direct;

import org.junit.Test;
import ru.yandex.autotests.direct.tests.BaseTestClass;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * @author xy6er
 */

public class AdvqTest { //TODO-b rename
    private final int maxWordsCount = 1005; //Кол-во слов в файле words.txt

    @Test
    public void canGetWordsData() throws Exception {
        int wordsCount = 2;
        Properties.getInstance().setTestWordsCount(wordsCount);
        assertThat(BaseTestClass.wordsData().size(), equalTo(wordsCount));
    }

    @Test
    public void checkMaxWordsCount() throws Exception {
        int wordsCount = maxWordsCount + 1000;
        Properties.getInstance().setTestWordsCount(wordsCount);
        assertThat(BaseTestClass.wordsData().size(), equalTo(maxWordsCount));
    }

}
