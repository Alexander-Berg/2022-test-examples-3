package ru.yandex.direct.core.entity.keyword.service;

import org.junit.Test;

import ru.yandex.direct.bshistory.History;
import ru.yandex.direct.core.entity.keyword.model.Keyword;

import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.CloneTestUtil.fillIgnoring;

public class KeywordUtilsKeywordCloneTest {

    @Test
    public void clone_WorksFine() {
        Keyword sourceKeyword = new Keyword()
                .withPhraseIdHistory(History.parse("O" + nextInt(0, Integer.MAX_VALUE)));
        fillIgnoring(sourceKeyword, Keyword.PHRASE_ID_HISTORY.name());

        Keyword clonedKeyword = KeywordUtils.cloneKeyword(sourceKeyword);
        assertThat(clonedKeyword, beanDiffer(sourceKeyword));
    }
}
