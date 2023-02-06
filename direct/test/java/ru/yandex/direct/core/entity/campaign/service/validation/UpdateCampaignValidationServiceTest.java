package ru.yandex.direct.core.entity.campaign.service.validation;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseValidator;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.apache.commons.lang3.StringUtils.rightPad;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.service.validation.UpdateAdGroupValidationService.MAX_ELEMENTS_PER_OPERATION;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.archivedCampaignModification;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.campaignNotFound;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.minusKeywordsNotAllowed;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseConstraints.CAMPAIGN_MINUS_KEYWORDS_MAX_LENGTH;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseConstraints.WORDS_MAX_COUNT;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.maxCountWordsInKeyword;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.maxLengthMinusKeywords;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.nestedOrEmptySquareBrackets;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxCollectionSize;
import static ru.yandex.direct.validation.defect.CollectionDefects.minCollectionSize;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.defect.StringDefects.notEmptyString;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UpdateCampaignValidationServiceTest {

    private static final String NEW_NAME = "новое имя " + randomNumeric(5);
    private static final String NEW_VALID_KEYWORD = "новая минус-фраза " + randomNumeric(5);
    private static final String NEW_INVALID_KEYWORD = "[новая [невалидная] минус-фраза] " + randomNumeric(5);

    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private UpdateCampaignValidationService validationService;

    @Autowired
    private ClientSteps clientSteps;

    @Autowired
    private Steps steps;
    @Autowired
    private CampaignSteps campaignSteps;

    @Autowired
    private DslContextProvider dslContextProvider;

    private CampaignInfo campaignInfo1;
    private CampaignInfo campaignInfo2;
    private long operatorUid;
    private ClientId clientId;
    private int shard;

    @Before
    public void before() {
        campaignInfo1 = campaignSteps.createActiveTextCampaign();
        campaignInfo2 = campaignSteps.createCampaign(activeTextCampaign(null, null), campaignInfo1.getClientInfo());

        operatorUid = campaignInfo1.getUid();
        clientId = campaignInfo1.getClientId();
        shard = campaignInfo1.getShard();
    }

    @Test
    public void preValidate_EmptyModelChanges_ResultHasOperationError() {
        ValidationResult<List<ModelChanges<Campaign>>, Defect> validationResult =
                validationService.preValidate(emptyList(), MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE,
                        operatorUid, clientId, shard
                );

        assertThat(validationResult, hasDefectDefinitionWith(validationError(path(), minCollectionSize(1))));
    }

    @Test
    public void preValidate_MaximumOfModelChanges_OperationResultIsSuccessful() {
        List<ModelChanges<Campaign>> modelChangesList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            modelChangesList.add(ModelChanges
                    .build(campaignInfo1.getCampaignId(), Campaign.class, Campaign.MINUS_KEYWORDS, singletonList(NEW_VALID_KEYWORD)));
        }
        ValidationResult<List<ModelChanges<Campaign>>, Defect> validationResult =
                validationService.preValidate(modelChangesList, MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE,
                        operatorUid, clientId, shard
                );

        assertThat(validationResult.getErrors(), hasSize(0));
    }

    @Test
    public void preValidate_MaximumOfModelChangesExceeded_ResultHasOperationError() {
        List<ModelChanges<Campaign>> modelChangesList = new ArrayList<>();
        for (int i = 0; i < 1001; i++) {
            ModelChanges<Campaign> modelChanges = ModelChanges.build(campaignInfo1.getCampaignId(), Campaign.class,
                    Campaign.MINUS_KEYWORDS, singletonList(NEW_VALID_KEYWORD));

            modelChangesList.add(modelChanges);
        }
        ValidationResult<List<ModelChanges<Campaign>>, Defect> validationResult =
                validationService.preValidate(modelChangesList, MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE,
                        operatorUid, clientId, shard
                );

        assertThat(validationResult,
                hasDefectDefinitionWith(validationError(path(), maxCollectionSize(MAX_ELEMENTS_PER_OPERATION))));
    }

    @Test
    public void preValidate_NameIsValid_ResultIsSuccessful() {
        ModelChanges<Campaign> modelChanges = ModelChanges.build(campaignInfo1.getCampaignId(), Campaign.class,
                Campaign.MINUS_KEYWORDS, singletonList(NEW_VALID_KEYWORD));

        ValidationResult<List<ModelChanges<Campaign>>, Defect> vr =
                validationService.preValidate(singletonList(modelChanges),
                        MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE, operatorUid, clientId, shard
                );

        assertThat("результат валидации не должен содержать ошибок", vr.hasAnyErrors(), is(false));
    }

    @Test
    public void preValidate_MinusKeywordsAreValid_ResultIsSuccessful() {
        ModelChanges<Campaign> modelChanges = ModelChanges.build(campaignInfo1.getCampaignId(), Campaign.class,
                Campaign.MINUS_KEYWORDS, singletonList(NEW_VALID_KEYWORD));
        ValidationResult<List<ModelChanges<Campaign>>, Defect> vr =
                validationService.preValidate(singletonList(modelChanges),
                        MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE, operatorUid, clientId, shard
                );

        assertThat("результат валидации не должен содержать ошибок", vr.hasAnyErrors(), is(false));
    }


    @Test
    public void preValidate_IdBelongsToOtherClient_ResultHasElementError() {
        CampaignInfo newCampaignInfo = campaignSteps.createActiveTextCampaign();
        ModelChanges<Campaign> modelChanges =
                ModelChanges.build(newCampaignInfo.getCampaignId(), Campaign.class, Campaign.NAME, NEW_NAME);
        ValidationResult<List<ModelChanges<Campaign>>, Defect> validationResult =
                validationService.preValidate(singletonList(modelChanges),
                        MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE, operatorUid, clientId, shard
                );

        checkElementError(validationResult, path(index(0), field("id")), objectNotFound());
    }

    @Test
    public void preValidate_OperatorHasNoRights_ResultHasElementError() {
        ClientInfo otherClient = clientSteps.createDefaultClient();
        ModelChanges<Campaign> modelChanges =
                ModelChanges.build(campaignInfo1.getCampaignId(), Campaign.class, Campaign.NAME, NEW_NAME);
        ValidationResult<List<ModelChanges<Campaign>>, Defect> validationResult =
                validationService.preValidate(singletonList(modelChanges),
                        MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE, otherClient.getUid(), clientId, shard
                );

        checkElementError(validationResult, path(index(0), field(Campaign.ID.name())), campaignNotFound());
    }

    @Test
    public void preValidate_MinusKeywordsAreNull_ResultIsSuccessful() {
        ModelChanges<Campaign> modelChanges = ModelChanges.build(campaignInfo1.getCampaignId(), Campaign.class,
                Campaign.MINUS_KEYWORDS, null);

        ValidationResult<List<ModelChanges<Campaign>>, Defect> vr =
                validationService.preValidate(singletonList(modelChanges),
                        MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE, operatorUid, clientId, shard
                );

        assertThat("результат валидации не должен содержать ошибок", vr.hasAnyErrors(), is(false));
    }

    @Test
    public void preValidate_MinusKeywordsHasFirstInvalidKeyword_ResultHasElementError() {
        ModelChanges<Campaign> modelChanges = ModelChanges.build(campaignInfo1.getCampaignId(), Campaign.class,
                Campaign.MINUS_KEYWORDS, asList(NEW_INVALID_KEYWORD, NEW_VALID_KEYWORD));

        ValidationResult<List<ModelChanges<Campaign>>, Defect> validationResult =
                validationService.preValidate(singletonList(modelChanges),
                        MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE, operatorUid, clientId, shard
                );

        checkElementError(validationResult, path(index(0), field("minusKeywords")),
                nestedOrEmptySquareBrackets(singletonList(NEW_INVALID_KEYWORD)));
    }

    @Test
    public void preValidate_OneValidAndOneInvalidItem_ResultHasElementError() {
        ModelChanges<Campaign> modelChanges1 = ModelChanges
                .build(campaignInfo1.getCampaignId(), Campaign.class, Campaign.MINUS_KEYWORDS, singletonList(NEW_INVALID_KEYWORD));
        ModelChanges<Campaign> modelChanges2 = ModelChanges
                .build(campaignInfo2.getCampaignId(), Campaign.class, Campaign.MINUS_KEYWORDS, singletonList(NEW_VALID_KEYWORD));
        ValidationResult<List<ModelChanges<Campaign>>, Defect> validationResult =
                validationService.preValidate(asList(modelChanges1, modelChanges2),
                        MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE, operatorUid, clientId, shard
                );

        checkElementError(validationResult, path(index(0), field("minusKeywords")),
                nestedOrEmptySquareBrackets(singletonList(NEW_INVALID_KEYWORD)));
        assertThat("второй элемент не должен содержать ошибок",
                validationResult.getSubResults().get(index(1)).hasAnyErrors(),
                is(false));
    }

    @Test
    public void validate_ModelValid_ResultIsSuccessful() {
        CampaignInfo campaignInfo = campaignSteps.createDefaultCampaign();
        Campaign campaign = convertCampaign(campaignInfo)
                .withName("test name")
                .withStatusArchived(false)
                .withMinusKeywords(null);

        ValidationResult<List<Campaign>, Defect> validationResult =
                new ValidationResult<>(singletonList(campaign));
        validationResult = validationService.validate(validationResult);
        assertThat("результат валидации не должен содержать ошибок",
                validationResult.hasAnyErrors(), is(false));
    }

    @Test
    public void validate_NameIsNull_ResultHasElementError() {
        CampaignInfo campaignInfo = campaignSteps.createDefaultCampaign();
        Campaign campaign = convertCampaign(campaignInfo)
                .withName(null);

        ValidationResult<List<Campaign>, Defect> validationResult =
                new ValidationResult<>(singletonList(campaign));
        validationResult = validationService.validate(validationResult);
        checkElementError(validationResult, path(index(0), field("name")), notNull());
    }

    @Test
    public void validate_NameIsEmpty_ResultHasElementError() {
        CampaignInfo campaignInfo = campaignSteps.createDefaultCampaign();
        Campaign campaign = convertCampaign(campaignInfo);
        campaign.withName("");

        ValidationResult<List<Campaign>, Defect> validationResult =
                new ValidationResult<>(singletonList(campaign));
        validationResult = validationService.validate(validationResult);
        checkElementError(validationResult, path(index(0), field("name")), notEmptyString());
    }

    @Test
    public void validate_Archived_ResultHasElementError() {
        CampaignInfo campaignInfo = campaignSteps.createDefaultCampaign();
        Campaign campaign = convertCampaign(campaignInfo);
        campaign.withStatusArchived(true);

        ValidationResult<List<Campaign>, Defect> validationResult =
                new ValidationResult<>(singletonList(campaign));
        validationResult = validationService.validate(validationResult);
        checkElementError(validationResult, path(index(0), field("statusArchived")), archivedCampaignModification());
    }

    @Test
    public void validate_MinusKeywordsTooLong_ResultHasElementError() {
        CampaignInfo campaignInfo = campaignSteps.createDefaultCampaign();
        // +2: один пробел между word и other не учитывается, еще 1 символ - чтобы превысить длину
        String minusKeyword = rightPad("word ", CAMPAIGN_MINUS_KEYWORDS_MAX_LENGTH + 2, "other");
        Campaign campaign = convertCampaign(campaignInfo)
                .withMinusKeywords(singletonList(minusKeyword));

        ValidationResult<List<Campaign>, Defect> validationResult =
                new ValidationResult<>(singletonList(campaign));
        validationResult = validationService.validate(validationResult);
        checkElementError(validationResult, path(index(0), field("minusKeywords")),
                maxLengthMinusKeywords(CAMPAIGN_MINUS_KEYWORDS_MAX_LENGTH));
    }

    @Test
    public void validate_MinusKeywordsTooMuchWords_ResultHasElementError() {
        CampaignInfo campaignInfo = campaignSteps.createDefaultCampaign();
        Campaign campaign = convertCampaign(campaignInfo)
                .withMinusKeywords(singletonList("один два три четыре пять шесть семь восемь"));

        ValidationResult<List<Campaign>, Defect> validationResult =
                new ValidationResult<>(singletonList(campaign));
        validationResult = validationService.validate(validationResult);
        checkElementError(validationResult, path(index(0), field("minusKeywords")),
                maxCountWordsInKeyword(WORDS_MAX_COUNT, singletonList("один два три четыре пять шесть семь восемь")));
    }

    @Test
    public void validate_MinusKeywordsCpmYndxCampaign_Error() {
        CampaignInfo campaignInfo = campaignSteps.createActiveCpmYndxFrontpageCampaign();
        Campaign campaign = convertCampaign(campaignInfo)
                .withMinusKeywords(singletonList("один два три четыре пять шесть семь восемь"));

        ValidationResult<List<Campaign>, Defect> validationResult =
                new ValidationResult<>(singletonList(campaign));
        validationResult = validationService.validate(validationResult);
        checkElementError(validationResult, path(index(0), field("minusKeywords")),
                minusKeywordsNotAllowed());
    }

    private <T> void checkElementError(ValidationResult<List<T>, Defect> validationResult,
                                       Path path, Defect defect) {
        assertThat("результат валидации не должен содержать ошибок уровня операции",
                validationResult.hasErrors(), is(false));
        assertThat("результат валидации должен содержать ошибку уровня элемента",
                validationResult, hasDefectDefinitionWith(validationError(path, defect)));
    }

    private Campaign convertCampaign(CampaignInfo campaignInfo) {
        return campaignRepository.getCampaigns(shard, singletonList(campaignInfo.getCampaignId())).iterator().next();
    }

}
