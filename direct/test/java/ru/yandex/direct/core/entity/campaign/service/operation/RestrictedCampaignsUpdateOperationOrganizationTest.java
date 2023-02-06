package ru.yandex.direct.core.entity.campaign.service.operation;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithOrganization;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignAdditionalActionsService;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientFactory;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsUpdateOperation;
import ru.yandex.direct.core.entity.campaign.service.type.update.CampaignUpdateOperationSupportFacade;
import ru.yandex.direct.core.entity.campaign.service.validation.UpdateRestrictedCampaignValidationService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.organizations.service.OrganizationService;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.UidClientIdShard;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(Parameterized.class)
public class RestrictedCampaignsUpdateOperationOrganizationTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private StrategyTypedRepository strategyTypedRepository;
    @Autowired
    private CampaignModifyRepository campaignModifyRepository;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private UpdateRestrictedCampaignValidationService updateRestrictedCampaignValidationService;
    @Autowired
    private CampaignUpdateOperationSupportFacade campaignUpdateOperationSupportFacade;
    @Autowired
    private CampaignAdditionalActionsService campaignAdditionalActionsService;
    @Autowired
    private DslContextProvider ppcDslContextProvider;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private OrganizationsClientStub organizationsClient;
    @Autowired
    private RbacService rbacService;
    @Autowired
    private Steps steps;
    @Autowired
    public MetrikaClientStub metrikaClient;
    @Autowired
    private RequestBasedMetrikaClientFactory metrikaClientFactory;
    @Autowired
    private FeatureService featureService;

    @Parameterized.Parameter
    public BiFunction<ClientId, Long, Campaign> campaignSupplier;

    private CampaignInfo campaignInfo;
    private CampaignInfo campaignInfoWithPermalink;

    private Long initialPermalink = 1010L;
    private Long validPermalink = 2020L;
    private Long invalidPermalink = 3030L;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return StreamEx.<BiFunction<ClientId, Long, Campaign>>of(
                TestCampaigns::activeTextCampaign,
                TestCampaigns::activeDynamicCampaign
        ).map(List::of).map(List::toArray).toList();
    }

    @Before
    public void before() {
        campaignInfo = steps.campaignSteps().createCampaign(
                campaignSupplier.apply(null, null)
                        .withStartTime(LocalDate.now())
                        .withEmail("example@example.com"));
        Long chiefUid = rbacService.getChiefByClientId(campaignInfo.getClientId());
        organizationsClient.addUidsByPermalinkId(validPermalink, List.of(chiefUid));
        Mockito.reset(organizationsClient);

        campaignInfoWithPermalink = steps.campaignSteps().createCampaign(
                campaignSupplier.apply(null, null)
                        .withStartTime(LocalDate.now())
                        .withEmail("example@example.com")
                        .withDefaultPermalink(initialPermalink));
        rbacService.getChiefByClientId(campaignInfoWithPermalink.getClientId());
    }

    @Test
    public void apply_CampaignWithoutOrganization_OrganizationAdded_Success() {
        ModelChanges<CampaignWithOrganization> modelChanges = ModelChanges.build(
                campaignInfo.getCampaignId(), CampaignWithOrganization.class,
                CampaignWithOrganization.DEFAULT_PERMALINK_ID, validPermalink);

        MassResult<Long> result = apply(campaignInfo, modelChanges);
        assertThat(result).matches(isFullySuccessful()::matches);

        CampaignWithOrganization actualCampaign = getActualCampaign(campaignInfo);

        assertThat(actualCampaign.getDefaultPermalinkId())
                .isEqualTo(validPermalink);
        verify(organizationsClient, times(1)).getMultipleOrganizationsInfo(
                anyCollection(), anyCollection(), anyString(), anyString());
    }

    @Test
    public void apply_CampaignWithoutOrganization_OrganizationAdded_InvalidPermalink_Fail() {
        ModelChanges<CampaignWithOrganization> modelChanges = ModelChanges.build(
                campaignInfo.getCampaignId(), CampaignWithOrganization.class,
                CampaignWithOrganization.DEFAULT_PERMALINK_ID, invalidPermalink);

        MassResult<Long> result = apply(campaignInfo, modelChanges);
        assertThat(result).matches(isSuccessful(false)::matches);

        CampaignWithOrganization actualCampaign = getActualCampaign(campaignInfo);

        assertThat(actualCampaign.getDefaultPermalinkId())
                .isNull();
        verify(organizationsClient, times(1)).getMultipleOrganizationsInfo(
                anyCollection(), anyCollection(), anyString(), anyString());
    }

    @Test
    public void apply_CampaignWithOrganization_OrganizationChanged_Success() {
        ModelChanges<CampaignWithOrganization> modelChanges = ModelChanges.build(
                campaignInfoWithPermalink.getCampaignId(), CampaignWithOrganization.class,
                CampaignWithOrganization.DEFAULT_PERMALINK_ID, validPermalink);

        MassResult<Long> result = apply(campaignInfoWithPermalink, modelChanges);
        assertThat(result).matches(isFullySuccessful()::matches);

        CampaignWithOrganization actualCampaign = getActualCampaign(campaignInfoWithPermalink);

        assertThat(actualCampaign.getDefaultPermalinkId())
                .isEqualTo(validPermalink);
        verify(organizationsClient, times(1)).getMultipleOrganizationsInfo(
                anyCollection(), anyCollection(), anyString(), anyString());
    }

    @Test
    public void apply_CampaignWithOrganization_OrganizationChanged_InvalidPermalink_Fail() {
        ModelChanges<CampaignWithOrganization> modelChanges = ModelChanges.build(
                campaignInfoWithPermalink.getCampaignId(), CampaignWithOrganization.class,
                CampaignWithOrganization.DEFAULT_PERMALINK_ID, invalidPermalink);

        MassResult<Long> result = apply(campaignInfoWithPermalink, modelChanges);
        assertThat(result).matches(isSuccessful(false)::matches);

        CampaignWithOrganization actualCampaign = getActualCampaign(campaignInfoWithPermalink);

        assertThat(actualCampaign.getDefaultPermalinkId())
                .isEqualTo(initialPermalink);
        verify(organizationsClient, times(1)).getMultipleOrganizationsInfo(
                anyCollection(), anyCollection(), anyString(), anyString());
    }

    /**
     * Если у клиента больше нет прав на уже привязанный пермалинк, но пермалинк не изменяется, то изменения должны
     * проходить успешно (не изменяющийся пермалинк не проверяем).
     */
    @Test
    public void apply_CampaignWithOrganization_SameOrganization_NoOrganizationRights_Success() {
        ModelChanges<CampaignWithOrganization> modelChanges = ModelChanges.build(
                campaignInfoWithPermalink.getCampaignId(), CampaignWithOrganization.class,
                CampaignWithOrganization.DEFAULT_PERMALINK_ID, initialPermalink);

        MassResult<Long> result = apply(campaignInfoWithPermalink, modelChanges);
        assertThat(result).matches(isFullySuccessful()::matches);

        CampaignWithOrganization actualCampaign = getActualCampaign(campaignInfoWithPermalink);

        assertThat(actualCampaign.getDefaultPermalinkId())
                .isEqualTo(initialPermalink);
        verify(organizationsClient, never()).getMultipleOrganizationsInfo(
                anyCollection(), anyCollection(), anyString(), anyString());
    }

    @Test
    public void apply_CampaignWithOrganization_OrganizationDeleted_Success() {
        ModelChanges<CampaignWithOrganization> modelChanges = ModelChanges.build(
                campaignInfoWithPermalink.getCampaignId(), CampaignWithOrganization.class,
                CampaignWithOrganization.DEFAULT_PERMALINK_ID, null);

        MassResult<Long> result = apply(campaignInfoWithPermalink, modelChanges);
        assertThat(result).matches(isFullySuccessful()::matches);

        CampaignWithOrganization actualCampaign = getActualCampaign(campaignInfoWithPermalink);

        assertThat(actualCampaign.getDefaultPermalinkId())
                .isNull();
        verify(organizationsClient, never()).getMultipleOrganizationsInfo(
                anyCollection(), anyCollection(), anyString(), anyString());
    }

    private MassResult<Long> apply(CampaignInfo campaignInfo, ModelChanges<? extends BaseCampaign> modelChanges) {
        var options = new CampaignOptions();
        RestrictedCampaignsUpdateOperation restrictedCampaignsUpdateOperation = new RestrictedCampaignsUpdateOperation(
                Collections.singletonList(modelChanges),
                campaignInfo.getUid(),
                UidClientIdShard.of(campaignInfo.getUid(), campaignInfo.getClientId(), campaignInfo.getShard()),
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

    private CampaignWithOrganization getActualCampaign(CampaignInfo campaignInfo) {
        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(campaignInfo.getShard(),
                        Collections.singletonList(campaignInfo.getCampaignId()));
        List<CampaignWithOrganization> campaignsWithOrganization = mapList(typedCampaigns,
                campaign -> (CampaignWithOrganization) campaign);
        return campaignsWithOrganization.get(0);
    }
}
