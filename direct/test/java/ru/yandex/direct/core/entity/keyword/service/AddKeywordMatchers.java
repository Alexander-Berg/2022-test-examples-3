package ru.yandex.direct.core.entity.keyword.service;

import java.util.List;

import org.hamcrest.Matcher;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.keyword.container.AddedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.container.StopwordsFixation;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.join;
import static org.hamcrest.Matchers.greaterThan;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFields;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public class AddKeywordMatchers {

    private static final CompareStrategy ALL_FIELDS_STRATEGY =
            allFields().forFields(newPath("id")).useMatcher(greaterThan(0L));

    private static final CompareStrategy ALL_FIELDS_EXCEPT_ADGROUP_ID_STRATEGY =
            allFieldsExcept(newPath("adGroupId")).forFields(newPath("id")).useMatcher(greaterThan(0L));

    public static Matcher<AddedKeywordInfo> isAdded(String resultPhrase) {
        return addedInfoMatcherExceptAdGroupId(true, resultPhrase, resultPhrase, null, null);
    }

    public static Matcher<AddedKeywordInfo> isAddedToAdGroup(String resultPhrase, long adGroupId) {
        return addedInfoMatcher(adGroupId, true, resultPhrase, resultPhrase, null, null);
    }

    public static Matcher<AddedKeywordInfo> isAddedWithMinus(String phraseBeforeUnglue, String addedMinus) {
        return addedInfoMatcherExceptAdGroupId(true, resultPhrase(phraseBeforeUnglue, addedMinus),
                phraseBeforeUnglue, null, singletonList(addedMinus));
    }

    public static Matcher<AddedKeywordInfo> isAddedWithMinuses(String phraseBeforeUnglue, List<String> addedMinuses) {
        return addedInfoMatcherExceptAdGroupId(true, resultPhrase(phraseBeforeUnglue, addedMinuses),
                phraseBeforeUnglue, null, addedMinuses);
    }

    public static Matcher<AddedKeywordInfo> isAddedWithMinusAndFixation(String phraseBeforeUnglue, String minus,
                                                                        String sourceFixation, String destFixation) {
        return addedInfoMatcherExceptAdGroupId(true, resultPhrase(phraseBeforeUnglue, minus), phraseBeforeUnglue,
                singletonList(new StopwordsFixation(sourceFixation, destFixation)), singletonList(minus));
    }

    public static Matcher<AddedKeywordInfo> isNotAdded(String resultPhrase) {
        return addedInfoMatcherExceptAdGroupId(false, resultPhrase, null, null, null);
    }

    public static Matcher<AddedKeywordInfo> isAddedWithFixation(String resultPhrase,
                                                                String sourceFixation, String destFixation) {
        return addedInfoMatcherExceptAdGroupId(true, resultPhrase, resultPhrase,
                singletonList(new StopwordsFixation(sourceFixation, destFixation)), null);
    }

    public static Matcher<AddedKeywordInfo> isAddedWithFixations(String resultPhrase,
                                                                 List<StopwordsFixation> fixations) {
        return addedInfoMatcherExceptAdGroupId(true, resultPhrase, resultPhrase, fixations, null);
    }

    public static Matcher<AddedKeywordInfo> isNotAddedWithFixation(String resultPhrase,
                                                                   String sourceFixation, String destFixation) {
        return addedInfoMatcherExceptAdGroupId(false, resultPhrase, null,
                singletonList(new StopwordsFixation(sourceFixation, destFixation)), null);
    }

    private static Matcher<AddedKeywordInfo> addedInfoMatcherExceptAdGroupId(boolean added,
                                                                             String resultPhrase,
                                                                             String phraseBeforeUnglue,
                                                                             List<StopwordsFixation> stopwordsFixations,
                                                                             List<String> addedMinuses) {
        AddedKeywordInfo expectedKeywordInfo = new AddedKeywordInfo()
                .withResultPhrase(resultPhrase)
                .withPhraseBeforeUnglue(phraseBeforeUnglue)
                .withAdded(added)
                .withFixations(stopwordsFixations)
                .withAddedMinuses(addedMinuses);
        return beanDiffer(expectedKeywordInfo).useCompareStrategy(ALL_FIELDS_EXCEPT_ADGROUP_ID_STRATEGY);
    }

    public static Matcher<AddedKeywordInfo> addedInfoMatcher(long adGroupId,
                                                             boolean added,
                                                             String resultPhrase,
                                                             String phraseBeforeUnglue,
                                                             List<StopwordsFixation> stopwordsFixations,
                                                             List<String> addedMinuses) {
        AddedKeywordInfo expectedKeywordInfo = new AddedKeywordInfo()
                .withAdGroupId(adGroupId)
                .withResultPhrase(resultPhrase)
                .withPhraseBeforeUnglue(phraseBeforeUnglue)
                .withAdded(added)
                .withFixations(stopwordsFixations)
                .withAddedMinuses(addedMinuses);
        return addedInfoMatcher(expectedKeywordInfo);
    }

    public static Matcher<AddedKeywordInfo> addedInfoMatcher(AddedKeywordInfo expectedKeywordInfo) {
        return beanDiffer(expectedKeywordInfo).useCompareStrategy(ALL_FIELDS_STRATEGY);
    }

    public static Matcher<AddedKeywordInfo> isNotAddedWithId(Long id, Long adGroupId, String resultPhrase) {
        AddedKeywordInfo expectedKeywordInfo = new AddedKeywordInfo()
                .withId(id)
                .withAdGroupId(adGroupId)
                .withResultPhrase(resultPhrase)
                .withAdded(false);
        return beanDiffer(expectedKeywordInfo).useCompareStrategy(allFields());
    }

    public static String resultPhrase(String phrase, String minuses) {
        return resultPhrase(phrase, singletonList(minuses));
    }

    public static String resultPhrase(String phrase, List<String> minuses) {
        return phrase + " " + join(mapList(minuses, m -> "-" + m), " ");
    }
}
