package ru.yandex.direct.core.testing.steps;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.yandex.direct.core.entity.brandSurvey.BrandSurvey;
import ru.yandex.direct.core.entity.brandlift.repository.BrandSurveyRepository;
import ru.yandex.direct.core.entity.bs.common.service.BsOrderIdCalculator;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignsMulticurrencySumsRepository;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.uac.grut.GrutTransactionProvider;
import ru.yandex.direct.core.entity.uac.service.GrutUacCampaignService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.grut.replication.GrutApiService;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MobileAppInfo;
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.repository.TestCpmYndxFrontpageRepository;
import ru.yandex.direct.core.testing.steps.campaign.model0.BalanceInfo;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.ManualStrategy;
import ru.yandex.direct.core.testing.steps.campaign.repository0.CampaignRepository;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsDayBudgetShowMode;
import ru.yandex.direct.dbschema.ppc.enums.WalletCampaignsAutopayMode;
import ru.yandex.direct.dbutil.exception.RollbackException;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.utils.CommonUtils;
import ru.yandex.grut.objects.proto.client.Schema;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeBalanceInfo;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCampaignByCampaignType;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeContentPromotionCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmDealsCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmPriceCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmYndxFrontpageCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeDynamicCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeInternalAutobudgetCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeInternalDistribCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeInternalFreeCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeMcBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeMobileAppCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activePerformanceCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activePerformanceCampaignWithStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeUacMobileAppCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeWalletCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.autobudgetMaxImpressionsCustomPeriodStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageBidStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageCpaPerCampStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageCpaStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultMulticurrencySums;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newBillingAggregate;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newCampaignByCampaignTypeOld;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newSmartCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newTextCampaign;
import static ru.yandex.direct.dbschema.ppc.Tables.AUTOPAY_SETTINGS;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.dbschema.ppc.Tables.USER_CAMPAIGNS_FAVORITE;
import static ru.yandex.direct.dbschema.ppc.Tables.WALLET_CAMPAIGNS;

@Component
public class CampaignSteps {
    final ClientSteps clientSteps;
    final InternalAdProductSteps internalAdProductSteps;
    final PricePackageSteps pricePackageSteps;
    final CampaignRepository campaignRepository0;
    final CampaignModifyRepository campaignModifyRepository;
    final ru.yandex.direct.core.entity.campaign.repository.CampaignRepository campaignRepository;
    final TestCampaignRepository testCampaignRepository;
    final TestCpmYndxFrontpageRepository testCpmYndxFrontpageRepository;
    private final GrutUacCampaignService grutUacCampaignService;
    private final GrutTransactionProvider grutTransactionProvider;
    private final BrandSurveyRepository brandSurveyRepository;
    private final GrutApiService grutApiService;

    final CampaignsMulticurrencySumsRepository campaignsMulticurrencySumsRepository;
    final DslContextProvider dslContextProvider;

    @Autowired
    public CampaignSteps(ClientSteps clientSteps,
                         InternalAdProductSteps internalAdProductSteps, PricePackageSteps pricePackageSteps,
                         CampaignRepository campaignRepository0,
                         ru.yandex.direct.core.entity.campaign.repository.CampaignRepository campaignRepository,
                         TestCampaignRepository testCampaignRepository,
                         TestCpmYndxFrontpageRepository testCpmYndxFrontpageRepository,
                         CampaignsMulticurrencySumsRepository campaignsMulticurrencySumsRepository,
                         DslContextProvider dslContextProvider,
                         CampaignModifyRepository campaignModifyRepository,
                         GrutUacCampaignService grutUacCampaignService,
                         GrutTransactionProvider grutTransactionProvider, BrandSurveyRepository brandSurveyRepository,
                         GrutApiService grutApiService) {
        this.clientSteps = clientSteps;
        this.internalAdProductSteps = internalAdProductSteps;
        this.pricePackageSteps = pricePackageSteps;
        this.campaignRepository0 = campaignRepository0;
        this.campaignRepository = campaignRepository;
        this.testCampaignRepository = testCampaignRepository;
        this.testCpmYndxFrontpageRepository = testCpmYndxFrontpageRepository;
        this.campaignsMulticurrencySumsRepository = campaignsMulticurrencySumsRepository;
        this.dslContextProvider = dslContextProvider;
        this.campaignModifyRepository = campaignModifyRepository;
        this.grutUacCampaignService = grutUacCampaignService;
        this.grutTransactionProvider = grutTransactionProvider;
        this.brandSurveyRepository = brandSurveyRepository;
        this.grutApiService = grutApiService;
    }

    public CampaignInfo createActiveCampaignByCampaignType(CampaignType campaignType, ClientInfo clientInfo) {
        switch (campaignType) {
            case TEXT:
                return createActiveTextCampaign(clientInfo);
            case DYNAMIC:
                return createActiveDynamicCampaign(clientInfo);
            case PERFORMANCE:
                return createActivePerformanceCampaign(clientInfo);
            case MOBILE_CONTENT:
                return createActiveMobileAppCampaign(clientInfo);
            case CPM_BANNER:
                return createActiveCpmBannerCampaign(clientInfo);
            case CPM_DEALS:
                return createActiveCpmDealsCampaign(clientInfo);
            case CPM_YNDX_FRONTPAGE:
                return createActiveCpmYndxFrontpageCampaign(clientInfo);
            case CPM_PRICE:
                return createActiveCpmPriceCampaignOld(clientInfo);
            case CONTENT_PROMOTION:
                return createActiveContentPromotionCampaign(clientInfo);
            case INTERNAL_FREE:
                return createActiveInternalFreeCampaign(clientInfo);
            case INTERNAL_DISTRIB:
                return createActiveInternalDistribCampaign(clientInfo);
            case INTERNAL_AUTOBUDGET:
                return createActiveInternalAutobudgetCampaign(clientInfo);
            case MCBANNER:
                return createActiveMcBannerCampaign(clientInfo);
            default:
                throw new IllegalArgumentException("Неизвестный тип кампании: " + campaignType);
        }
    }

    public CampaignInfo createActiveCampaignAutoStrategyByCampaignType(ClientInfo clientInfo,
                                                                       CampaignType campaignType) {
        switch (campaignType) {
            case TEXT:
                return createActiveTextCampaignAutoStrategy(clientInfo);
            case DYNAMIC:
                return createActiveDynamicCampaignAutoStrategy(clientInfo);
            case PERFORMANCE:
                return createActiveSmartCampaign(clientInfo);
            case MCBANNER:
                return createActiveMcBannerCampaignAutoStrategy(clientInfo);
            case CPM_BANNER:
                return createActiveCpmBannerCampaign(clientInfo);
            default:
                throw new IllegalArgumentException("Неизвестный тип кампании: " + campaignType);
        }
    }

    public CampaignInfo createActiveTextCampaign() {
        return createCampaign(activeTextCampaign(null, null));
    }

    public CampaignInfo createActiveTextCampaign(ClientInfo clientInfo) {
        return createCampaign(activeTextCampaign(null, null), clientInfo);
    }

    public CampaignInfo createActiveTextCampaignWithCalculatedOrderId(ClientInfo clientInfo) {
        return createActiveTextCampaignWithCalculatedOrderId(activeTextCampaign(null, null), clientInfo);
    }

    public CampaignInfo createActiveTextCampaignWithCalculatedOrderId(Campaign campaign, ClientInfo clientInfo) {
        var result = createCampaign(campaign, clientInfo);
        var orderId = result.getCampaignId() + BsOrderIdCalculator.ORDER_ID_OFFSET;
        setOrderId(result.getShard(), result.getCampaignId(), orderId);
        result.getCampaign().withOrderId(orderId);
        return result;
    }

    public CampaignInfo createActiveTextCampaignAutoStrategy(ClientInfo clientInfo) {
        return createCampaign(
                activeTextCampaign(null, null)
                        .withStrategy(averageBidStrategy()),
                clientInfo
        );
    }

    public CampaignInfo createActiveSmartCampaign() {
        return createCampaign(activePerformanceCampaign(null, null));
    }

    public CampaignInfo createActiveSmartCampaign(ClientInfo clientInfo) {
        return createCampaign(activePerformanceCampaign(null, null), clientInfo);
    }

    public CampaignInfo createActiveDynamicCampaignAutoStrategy(ClientInfo clientInfo) {
        return createCampaign(
                activeDynamicCampaign(null, null)
                        .withStrategy(averageBidStrategy()),
                clientInfo
        );
    }

    public CampaignInfo createActiveMobileAppCampaign() {
        return createCampaign(activeMobileAppCampaign(null, null));
    }

    public CampaignInfo createActiveMobileAppCampaign(ClientInfo clientInfo) {
        return createCampaign(activeMobileAppCampaign(null, null), clientInfo);
    }

    public CampaignInfo createActiveMobileAppCampaign(ClientInfo clientInfo, MobileAppInfo mobileAppInfo) {
        CampaignInfo campaignInfo = createCampaign(activeMobileAppCampaign(null, null), clientInfo);
        campaignRepository.setMobileAppIds(clientInfo.getShard(),
                singletonMap(campaignInfo.getCampaignId(), mobileAppInfo.getMobileAppId()));
        return campaignInfo;
    }

    public CampaignInfo createActiveUacMobileAppCampaign(ClientInfo clientInfo, MobileAppInfo mobileAppInfo) {
        CampaignInfo campaignInfo = createCampaign(activeUacMobileAppCampaign(null, null), clientInfo);
        campaignRepository.setMobileAppIds(clientInfo.getShard(),
                singletonMap(campaignInfo.getCampaignId(), mobileAppInfo.getMobileAppId()));
        return campaignInfo;
    }

    public CampaignInfo createActiveDynamicCampaign() {
        return createCampaign(activeDynamicCampaign(null, null));
    }

    public CampaignInfo createActiveDynamicCampaign(ClientInfo clientInfo) {
        return createCampaign(activeDynamicCampaign(null, null), clientInfo);
    }

    public CampaignInfo createActivePerformanceCampaign() {
        return createCampaign(activePerformanceCampaign(null, null));
    }

    public CampaignInfo createActivePerformanceCampaign(ClientInfo clientInfo) {
        return createCampaign(activePerformanceCampaign(null, null), clientInfo);
    }

    public CampaignInfo createActivePerformanceCampaignWithStrategy(ClientInfo clientInfo) {
        return createCampaign(activePerformanceCampaignWithStrategy(null, null), clientInfo);
    }

    public CampaignInfo createDraftPerformanceCampaign(ClientInfo clientInfo) {
        Campaign campaign = newSmartCampaign(null, null)
                .withStatusModerate(StatusModerate.NEW)
                .withStrategy(averageCpaPerCampStrategy());
        return createCampaign(campaign, clientInfo);
    }

    public CampaignInfo createPerformanceCampaignWithStrategy(ClientInfo clientInfo) {
        return createCampaign(activePerformanceCampaignWithStrategy(null, null), clientInfo);
    }

    public CampaignInfo createActiveCpmBannerCampaign() {
        return createCampaign(activeCpmBannerCampaign(null, null));
    }

    public CampaignInfo createActiveCpmBannerCampaign(ClientInfo clientInfo) {
        return createCampaign(activeCpmBannerCampaign(clientInfo.getClientId(), clientInfo.getUid()), clientInfo);
    }

    public CampaignInfo createActiveCpmBannerCampaignWithBrandLift(ClientInfo clientInfo, String brandSurveyId,
                                                                   LocalDate startTime, LocalDate finishTime) {
        var strategy = autobudgetMaxImpressionsCustomPeriodStrategy()
                .withStartDate(startTime)
                .withFinishDate(finishTime);
        Campaign camp = activeCpmBannerCampaign(clientInfo.getClientId(), clientInfo.getUid())
                .withStartTime(startTime)
                .withFinishTime(finishTime)
                .withStrategy(strategy);
        CampaignInfo campaign = createCampaign(camp, clientInfo);
        testCampaignRepository.updateBrandSurveyIdForCampaignId(clientInfo.getShard(), campaign.getCampaignId(),
                brandSurveyId);
        return campaign;
    }

    public CampaignInfo createActiveCpmBannerCampaignWithBrandLift(ClientInfo clientInfo, BrandSurvey brandSurvey) {
        Campaign camp = activeCpmBannerCampaign(clientInfo.getClientId(), clientInfo.getUid())
                .withStrategy(autobudgetMaxImpressionsCustomPeriodStrategy());
        CampaignInfo campaign = createCampaign(camp, clientInfo);
        testCampaignRepository.updateBrandSurveyIdForCampaignId(clientInfo.getShard(), campaign.getCampaignId(),
                brandSurvey.getBrandSurveyId());
        brandSurveyRepository.addBrandSurvey(clientInfo.getShard(), brandSurvey);
        return campaign;
    }

    public CampaignInfo createActiveCpmBannerCampaignWithBrandLift(ClientInfo clientInfo, String brandSurveyId) {
        CampaignInfo campaign = createCampaign(activeCpmBannerCampaign(clientInfo.getClientId(),
                clientInfo.getUid()), clientInfo);
        testCampaignRepository.updateBrandSurveyIdForCampaignId(clientInfo.getShard(), campaign.getCampaignId(),
                brandSurveyId);
        return campaign;
    }

    public CampaignInfo createActiveCpmDealsCampaign(ClientInfo clientInfo) {
        return createCampaign(activeCpmDealsCampaign(clientInfo.getClientId(), clientInfo.getUid()), clientInfo);
    }

    public CampaignInfo createActiveCpmYndxFrontpageCampaign() {
        return createCampaign(activeCpmYndxFrontpageCampaign(null, null));
    }

    public CampaignInfo createActiveCpmYndxFrontpageCampaign(ClientInfo clientInfo) {
        return createCampaign(activeCpmYndxFrontpageCampaign(clientInfo.getClientId(), clientInfo.getUid()),
                clientInfo);
    }

    /**
     * @deprecated значимый pricePackageId отсутствует в модели
     */
    @Deprecated
    public CampaignInfo createActiveCpmPriceCampaignOld(ClientInfo clientInfo) {
        return createCampaign(activeCpmPriceCampaign(clientInfo.getClientId(), clientInfo.getUid()),
                clientInfo);
    }

    public CpmPriceCampaign createActiveCpmPriceCampaign(ClientInfo clientInfo) {
        var pricePackage = pricePackageSteps.createApprovedPricePackageWithClients(clientInfo).getPricePackage();
        return createActiveCpmPriceCampaign(clientInfo, pricePackage);
    }

    public CpmPriceCampaign createActiveCpmPriceCampaign(ClientInfo clientInfo, PricePackage pricePackage) {
        var cpmPriceCampaign = TestCampaigns.defaultCpmPriceCampaignWithSystemFields(clientInfo, pricePackage);
        return createActiveCpmPriceCampaign(clientInfo, cpmPriceCampaign);
    }

    public CpmPriceCampaign createActiveCpmPriceCampaign(ClientInfo clientInfo, CpmPriceCampaign cpmPriceCampaign) {
        var addCampaignParametersContainer = RestrictedCampaignsAddOperationContainer.create(
                clientInfo.getShard(), clientInfo.getUid(), clientInfo.getClientId(), clientInfo.getUid(),
                clientInfo.getUid());
        campaignModifyRepository.addCampaigns(dslContextProvider.ppc(clientInfo.getShard()),
                addCampaignParametersContainer, List.of(cpmPriceCampaign));
        return cpmPriceCampaign;
    }

    public TextCampaign createActiveTextCampaign(ClientInfo clientInfo, TextCampaign textCampaign) {
        var addCampaignParametersContainer = RestrictedCampaignsAddOperationContainer.create(
                clientInfo.getShard(), clientInfo.getUid(), clientInfo.getClientId(), clientInfo.getUid(),
                clientInfo.getUid());
        campaignModifyRepository.addCampaigns(dslContextProvider.ppc(clientInfo.getShard()),
                addCampaignParametersContainer, List.of(textCampaign));
        return textCampaign;
    }

    @Deprecated //use steps.contentPromotionCampaignSteps()
    public CampaignInfo createActiveContentPromotionCampaign(ClientInfo clientInfo) {
        return createCampaign(activeContentPromotionCampaign(clientInfo.getClientId(), clientInfo.getUid()),
                clientInfo);
    }

    public CampaignInfo createActiveInternalFreeCampaign() {
        return createInternalCampaign(activeInternalFreeCampaign(null, null));
    }

    public CampaignInfo createActiveInternalFreeCampaign(ClientInfo clientInfo) {
        return createInternalCampaign(activeInternalFreeCampaign(null, null), clientInfo);
    }

    public CampaignInfo createActiveInternalDistribCampaignWithModeratedPlace(ClientInfo clientInfo) {
        return createInternalCampaign(activeInternalDistribCampaign(null, null), clientInfo,
                TestCampaigns.MODERATED_PLACE_ID_FOR_INTERNAL_CAMPAIGNS);
    }

    public CampaignInfo createActiveInternalDistribCampaign() {
        return createInternalCampaign(activeInternalDistribCampaign(null, null));
    }

    public CampaignInfo createActiveInternalDistribCampaign(ClientInfo clientInfo) {
        return createInternalCampaign(activeInternalDistribCampaign(null, null), clientInfo);
    }

    public CampaignInfo createActiveInternalAutobudgetCampaign() {
        return createInternalCampaign(activeInternalAutobudgetCampaign(null, null));
    }

    public CampaignInfo createActiveInternalAutobudgetCampaign(ClientInfo clientInfo) {
        return createInternalCampaign(activeInternalAutobudgetCampaign(null, null), clientInfo);
    }

    public CampaignInfo createInternalCampaign(Campaign campaign) {
        return createInternalCampaign(campaign, null);
    }

    public CampaignInfo createInternalCampaign(Campaign campaign, @Nullable ClientInfo clientInfo) {
        checkArgument(campaign.getType() == CampaignType.INTERNAL_FREE
                || campaign.getType() == CampaignType.INTERNAL_AUTOBUDGET
                || campaign.getType() == CampaignType.INTERNAL_DISTRIB);
        return createInternalCampaign(campaign, clientInfo, TestCampaigns.DEFAULT_PLACE_ID_FOR_INTERNAL_CAMPAIGNS);
    }

    public CampaignInfo createInternalCampaign(Campaign campaign, @Nullable ClientInfo clientInfo,
                                               Long placeId) {
        if (clientInfo == null) {
            clientInfo = internalAdProductSteps.createDefaultInternalAdProduct();
        }
        CampaignInfo campaignInfo = createCampaign(campaign, clientInfo);
        testCampaignRepository.setServiceAndPlaceToInternalCampaign(
                campaignInfo.getShard(), campaignInfo.getCampaignId(), placeId);
        return campaignInfo;
    }

    public CampaignInfo createActiveMcBannerCampaign() {
        return createCampaign(activeMcBannerCampaign(null, null));
    }

    public CampaignInfo createActiveMcBannerCampaign(ClientInfo clientInfo) {
        return createCampaign(activeMcBannerCampaign(clientInfo.getClientId(), clientInfo.getUid()),
                clientInfo);
    }

    public CampaignInfo createActiveMcBannerCampaignAutoStrategy(ClientInfo clientInfo) {
        return createCampaign(
                activeMcBannerCampaign(null, null)
                        .withStrategy(averageBidStrategy()),
                clientInfo
        );
    }

    public CampaignInfo createDefaultCampaignByCampaignType(CampaignType campaignType) {
        return createCampaign(newCampaignByCampaignTypeOld(campaignType, null, null));
    }

    public CampaignInfo createDefaultCampaign() {
        return createCampaign((Campaign) null);
    }

    public CampaignInfo createCampaign(Campaign campaign) {
        return createCampaign(new CampaignInfo().withCampaign(campaign));
    }

    public CampaignInfo createActiveCampaign(ClientInfo clientInfo) {
        return createCampaign(new TextCampaignInfo()
                .withTypedCampaign(TestTextCampaigns.fullTextCampaign())
                .withCampaign(activeTextCampaign(null, null))
                .withClientInfo(clientInfo));
    }

    public CampaignInfo createActiveCampaign(ClientInfo clientInfo, Long orderId) {
        return createCampaign(new TextCampaignInfo()
                .withTypedCampaign(TestTextCampaigns.fullTextCampaign())
                .withCampaign(activeTextCampaign(null, null).withOrderId(orderId))
                .withClientInfo(clientInfo));
    }

    public CampaignInfo createActiveCampaign(ClientInfo clientInfo, CampaignsPlatform platform) {
        Campaign campaign = activeTextCampaign(null, null);
        campaign.getStrategy().cast(ManualStrategy.class).withPlatform(platform);
        var typedCampaign = TestTextCampaigns.fullTextCampaign();
        typedCampaign.getStrategy().withPlatform(platform);
        return createCampaign(new TextCampaignInfo()
                .withTypedCampaign(typedCampaign)
                .withCampaign(campaign)
                .withClientInfo(clientInfo));
    }

    public CampaignInfo createSubCampaign(ClientInfo clientInfo, Long masterCid) {
        return createCampaign(newTextCampaign(null, null).withMasterCid(masterCid), clientInfo);
    }

    public CampaignInfo createActiveSubCampaign(ClientInfo clientInfo, Long masterCid) {
        return createCampaign(activeTextCampaign(null, null).withMasterCid(masterCid), clientInfo);
    }

    public CampaignInfo createCampaignUnderWalletByCampaignType(CampaignType campaignType,
                                                                ClientInfo clientInfo,
                                                                Long walletCid,
                                                                BigDecimal chipsCost) {
        return createCampaignUnderWallet(clientInfo, chipsCost,
                activeCampaignByCampaignType(campaignType, null, null)
                        .withBalanceInfo(activeBalanceInfo(CurrencyCode.RUB)
                                .withWalletCid(walletCid)));
    }

    private CampaignInfo createCampaignUnderWallet(ClientInfo clientInfo,
                                                   BigDecimal chipsCost,
                                                   Campaign campaignUnderWallet) {
        CampaignInfo campaignInfo = createCampaign(campaignUnderWallet, clientInfo);

        campaignsMulticurrencySumsRepository.insertCampaignsMulticurrencySums(clientInfo.getShard(),
                defaultMulticurrencySums(campaignInfo.getCampaignId()).withChipsCost(chipsCost));

        return campaignInfo;
    }

    public CampaignInfo createCampaignUnderWallet(ClientInfo clientInfo, Long walletCid, BigDecimal chipsCost) {
        Campaign campaignUnderWallet =
                activeTextCampaign(null, null)
                        .withBalanceInfo(activeBalanceInfo(CurrencyCode.RUB).withWalletCid(walletCid));
        return createCampaignUnderWallet(clientInfo, chipsCost, campaignUnderWallet);
    }

    public CampaignInfo createCampaignUnderWallet(ClientInfo clientInfo, BalanceInfo balanceInfo) {
        Campaign campaignUnderWallet =
                activeTextCampaign(null, null)
                        .withBalanceInfo(balanceInfo);

        return createCampaign(campaignUnderWallet, clientInfo);
    }

    public CampaignInfo createActiveCampaignUnderWallet(ClientInfo clientInfo) {
        CampaignInfo wallet = createWalletCampaign(clientInfo);

        BalanceInfo campBalanceInfo = activeBalanceInfo(CurrencyCode.RUB).withWalletCid(wallet.getCampaignId());
        CampaignInfo textCampaignInfo = new TextCampaignInfo()
                .withTypedCampaign(TestTextCampaigns.fullTextCampaign().withWalletId(wallet.getCampaignId()))
                .withCampaign(activeTextCampaign(wallet.getClientId(), wallet.getUid())
                        .withBalanceInfo(campBalanceInfo))
                .withClientInfo(clientInfo);
        return createCampaign(textCampaignInfo);
    }

    public CampaignInfo createWalletCampaign(ClientInfo clientInfo) {
        CampaignInfo walletCampaignInfo = new CampaignInfo()
                .withCampaign(activeWalletCampaign(clientInfo.getClientId(), clientInfo.getUid()))
                .withClientInfo(clientInfo);
        return createCampaign(walletCampaignInfo);
    }


    public CampaignInfo createBillingAggregate(ClientInfo clientInfo, BalanceInfo balanceInfo) {
        return createCampaign(
                newBillingAggregate(clientInfo.getClientId(), clientInfo.getUid())
                        .withWalletId(balanceInfo.getWalletCid())
                        .withBalanceInfo(balanceInfo),
                clientInfo
        );
    }

    public CampaignInfo createCampaign(Campaign campaign, ClientInfo clientInfo) {
        return createCampaign(new CampaignInfo()
                .withClientInfo(clientInfo)
                .withCampaign(campaign));
    }

    public <T extends CampaignInfo> T createCampaign(T campaignInfo) {
        return createCampaign(campaignInfo, false);
    }

    public <T extends CampaignInfo> T createCampaign(T campaignInfo, boolean createDefaultInternalProduct) {
        if (campaignInfo.getCampaign() == null) {
            campaignInfo.withCampaign(newTextCampaign(null, null));
        }
        if (campaignInfo.getCampaignId() == null) {
            var client = clientSteps.createClient(campaignInfo.getClientInfo());
            if (createDefaultInternalProduct) {
                internalAdProductSteps.createDefaultInternalAdProduct(client, emptySet(), "");
            }
            campaignInfo.getCampaign()
                    .withClientId(campaignInfo.getClientId().asLong())
                    .withUid(campaignInfo.getUid());
            CommonUtils.ifNotNull(client.getClient().getAgencyClientId(), campaignInfo.getCampaign()::withAgencyId);
            campaignRepository0.addCampaigns(campaignInfo.getShard(), campaignInfo.getClientId(),
                    singletonList(campaignInfo.getCampaign())
            );
        }
        return campaignInfo;
    }

    public CampaignInfo createActiveCampaignWithAvgCpaStrategy(ClientInfo clientInfo) {
        return createCampaign(new TextCampaignInfo()
                .withTypedCampaign(TestTextCampaigns.fullTextCampaign())
                .withCampaign(
                        activeTextCampaign(null, null)
                                .withStrategy(averageCpaStrategy()))
                .withClientInfo(clientInfo));
    }

    /**
     * Вызывается в тесте удаления представителя клиента.
     * Добавляет в таблицы только поля нужные для теста.
     * Когда в java-коде появится полноценная поддержка работы с кошельками и степы для них
     * нужно поменять вызов этого метода на стандарные (которых сейчас нет).
     * Для других тестов использовать не рекомендуется.
     *
     * @param walletInfo
     * @param payer
     * @param isEnabled
     */
    @Deprecated
    public void addFakeAutoPay(CampaignInfo walletInfo, User payer, boolean isEnabled) {
        int shard = walletInfo.getShard();
        String fakePaymentMethodId = "card-x2181";

        var mode = isEnabled ? WalletCampaignsAutopayMode.min_balance : WalletCampaignsAutopayMode.none;
        var walletCid = walletInfo.getCampaignId();
        dslContextProvider.ppc(shard)
                .insertInto(WALLET_CAMPAIGNS)
                .set(WALLET_CAMPAIGNS.WALLET_CID, walletCid)
                .set(WALLET_CAMPAIGNS.AUTOPAY_MODE, mode)
                .onDuplicateKeyUpdate()
                .set(WALLET_CAMPAIGNS.AUTOPAY_MODE, mode)
                .execute();

        dslContextProvider.ppc(shard).insertInto(AUTOPAY_SETTINGS)
                .set(AUTOPAY_SETTINGS.PAYER_UID, payer.getId())
                .set(AUTOPAY_SETTINGS.WALLET_CID, walletCid)
                .set(AUTOPAY_SETTINGS.PAYMETHOD_ID, fakePaymentMethodId)
                .set(AUTOPAY_SETTINGS.PAYMENT_SUM, BigDecimal.TEN)
                .set(AUTOPAY_SETTINGS.REMAINING_SUM, BigDecimal.TEN)
                .onDuplicateKeyUpdate()
                .set(AUTOPAY_SETTINGS.PAYER_UID, payer.getId())
                .execute();
    }

    /**
     * Выполнить тест в рамках одной транзакции, в началае которой очищаются таблицы с кампанией.
     * После выполнения теста все изменения откатываются
     *
     * @param shard шард
     * @param test  тест, получающий аргументов транзакционный dsl-контекст
     */
    public void runWithEmptyCampaignsTables(int shard, Consumer<DSLContext> test) {
        try {
            campaignRepository0.runWithEmptyCampaignsTables(shard, test);
        } catch (RollbackException ignored) {
        }
    }

    public void createCampSecondaryOptions(int shard, Long cid, String key, String options) {
        testCampaignRepository.addCampSecondaryOptions(shard, cid, key, options);
    }

    public void setStrategy(CampaignInfo campaignInfo, StrategyName strategyName) {
        dslContextProvider.ppc(campaignInfo.getShard())
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.STRATEGY_NAME, StrategyName.toSource(strategyName))
                .where(CAMPAIGNS.CID.eq(campaignInfo.getCampaignId()))
                .execute();
    }

    public void setPlatform(CampaignInfo campaignInfo, CampaignsPlatform platform) {
        dslContextProvider.ppc(campaignInfo.getShard())
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.PLATFORM, CampaignsPlatform.toSource(platform))
                .where(CAMPAIGNS.CID.eq(campaignInfo.getCampaignId()))
                .execute();
    }

    public void setFavorite(CampaignInfo campaignInfo, long uid) {
        dslContextProvider.ppc(campaignInfo.getShard())
                .insertInto(USER_CAMPAIGNS_FAVORITE)
                .values(uid, campaignInfo.getCampaignId())
                .execute();
    }

    //used in direct/autotests
    public void setManager(CampaignInfo campaignInfo, Long managerUid) {
        dslContextProvider.ppc(campaignInfo.getShard())
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.MANAGER_UID, managerUid)
                .where(CAMPAIGNS.CID.eq(campaignInfo.getCampaignId()))
                .execute();
    }

    //used in direct/autotests
    public void setAgency(CampaignInfo campaignInfo, ClientId agencyClientId, Long agencyUid) {
        dslContextProvider.ppc(campaignInfo.getShard())
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.AGENCY_ID, agencyClientId.asLong())
                .set(CAMPAIGNS.AGENCY_UID, agencyUid)
                .where(CAMPAIGNS.CID.eq(campaignInfo.getCampaignId()))
                .execute();
    }

    public <V> void setCampaignProperty(CampaignInfo campaignInfo,
                                        ModelProperty<? super ru.yandex.direct.core.entity.campaign.model.Campaign,
                                                V> property,
                                        V value) {
        int shard = campaignInfo.getShard();
        var campaign = campaignRepository.getCampaigns(shard, singleton(campaignInfo.getCampaignId())).get(0);
        if (!ru.yandex.direct.core.entity.campaign.model.Campaign.allModelProperties().contains(property)) {
            throw new IllegalArgumentException(
                    "Model " + campaign.getName() + " doesn't contain property " + property.name());
        }
        AppliedChanges<ru.yandex.direct.core.entity.campaign.model.Campaign> appliedChanges =
                new ModelChanges<>(campaign.getId(), ru.yandex.direct.core.entity.campaign.model.Campaign.class)
                        .process(value, property)
                        .applyTo(campaign);
        campaignRepository.updateCampaigns(shard, singletonList(appliedChanges));
    }

    public void setAllowedFrontpageTypes(CampaignInfo campaignInfo,
                                         Collection<FrontpageCampaignShowType> allowedTypes) {
        testCpmYndxFrontpageRepository.setCpmYndxFrontpageCampaignsAllowedFrontpageTypes(
                campaignInfo.getShard(),
                campaignInfo.getCampaignId(),
                allowedTypes);
    }

    public void deleteCampaign(int shard, Long campaignId) {
        testCampaignRepository.makeCampaignReadyToDelete(shard, campaignId);
    }

    public void setOrderId(int shard, Long campaignId, Long orderId) {
        testCampaignRepository.updateCampaignOrderIdByCid(shard, campaignId, orderId);
    }

    public void setStatusModerate(int shard, Long campaignId, CampaignStatusModerate campaignStatusModerate) {
        testCampaignRepository.updateStatusModerate(shard, List.of(campaignId), campaignStatusModerate);
    }

    public void setStatusBsSynced(int shard, Long campaignId, CampaignStatusBsSynced campaignStatusBsSynced) {
        testCampaignRepository.updateStatusBsSynced(shard, List.of(campaignId), campaignStatusBsSynced);
    }

    public void archiveCampaign(CampaignInfo campaignInfo) {
        archiveCampaign(campaignInfo.getShard(), campaignInfo.getCampaignId());
    }

    public void archiveCampaign(int shard, Long campaignId) {
        testCampaignRepository.archiveCampaign(shard, campaignId);
    }

    public void unarchiveCampaign(int shard, Long campaignId) {
        testCampaignRepository.unarchiveCampaign(shard, campaignId);
    }

    public void makeNewCampaignReadyForSendingToBS(int shard, Long campaignId) {
        testCampaignRepository.makeNewCampaignReadyForSendingToBS(shard, campaignId);
    }

    public void makeCampaignFullyModerated(int shard, Long campaignId) {
        testCampaignRepository.makeCampaignFullyModerated(shard, campaignId);
    }

    public void makeCampaignReadyForDelete(int shard, Long campaignId) {
        testCampaignRepository.makeCampaignReadyForDelete(shard, campaignId);
    }

    public void makeCampaignReadyForDeleteInGrut(Long campaignId) {
        grutTransactionProvider.runInTransaction(null,
                () -> {
                    var campaignBuilder = Schema.TCampaign.newBuilder();
                    campaignBuilder.setMeta(Schema.TCampaignMeta.newBuilder().setId(campaignId).build());
                    campaignBuilder.setSpec(ru.yandex.grut.objects.proto.Campaign.TCampaignSpec.newBuilder().build());
                    grutApiService.getBriefGrutApi().updateBrief(
                            campaignBuilder.build(), List.of(), List.of("/spec/start_time"));
                    return null;
                }
        );
    }

    public void makeCampaignActive(int shard, Long campaignId) {
        testCampaignRepository.makeCampaignActive(shard, campaignId);
    }

    public void campaignsSuspend(int shard, Long campaignId) {
        testCampaignRepository.makeCampaignStopped(shard, campaignId);
    }

    public void campaignsSuspend(int shard, Long campaignId, LocalDateTime stopTime) {
        testCampaignRepository.makeCampaignStopped(shard, campaignId, stopTime);
    }

    public void setLastChange(int shard, Long campaignId, LocalDateTime lastChange) {
        testCampaignRepository.updateLastChange(shard, campaignId, lastChange);
    }

    public void setCreateTime(int shard, Long campaignId, LocalDateTime createTime) {
        testCampaignRepository.updateCreateTime(shard, campaignId, createTime);
    }

    public void setCurrency(int shard, Long campaignId, CurrencyCode currencyCode) {
        testCampaignRepository.setCurrency(shard, campaignId, currencyCode);
    }

    public void setDayBudget(CampaignInfo campaignInfo, BigDecimal sum,
                             @Nullable CampaignsDayBudgetShowMode dayBudgetShowMode, @Nullable Integer changesCount) {
        testCampaignRepository.setDayBudget(campaignInfo.getShard(), campaignInfo.getCampaignId(), sum,
                dayBudgetShowMode, changesCount);
    }

    public void setStrategyData(CampaignInfo campaignInfo, StrategyData strategyData) {
        testCampaignRepository.setStrategyData(campaignInfo.getShard(), campaignInfo.getCampaignId(), strategyData);
    }
}
