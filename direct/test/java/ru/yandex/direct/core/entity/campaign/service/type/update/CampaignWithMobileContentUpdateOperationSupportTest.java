package ru.yandex.direct.core.entity.campaign.service.type.update;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import jdk.jfr.Description;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.aggregatedstatuses.repository.AggregatedStatusesRepository;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithMobileContent;
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignAdditionalActionsService;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientFactory;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsUpdateOperation;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.entity.campaign.service.validation.UpdateRestrictedCampaignValidationService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppAlternativeStore;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MobileAppInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.UidClientIdShard;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes.RECALC_BRAND_LIFT_CAMPAIGNS;
import static ru.yandex.direct.core.entity.uac.UacTestDataKt.STORE_URL;
import static ru.yandex.direct.core.testing.data.TestBanners.activeMobileAppBanner;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultMobileContentCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignWithMobileContentUpdateOperationSupportTest {
    @Autowired
    private BannerTypedRepository bannerTypedRepository;

    @Autowired
    private RbacService rbacService;

    @Autowired
    private Steps steps;

    @Autowired
    private StrategyTypedRepository strategyTypedRepository;

    @Autowired
    private CampaignModifyRepository campaignModifyRepository;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    @Autowired
    private DslContextProvider ppcDslContextProvider;

    @Autowired
    private UpdateRestrictedCampaignValidationService updateRestrictedCampaignValidationService;

    @Autowired
    private CampaignUpdateOperationSupportFacade campaignUpdateOperationSupportFacade;

    @Autowired
    private CampaignAdditionalActionsService campaignAdditionalActionsService;

    @Autowired
    protected AggregatedStatusesRepository aggregatedStatusesRepository;

    @Autowired
    private RequestBasedMetrikaClientFactory metrikaClientFactory;

    @Autowired
    private FeatureService featureService;

    private ClientInfo defaultClient;
    private Long operatorUid;

    private final EnumSet<MobileAppAlternativeStore> alternativeAppStores = EnumSet.of(
            MobileAppAlternativeStore.XIAOMI_GET_APPS,
            MobileAppAlternativeStore.HUAWEI_APP_GALLERY
    );

    @Before
    public void before() {
        defaultClient = steps.clientSteps().createClient(defaultClient().withWorkCurrency(CurrencyCode.RUB));
        operatorUid = defaultClient.getUid();
        steps.dbQueueSteps().registerJobType(RECALC_BRAND_LIFT_CAMPAIGNS);
    }

    void createMobileContentCampaign(CampaignWithMobileContent mobileContentCampaign) {
        var addCampaignParametersContainer = RestrictedCampaignsAddOperationContainer.create(
                defaultClient.getShard(), operatorUid, defaultClient.getClientId(), defaultClient.getUid(),
                defaultClient.getUid());
        campaignModifyRepository.addCampaigns(ppcDslContextProvider.ppc(defaultClient.getShard()),
                addCampaignParametersContainer, List.of(mobileContentCampaign));
    }

    MassResult<Long> apply(ModelChanges<? extends BaseCampaign> modelChanges) {
        var defaultOptions = new CampaignOptions();
        return apply(modelChanges, defaultOptions);
    }

    MassResult<Long> apply(ModelChanges<? extends BaseCampaign> modelChanges, CampaignOptions options) {
        RestrictedCampaignsUpdateOperation restrictedCampaignsUpdateOperation = new RestrictedCampaignsUpdateOperation(
                Collections.singletonList(modelChanges),
                operatorUid,
                UidClientIdShard.of(defaultClient.getUid(), defaultClient.getClientId(), defaultClient.getShard()),
                campaignModifyRepository,
                campaignTypedRepository,
                strategyTypedRepository,
                updateRestrictedCampaignValidationService,
                campaignUpdateOperationSupportFacade,
                campaignAdditionalActionsService,
                ppcDslContextProvider, rbacService, metrikaClientFactory, featureService,
                Applicability.PARTIAL, options);
        return restrictedCampaignsUpdateOperation.apply();
    }

    MobileContentCampaign getCampaignFromRepository(Long campaignId) {
        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(defaultClient.getShard(),
                        Collections.singletonList(campaignId));
        return (MobileContentCampaign) typedCampaigns.get(0);
    }

    @Test
    @Description("Обновление альтернативных сторов кампании должно сбрасывать флаг statusBsSynced на всех баннерах " +
            "кампании")
    public void resetBannerStatusBsSyncedOnAltAppStoresUpdate() {
        MobileAppInfo mobileAppInfo = steps.mobileAppSteps().createMobileApp(defaultClient, STORE_URL);
        var mobileAppId = mobileAppInfo.getMobileAppId();
        var campaign = defaultMobileContentCampaignWithSystemFields(defaultClient)
                .withMobileAppId(mobileAppId)
                .withAlternativeAppStores(alternativeAppStores);

        createMobileContentCampaign(campaign);

        var campaignInfo = steps.mobileContentCampaignSteps()
                .createCampaign(defaultClient, campaign);
        var adGroup = steps.adGroupSteps().createActiveMobileContentAdGroup(campaignInfo);
        Long creativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultVideoAdditionCreative(defaultClient, creativeId);
        var mobileAppBanner = activeMobileAppBanner(adGroup.getCampaignId(), adGroup.getAdGroupId())
                .withStatusBsSynced(StatusBsSynced.YES);

        steps.bannerSteps().createActiveMobileAppBanner(mobileAppBanner, adGroup);

        ModelChanges<MobileContentCampaign> modelChanges =
                new ModelChanges<>(campaign.getId(), MobileContentCampaign.class);
        modelChanges.process(
                EnumSet.noneOf(MobileAppAlternativeStore.class), MobileContentCampaign.ALTERNATIVE_APP_STORES);
        MassResult<Long> result = apply(modelChanges);
        assumeThat(result, isFullySuccessful());

        MobileContentCampaign actualCampaign = getCampaignFromRepository(campaign.getId());
        assertThat(actualCampaign.getAlternativeAppStores()).isEmpty();

        //statusBsSynced на всех баннерах кампании сбросился
        MobileAppBanner banner = (MobileAppBanner) bannerTypedRepository.getTyped(defaultClient.getShard(),
                List.of(mobileAppBanner.getId())).get(0);
        assertThat(banner.getStatusBsSynced()).isEqualTo(StatusBsSynced.NO);
    }

}
