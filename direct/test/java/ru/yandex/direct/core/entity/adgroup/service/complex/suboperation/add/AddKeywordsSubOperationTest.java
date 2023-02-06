package ru.yandex.direct.core.entity.adgroup.service.complex.suboperation.add;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.keyword.container.AdGroupInfoForKeywordAdd;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.service.KeywordOperationFactory;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.allOf;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefects.illegalCharacters;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefects.invalidBrackets;
import static ru.yandex.direct.core.testing.data.TestKeywords.keywordWithText;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AddKeywordsSubOperationTest {

    @Autowired
    private KeywordOperationFactory keywordOperationFactory;

    @Autowired
    private AdGroupSteps adGroupSteps;

    private AdGroupInfo adGroup1;
    private AdGroupInfoForKeywordAdd adGroupInfoForKeywordAdd1;
    private AdGroupInfo adGroup2;
    private AdGroupInfoForKeywordAdd adGroupInfoForKeywordAdd2;

    @Before
    public void before() {
        adGroup1 = adGroupSteps.createActiveTextAdGroup();
        adGroupInfoForKeywordAdd1 =
                new AdGroupInfoForKeywordAdd(0, adGroup1.getCampaignId(), adGroup1.getAdGroupType());
        adGroup2 = adGroupSteps.createActiveTextAdGroup(adGroup1.getClientInfo());
        adGroupInfoForKeywordAdd2 =
                new AdGroupInfoForKeywordAdd(0, adGroup2.getCampaignId(), adGroup2.getAdGroupType());
    }

    @Test
    public void phrasesParsedResultIsOk() {
        List<Keyword> keywords =
                asList(keywordWithText("(правильная| подходящая) фраза").withAdGroupId(adGroup1.getAdGroupId())
                                .withPrice(BigDecimal.TEN).withPriceContext(BigDecimal.TEN),
                        keywordWithText("фраза без скобок").withAdGroupId(adGroup2.getAdGroupId())
                                .withPrice(BigDecimal.TEN).withPriceContext(BigDecimal.TEN)
                );
        AddKeywordsSubOperation subOperation = createOperation(keywords);
        Map<Integer, AdGroupInfoForKeywordAdd> adGroupsInfo = new HashMap<>();
        adGroupsInfo.put(0, adGroupInfoForKeywordAdd1);
        adGroupsInfo.put(1, adGroupInfoForKeywordAdd2);

        subOperation.setAdGroupInfoByKeywordIndex(adGroupsInfo);
        ValidationResult<List<Keyword>, Defect> vr = subOperation.prepare();
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void phrasesParsedButHasAnotherValidationErrors() {
        List<Keyword> keywords =
                asList(keywordWithText("нормальная фраза").withAdGroupId(adGroup1.getAdGroupId()),
                        keywordWithText("фраза (*неверная| []с ошибкой)").withAdGroupId(adGroup2.getAdGroupId()));
        AddKeywordsSubOperation subOperation = createOperation(keywords);
        Map<Integer, AdGroupInfoForKeywordAdd> adGroupsInfo = new HashMap<>();
        adGroupsInfo.put(0, adGroupInfoForKeywordAdd1);
        adGroupsInfo.put(1, adGroupInfoForKeywordAdd2);

        subOperation.setAdGroupInfoByKeywordIndex(adGroupsInfo);
        ValidationResult<List<Keyword>, Defect> vr = subOperation.prepare();
        assertThat(vr, allOf(
                hasDefectDefinitionWith(validationError(path(index(1), field("phrase")),
                        illegalCharacters(singletonList("*неверная")))),
                hasDefectDefinitionWith(validationError(path(index(1), field("phrase")), invalidBrackets()))));
    }

    @Test
    public void noExceptionWhenPhraseIsNull() {
        List<Keyword> keywords =
                asList(keywordWithText(null).withAdGroupId(adGroup1.getAdGroupId()),
                        keywordWithText("(правильная| подходящая) фраза").withAdGroupId(adGroup2.getAdGroupId()));
        AddKeywordsSubOperation subOperation = createOperation(keywords);
        Map<Integer, AdGroupInfoForKeywordAdd> adGroupsInfo = new HashMap<>();
        adGroupsInfo.put(0, adGroupInfoForKeywordAdd1);
        adGroupsInfo.put(1, adGroupInfoForKeywordAdd2);

        subOperation.setAdGroupInfoByKeywordIndex(adGroupsInfo);
        ValidationResult<List<Keyword>, Defect> vr = subOperation.prepare();
        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0), field("phrase")), notNull())));
    }

    private AddKeywordsSubOperation createOperation(List<Keyword> keywords) {
        return new AddKeywordsSubOperation(keywordOperationFactory,
                keywords, false, null,
                adGroup1.getUid(), adGroup1.getClientId(), adGroup1.getUid());
    }
}
