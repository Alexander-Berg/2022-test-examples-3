package ru.yandex.direct.grid.processing.service.showcondition;

import java.util.List;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.grid.core.entity.showcondition.service.GridShowConditionConstants;
import ru.yandex.direct.grid.model.campaign.GdCampaignType;
import ru.yandex.direct.grid.model.campaign.GdTextCampaign;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyManual;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.client.GdClient;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.model.group.GdTextAdGroup;
import ru.yandex.direct.grid.processing.model.showcondition.GdKeyword;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowCondition;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionWithTotals;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionsContainer;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionsContext;
import ru.yandex.direct.grid.processing.service.cache.GridCacheService;
import ru.yandex.direct.grid.processing.service.showcondition.keywords.ShowConditionDataService;
import ru.yandex.direct.grid.processing.service.showcondition.validation.ShowConditionValidationService;
import ru.yandex.direct.grid.processing.util.ContextHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsSecondArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.grid.processing.util.ShowConditionTestDataUtils.getDefaultGdShowConditionsContainer;

public class ShowConditionHasObjectsOverLimitTest {

    private GridGraphQLContext gridGraphQLContext;
    private GdShowConditionsContainer inputContainer;

    @Mock
    private GridCacheService gridCacheService;

    @Mock
    private ShowConditionDataService showConditionDataService;

    @Mock
    private ShowConditionValidationService showConditionValidationService;

    @Mock
    private FeatureService featureService;

    @InjectMocks
    private ShowConditionGraphQlService showConditionGraphQlService;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        gridGraphQLContext = ContextHelper.buildDefaultContext();

        when(gridCacheService.getResultAndSaveToCacheIfRequested(any(), any(), any(), any(), anyBoolean()))
                .then(returnsSecondArg());

        inputContainer = getDefaultGdShowConditionsContainer();
    }


    @Test
    public void checkHasNotObjectsOverLimit() {
        generateShowConditions(GridShowConditionConstants.getMaxConditionRows() - 1);
        GdShowConditionsContext showConditionsContext = showConditionGraphQlService
                .getShowConditions(gridGraphQLContext, mock(GdClient.class), inputContainer);

        assertThat(showConditionsContext.getHasObjectsOverLimit())
                .isFalse();
    }

    @Test
    public void checkHasObjectsOverLimit() {
        generateShowConditions(GridShowConditionConstants.getMaxConditionRows());
        GdShowConditionsContext showConditionsContext = showConditionGraphQlService
                .getShowConditions(gridGraphQLContext, mock(GdClient.class), inputContainer);

        assertThat(showConditionsContext.getHasObjectsOverLimit())
                .isTrue();
    }

    private void generateShowConditions(int showConditionsCount) {
        List<GdShowCondition> gdShowConditions =
                StreamEx.generate(ShowConditionHasObjectsOverLimitTest::generateGdKeyword)
                        .limit(showConditionsCount)
                        .collect(Collectors.toList());

        GdClientInfo queriedClient = gridGraphQLContext.getQueriedClient();
        doReturn(new GdShowConditionWithTotals().withGdShowConditions(gdShowConditions))
                .when(showConditionDataService)
                .getShowConditions(eq(queriedClient), eq(inputContainer), eq(gridGraphQLContext), any(), eq(false));
    }

    private static GdShowCondition generateGdKeyword() {
        return new GdKeyword()
                .withAdGroup(new GdTextAdGroup()
                        .withCampaign(new GdTextCampaign()
                                .withType(GdCampaignType.TEXT)
                                .withFlatStrategy(new GdCampaignStrategyManual())
                        )
                );
    }

}
