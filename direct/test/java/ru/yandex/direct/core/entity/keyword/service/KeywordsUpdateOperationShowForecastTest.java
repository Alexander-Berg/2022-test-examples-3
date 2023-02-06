package ru.yandex.direct.core.entity.keyword.service;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.keyword.container.UpdatedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsUpdateOperationShowForecastTest extends KeywordsUpdateOperationBaseTest {
    private static final long INITIAL_FORECAST = 12738L;

    /**
     * Если меняют не текст фразы, прогноз не должен меняться
     */
    @Test
    public void execute_ChangePrice_ForecastNotChanged() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeywordWithInitialForecast();

        Long newPrice = 10L;
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_1, newPrice));
        doUpdate(changesKeywords);

        assertThat(getKeyword(keywordIdToUpdate).getShowsForecast(), is(INITIAL_FORECAST));
    }

    /**
     * Если меняют текст фразы, должны обновить прогноз показов
     */
    @Test
    public void execute_ChangePhrase_ForecastChanged() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeywordWithInitialForecast();

        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_2));
        doUpdate(changesKeywords);

        assertThat(getKeyword(keywordIdToUpdate).getShowsForecast(), is(DEFAULT_SHOWS_FORECAST));
    }

    private void doUpdate(List<ModelChanges<Keyword>> changesKeywords) {
        KeywordsUpdateOperation operation = createOperation(Applicability.PARTIAL, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isFullySuccessful());
    }

    private long createKeywordWithInitialForecast() {
        Keyword keyword = getDefaultActiveKeyword(PHRASE_1).withShowsForecast(INITIAL_FORECAST);
        return keywordSteps.createKeyword(adGroupInfo1, keyword).getId();
    }
}
