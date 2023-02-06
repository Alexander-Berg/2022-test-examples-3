package ru.yandex.direct.core.entity.minuskeywordspack.service;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.model.StatusShowsForecast;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.MinusKeywordPreparingTool;
import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseValidator;
import ru.yandex.direct.core.entity.minuskeywordspack.container.UpdatedMinusKeywordsPackInfo;
import ru.yandex.direct.core.entity.minuskeywordspack.model.MinusKeywordsPack;
import ru.yandex.direct.core.entity.minuskeywordspack.repository.MinusKeywordsPackRepository;
import ru.yandex.direct.core.entity.minuskeywordspack.service.validation.UpdateMinusKeywordsPackValidationService;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MinusKeywordsPackInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.repository.TestMinusKeywordsPackRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.entity.minuskeywordspack.service.validation.MinusKeywordsPackValidationService.MAX_NAME_LENGTH;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestMinusKeywordsPacks.libraryMinusKeywordsPack;

public abstract class MinusKeywordsPacksUpdateOperationBaseTest {

    protected static final String DEFAULT_NAME = "name";
    protected static final String SECOND_NAME = "second name";
    protected static final String MINUS_WORD = "minus";
    protected static final String MINUS_WORD_2 = "second";
    protected static final String MINUS_WORD_3 = "third";

    protected static final String PREINVALID_MINUS_KEYWORD = "синтаксически невалидная %$&";
    protected static final String INVALID_NAME = RandomStringUtils.randomAlphabetic(MAX_NAME_LENGTH + 1);

    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    protected MinusKeywordsPackRepository minusKeywordsPackRepository;
    @Autowired
    private MinusKeywordPreparingTool minusKeywordPreparingTool;
    @Autowired
    private UpdateMinusKeywordsPackValidationService validationService;
    @Autowired
    protected AdGroupRepository adGroupRepository;
    @Autowired
    protected CampaignRepository campaignRepository;
    @Autowired
    protected TestCampaignRepository testCampaignRepository;
    @Autowired
    protected BannerCommonRepository bannerCommonRepository;
    @Autowired
    protected TestMinusKeywordsPackRepository testMinusKeywordsPackRepository;
    @Autowired
    protected Steps steps;

    protected int shard;
    protected ClientInfo clientInfo;
    protected AdGroupInfo textAdGroup;
    protected AdGroupInfo dynamicAdGroup;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        CampaignInfo activeTextCampaign = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        textAdGroup = steps.adGroupSteps().createAdGroup(
                activeTextAdGroup(null).withStatusShowsForecast(StatusShowsForecast.PROCESSED), activeTextCampaign);
        CampaignInfo activeDynamicCampaign = steps.campaignSteps().createActiveDynamicCampaign(clientInfo);
        dynamicAdGroup = steps.adGroupSteps().createAdGroup(activeDynamicTextAdGroup(null), activeDynamicCampaign);
    }

    protected MinusKeywordsPack createMinusKeywordsPackInAdGroup(AdGroupInfo adGroupInfo, String... minusWords) {
        MinusKeywordsPackInfo libraryMinusKeywordsPack = createLibraryMinusKeywordsPack(minusWords);
        testMinusKeywordsPackRepository
                .linkLibraryMinusKeywordPackToAdGroup(adGroupInfo.getShard(),
                        libraryMinusKeywordsPack.getMinusKeywordPackId(),
                        adGroupInfo.getAdGroupId());
        return libraryMinusKeywordsPack.getMinusKeywordsPack();
    }

    protected MinusKeywordsPackInfo createLibraryMinusKeywordsPack(String... minusWords) {
        return steps.minusKeywordsPackSteps()
                .createMinusKeywordsPack(
                        libraryMinusKeywordsPack()
                                .withName(DEFAULT_NAME)
                                .withMinusKeywords(Arrays.asList(minusWords)),
                        clientInfo);

    }

    protected MinusKeywordsPack getMinusKeywordsPack(Long id) {
        return minusKeywordsPackRepository
                .get(clientInfo.getShard(), clientInfo.getClientId(), singletonList(id)).get(0);
    }

    protected ModelChanges<MinusKeywordsPack> minusKeywordsModelChanges(Long id, String... minusKeywords) {
        return ModelChanges
                .build(id, MinusKeywordsPack.class, MinusKeywordsPack.MINUS_KEYWORDS, Arrays.asList(minusKeywords));
    }

    protected ModelChanges<MinusKeywordsPack> nameModelChanges(Long id, String name) {
        return ModelChanges
                .build(id, MinusKeywordsPack.class, MinusKeywordsPack.NAME, name);
    }

    protected MassResult<UpdatedMinusKeywordsPackInfo> executePartial(
            List<ModelChanges<MinusKeywordsPack>> modelChanges) {
        return createOperation(Applicability.PARTIAL, modelChanges).prepareAndApply();
    }

    protected MassResult<UpdatedMinusKeywordsPackInfo> executeFull(
            List<ModelChanges<MinusKeywordsPack>> modelChanges) {
        return createOperation(Applicability.FULL, modelChanges).prepareAndApply();
    }

    protected MinusKeywordsPacksUpdateOperation createOperation(Applicability applicability,
                                                                List<ModelChanges<MinusKeywordsPack>> modelChanges) {
        return createOperation(applicability, MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE_AND_KEYWORD,
                modelChanges);
    }

    protected MinusKeywordsPacksUpdateOperation createOperation(Applicability applicability,
                                                                MinusPhraseValidator.ValidationMode minusPhraseValidationMode,
                                                                List<ModelChanges<MinusKeywordsPack>> modelChanges) {
        return new MinusKeywordsPacksUpdateOperation(applicability, modelChanges, dslContextProvider,
                minusKeywordsPackRepository, minusKeywordPreparingTool, validationService, adGroupRepository,
                campaignRepository, bannerCommonRepository, minusPhraseValidationMode,
                clientInfo.getClientId(), clientInfo.getShard());
    }

    protected UpdatedMinusKeywordsPackInfo expectedResult(Long id, List<String> minusKeywords) {
        return new UpdatedMinusKeywordsPackInfo()
                .withId(id)
                .withName(DEFAULT_NAME)
                .withMinusKeywords(minusKeywords);
    }

}
