package ru.yandex.direct.web.entity.grid.service;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.core.AllowedTypesCampaignAccessibilityChecker;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.campaign.CampaignAccessService;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper;
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignAccessService.GRID_EDITABLE_SOURCES;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignAccessService.GRID_SUPPORTED_TYPES;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignAccessService.GRID_VISIBLE_SOURCES;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignAccessService.GRID_VISIBLE_TYPES;
import static ru.yandex.direct.utils.FunctionalUtils.mapSet;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class GridServiceTest {

    private static final String QUERY = "{ getReqId }";

    private static final AllowedTypesCampaignAccessibilityChecker ACCESSIBLE_CAMPAIGN_TYPES = new AllowedTypesCampaignAccessibilityChecker(
            mapSet(GRID_SUPPORTED_TYPES.getGlobal(), CampaignDataConverter::toCampaignType),
            mapSet(GRID_VISIBLE_TYPES.getGlobal(), CampaignDataConverter::toCampaignType),
            GRID_EDITABLE_SOURCES.getGlobal(),
            GRID_VISIBLE_SOURCES.getGlobal());

    @Autowired
    private TestAuthHelper testAuthHelper;

    @Autowired
    private UserService userService;

    @Autowired
    private DirectWebAuthenticationSource directWebAuthenticationSource;

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor gridGraphQlProcessor;

    @Autowired
    private GridErrorProcessingService responseHandlingService;

    @Mock
    private CampaignAccessService campaignAccessService;

    @Mock
    private RequestCampaignAccessibilityCheckerProvider requestAccessibleCampaignTypes;

    @Captor
    private ArgumentCaptor<AllowedTypesCampaignAccessibilityChecker> argumentCaptor;

    private GridService gridService;
    private UserInfo operatorInfo;

    @Before
    public void initTestData() {
        initMocks(this);
        when(campaignAccessService.getCampaignAccessibilityChecker(any()))
                .thenReturn(ACCESSIBLE_CAMPAIGN_TYPES);

        operatorInfo = testAuthHelper.createDefaultUser();

        gridService = new GridService(gridGraphQlProcessor, directWebAuthenticationSource, responseHandlingService,
                requestAccessibleCampaignTypes, campaignAccessService, gridContextProvider);
    }


    @Test
    public void checkSetCustomAccessibleCampaignTypes() {
        gridService.executeGraphQL(null, QUERY, null);

        verify(campaignAccessService).getCampaignAccessibilityChecker(any());
        verify(requestAccessibleCampaignTypes).setCustom(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).isSameAs(ACCESSIBLE_CAMPAIGN_TYPES);
    }

    @Test
    public void checkSetGridContextToProvider() {
        assertThat(gridContextProvider.getGridContext())
                .isNull();

        gridService.executeGraphQL(null, QUERY, null);
        GridGraphQLContext gridContext = gridContextProvider.getGridContext();
        assertThat(gridContext)
                .isNotNull();

        User operator = userService.getUser(operatorInfo.getUid());
        GridGraphQLContext expectedContext = new GridGraphQLContext(operator)
                .withInstant(gridContext.getInstant());
        assertThat(gridContext)
                .isEqualTo(expectedContext);
    }

}
