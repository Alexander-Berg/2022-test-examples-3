package ru.yandex.direct.core.entity.keyword.service;

import java.util.List;

import javax.annotation.Nullable;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.common.log.service.LogPriceService;
import ru.yandex.direct.core.aggregatedstatuses.repository.AggregatedStatusesRepository;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexAdGroupAddOperationFactory;
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexAdGroupService;
import ru.yandex.direct.core.entity.autobudget.service.AutobudgetAlertService;
import ru.yandex.direct.core.entity.autoprice.service.AutoPriceCampQueueService;
import ru.yandex.direct.core.entity.banner.repository.BannerRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.client.service.ClientGeoService;
import ru.yandex.direct.core.entity.client.service.ClientLimitsService;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.keyword.container.AddedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.container.KeywordsModificationContainer;
import ru.yandex.direct.core.entity.keyword.container.KeywordsModificationResult;
import ru.yandex.direct.core.entity.keyword.container.KeywordsModifyOperationParams;
import ru.yandex.direct.core.entity.keyword.container.UpdatedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.processing.KeywordNormalizer;
import ru.yandex.direct.core.entity.keyword.processing.unglue.KeywordUngluer;
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository;
import ru.yandex.direct.core.entity.keyword.service.internal.InternalKeywordFactory;
import ru.yandex.direct.core.entity.keyword.service.validation.DeleteKeywordValidationService;
import ru.yandex.direct.core.entity.keyword.service.validation.KeywordsAddValidationService;
import ru.yandex.direct.core.entity.keyword.service.validation.UpdateKeywordValidationService;
import ru.yandex.direct.core.entity.mailnotification.service.MailNotificationEventService;
import ru.yandex.direct.core.entity.moderation.service.ModerationService;
import ru.yandex.direct.core.entity.showcondition.container.ShowConditionAutoPriceParams;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.libs.keywordutils.helper.SingleKeywordsCache;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.Result;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.container.KeywordsModificationContainer.addUpdateDelete;
import static ru.yandex.direct.core.entity.keyword.container.KeywordsModificationContainer.addWithCopyOnOversize;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;

public class KeywordsModifyOperationBaseTest extends KeywordsBaseTest {

    @Autowired
    private FixStopwordsService fixStopwordsService;

    @Autowired
    private KeywordNormalizer keywordNormalizer;

    @Autowired
    private KeywordUngluer keywordUngluer;

    @Autowired
    private KeywordsAddValidationService addValidationService;

    @Autowired
    private UpdateKeywordValidationService updateValidationService;

    @Autowired
    private DeleteKeywordValidationService deleteValidationService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private RbacService rbacService;

    @Autowired
    private ComplexAdGroupService complexAdGroupService;

    @Autowired
    private ComplexAdGroupAddOperationFactory complexAdGroupAddOperationFactory;

    @Autowired
    private ModerationService moderationService;

    @Autowired
    private ClientGeoService clientGeoService;

    @Autowired
    private AutoPriceCampQueueService autoPriceCampQueueService;

    @Autowired
    private MailNotificationEventService mailNotificationEventService;

    @Autowired
    private KeywordModerationService keywordModerationService;

    @Autowired
    private LogPriceService logPriceService;

    @Autowired
    private AutobudgetAlertService autobudgetAlertService;

    @Autowired
    private SingleKeywordsCache singleKeywordsCache;

    @Autowired
    private InternalKeywordFactory internalKeywordFactory;

    @Autowired
    private ShardHelper shardHelper;


    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private KeywordRepository keywordRepository;

    @Autowired
    private BannerRepository bannerRepository;

    @Autowired
    protected AdGroupRepository adGroupRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private AggregatedStatusesRepository aggregatedStatusesRepository;

    @Autowired
    private ClientLimitsService clientLimitsService;

    private KeywordOperationFactory keywordOperationFactoryWithMockedAuctionService;

    @Autowired
    @Before
    public void before() {
        super.before();

        keywordOperationFactoryWithMockedAuctionService = new KeywordOperationFactory(fixStopwordsService,
                keywordNormalizer, keywordUngluer, addValidationService, updateValidationService,
                deleteValidationService, clientService, clientLimitsService, moderationService,
                autoPriceCampQueueService, mailNotificationEventService,
                logPriceService, keywordBsAuctionService, autobudgetAlertService,
                singleKeywordsCache, keywordModerationService, internalKeywordFactory, keywordShowsForecastService,
                shardHelper, dslContextProvider, keywordRepository,
                adGroupRepository, campaignRepository,
                aggregatedStatusesRepository, bannerRepository
        );
    }

    protected Result<KeywordsModificationResult> executeAdd(List<Keyword> addList) {
        return execute(addList, emptyList(), emptyList());
    }

    protected Result<KeywordsModificationResult> executeUpdate(List<ModelChanges<Keyword>> updateList) {
        return execute(emptyList(), updateList, emptyList());
    }

    protected Result<KeywordsModificationResult> executeDelete(List<Long> deleteList) {
        return execute(emptyList(), emptyList(), deleteList);
    }

    protected Result<KeywordsModificationResult> execute(List<Keyword> addList,
                                                         List<ModelChanges<Keyword>> updateList, List<Long> deleteList) {
        return createOperation(addUpdateDelete(addList, updateList, deleteList), false, null).prepareAndApply();
    }

    protected Result<KeywordsModificationResult> executeWithAutoPrices(List<Keyword> addList,
                                                                       List<ModelChanges<Keyword>> updateList, List<Long> deleteList,
                                                                       ShowConditionAutoPriceParams autoPriceParams) {
        return createOperation(addUpdateDelete(addList, updateList, deleteList), true, autoPriceParams).prepareAndApply();
    }

    protected Result<KeywordsModificationResult> executeAddWithOversize(List<Keyword> addList) {
        return createOperation(addWithCopyOnOversize(addList), false, null).prepareAndApply();
    }

    protected Result<KeywordsModificationResult> executeAddWithOversizeAndAutoPrices(
            List<Keyword> addList, ShowConditionAutoPriceParams autoPriceParams) {
        return createOperation(addWithCopyOnOversize(addList), true, autoPriceParams).prepareAndApply();
    }

    protected KeywordsModifyOperation createOperation(KeywordsModificationContainer inputContainer) {
        return createOperation(inputContainer, false, null);
    }

    protected KeywordsModifyOperation createOperation(KeywordsModificationContainer inputContainer,
                                                      boolean isAutoPrices, @Nullable ShowConditionAutoPriceParams autoPriceParams) {
        KeywordsModifyOperationParams operationParams = KeywordsModifyOperationParams.builder()
                .withAutoPrices(isAutoPrices)
                .build();
        return new KeywordsModifyOperation(operationParams, keywordOperationFactoryWithMockedAuctionService,
                keywordRepository, keywordNormalizer, adGroupRepository,
                complexAdGroupService, clientLimitsService, clientGeoService, rbacService, clientService,
                complexAdGroupAddOperationFactory, autoPriceParams, operatorClientInfo.getUid(), clientInfo.getClientId(),
                clientInfo.getUid(), clientInfo.getShard(), inputContainer);
    }

    protected void assertAddResultIsSuccessful(Result<KeywordsModificationResult> result,
                                               List<Matcher<AddedKeywordInfo>> addMatchers) {
        assertResultIsSuccessful(result, addMatchers, null, null);
    }

    protected void assertUpdateResultIsSuccessful(Result<KeywordsModificationResult> result,
                                                  List<Matcher<UpdatedKeywordInfo>> updateMatchers) {
        assertResultIsSuccessful(result, null, updateMatchers, null);
    }

    protected void assertDeleteResultIsSuccessful(Result<KeywordsModificationResult> result,
                                                  List<Long> expectedIds) {
        assertResultIsSuccessful(result, null, null, expectedIds);
    }

    @SuppressWarnings("ConstantConditions")
    protected void assertResultIsSuccessful(Result<KeywordsModificationResult> result,
                                            List<Matcher<AddedKeywordInfo>> addMatchers,
                                            List<Matcher<UpdatedKeywordInfo>> updateMatchers,
                                            List<Long> expectedDeletedIds) {
        assertThat(result.isSuccessful(), is(true));
        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());

        KeywordsModificationResult resultValue = result.getResult();

        if (addMatchers != null) {
            List<AddedKeywordInfo> addResults = resultValue.getAddResults();

            assertThat("количество результатов добавления не соответствует ожидаемому",
                    addResults, hasSize(addMatchers.size()));

            for (int i = 0; i < addResults.size(); i++) {
                assertThat(addResults.get(i), addMatchers.get(i));
            }
        } else {
            assertThat("результаты добавления должны отсутствовать",
                    resultValue.getAddResults(), nullValue());
        }

        if (updateMatchers != null) {
            List<UpdatedKeywordInfo> updateResults = resultValue.getUpdateResults();

            assertThat("количество результатов обновления не соответствует ожидаемому",
                    updateResults, hasSize(updateMatchers.size()));

            for (int i = 0; i < updateResults.size(); i++) {
                assertThat(updateResults.get(i), updateMatchers.get(i));
            }
        } else {
            assertThat("результаты обновления должны отсутствовать",
                    resultValue.getUpdateResults(), nullValue());
        }

        if (expectedDeletedIds != null) {
            assertThat("результаты удаления не соответствуют ожидаемым",
                    resultValue.getDeleteResults(), contains(expectedDeletedIds.toArray()));
        } else {
            assertThat("результаты удаления должны отсутствовать",
                    resultValue.getDeleteResults(), nullValue());
        }
    }

    protected void assertResultIsFailed(Result<KeywordsModificationResult> result, String... existingPhrases) {
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.getResult(), nullValue());
        assertExistingPhrases(existingPhrases);
    }

    protected void assertExistingPhrases(String... existingPhrases) {
        List<String> clientPhrases = testKeywordRepository
                .getClientPhrases(clientInfo.getShard(), clientInfo.getClientId());
        assertThat("фразы клиента не соответствуют ожидаемым",
                clientPhrases, containsInAnyOrder(existingPhrases));
    }
}
