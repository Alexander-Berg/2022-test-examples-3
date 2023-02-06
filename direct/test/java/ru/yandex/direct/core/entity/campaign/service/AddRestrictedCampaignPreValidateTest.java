package ru.yandex.direct.core.entity.campaign.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignCounts;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.validation.AddRestrictedCampaignValidationService;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignLimitsValidationService;
import ru.yandex.direct.core.entity.campaign.service.validation.type.add.CampaignAddValidationTypeSupportFacade;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.entity.client.model.ClientLimits;
import ru.yandex.direct.core.entity.client.service.ClientLimitsService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.rbac.RbacSubrole;
import ru.yandex.direct.rbac.UserPerminfo;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.maxCampaignsForClientId;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(Parameterized.class)
public class AddRestrictedCampaignPreValidateTest {

    private static final Long UNARCHIVED_CAMPAIGNS_COUNT_LIMIT = 2L;
    private static final Long CAMPAIGNS_COUNT_LIMIT = 3L;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private ClientLimitsService clientLimitsService;
    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private RbacService rbacService;
    @Mock
    private CampaignAddValidationTypeSupportFacade typeSupportFacade;
    @Mock
    private FeatureService featureService;

    private AddRestrictedCampaignValidationService validationService;

    private ClientId clientId;
    private Integer shard;
    private Long operatorUid;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.PERFORMANCE},
                {CampaignType.MOBILE_CONTENT},
                {CampaignType.MCBANNER},
                {CampaignType.DYNAMIC}
        });
    }

    @Before
    public void before() {
        clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        shard = RandomNumberUtils.nextPositiveInteger();
        operatorUid = RandomNumberUtils.nextPositiveLong();

        var clientLimits = new ClientLimits()
                .withCampsCountLimit(CAMPAIGNS_COUNT_LIMIT)
                .withUnarcCampsCountLimit(UNARCHIVED_CAMPAIGNS_COUNT_LIMIT);
        doReturn(true)
                .when(rbacService).canWrite(eq(operatorUid), anyLong());

        doReturn(clientLimits)
                .when(clientLimitsService).getClientLimits(clientId);

        doReturn(new UserPerminfo(clientId, 0L, RbacRole.CLIENT, RbacSubrole.SUPERTEAMLEADER, clientId, Set.of(0L), 0L, null,
                null, null,
                null, true, Set.of(), null, null))
                .when(rbacService).getUserPermInfo(operatorUid);

        var campaignLimitsValidationService =
                new CampaignLimitsValidationService(clientLimitsService, campaignRepository);
        validationService = new AddRestrictedCampaignValidationService(featureService, rbacService,
                typeSupportFacade, campaignLimitsValidationService);
    }

    @Test
    public void preValidate_Successfully() {
        doReturn(new CampaignCounts(3, 2))
                .when(campaignRepository).getCampaignCountsForCountLimitCheck(shard, clientId.asLong());
        var vr = validationService
                .preValidate(CampaignValidationContainer.create(shard, operatorUid, clientId), operatorUid,
                        Collections.emptyList());
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void preValidate_hasUnarchivedCampaignsCountLimitDefect() {
        doReturn(new CampaignCounts(4, 3))
                .when(campaignRepository).getCampaignCountsForCountLimitCheck(shard, clientId.asLong());
        var vr = validationService
                .preValidate(CampaignValidationContainer.create(shard, operatorUid, clientId), operatorUid,
                        Collections.emptyList());
        assertThat(vr, hasDefectWithDefinition(validationError(path(),
                maxCampaignsForClientId(UNARCHIVED_CAMPAIGNS_COUNT_LIMIT))));
    }

    @Test
    public void preValidate_hasCampaignsCountLimitDefect() {
        doReturn(new CampaignCounts(4, 1))
                .when(campaignRepository).getCampaignCountsForCountLimitCheck(shard, clientId.asLong());
        var vr = validationService
                .preValidate(CampaignValidationContainer.create(shard, operatorUid, clientId), operatorUid,
                        Collections.emptyList());
        assertThat(vr, hasDefectWithDefinition(validationError(path(),
                maxCampaignsForClientId(CAMPAIGNS_COUNT_LIMIT))));
    }

}
