package ru.yandex.direct.grid.processing.service.group;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.grid.core.entity.group.service.GridAdGroupConstants;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.client.GdClient;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.model.group.GdAdGroup;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupWithTotals;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupsContainer;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupsContext;
import ru.yandex.direct.grid.processing.service.cache.GridCacheService;
import ru.yandex.direct.grid.processing.service.validation.GridValidationService;
import ru.yandex.direct.grid.processing.util.ContextHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsSecondArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.generateAdGroupsList;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.getDefaultGdAdGroupsContainer;

public class AdGroupHasObjectsOverLimitTest {

    private GridGraphQLContext gridGraphQLContext;
    private GdAdGroupsContainer inputContainer;

    @Mock
    private GridCacheService gridCacheService;

    @Mock
    private GroupDataService groupDataService;

    @Mock
    private AdGroupMutationService adGroupMutationService;

    @Mock
    private GridValidationService gridValidationService;

    @Mock
    private FeatureService featureService;

    @InjectMocks
    private AdGroupGraphQlService adGroupGraphQlService;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        gridGraphQLContext = ContextHelper.buildDefaultContext();

        when(gridCacheService.getResultAndSaveToCacheIfRequested(any(), any(), any(), any(), anyBoolean()))
                .then(returnsSecondArg());

        inputContainer = getDefaultGdAdGroupsContainer();
    }


    @Test
    public void checkHasNotObjectsOverLimit() {
        generateAdGroups(GridAdGroupConstants.getMaxGroupRows() - 1);
        GdAdGroupsContext groupsContext =
                adGroupGraphQlService.getAdGroups(gridGraphQLContext, mock(GdClient.class), inputContainer);

        assertThat(groupsContext.getFeatures().getHasAdGroupsOverLimit())
                .isFalse();
    }

    @Test
    public void checkHasObjectsOverLimit() {
        generateAdGroups(GridAdGroupConstants.getMaxGroupRows());
        GdAdGroupsContext groupsContext =
                adGroupGraphQlService.getAdGroups(gridGraphQLContext, mock(GdClient.class), inputContainer);

        assertThat(groupsContext.getFeatures().getHasAdGroupsOverLimit())
                .isTrue();
    }


    private void generateAdGroups(int adGroupsCount) {
        List<GdAdGroup> gdAdGroups = generateAdGroupsList(adGroupsCount);

        GdClientInfo queriedClient = gridGraphQLContext.getQueriedClient();
        doReturn(new GdAdGroupWithTotals().withGdAdGroups(gdAdGroups))
                .when(groupDataService)
                .getAdGroups(eq(queriedClient), eq(inputContainer), eq(gridGraphQLContext));
    }

}
