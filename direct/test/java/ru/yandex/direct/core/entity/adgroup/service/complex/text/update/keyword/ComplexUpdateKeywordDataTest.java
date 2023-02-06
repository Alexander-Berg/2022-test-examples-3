package ru.yandex.direct.core.entity.adgroup.service.complex.text.update.keyword;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.KeywordInfo;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

/**
 * Тесты сохранения и удаления ключевых фраз в соответствующих группах
 */
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexUpdateKeywordDataTest extends ComplexUpdateKeywordTestBase {

    @Test
    public void oneAdGroupWithEmptyKeywords() {
        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(emptyList());
        updateAndCheckResultIsEntirelySuccessful(adGroup);
    }

    // добавление

    @Test
    public void oneAdGroupWithOneAddedKeyword() {
        Keyword keyword = randomKeyword();
        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(singletonList(keyword));

        updateAndCheckResultIsEntirelySuccessful(adGroup);

        List<String> adGroupKeywords = findKeywordsInAdGroup(adGroupInfo1.getAdGroupId());
        assertThat("в группе должна присутствовать добавленная фраза",
                adGroupKeywords,
                contains(keyword.getPhrase()));

        int clientKeywordsCount = getClientKeywordsCount();
        assertThat("общее количество ключевых фраз клиента не соответствует ожидаемому",
                clientKeywordsCount, is(1));
    }

    @Test
    public void oneAdGroupWithOneAddedKeywordWithParenthesis() {
        Keyword keyword = randomKeyword().withPhrase("(раз|два) фраза");
        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(singletonList(keyword));

        updateAndCheckResultIsEntirelySuccessful(adGroup);

        List<String> adGroupKeywords = findKeywordsInAdGroup(adGroupInfo1.getAdGroupId());
        assertThat("в группе должны присутствовать добавленные фразы",
                adGroupKeywords,
                containsInAnyOrder("раз фраза", "два фраза"));

        int clientKeywordsCount = getClientKeywordsCount();
        assertThat("общее количество ключевых фраз клиента не соответствует ожидаемому",
                clientKeywordsCount, is(2));
    }

    @Test
    public void oneAdGroupWithTwoAddedKeywords() {
        Keyword keyword1 = randomKeyword();
        Keyword keyword2 = randomKeyword();
        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(asList(keyword1, keyword2));

        updateAndCheckResultIsEntirelySuccessful(adGroup);

        List<String> adGroupKeywords = findKeywordsInAdGroup(adGroupInfo1.getAdGroupId());
        assertThat("в группе должны присутствовать добавленные фразы",
                adGroupKeywords,
                containsInAnyOrder(keyword1.getPhrase(), keyword2.getPhrase()));

        int clientKeywordsCount = getClientKeywordsCount();
        assertThat("общее количество ключевых фраз клиента не соответствует ожидаемому",
                clientKeywordsCount, is(2));
    }

    @Test
    public void oneAdGroupWithTwoAddedKeywordsWithParenthesis() {
        Keyword keyword1 = randomKeyword().withPhrase("(раз|два) фраза");
        Keyword keyword2 = randomKeyword().withPhrase("фраза (три|четыре)");
        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(asList(keyword1, keyword2));

        updateAndCheckResultIsEntirelySuccessful(adGroup);

        List<String> adGroupKeywords = findKeywordsInAdGroup(adGroupInfo1.getAdGroupId());
        assertThat("в группе должны присутствовать добавленные фразы",
                adGroupKeywords,
                containsInAnyOrder("раз фраза", "два фраза", "фраза три", "фраза четыре"));

        int clientKeywordsCount = getClientKeywordsCount();
        assertThat("общее количество ключевых фраз клиента не соответствует ожидаемому",
                clientKeywordsCount, is(4));
    }

    @Test
    public void oneEmptyAdGroupAndOneAdGroupWithOneAddedKeyword() {
        createSecondAdGroup();

        Keyword keyword = randomKeyword();
        ComplexTextAdGroup adGroup1 = createValidAdGroupForUpdate(adGroupInfo1);
        ComplexTextAdGroup adGroup2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withKeywords(singletonList(keyword));

        updateAndCheckResultIsEntirelySuccessful(asList(adGroup1, adGroup2));

        List<String> adGroup2Keywords = findKeywordsInAdGroup(adGroupInfo2.getAdGroupId());
        assertThat("в группе должна присутствовать добавленная фраза",
                adGroup2Keywords,
                contains(keyword.getPhrase()));

        int clientKeywordsCount = getClientKeywordsCount();
        assertThat("общее количество ключевых фраз клиента не соответствует ожидаемому",
                clientKeywordsCount, is(1));
    }

    /**
     * Проверяем, что включение режима {@code autoPrices} корректно
     * прокидывается до {@link ru.yandex.direct.core.entity.keyword.service.KeywordsModifyOperation}
     */
    @Test
    public void oneAdGroupWithAddedKeywordWithAutoPrices() {
        assumeManualStrategyWithDifferentPlaces();

        Keyword keyword = randomKeyword()
                .withPrice(null)
                .withPriceContext(null);
        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(singletonList(keyword));

        updateWithAutoPricesAndCheckResultIsEntirelySuccessful(adGroup);

        List<Keyword> keywords = keywordRepository.getKeywordsByAdGroupId(shard, adGroupInfo1.getAdGroupId());
        assumeThat("в группе появилась одна фраза", keywords, hasSize(1));
        keyword = keywords.get(0);
        assertThat("у фразы выставилась автоматическая поисковая ставка", keyword.getPrice(), is(FIXED_AUTO_PRICE));
        assertThat("у фразы выставилась автоматическая ставка в сети", keyword.getPriceContext(), is(FIXED_AUTO_PRICE));
    }

    // обновление

    @Test
    public void oneAdGroupWithOneUpdatedKeyword() {
        KeywordInfo keywordInfo = createKeyword(adGroupInfo1);

        Keyword keyword = randomKeyword(keywordInfo.getId());
        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(singletonList(keyword));

        updateAndCheckResultIsEntirelySuccessful(adGroup);

        List<String> adGroupKeywords = findKeywordsInAdGroup(adGroupInfo1.getAdGroupId());
        assertThat("в группе должна присутствовать обновленная фраза",
                adGroupKeywords,
                contains(keyword.getPhrase()));

        int clientKeywordsCount = getClientKeywordsCount();
        assertThat("общее количество ключевых фраз клиента не соответствует ожидаемому",
                clientKeywordsCount, is(1));
    }

    @Test
    public void oneAdGroupWithOneUpdatedKeywordWithParenthesis() {
        KeywordInfo keywordInfo = createKeyword(adGroupInfo1);

        Keyword keyword = randomKeyword(keywordInfo.getId()).withPhrase("(раз|два) фраза");
        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(singletonList(keyword));

        updateAndCheckResultIsEntirelySuccessful(adGroup);

        List<String> adGroupKeywords = findKeywordsInAdGroup(adGroupInfo1.getAdGroupId());
        assertThat("в группе должно присутствовать обновленные фразы",
                adGroupKeywords,
                contains("раз фраза", "два фраза"));

        int clientKeywordsCount = getClientKeywordsCount();
        assertThat("общее количество ключевых фраз клиента не соответствует ожидаемому",
                clientKeywordsCount, is(2));
    }

    @Test
    public void oneAdGroupWithTwoUpdatedKeywords() {
        KeywordInfo keywordInfo1 = createKeyword(adGroupInfo1);
        KeywordInfo keywordInfo2 = createKeyword(adGroupInfo1);

        Keyword keyword1 = randomKeyword(keywordInfo1.getId());
        Keyword keyword2 = randomKeyword(keywordInfo2.getId());
        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(asList(keyword1, keyword2));

        updateAndCheckResultIsEntirelySuccessful(adGroup);

        List<String> adGroupKeywords = findKeywordsInAdGroup(adGroupInfo1.getAdGroupId());
        assertThat("в группе должны присутствовать обновленные фразы",
                adGroupKeywords,
                containsInAnyOrder(keyword1.getPhrase(), keyword2.getPhrase()));

        int clientKeywordsCount = getClientKeywordsCount();
        assertThat("общее количество ключевых фраз клиента не соответствует ожидаемому",
                clientKeywordsCount, is(2));
    }

    @Test
    public void oneAdGroupWithTwoUpdatedKeywordsWithParenthesis() {
        KeywordInfo keywordInfo1 = createKeyword(adGroupInfo1);
        KeywordInfo keywordInfo2 = createKeyword(adGroupInfo1);

        Keyword keyword1 = randomKeyword(keywordInfo1.getId()).withPhrase("(раз|два) фраза");
        Keyword keyword2 = randomKeyword(keywordInfo2.getId()).withPhrase("фраза (три|четыре)");
        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(asList(keyword1, keyword2));

        updateAndCheckResultIsEntirelySuccessful(adGroup);

        List<String> adGroupKeywords = findKeywordsInAdGroup(adGroupInfo1.getAdGroupId());
        assertThat("в группе должны присутствовать обновленные фразы",
                adGroupKeywords,
                containsInAnyOrder("раз фраза", "два фраза", "фраза три", "фраза четыре"));

        int clientKeywordsCount = getClientKeywordsCount();
        assertThat("общее количество ключевых фраз клиента не соответствует ожидаемому",
                clientKeywordsCount, is(4));
    }

    @Test
    public void oneEmptyAdGroupAndOneAdGroupWithOneUpdatedKeyword() {
        createSecondAdGroup();
        KeywordInfo keywordInfo = createKeyword(adGroupInfo2);

        Keyword keyword = randomKeyword(keywordInfo.getId());
        ComplexTextAdGroup adGroup1 = createValidAdGroupForUpdate(adGroupInfo1);
        ComplexTextAdGroup adGroup2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withKeywords(singletonList(keyword));

        updateAndCheckResultIsEntirelySuccessful(asList(adGroup1, adGroup2));

        List<String> adGroup2Keywords = findKeywordsInAdGroup(adGroupInfo2.getAdGroupId());
        assertThat("в группе должна присутствовать обновленная фраза",
                adGroup2Keywords,
                contains(keyword.getPhrase()));

        int clientKeywordsCount = getClientKeywordsCount();
        assertThat("общее количество ключевых фраз клиента не соответствует ожидаемому",
                clientKeywordsCount, is(1));
    }

    // удаление

    @Test
    public void oneAdGroupWithOneDeletedKeyword() {
        createKeyword(adGroupInfo1);

        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1);

        updateAndCheckResultIsEntirelySuccessful(adGroup);

        List<String> adGroupKeywords = findKeywordsInAdGroup(adGroupInfo1.getAdGroupId());
        assertThat("в группе не должно присутствовать фраз", adGroupKeywords, emptyIterable());

        int clientKeywordsCount = getClientKeywordsCount();
        assertThat("общее количество ключевых фраз клиента не соответствует ожидаемому",
                clientKeywordsCount, is(0));
    }

    @Test
    public void oneAdGroupWithTwoDeletedKeyword() {
        createKeyword(adGroupInfo1);
        createKeyword(adGroupInfo1);

        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1);

        updateAndCheckResultIsEntirelySuccessful(adGroup);

        List<String> adGroupKeywords = findKeywordsInAdGroup(adGroupInfo1.getAdGroupId());
        assertThat("в группе не должно присутствовать фраз", adGroupKeywords, emptyIterable());

        int clientKeywordsCount = getClientKeywordsCount();
        assertThat("общее количество ключевых фраз клиента не соответствует ожидаемому",
                clientKeywordsCount, is(0));
    }

    @Test
    public void oneAdGroupWithOneDeletedKeywordAndOneUntouchedAdGroup() {
        createSecondAdGroup();
        createKeyword(adGroupInfo1);
        createKeyword(adGroupInfo2);

        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1);

        updateAndCheckResultIsEntirelySuccessful(adGroup);

        List<String> adGroup1Keywords = findKeywordsInAdGroup(adGroupInfo1.getAdGroupId());
        assertThat("в группе не должно присутствовать фраз", adGroup1Keywords, emptyIterable());

        List<String> adGroup2Keywords = findKeywordsInAdGroup(adGroupInfo2.getAdGroupId());
        assertThat("в незатронутой группе должна присутствовать фраза", adGroup2Keywords, hasSize(1));

        int clientKeywordsCount = getClientKeywordsCount();
        assertThat("общее количество ключевых фраз клиента не соответствует ожидаемому",
                clientKeywordsCount, is(1));
    }

    // добавление/обновление/удаление

    @Test
    public void oneAdGroupWithOneUpdatedAndOneAddedKeyword() {
        KeywordInfo keywordInfo = createKeyword(adGroupInfo1);

        Keyword updatedKeyword = randomKeyword(keywordInfo.getId());
        Keyword addedKeyword = randomKeyword();
        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(asList(updatedKeyword, addedKeyword));

        updateAndCheckResultIsEntirelySuccessful(adGroup);

        List<String> adGroupKeywords = findKeywordsInAdGroup(adGroupInfo1.getAdGroupId());
        assertThat("в группе должна присутствовать добавленная и обновленная фразы",
                adGroupKeywords,
                containsInAnyOrder(updatedKeyword.getPhrase(), addedKeyword.getPhrase()));

        int clientKeywordsCount = getClientKeywordsCount();
        assertThat("общее количество ключевых фраз клиента не соответствует ожидаемому",
                clientKeywordsCount, is(2));
    }

    @Test
    public void oneAdGroupWithOneUpdatedAndOneAddedKeywordWithParenthesis() {
        KeywordInfo keywordInfo = createKeyword(adGroupInfo1);

        Keyword updatedKeyword = randomKeyword(keywordInfo.getId()).withPhrase("(раз|два) фраза");
        Keyword addedKeyword = randomKeyword().withPhrase("фраза (три|четыре)");
        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(asList(updatedKeyword, addedKeyword));

        updateAndCheckResultIsEntirelySuccessful(adGroup);

        List<String> adGroupKeywords = findKeywordsInAdGroup(adGroupInfo1.getAdGroupId());
        assertThat("в группе должны присутствовать добавленные и обновленные фразы",
                adGroupKeywords,
                containsInAnyOrder("раз фраза", "два фраза", "фраза три", "фраза четыре"));

        int clientKeywordsCount = getClientKeywordsCount();
        assertThat("общее количество ключевых фраз клиента не соответствует ожидаемому",
                clientKeywordsCount, is(4));
    }

    @Test
    public void oneAdGroupWithOneUpdatedAndOneDeletedKeyword() {
        KeywordInfo keywordInfo = createKeyword(adGroupInfo1);
        createKeyword(adGroupInfo1);

        Keyword updatedKeyword = randomKeyword(keywordInfo.getId());
        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(singletonList(updatedKeyword));

        updateAndCheckResultIsEntirelySuccessful(adGroup);

        List<String> adGroupKeywords = findKeywordsInAdGroup(adGroupInfo1.getAdGroupId());
        assertThat("в группе должна присутствовать обновленная фраза",
                adGroupKeywords,
                contains(updatedKeyword.getPhrase()));

        int clientKeywordsCount = getClientKeywordsCount();
        assertThat("общее количество ключевых фраз клиента не соответствует ожидаемому",
                clientKeywordsCount, is(1));
    }

    @Test
    public void oneAdGroupWithOneAddedAndOneDeletedKeyword() {
        createKeyword(adGroupInfo1);

        Keyword addedKeyword = randomKeyword();
        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(singletonList(addedKeyword));

        updateAndCheckResultIsEntirelySuccessful(adGroup);

        List<String> adGroupKeywords = findKeywordsInAdGroup(adGroupInfo1.getAdGroupId());
        assertThat("в группе должна присутствовать добавленная фраза",
                adGroupKeywords,
                contains(addedKeyword.getPhrase()));

        int clientKeywordsCount = getClientKeywordsCount();
        assertThat("общее количество ключевых фраз клиента не соответствует ожидаемому",
                clientKeywordsCount, is(1));
    }

    @Test
    public void oneAdGroupWithOneUpdatedAndOneAddedAndOneDeletedKeyword() {
        KeywordInfo keywordInfo = createKeyword(adGroupInfo1);
        createKeyword(adGroupInfo1);

        Keyword updatedKeyword = randomKeyword(keywordInfo.getId());
        Keyword addedKeyword = randomKeyword();
        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(asList(updatedKeyword, addedKeyword));

        updateAndCheckResultIsEntirelySuccessful(adGroup);

        List<String> adGroupKeywords = findKeywordsInAdGroup(adGroupInfo1.getAdGroupId());
        assertThat("в группе должна присутствовать добавленная и обновленная фразы",
                adGroupKeywords,
                containsInAnyOrder(updatedKeyword.getPhrase(), addedKeyword.getPhrase()));

        int clientKeywordsCount = getClientKeywordsCount();
        assertThat("общее количество ключевых фраз клиента не соответствует ожидаемому",
                clientKeywordsCount, is(2));
    }

    @Test
    public void oneAdGroupWithOneUpdatedAndOneAddedAndOneDeletedKeywordAndUntouchedAdGroup() {
        KeywordInfo keywordInfo1 = createKeyword(adGroupInfo1);
        createKeyword(adGroupInfo1);

        createSecondAdGroup();
        KeywordInfo keywordInfo2 = createKeyword(adGroupInfo2);

        Keyword updatedKeyword = randomKeyword(keywordInfo1.getId());
        Keyword addedKeyword = randomKeyword();
        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(asList(updatedKeyword, addedKeyword));

        updateAndCheckResultIsEntirelySuccessful(adGroup);

        List<String> adGroup1Keywords = findKeywordsInAdGroup(adGroupInfo1.getAdGroupId());
        assertThat("в группе должна присутствовать добавленная и обновленная фразы",
                adGroup1Keywords,
                containsInAnyOrder(updatedKeyword.getPhrase(), addedKeyword.getPhrase()));

        List<String> adGroup2Keywords = findKeywordsInAdGroup(adGroupInfo2.getAdGroupId());
        assertThat("в группе должна присутствовать фразы",
                adGroup2Keywords,
                contains(keywordInfo2.getKeyword().getPhrase()));

        int clientKeywordsCount = getClientKeywordsCount();
        assertThat("общее количество ключевых фраз клиента не соответствует ожидаемому",
                clientKeywordsCount, is(3));
    }

    @Test
    public void oneAdGroupWithAddedKeywordAndOneEmptyAdGroupAndOneAdGroupWithAddedAndUpdatedKeywords() {
        createSecondAdGroup();
        createThirdAdGroup();
        KeywordInfo keywordInfo = createKeyword(adGroupInfo3);

        Keyword addedKeyword1 = randomKeyword();
        Keyword addedKeyword2 = randomKeyword();
        Keyword updatedKeyword = randomKeyword(keywordInfo.getId());
        ComplexTextAdGroup adGroup1 = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(singletonList(addedKeyword1));
        ComplexTextAdGroup adGroup2 = createValidAdGroupForUpdate(adGroupInfo2);
        ComplexTextAdGroup adGroup3 = createValidAdGroupForUpdate(adGroupInfo3)
                .withKeywords(asList(addedKeyword2, updatedKeyword));

        updateAndCheckResultIsEntirelySuccessful(asList(adGroup1, adGroup2, adGroup3));

        List<String> adGroup1Keywords = findKeywordsInAdGroup(adGroupInfo1.getAdGroupId());
        assertThat("в первой группе должна присутствовать добавленная фраза",
                adGroup1Keywords,
                contains(addedKeyword1.getPhrase()));

        List<String> adGroup3Keywords = findKeywordsInAdGroup(adGroupInfo3.getAdGroupId());
        assertThat("в третьей группе должна присутствовать добавленная и обновленная фразы",
                adGroup3Keywords,
                containsInAnyOrder(addedKeyword2.getPhrase(), updatedKeyword.getPhrase()));

        int clientKeywordsCount = getClientKeywordsCount();
        assertThat("общее количество ключевых фраз клиента не соответствует ожидаемому",
                clientKeywordsCount, is(3));
    }

    // несколько групп

    @Test
    public void twoAdGroupsWithUpdatedKeywordsWithParenthesisAndWithout() {
        createSecondAdGroup();
        KeywordInfo keywordInfo11 = createKeyword(adGroupInfo1);
        KeywordInfo keywordInfo12 = createKeyword(adGroupInfo1);
        KeywordInfo keywordInfo21 = createKeyword(adGroupInfo2);
        KeywordInfo keywordInfo22 = createKeyword(adGroupInfo2);

        Keyword keyword11 = randomKeyword(keywordInfo11.getId()).withPhrase("(раз|два) фраза");
        Keyword keyword12 = randomKeyword(keywordInfo12.getId());
        Keyword keyword21 = randomKeyword(keywordInfo21.getId());
        Keyword keyword22 = randomKeyword(keywordInfo22.getId()).withPhrase("фраза (три|четыре)");
        ComplexTextAdGroup adGroup1 = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(asList(keyword11, keyword12));
        ComplexTextAdGroup adGroup2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withKeywords(asList(keyword21, keyword22));

        updateAndCheckResultIsEntirelySuccessful(adGroup1, adGroup2);

        List<String> adGroupKeywords1 = findKeywordsInAdGroup(adGroupInfo1.getAdGroupId());
        assertThat("в группе должны присутствовать обновленные фразы",
                adGroupKeywords1,
                containsInAnyOrder("раз фраза", "два фраза", keyword12.getPhrase()));

        List<String> adGroupKeywords2 = findKeywordsInAdGroup(adGroupInfo2.getAdGroupId());
        assertThat("в группе должны присутствовать обновленные фразы",
                adGroupKeywords2,
                containsInAnyOrder("фраза три", "фраза четыре", keyword21.getPhrase()));

        int clientKeywordsCount = getClientKeywordsCount();
        assertThat("общее количество ключевых фраз клиента не соответствует ожидаемому",
                clientKeywordsCount, is(6));
    }

    @Test
    public void twoAdGroupsWithUpdatedAndAddedKeywordsWithParenthesisAndWithout() {
        createSecondAdGroup();
        KeywordInfo keywordInfo1 = createKeyword(adGroupInfo1);
        KeywordInfo keywordInfo2 = createKeyword(adGroupInfo2);

        Keyword keyword11 = randomKeyword().withPhrase("(раз|два) фраза");
        Keyword keyword12 = randomKeyword(keywordInfo1.getId());
        Keyword keyword21 = randomKeyword();
        Keyword keyword22 = randomKeyword(keywordInfo2.getId()).withPhrase("фраза (три|четыре)");
        ComplexTextAdGroup adGroup1 = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(asList(keyword11, keyword12));
        ComplexTextAdGroup adGroup2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withKeywords(asList(keyword21, keyword22));

        updateAndCheckResultIsEntirelySuccessful(adGroup1, adGroup2);

        List<String> adGroupKeywords1 = findKeywordsInAdGroup(adGroupInfo1.getAdGroupId());
        assertThat("в группе должны присутствовать обновленные фразы",
                adGroupKeywords1,
                containsInAnyOrder("раз фраза", "два фраза", keyword12.getPhrase()));

        List<String> adGroupKeywords2 = findKeywordsInAdGroup(adGroupInfo2.getAdGroupId());
        assertThat("в группе должны присутствовать обновленные фразы",
                adGroupKeywords2,
                containsInAnyOrder("фраза три", "фраза четыре", keyword21.getPhrase()));

        int clientKeywordsCount = getClientKeywordsCount();
        assertThat("общее количество ключевых фраз клиента не соответствует ожидаемому",
                clientKeywordsCount, is(6));
    }
}
