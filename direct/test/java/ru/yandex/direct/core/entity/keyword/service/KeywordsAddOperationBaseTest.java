package ru.yandex.direct.core.entity.keyword.service;

import java.math.BigDecimal;
import java.util.List;

import javax.annotation.Nullable;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.common.log.service.LogPriceService;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.autobudget.service.AutobudgetAlertService;
import ru.yandex.direct.core.entity.autoprice.service.AutoPriceCampQueueService;
import ru.yandex.direct.core.entity.banner.repository.BannerRepository;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.client.service.ClientLimitsService;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.keyword.container.AddedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.container.KeywordsAddOperationParams;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.processing.KeywordNormalizer;
import ru.yandex.direct.core.entity.keyword.processing.unglue.KeywordUngluer;
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository;
import ru.yandex.direct.core.entity.keyword.service.internal.InternalKeywordFactory;
import ru.yandex.direct.core.entity.keyword.service.validation.KeywordsAddValidationService;
import ru.yandex.direct.core.entity.mailnotification.service.MailNotificationEventService;
import ru.yandex.direct.core.entity.moderation.service.ModerationService;
import ru.yandex.direct.core.entity.showcondition.container.ShowConditionFixedAutoPrices;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.libs.keywordutils.helper.SingleKeywordsCache;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static ru.yandex.direct.core.entity.keyword.service.validation.KeywordDefects.duplicatedWithExisting;
import static ru.yandex.direct.core.entity.keyword.service.validation.KeywordDefects.duplicatedWithNew;

public class KeywordsAddOperationBaseTest extends KeywordsBaseTest {

    @Autowired
    private FixStopwordsService fixStopwordsService;

    @Autowired
    protected KeywordNormalizer keywordNormalizer;

    @Autowired
    private KeywordUngluer keywordUngluer;

    @Autowired
    private KeywordsAddValidationService validationService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private ModerationService moderationService;

    @Autowired
    private AutoPriceCampQueueService autoPriceCampQueueService;

    @Autowired
    private MailNotificationEventService mailNotificationEventService;

    @Autowired
    private LogPriceService logPriceService;

    @Autowired
    private AutobudgetAlertService autobudgetAlertService;

    @Autowired
    private SingleKeywordsCache singleKeywordsCache;

    @Autowired
    private InternalKeywordFactory internalKeywordFactory;

    @Autowired
    protected DslContextProvider dslContextProvider;

    @Autowired
    protected KeywordRepository keywordRepository;

    @Autowired
    protected OldBannerRepository bannerRepository;

    @Autowired
    protected BannerRepository newBannerRepository;

    @Autowired
    protected AdGroupRepository adGroupRepository;

    @Autowired
    protected CampaignRepository campaignRepository;

    @Autowired
    protected ClientLimitsService clientLimitsService;

    @Before
    public void before() {
        super.before();
    }

    protected MassResult<AddedKeywordInfo> executePartial(List<Keyword> keywords) {
        return createOperation(Applicability.PARTIAL, keywords).prepareAndApply();
    }

    protected MassResult<AddedKeywordInfo> executePartialWithOutUnglue(List<Keyword> keywords) {
        return createOperationWithoutUnglue(Applicability.PARTIAL, keywords).prepareAndApply();
    }

    protected MassResult<AddedKeywordInfo> executePartial(List<Keyword> keywords, long operatorUid) {
        KeywordsAddOperationParams operationParams = KeywordsAddOperationParams.builder()
                .withAdGroupsNonexistentOnPrepare(false)
                .withIgnoreOversize(false)
                .withUnglueEnabled(true)
                .withAutoPrices(false)
                .withWeakenValidation(false)
                .build();
        return createOperation(Applicability.PARTIAL, operationParams, keywords, null, operatorUid)
                .prepareAndApply();
    }

    protected MassResult<AddedKeywordInfo> executeFull(List<Keyword> keywords) {
        return createOperation(Applicability.FULL, keywords).prepareAndApply();
    }

    protected MassResult<AddedKeywordInfo> execute(Applicability applicability, List<Keyword> keywords) {
        return createOperation(applicability, keywords).prepareAndApply();
    }

    protected KeywordsAddOperation createOperation(Applicability applicability, List<Keyword> keywords) {
        KeywordsAddOperationParams operationParams = KeywordsAddOperationParams.builder()
                .withAdGroupsNonexistentOnPrepare(false)
                .withUnglueEnabled(true)
                .withIgnoreOversize(false)
                .withAutoPrices(false)
                .withWeakenValidation(false)
                .build();
        return createOperation(applicability, operationParams, keywords, null, clientInfo.getUid());
    }

    protected KeywordsAddOperation createOperationWithAutoPrices(Applicability applicability, BigDecimal fixedPrice,
                                                                 List<Keyword> keywords) {
        KeywordsAddOperationParams operationParams = KeywordsAddOperationParams.builder()
                .withAdGroupsNonexistentOnPrepare(false)
                .withUnglueEnabled(true)
                .withIgnoreOversize(false)
                .withAutoPrices(true)
                .withWeakenValidation(false)
                .build();
        return new KeywordsAddOperation(applicability, operationParams, keywords,
                fixStopwordsService, keywordNormalizer,
                keywordUngluer, validationService, clientService, moderationService,
                autoPriceCampQueueService, mailNotificationEventService, logPriceService,
                keywordBsAuctionService, clientLimitsService, autobudgetAlertService,
                singleKeywordsCache, internalKeywordFactory, keywordShowsForecastService,
                dslContextProvider, keywordRepository, newBannerRepository,
                adGroupRepository, campaignRepository,
                clientInfo.getUid(), clientInfo.getClientId(), clientInfo.getUid(), clientInfo.getShard(),
                null, ShowConditionFixedAutoPrices.ofGlobalFixedPrice(fixedPrice), null);
    }

    protected KeywordsAddOperation createOperationWithoutUnglue(Applicability applicability, List<Keyword> keywords) {
        KeywordsAddOperationParams operationParams = KeywordsAddOperationParams.builder()
                .withAdGroupsNonexistentOnPrepare(false)
                .withUnglueEnabled(false)
                .withIgnoreOversize(false)
                .withAutoPrices(false)
                .withWeakenValidation(false)
                .build();
        return createOperation(applicability, operationParams, keywords, null, clientInfo.getUid());
    }

    protected KeywordsAddOperation createOperationWithCopyAction(List<Keyword> keywords) {
        KeywordsAddOperationParams operationParams = KeywordsAddOperationParams.builder()
                .withAdGroupsNonexistentOnPrepare(true)
                .withUnglueEnabled(true)
                .withIgnoreOversize(false)
                .withAutoPrices(false)
                .withWeakenValidation(false)
                .build();
        return createOperation(Applicability.FULL, operationParams, keywords, null, clientInfo.getUid());
    }

    protected KeywordsAddOperation createOperationWithIgnoreOversize(List<Keyword> keywords) {
        KeywordsAddOperationParams operationParams = KeywordsAddOperationParams.builder()
                .withAdGroupsNonexistentOnPrepare(false)
                .withUnglueEnabled(true)
                .withIgnoreOversize(true)
                .withAutoPrices(false)
                .withWeakenValidation(false)
                .build();
        return createOperation(Applicability.FULL, operationParams, keywords, null, clientInfo.getUid());
    }

    protected KeywordsAddOperation createOperation(Applicability applicability,
                                                   KeywordsAddOperationParams operationParams,
                                                   List<Keyword> keywords, List<Keyword> existingKeywords, long operatorUid) {
        return createOperation(applicability, operationParams, keywords, existingKeywords, operatorUid,
                null, null);
    }

    protected KeywordsAddOperation createOperationWithAutoPrices(
            List<Keyword> keywords, List<Keyword> existingKeywords,
            @Nullable ShowConditionFixedAutoPrices fixedAutoPrices,
            @Nullable KeywordAutoPricesCalculator keywordAutoPricesCalculator
    ) {
        KeywordsAddOperationParams operationParams = KeywordsAddOperationParams.builder()
                .withAdGroupsNonexistentOnPrepare(false)
                .withUnglueEnabled(true)
                .withIgnoreOversize(false)
                .withAutoPrices(true)
                .withWeakenValidation(false)
                .build();
        return createOperation(Applicability.FULL, operationParams, keywords, existingKeywords, clientInfo.getUid(),
                fixedAutoPrices, keywordAutoPricesCalculator);
    }

    protected KeywordsAddOperation createOperation(Applicability applicability,
                                                   KeywordsAddOperationParams operationParams,
                                                   List<Keyword> keywords, List<Keyword> existingKeywords, long operatorUid,
                                                   @Nullable ShowConditionFixedAutoPrices fixedAutoPrices,
                                                   @Nullable KeywordAutoPricesCalculator keywordAutoPricesCalculator
    ) {
        return new KeywordsAddOperation(applicability, operationParams, keywords,
                fixStopwordsService, keywordNormalizer,
                keywordUngluer, validationService, clientService, moderationService,
                autoPriceCampQueueService, mailNotificationEventService, logPriceService,
                keywordBsAuctionService, clientLimitsService, autobudgetAlertService,
                singleKeywordsCache, internalKeywordFactory, keywordShowsForecastService,
                dslContextProvider, keywordRepository, newBannerRepository,
                adGroupRepository, campaignRepository,
                operatorUid, clientInfo.getClientId(), clientInfo.getUid(), clientInfo.getShard(),
                existingKeywords, fixedAutoPrices, keywordAutoPricesCalculator);
    }

    protected void checkValidationHasDuplicateInExistingWarnings(MassResult<?> result, boolean... hasWarningFlags) {
        checkValidationHasWarnings(result, duplicatedWithExisting(), hasWarningFlags);
    }

    protected void checkValidationHasDuplicateInNewWarnings(MassResult<?> result, boolean... hasWarningFlags) {
        checkValidationHasWarnings(result, duplicatedWithNew(), hasWarningFlags);
    }
}
