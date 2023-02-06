package ru.yandex.direct.core.entity.keyword.service;

import java.util.List;

import org.hamcrest.Matcher;

import ru.yandex.direct.core.entity.keyword.container.StopwordsFixation;
import ru.yandex.direct.core.entity.keyword.container.UpdatedKeywordInfo;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.join;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public class UpdateKeywordMatchers {

    public static Matcher<UpdatedKeywordInfo> isUpdated(Long id, String resultPhrase) {
        return isUpdated(id, resultPhrase, false);
    }

    public static Matcher<UpdatedKeywordInfo> isUpdated(Long id, String resultPhrase, Boolean isSuspended) {
        return updatedInfoMatcher(id, resultPhrase, resultPhrase, false, null, null, isSuspended);
    }

    public static Matcher<UpdatedKeywordInfo> isUpdatedWithMinuses(Long id, String phraseBeforeUnglue,
                                                                   List<String> addedMinuses) {
        return updatedInfoMatcher(id, resultPhrase(phraseBeforeUnglue, addedMinuses), phraseBeforeUnglue, false, null,
                addedMinuses, false);
    }

    public static Matcher<UpdatedKeywordInfo> isUpdatedWithMinus(Long id, String phraseBeforeUnglue,
                                                                 String addedMinus) {
        return updatedInfoMatcher(id, resultPhrase(phraseBeforeUnglue, addedMinus), phraseBeforeUnglue, false, null,
                singletonList(addedMinus), false);
    }

    public static Matcher<UpdatedKeywordInfo> isUpdatedWithMinusAndFixation(Long id, String phraseBeforeUnglue,
                                                                            String addedMinus, String sourceFixation, String destFixation) {
        StopwordsFixation fixation = new StopwordsFixation(sourceFixation, destFixation);
        return updatedInfoMatcher(id, resultPhrase(phraseBeforeUnglue, addedMinus),
                phraseBeforeUnglue, false, singletonList(fixation), singletonList(addedMinus), false);
    }

    public static Matcher<UpdatedKeywordInfo> isUpdatedWithFixation(Long id, String resultPhrase,
                                                                    String sourceFixation, String destFixation) {
        return updatedInfoMatcher(id, resultPhrase, resultPhrase, false,
                singletonList(new StopwordsFixation(sourceFixation, destFixation)), null, false);
    }

    public static Matcher<UpdatedKeywordInfo> isUpdatedWithFixations(Long id, String resultPhrase,
                                                                     List<StopwordsFixation> fixations) {
        return updatedInfoMatcher(id, resultPhrase, resultPhrase, false, fixations, null, false);
    }

    public static Matcher<UpdatedKeywordInfo> isNotUpdated(Long newId, String resultPhrase) {
        return updatedInfoMatcher(newId, resultPhrase, null, true, null, null, null);
    }


    public static Matcher<UpdatedKeywordInfo> isNotUpdatedWithFixation(Long newId, String resultPhrase,
                                                                       String sourceFixation, String destFixation) {
        return updatedInfoMatcher(newId, resultPhrase, null, true,
                singletonList(new StopwordsFixation(sourceFixation, destFixation)), null, null);
    }

    public static Matcher<UpdatedKeywordInfo> updatedInfoMatcher(Long id, String resultPhrase,
                                                                 String phraseBeforeUnglue,
                                                                 boolean deleted,
                                                                 List<StopwordsFixation> stopwordsFixations,
                                                                 List<String> addedMinuses,
                                                                 Boolean isSuspended) {
        UpdatedKeywordInfo expectedKeywordInfo = new UpdatedKeywordInfo()
                .withId(id)
                .withResultPhrase(resultPhrase)
                .withPhraseBeforeUnglue(phraseBeforeUnglue)
                .withDeleted(deleted)
                .withFixations(stopwordsFixations)
                .withAddedMinuses(addedMinuses)
                .withIsSuspended(isSuspended);
        return beanDiffer(expectedKeywordInfo);
    }

    public static String resultPhrase(String phrase, String minuses) {
        return resultPhrase(phrase, singletonList(minuses));
    }

    public static String resultPhrase(String phrase, List<String> minuses) {
        return phrase + " " + join(mapList(minuses, m -> "-" + m), " ");
    }
}
