package ru.yandex.direct.core.entity.keyword.service;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.log.service.LogPriceService;
import ru.yandex.direct.core.aggregatedstatuses.repository.AggregatedStatusesRepository;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.autobudget.service.AutobudgetAlertService;
import ru.yandex.direct.core.entity.autoprice.service.AutoPriceCampQueueService;
import ru.yandex.direct.core.entity.banner.repository.BannerRepository;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.keyword.container.KeywordsUpdateOperationParams;
import ru.yandex.direct.core.entity.keyword.container.UpdatedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.processing.unglue.KeywordUngluer;
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository;
import ru.yandex.direct.core.entity.keyword.service.internal.InternalKeywordFactory;
import ru.yandex.direct.core.entity.keyword.service.validation.UpdateKeywordValidationService;
import ru.yandex.direct.core.entity.mailnotification.service.MailNotificationEventService;
import ru.yandex.direct.core.entity.moderation.service.ModerationService;
import ru.yandex.direct.core.entity.showcondition.container.ShowConditionFixedAutoPrices;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.libs.keywordutils.helper.SingleKeywordsCache;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.common.db.PpcPropertyNames.MODERATE_EVERY_KEYWORD_CHANGE;
import static ru.yandex.direct.core.entity.keyword.service.validation.KeywordDefects.duplicatedWithExisting;
import static ru.yandex.direct.core.entity.keyword.service.validation.KeywordDefects.duplicatedWithUpdated;

public abstract class KeywordsUpdateOperationBaseTest extends KeywordsBaseTest {

    // Включаем проперти переотправки любого изменения текста фраз на модерацию
    private static final Boolean MODERATE_EVERY_KEYWORD_CHANGE_PROPERTY_VALUE = true;

    @Autowired
    private FixStopwordsService fixStopwordsService;

    @Autowired
    private KeywordUngluer keywordUngluer;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    protected AdGroupRepository adGroupRepository;

    @Autowired
    protected OldBannerRepository bannerRepository;

    @Autowired
    protected BannerRepository newBannerRepository;

    @Autowired
    protected KeywordRepository keywordRepository;

    @Autowired
    protected AggregatedStatusesRepository aggregatedStatusesRepository;

    @Autowired
    protected UpdateKeywordValidationService validationService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private ModerationService moderationService;

    @Autowired
    private AutoPriceCampQueueService autoPriceCampQueueService;

    @Autowired
    private AutobudgetAlertService autobudgetAlertService;

    @Autowired
    private MailNotificationEventService mailNotificationEventService;

    @Autowired
    private LogPriceService logPriceService;

    @Autowired
    private SingleKeywordsCache singleKeywordsCache;

    @Autowired
    private InternalKeywordFactory internalKeywordFactory;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    protected PpcPropertiesSupport ppcPropertiesSupport;

    private KeywordModerationService keywordModerationService;

    @Before
    public void before() {
        super.before();
        keywordModerationService = new KeywordModerationService(ppcPropertiesSupport);
        ppcPropertiesSupport.set(MODERATE_EVERY_KEYWORD_CHANGE.getName(),
                MODERATE_EVERY_KEYWORD_CHANGE_PROPERTY_VALUE.toString());
    }

    protected MassResult<UpdatedKeywordInfo> executePartial(List<ModelChanges<Keyword>> keywords) {
        return createOperation(Applicability.PARTIAL, keywords).prepareAndApply();
    }

    protected MassResult<UpdatedKeywordInfo> executePartialWithoutUnglue(List<ModelChanges<Keyword>> keywords) {
        return createOperationWithoutUnglue(Applicability.PARTIAL, keywords).prepareAndApply();
    }

    protected MassResult<UpdatedKeywordInfo> executePartial(List<ModelChanges<Keyword>> keywords,
                                                            List<Keyword> existingKeywords) {
        return createOperation(Applicability.PARTIAL, keywords, existingKeywords).prepareAndApply();
    }

    protected MassResult<UpdatedKeywordInfo> executePartialWithoutPhraseModification(
            List<ModelChanges<Keyword>> keywords)
    {
        return createOperationWithoutPhraseModification(Applicability.PARTIAL, keywords).prepareAndApply();
    }

    protected MassResult<UpdatedKeywordInfo> executeFull(List<ModelChanges<Keyword>> keywords) {
        return createOperation(Applicability.FULL, keywords).prepareAndApply();
    }

    protected MassResult<UpdatedKeywordInfo> execute(Applicability applicability,
                                                     List<ModelChanges<Keyword>> keywords) {
        return createOperation(applicability, keywords).prepareAndApply();
    }

    protected MassResult<UpdatedKeywordInfo> executeWithAutoPrices(List<ModelChanges<Keyword>> keywords,
                                                                   ShowConditionFixedAutoPrices fixedAutoPrices,
                                                                   KeywordAutoPricesCalculator autoPricesCalculator) {
        return createOperationWithAutoPrices(keywords, fixedAutoPrices, autoPricesCalculator).prepareAndApply();
    }

    protected KeywordsUpdateOperation createOperation(Applicability applicability,
                                                      List<ModelChanges<Keyword>> keywords) {
        KeywordsUpdateOperationParams operationParams = KeywordsUpdateOperationParams.builder()
                .withUnglueEnabled(true)
                .withAutoPrices(false)
                .withModificationDisabled(false)
                .build();
        return createOperation(applicability, operationParams, keywords, null, null, null);
    }

    protected KeywordsUpdateOperation createOperationWithoutUnglue(Applicability applicability,
                                                                   List<ModelChanges<Keyword>> keywords) {
        KeywordsUpdateOperationParams operationParams = KeywordsUpdateOperationParams.builder()
                .withUnglueEnabled(false)
                .withAutoPrices(false)
                .withModificationDisabled(false)
                .build();
        return createOperation(applicability, operationParams, keywords, null, null, null);
    }

    protected KeywordsUpdateOperation createOperationWithoutPhraseModification(Applicability applicability,
            List<ModelChanges<Keyword>> keywords)
    {
        KeywordsUpdateOperationParams operationParams = KeywordsUpdateOperationParams.builder()
                .withUnglueEnabled(false)
                .withAutoPrices(false)
                .withModificationDisabled(true)
                .build();
        return createOperation(applicability, operationParams, keywords, null, null, null);
    }

    protected KeywordsUpdateOperation createOperation(Applicability applicability, List<ModelChanges<Keyword>> keywords,
                                                      List<Keyword> existingKeywords) {
        KeywordsUpdateOperationParams operationParams = KeywordsUpdateOperationParams.builder()
                .withUnglueEnabled(true)
                .withAutoPrices(false)
                .withModificationDisabled(false)
                .build();
        return createOperation(applicability, operationParams, keywords, existingKeywords, null, null);
    }

    protected KeywordsUpdateOperation createOperationWithAutoPrices(List<ModelChanges<Keyword>> keywords,
                                                                    ShowConditionFixedAutoPrices fixedAutoPrices,
                                                                    KeywordAutoPricesCalculator autoPricesCalculator) {
        KeywordsUpdateOperationParams operationParams = KeywordsUpdateOperationParams.builder()
                .withUnglueEnabled(true)
                .withAutoPrices(true)
                .withModificationDisabled(false)
                .build();

        return createOperation(Applicability.FULL, operationParams, keywords, null, fixedAutoPrices,
                autoPricesCalculator);
    }

    protected KeywordsUpdateOperation createOperation(Applicability applicability,
                                                      KeywordsUpdateOperationParams operationParams,
                                                      List<ModelChanges<Keyword>> keywords,
                                                      List<Keyword> existingKeywords,
                                                      ShowConditionFixedAutoPrices fixedAutoPrices,
                                                      KeywordAutoPricesCalculator autoPricesCalculator) {
        return new KeywordsUpdateOperation(applicability, operationParams, keywords, fixStopwordsService,
                keywordNormalizer, keywordUngluer, validationService, clientService, moderationService,
                autoPriceCampQueueService, autobudgetAlertService, mailNotificationEventService, logPriceService,
                keywordBsAuctionService, keywordModerationService, singleKeywordsCache, internalKeywordFactory,
                keywordShowsForecastService,
                dslContextProvider, keywordRepository, newBannerRepository,
                adGroupRepository, campaignRepository,
                aggregatedStatusesRepository,
                operatorClientInfo.getUid(), clientInfo.getClientId(), clientInfo.getUid(), clientInfo.getShard(),
                existingKeywords, fixedAutoPrices, autoPricesCalculator);
    }

    protected KeywordsUpdateOperation createOperationWithAutoPrices(BigDecimal fixedPrice,
                                                                    List<ModelChanges<Keyword>> keywords) {
        KeywordsUpdateOperationParams operationParams = KeywordsUpdateOperationParams.builder()
                .withUnglueEnabled(true)
                .withAutoPrices(true)
                .withModificationDisabled(false)
                .build();
        return new KeywordsUpdateOperation(Applicability.PARTIAL, operationParams, keywords, fixStopwordsService,
                keywordNormalizer, keywordUngluer, validationService, clientService, moderationService,
                autoPriceCampQueueService, autobudgetAlertService, mailNotificationEventService, logPriceService,
                keywordBsAuctionService, keywordModerationService, singleKeywordsCache, internalKeywordFactory,
                keywordShowsForecastService,
                dslContextProvider, keywordRepository, newBannerRepository,
                adGroupRepository, campaignRepository,
                aggregatedStatusesRepository,
                operatorClientInfo.getUid(), clientInfo.getClientId(), clientInfo.getUid(), clientInfo.getShard(),
                null, ShowConditionFixedAutoPrices.ofGlobalFixedPrice(fixedPrice), null);
    }

    protected void checkValidationHasDuplicateInExistingWarnings(MassResult<?> result, boolean... hasWarningFlags) {
        checkValidationHasWarnings(result, duplicatedWithExisting(), hasWarningFlags);
    }

    protected void checkValidationHasDuplicateInUpdatedWarnings(MassResult<?> result, boolean... hasWarningFlags) {
        checkValidationHasWarnings(result, duplicatedWithUpdated(), hasWarningFlags);
    }

    protected void checkKeywordsDeleted(Long... ids) {
        List<Keyword> keywords =
                keywordRepository.getKeywordsByIds(clientInfo.getShard(), clientInfo.getClientId(), asList(ids));
        assertThat("фразы дожны быть удалены", keywords, empty());
    }
}
