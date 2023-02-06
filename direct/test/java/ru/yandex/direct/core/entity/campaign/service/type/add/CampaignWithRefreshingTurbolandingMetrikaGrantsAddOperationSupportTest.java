package ru.yandex.direct.core.entity.campaign.service.type.add;

import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithRefreshingTurbolandingMetrikaGrants;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.AddServicedCampaignInfo;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.entity.turbolanding.service.TurboLandingService;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.direct.core.entity.campaign.service.type.add.CampaignWithRefreshingTurbolandingMetrikaGrantsAddOperationSupport.MANAGER_SEARCH_CAMPAIGN_TYPES;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class CampaignWithRefreshingTurbolandingMetrikaGrantsAddOperationSupportTest {

    @Mock
    private AddServicedCampaignService addServicedCampaignService;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private TurboLandingService turboLandingService;

    @InjectMocks
    private CampaignWithRefreshingTurbolandingMetrikaGrantsAddOperationSupport support;

    private static Object[] parametrizedTestData() {
        return new Object[][]{
                {"isServiced = false", defaultServicedInfo()},
                {"managerUid != null", defaultServicedInfo().withManagerUid(RandomNumberUtils.nextPositiveLong())},
        };
    }

    private static Object[] parametrizedCampaignTypes() {
        return new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.PERFORMANCE},
                {CampaignType.DYNAMIC},
        };
    }

    private RestrictedCampaignsAddOperationContainer parametersContainer;

    @Before
    public void initTestData() {
        MockitoAnnotations.initMocks(this);
        long chiefUid = RandomNumberUtils.nextPositiveInteger();
        parametersContainer = RestrictedCampaignsAddOperationContainer
                .create(RandomNumberUtils.nextPositiveInteger(), RandomNumberUtils.nextPositiveLong(),
                        ClientId.fromLong(RandomNumberUtils.nextPositiveLong()), chiefUid, chiefUid);
    }

    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("when = {0}")
    public void notRefreshTurbolandingMetrikaGrantsForTextCampaign(String testDescription,
                                                                   AddServicedCampaignInfo servicedInfo) {
        notRefreshTurbolandingMetrikaGrants(newCampaignByCampaignType(CampaignType.TEXT), servicedInfo);
    }

    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("when = {0}")
    public void notRefreshTurbolandingMetrikaGrantsForDynamicCampaign(String testDescription,
                                                                      AddServicedCampaignInfo servicedInfo) {
        notRefreshTurbolandingMetrikaGrants(newCampaignByCampaignType(CampaignType.DYNAMIC), servicedInfo);
    }

    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("when = {0}")
    public void notRefreshTurbolandingMetrikaGrantsForSmartCampaign(String testDescription,
                                                                    AddServicedCampaignInfo servicedInfo) {
        notRefreshTurbolandingMetrikaGrants(newCampaignByCampaignType(CampaignType.PERFORMANCE), servicedInfo);
    }

    private void notRefreshTurbolandingMetrikaGrants(CampaignWithRefreshingTurbolandingMetrikaGrants campaign,
                                                     AddServicedCampaignInfo servicedInfo) {
        doReturn(List.of(servicedInfo))
                .when(addServicedCampaignService).getServicedInfoForNewCampaigns(same(parametersContainer),
                any(List.class));

        support.afterExecution(parametersContainer, List.of(campaign));

        verify(addServicedCampaignService)
                .getServicedInfoForNewCampaigns(same(parametersContainer), any(List.class));

        verifyZeroInteractions(campaignRepository);
        verifyZeroInteractions(turboLandingService);
    }

    @Test
    @Parameters(method = "parametrizedCampaignTypes")
    @TestCaseName("{0}")
    public void notRefreshTurbolandingMetrikaGrants_whenIsServicedTrue_andManagerHasCampaignsOfClient(
            CampaignType campaignType) {
        CampaignWithRefreshingTurbolandingMetrikaGrants campaign = newCampaignByCampaignType(campaignType);
        AddServicedCampaignInfo servicedInfo = defaultServicedInfo().withIsServiced(true)
                .withManagerUid(RandomNumberUtils.nextPositiveLong());
        doReturn(List.of(servicedInfo))
                .when(addServicedCampaignService).getServicedInfoForNewCampaigns(same(parametersContainer),
                any(List.class));
        doReturn(true).when(campaignRepository)
                .managerHasClientCampaignsIgnoringCurrentCampaigns(parametersContainer.getShard(),
                        parametersContainer.getClientId(),
                        servicedInfo.getManagerUid(), Set.of(campaign.getId()), MANAGER_SEARCH_CAMPAIGN_TYPES);

        support.afterExecution(parametersContainer, List.of(campaign));

        verify(addServicedCampaignService)
                .getServicedInfoForNewCampaigns(same(parametersContainer), any(List.class));
        verify(campaignRepository)
                .managerHasClientCampaignsIgnoringCurrentCampaigns(parametersContainer.getShard(),
                        parametersContainer.getClientId(),
                        servicedInfo.getManagerUid(), Set.of(campaign.getId()), MANAGER_SEARCH_CAMPAIGN_TYPES);

        verifyZeroInteractions(turboLandingService);
    }

    @Test
    @Parameters(method = "parametrizedCampaignTypes")
    @TestCaseName("{0}")
    public void refreshTurbolandingMetrikaGrants(CampaignType campaignType) {
        CampaignWithRefreshingTurbolandingMetrikaGrants campaign = newCampaignByCampaignType(campaignType);
        AddServicedCampaignInfo servicedInfo = defaultServicedInfo().withIsServiced(true)
                .withManagerUid(RandomNumberUtils.nextPositiveLong());
        doReturn(List.of(servicedInfo))
                .when(addServicedCampaignService).getServicedInfoForNewCampaigns(same(parametersContainer),
                any(List.class));
        doReturn(false).when(campaignRepository)
                .managerHasClientCampaignsIgnoringCurrentCampaigns(parametersContainer.getShard(),
                        parametersContainer.getClientId(),
                        servicedInfo.getManagerUid(), Set.of(campaign.getId()), MANAGER_SEARCH_CAMPAIGN_TYPES);

        support.afterExecution(parametersContainer, List.of(campaign));

        verify(addServicedCampaignService).getServicedInfoForNewCampaigns(same(parametersContainer),
                any(List.class));
        verify(campaignRepository)
                .managerHasClientCampaignsIgnoringCurrentCampaigns(parametersContainer.getShard(),
                        parametersContainer.getClientId(),
                        servicedInfo.getManagerUid(), Set.of(campaign.getId()), MANAGER_SEARCH_CAMPAIGN_TYPES);
        verify(turboLandingService)
                .refreshTurbolandingMetrikaGrants(parametersContainer.getOperatorUid(),
                        parametersContainer.getClientId());
    }

    private static AddServicedCampaignInfo defaultServicedInfo() {
        return new AddServicedCampaignInfo()
                .withIsServiced(false);
    }

    private CampaignWithRefreshingTurbolandingMetrikaGrants newCampaignByCampaignType(CampaignType campaignType) {
        return (CampaignWithRefreshingTurbolandingMetrikaGrants) TestCampaigns.newCampaignByCampaignType(campaignType)
                .withId(RandomNumberUtils.nextPositiveLong());
    }
}
