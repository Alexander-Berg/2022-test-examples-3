package ru.yandex.direct.core.entity.keyword.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import javax.annotation.Nullable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.client.model.ClientLimits;
import ru.yandex.direct.core.entity.keyword.container.KeywordsModificationResult;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.showcondition.container.ShowConditionAutoPriceParams;
import ru.yandex.direct.core.entity.showcondition.container.ShowConditionFixedAutoPrices;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.Result;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.container.KeywordsModificationContainer.addUpdateDelete;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAdded;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdated;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsModifyOperationAutoPricesTest extends KeywordsModifyOperationBaseTest {
    private static final BigDecimal OLD_SEARCH_PRICE = BigDecimal.valueOf(854).setScale(2, RoundingMode.UNNECESSARY);
    private static final BigDecimal OLD_CONTEXT_PRICE = BigDecimal.valueOf(472).setScale(2, RoundingMode.UNNECESSARY);
    private static final BigDecimal OLD_SEARCH_PRICE2 = BigDecimal.valueOf(63).setScale(2, RoundingMode.UNNECESSARY);
    private static final BigDecimal OLD_CONTEXT_PRICE2 = BigDecimal.valueOf(90).setScale(2, RoundingMode.UNNECESSARY);
    private static final BigDecimal FIXED_AUTO_PRICE = BigDecimal.valueOf(885).setScale(2, RoundingMode.UNNECESSARY);

    /**
     * Если создать операцию в режиме {@code autoPrices}, и не передать
     * контейнер с фиксированными ставками, конструктор сразу упадет.
     */
    @Test(expected = IllegalArgumentException.class)
    public void create_withNullFixedAutoPrices_throwsException() {
        createOneActiveAdGroup();
        List<Keyword> addList = singletonList(newKeywordEmptyPrices(adGroupInfo1, PHRASE_1));
        createOperation(addUpdateDelete(addList, emptyList(), emptyList()), true, null);
    }

    /**
     * Если удалить фразу, и тут же ее добавить в эту же группу с тем же текстом,
     * должны быть выставлены ставки от старой фразы.
     * Тут проверяется, что в операцию {@link KeywordsAddOperation} прокидывается
     * режим {@code autoPrices}, и передается внешний калькулятор автоставок.
     * Т.к. состояние старых ставок можно передать только через внешний калькулятор,
     * т.к. в момент выполнения добавления старая фраза уже была удалена.
     */
    @Test
    public void execute_deletePhraseAndInsertSame_oldPricesAreSet() {
        createOneActiveAdGroup();
        Keyword oldKeyword = createKeyword(adGroupInfo1, PHRASE_1, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE).getKeyword();
        List<Keyword> addList = singletonList(newKeywordEmptyPrices(adGroupInfo1, PHRASE_1));
        List<Long> deleteList = singletonList(oldKeyword.getId());
        ShowConditionAutoPriceParams autoPriceParams = makeAutoPriceParams(null);
        Result<KeywordsModificationResult> result =
                executeWithAutoPrices(addList, emptyList(), deleteList, autoPriceParams);
        assertResultIsSuccessful(result, singletonList(isAdded(PHRASE_1)), null, deleteList);
        assertKeywordPrices(
                result.getResult().getAddResults().get(0).getId(),
                oldKeyword.getPrice(),
                oldKeyword.getPriceContext()
        );
    }

    /**
     * Если удалить фразу, и одновременно обновить другую фразу, выставив там текст
     * от удаленной, обновленной фразе должны быть выставлены ставки от удаленной.
     * Тут проверяется, что в операцию {@link KeywordsUpdateOperation} прокидывается
     * режим {@code autoPrices}, и передается внешний калькулятор автоставок.
     * Т.к. состояние старых ставок можно передать только через внешний калькулятор,
     * т.к. в момент выполнения обновления старая фраза уже была удалена.
     */
    @Test
    public void execute_deletePhraseAndUpdateToSame_oldPricesAreSet() {
        createOneActiveAdGroup();
        Keyword oldKeyword = createKeyword(adGroupInfo1, PHRASE_1, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE).getKeyword();
        Keyword keywordToUpdate =
                createKeyword(adGroupInfo1, PHRASE_2, OLD_SEARCH_PRICE2, OLD_CONTEXT_PRICE2).getKeyword();
        List<Long> deleteList = singletonList(oldKeyword.getId());
        List<ModelChanges<Keyword>> updateList = singletonList(keywordModelChanges(keywordToUpdate.getId(), PHRASE_1));
        ShowConditionAutoPriceParams autoPriceParams = makeAutoPriceParams(null);
        Result<KeywordsModificationResult> result =
                executeWithAutoPrices(emptyList(), updateList, deleteList, autoPriceParams);
        assertResultIsSuccessful(result, null, singletonList(isUpdated(keywordToUpdate.getId(), PHRASE_1)), deleteList);
        assertKeywordPrices(keywordToUpdate.getId(), oldKeyword.getPrice(), oldKeyword.getPriceContext());
    }

    /**
     * Если добавить и обновить фразу в указанием фиксированной автоставки,
     * у них выставится она.
     * Тут проверяется, что фиксированные автоставки корректно передаются в
     * {@link KeywordsAddOperation} и {@link KeywordsUpdateOperation}.
     */
    @Test
    public void execute_addAndUpdateWithFixedAutoPrice_setFixedAutoPrice() {
        createOneActiveAdGroup();
        List<Keyword> addList = singletonList(newKeywordEmptyPrices(adGroupInfo1, PHRASE_1));
        Keyword keywordToUpdate =
                createKeyword(adGroupInfo1, PHRASE_2, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE).getKeyword();
        List<ModelChanges<Keyword>> updateList = singletonList(keywordModelChanges(keywordToUpdate.getId(), PHRASE_3));

        ShowConditionAutoPriceParams autoPriceParams = makeAutoPriceParams(FIXED_AUTO_PRICE);
        Result<KeywordsModificationResult> result =
                executeWithAutoPrices(addList, updateList, emptyList(), autoPriceParams);
        assertResultIsSuccessful(
                result,
                singletonList(isAdded(PHRASE_1)),
                singletonList(isUpdated(keywordToUpdate.getId(), PHRASE_3)),
                null
        );

        assertKeywordPrices(result.getResult().getAddResults().get(0).getId(), FIXED_AUTO_PRICE, FIXED_AUTO_PRICE);
        assertKeywordPrices(keywordToUpdate.getId(), FIXED_AUTO_PRICE, FIXED_AUTO_PRICE);
    }

    /**
     * Если добавить фразы в группу с переполнением и с указанием фиксированной
     * автоставки, в скопированную группу будет добавлена фраза с фиксированной
     * ставкой.
     * Тут проверяется, что режим {@code autoPrices} и его параметры корректно
     * передаются в {@link KeywordsAddOperation} при переполнении.
     */
    @Test
    public void execute_addWithOversize_groupCopyCreatedWithAutoPrices() {
        createOneActiveAdGroup();
        setKeywordsCountLimit(1L);
        List<Keyword> keywords =
                asList(newKeywordEmptyPrices(adGroupInfo1, PHRASE_1), newKeywordEmptyPrices(adGroupInfo1, PHRASE_2));
        ShowConditionAutoPriceParams autoPriceParams = makeAutoPriceParams(FIXED_AUTO_PRICE);
        Result<KeywordsModificationResult> result = executeAddWithOversizeAndAutoPrices(keywords, autoPriceParams);
        assertResultIsSuccessful(result, asList(isAdded(PHRASE_1), isAdded(PHRASE_2)), null, null);

        assertKeywordPrices(result.getResult().getAddResults().get(0).getId(), FIXED_AUTO_PRICE, FIXED_AUTO_PRICE);
        assertKeywordPrices(result.getResult().getAddResults().get(1).getId(), FIXED_AUTO_PRICE, FIXED_AUTO_PRICE);
    }

    /**
     * Установка ограничения на количество фраз в группе.
     */
    private void setKeywordsCountLimit(long count) {
        steps.clientSteps().updateClientLimits(clientInfo
                .withClientLimits((ClientLimits) new ClientLimits().withClientId(clientInfo.getClientId())
                        .withKeywordsCountLimit(count)));
    }

    /**
     * Создание контейнера с параметрами для автоматического вычисления ставок.
     * В параметре {@code fixedAutoPrice} указывается фиксированная автоставка,
     * и может быть {@code null}. В контейнере устанавливается фейковый
     * поставщик недавней статистики по фразам, который всегда отвечает,
     * что статистики нет.
     */
    private ShowConditionAutoPriceParams makeAutoPriceParams(@Nullable BigDecimal fixedAutoPrice) {
        KeywordRecentStatisticsProvider recentStatisticsProvider = keywordRequests -> emptyMap();
        return new ShowConditionAutoPriceParams(
                ShowConditionFixedAutoPrices.ofGlobalFixedPrice(fixedAutoPrice),
                recentStatisticsProvider
        );
    }

    /**
     * Проверка, что у фразы выставились указанные ставки, а приоритет
     * автобюджета сбросился в {@code null}
     */
    private void assertKeywordPrices(long keywordId, BigDecimal searchPrice, BigDecimal contextPrice) {
        Keyword keyword = getKeyword(keywordId);
        assertThat(keyword.getPrice(), is(searchPrice));
        assertThat(keyword.getPriceContext(), is(contextPrice));
        assertThat(keyword.getAutobudgetPriority(), nullValue());
    }
}
