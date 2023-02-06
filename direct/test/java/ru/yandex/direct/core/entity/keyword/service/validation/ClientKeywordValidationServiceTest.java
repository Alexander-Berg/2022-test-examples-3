package ru.yandex.direct.core.entity.keyword.service.validation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bids.validation.BidsDefects;
import ru.yandex.direct.core.entity.keyword.container.InternalKeyword;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatusarch;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.libs.keywordutils.model.KeywordWithMinuses;
import ru.yandex.direct.libs.keywordutils.parser.KeywordParser;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.archivedCampaignModification;
import static ru.yandex.direct.core.entity.keyword.service.validation.KeywordDefects.adGroupNotFound;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseConstraints.WORDS_MAX_COUNT;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefects.tooManyWords;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestCampaigns.manualStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newTextCampaign;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.matchesWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.inconsistentState;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientKeywordValidationServiceTest {

    @Autowired
    private ClientKeywordValidationService clientKeywordValidationService;

    @Autowired
    private TestCampaignRepository testCampaignRepository;

    @Autowired
    private TestBannerRepository testBannerRepository;

    @Autowired
    private Steps steps;

    private KeywordInfo keywordInfo;
    private Keyword keyword;
    private Integer index;

    private int shard;
    private ClientId clientId;
    private Long operatorUid;
    private Long bannerId;
    private ValidationResult<List<Keyword>, Defect> inputVr;
    private Map<Integer, InternalKeyword> internalKeywordMap;

    @Before
    @SuppressWarnings("unchecked")
    public void before() {
        index = 0;
        CampaignInfo campaignInfo = steps.campaignSteps()
                .createCampaign(newTextCampaign(null, null).withStrategy(manualStrategy().withSeparateBids(false)));
        TextBannerInfo bannerInfo = steps.bannerSteps().createBanner(activeTextBanner(), campaignInfo);
        shard = bannerInfo.getShard();
        bannerId = bannerInfo.getBannerId();
        clientId = bannerInfo.getClientId();
        operatorUid = bannerInfo.getUid();
        keywordInfo = steps.keywordSteps().createKeyword(bannerInfo.getAdGroupInfo());
        keyword = keywordInfo.getKeyword().withPrice(new BigDecimal(1));
        inputVr = new ValidationResult<>(singletonList(keyword));

        KeywordWithMinuses keywordWithMinuses = KeywordParser.parseWithMinuses(keyword.getNormPhrase().trim());
        InternalKeyword internalKeyword = new InternalKeyword(keyword, keywordWithMinuses, keywordWithMinuses);
        internalKeywordMap = singletonMap(index, internalKeyword);
    }

    @SuppressWarnings("unchecked")
    private ValidationResult<List<Keyword>, Defect> getVr(Keyword keyword) {
        return new ValidationResult<>(singletonList(keyword));
    }

    //default
    @Test
    public void validate_OneValidKeyword_NoErrors() {
        ValidationResult<List<Keyword>, Defect> vr = clientKeywordValidationService
                .validate(inputVr, internalKeywordMap, true, false, operatorUid, clientId);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_CampaignIsNotWritable_NoRightsDefinition() {
        operatorUid = steps.clientSteps().createDefaultClient().getUid();
        ValidationResult<List<Keyword>, Defect> vr = clientKeywordValidationService
                .validate(inputVr, internalKeywordMap, true, false, operatorUid, clientId);
        assertThat(vr, hasDefectDefinitionWith(matchesWith(adGroupNotFound())));
    }

    //ClientKeywordCommonValidator подключен
    @Test
    public void validate_CommonKeywordValidator_SearchPriceIsNotSetForManualStrategyDefinition() {
        inputVr = getVr(keyword.withPrice(null));
        ValidationResult<List<Keyword>, Defect> vr = clientKeywordValidationService
                .validate(inputVr, internalKeywordMap, true, false, operatorUid, clientId);
        assertSingleKeywordError(vr, "price", new Defect<>(
                BidsDefects.Ids.SEARCH_PRICE_IS_NOT_SET_FOR_MANUAL_STRATEGY));
    }

    //PhraseValidator подключен
    @Test
    public void validate_KeywordsMoreThanMax_TooManyWordsDefinition() {
        KeywordWithMinuses keywordWithMinuses = KeywordParser.parseWithMinuses(
                "длинная фраза длиннее максимальной длинная фраза длиннее максимальной");
        InternalKeyword internalKeyword = new InternalKeyword(keyword, keywordWithMinuses, keywordWithMinuses);

        ValidationResult<List<Keyword>, Defect> vr = clientKeywordValidationService
                .validate(inputVr, singletonMap(index, internalKeyword), true, false, operatorUid, clientId);
        assertSingleKeywordError(vr, "phrase", tooManyWords(WORDS_MAX_COUNT));
    }

    //Если во входном результате валидации есть ошибки, то дальше не валидируем
    @Test
    public void validate_NotValidateWhenHasErrorOnPreValidation() {
        inputVr.getOrCreateSubValidationResult(index(index), keyword).addError(inconsistentState());

        KeywordWithMinuses keywordWithMinuses = KeywordParser.parseWithMinuses(
                "один два три четыре пять шесть семь восемь");
        InternalKeyword internalKeyword = new InternalKeyword(keyword, keywordWithMinuses, keywordWithMinuses);

        ValidationResult<List<Keyword>, Defect> vr = clientKeywordValidationService
                .validate(inputVr, singletonMap(index, internalKeyword), true, false, operatorUid, clientId);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), inconsistentState())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }


    @Test
    public void validate_AddKeywordsInArchiveCampaign_ArchivedCampaignModification() {
        testCampaignRepository.archiveCampaign(keywordInfo.getShard(), keywordInfo.getCampaignId());

        KeywordWithMinuses keywordWithMinuses = KeywordParser.parseWithMinuses(
                "фраза в архивной кампании");
        InternalKeyword internalKeyword = new InternalKeyword(keyword, keywordWithMinuses, keywordWithMinuses);

        ValidationResult<List<Keyword>, Defect> vr = clientKeywordValidationService
                .validate(inputVr, singletonMap(index, internalKeyword), true, false, operatorUid, clientId);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0), field(Keyword.AD_GROUP_ID.name())),
                archivedCampaignModification())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void validate_KeywordInArchivedAdGroup_NoErrors() {
        testBannerRepository.updateStatusArchive(shard, bannerId, BannersStatusarch.Yes);

        KeywordWithMinuses keywordWithMinuses = KeywordParser.parseWithMinuses(
                "фраза в архивной группе");
        InternalKeyword internalKeyword = new InternalKeyword(keyword, keywordWithMinuses, keywordWithMinuses);

        ValidationResult<List<Keyword>, Defect> vr = clientKeywordValidationService
                .validate(inputVr, singletonMap(index, internalKeyword), true, false, operatorUid, clientId);

        assertThat("Должна быть возможность добавлять и изменять ключевые фразы в архивных группах",
                vr, hasNoDefectsDefinitions());
    }

    private void assertSingleKeywordError(ValidationResult<List<Keyword>, Defect> validationResult,
                                          String field, Defect expectedDefect) {
        assertThat(validationResult,
                hasDefectDefinitionWith(validationError(path(index(0), field(field)), expectedDefect)));
    }
}
