package ru.yandex.direct.core.entity.keyword.service;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static com.google.common.primitives.Longs.asList;
import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class KeywordServiceTest {

    @Autowired
    private KeywordService serviceUnderTest;

    @Autowired
    private Steps steps;

    @Test
    public void getKeywords_success_getByKeywordIds() throws Exception {
        KeywordInfo keyword = steps.keywordSteps().createDefaultKeyword();
        List<Keyword> keywords = serviceUnderTest.getKeywords(keyword.getAdGroupInfo().getClientId(),
                asList(keyword.getId()));
        assertThat(keywords).hasSize(1);
    }

    @Test
    public void getKeywords_getOnlyOneItem_whenRequestedKeywordIdsForManyClients() throws Exception {
        KeywordInfo keyword = steps.keywordSteps().createDefaultKeyword();
        @SuppressWarnings("unused")
        KeywordInfo keywordOtherClient = steps.keywordSteps().createDefaultKeyword();

        List<Keyword> keywords = serviceUnderTest.getKeywords(keyword.getAdGroupInfo().getClientId(),
                asList(keyword.getId()));
        assertThat(keywords).hasSize(1);
    }

}
