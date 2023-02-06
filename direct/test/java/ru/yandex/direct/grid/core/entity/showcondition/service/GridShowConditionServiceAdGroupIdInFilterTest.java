package ru.yandex.direct.grid.core.entity.showcondition.service;

import java.time.LocalDate;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.entity.AdGroupIdInFilterBaseTest;
import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.ShowConditionFetchedFieldsResolver;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowConditionFilter;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowConditionOrderBy;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowConditionWithTotals;
import ru.yandex.direct.grid.core.entity.showcondition.repository.GridShowConditionYtRepository;
import ru.yandex.direct.grid.core.util.FetchedFieldsResolverCoreUtil;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class GridShowConditionServiceAdGroupIdInFilterTest extends AdGroupIdInFilterBaseTest {

    private static final int SHARD = 1;
    private static final Long OPERATOR_UID = 2L;
    private static final ClientId CLIENT_ID = ClientId.fromLong(3L);
    private static final List<GdiShowConditionOrderBy> SHOW_CONDITION_ORDER_BY_LIST = emptyList();
    private static final LocalDate STAT_FROM = LocalDate.now();
    private static final LocalDate STAT_TO = LocalDate.now();
    private static final ShowConditionFetchedFieldsResolver SHOW_CONDITION_FETCHED_FIELDS_RESOLVER =
            FetchedFieldsResolverCoreUtil.buildShowConditionFetchedFieldsResolver(true);

    private GdiShowConditionFilter filter;

    @Mock
    private FeatureService featureService;
    @Mock
    private GridShowConditionYtRepository gridShowConditionYtRepository;

    @InjectMocks
    private GridShowConditionService gridShowConditionService;

    @Captor
    private ArgumentCaptor<GdiShowConditionFilter> filterArgumentCaptor;

    @Before
    public void initTestData() {
        MockitoAnnotations.initMocks(this);

        filter = new GdiShowConditionFilter()
                .withAdGroupIdIn(adGroupIdIn)
                .withCampaignIdIn(campaignIdIn);

        doReturn(new GdiShowConditionWithTotals().withGdiShowConditions(emptyList())).when(gridShowConditionYtRepository)
                .getShowConditions(eq(SHARD), eq(filter), eq(SHOW_CONDITION_ORDER_BY_LIST), eq(STAT_FROM), eq(STAT_TO),
                        any(), eq(emptySet()), isNull(), any(), eq(false), eq(false));
    }


    @Test
    public void getShowConditionsTest() {
        if (expectedException) {
            checkGetShowConditionsThrowException();
            return;
        }

        gridShowConditionService.getShowConditions(SHARD, OPERATOR_UID, CLIENT_ID,
                filter, SHOW_CONDITION_ORDER_BY_LIST, STAT_FROM, STAT_TO,
                emptySet(), SHOW_CONDITION_FETCHED_FIELDS_RESOLVER, false, false);

        verify(gridShowConditionYtRepository)
                .getShowConditions(eq(SHARD), filterArgumentCaptor.capture(), eq(SHOW_CONDITION_ORDER_BY_LIST),
                        eq(STAT_FROM), eq(STAT_TO), any(), eq(emptySet()), isNull(), any(), eq(false), eq(false));

        assertThat(filterArgumentCaptor.getValue().getAdGroupIdIn())
                .isEqualTo(expectedAdGroupIdIn);
    }

    private void checkGetShowConditionsThrowException() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("campaignIdIn from filter must be not empty, when count of adGroupIdIn over limit");

        gridShowConditionService.getShowConditions(SHARD, OPERATOR_UID, CLIENT_ID,
                filter, SHOW_CONDITION_ORDER_BY_LIST, STAT_FROM, STAT_TO,
                emptySet(), SHOW_CONDITION_FETCHED_FIELDS_RESOLVER, false, false);
    }

}
