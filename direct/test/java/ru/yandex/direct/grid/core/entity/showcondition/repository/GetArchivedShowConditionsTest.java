package ru.yandex.direct.grid.core.entity.showcondition.repository;

import java.time.LocalDate;
import java.util.List;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.ShowConditionFetchedFieldsResolver;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowCondition;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowConditionFilter;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowConditionOrderBy;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowConditionPrimaryStatus;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowConditionType;
import ru.yandex.direct.grid.core.util.FetchedFieldsResolverCoreUtil;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.multitype.entity.LimitOffset;
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.common.util.RepositoryUtils.booleanToLong;
import static ru.yandex.direct.grid.schema.yt.Tables.BIDSTABLE_DIRECT;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

@RunWith(JUnitParamsRunner.class)
public class GetArchivedShowConditionsTest {

    private static final int SHARD = 1;
    private static final long CAMPAIGN_ID = 11L;
    private static final LocalDate DATE = LocalDate.of(2019, 1, 1);
    private static final List<GdiShowConditionOrderBy> EMPTY_ORDER_BY_LIST = emptyList();
    private static final int LIMIT = 1000;
    private static final ShowConditionFetchedFieldsResolver AD_FETCHED_FIELDS_RESOLVER_SIMPLE =
            FetchedFieldsResolverCoreUtil.buildShowConditionFetchedFieldsResolver(false);

    @Mock
    private YtDynamicSupport ytSupport;

    @Mock
    @SuppressWarnings("unused")
    private GridKeywordsParser keywordsParser;

    @InjectMocks
    private GridShowConditionYtRepository gridShowConditionYtRepository;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @TestCaseName("expected archived = {0}")
    @Parameters(method = "parametersForTestGetShowConditions")
    public void testGetShowConditions(boolean isArchived) {
        doReturn(convertToKeywordNode(isArchived))
                .when(ytSupport).selectRows(eq(SHARD), any(), anyBoolean());

        var filter = new GdiShowConditionFilter()
                .withShowConditionStatusIn(singleton(GdiShowConditionPrimaryStatus.ARCHIVED))
                .withCampaignIdIn(singleton(CAMPAIGN_ID));
        var showConditionWithTotals = gridShowConditionYtRepository
                .getShowConditions(SHARD, filter, EMPTY_ORDER_BY_LIST, DATE, DATE,
                        LimitOffset.limited(LIMIT), emptySet(), null, AD_FETCHED_FIELDS_RESOLVER_SIMPLE, false, false);

        assertThat(showConditionWithTotals.getGdiShowConditions())
                .hasSize(1)
                .extracting(GdiShowCondition::getArchived)
                .containsExactly(isArchived);
    }

    public static Object[][] parametersForTestGetShowConditions() {
        return new Object[][]{
                {true},
                {false}
        };
    }

    private static UnversionedRowset convertToKeywordNode(boolean isArchived) {
        RowsetBuilder builder = rowsetBuilder();
        builder.add(
                rowBuilder()
                        .withColValue(BIDSTABLE_DIRECT.ID.getName(), RandomUtils.nextLong(0, Long.MAX_VALUE))
                        .withColValue(BIDSTABLE_DIRECT.PRICE.getName(), RandomUtils.nextLong(0, Long.MAX_VALUE))
                        .withColValue(BIDSTABLE_DIRECT.PID.getName(), RandomUtils.nextLong(0, Long.MAX_VALUE))
                        .withColValue(BIDSTABLE_DIRECT.CID.getName(), RandomUtils.nextLong(0, Long.MAX_VALUE))
                        .withColValue(BIDSTABLE_DIRECT.PHRASE_ID.getName(), RandomUtils.nextLong(0, Long.MAX_VALUE))
                        .withColValue(BIDSTABLE_DIRECT.PHRASE.getName(), RandomStringUtils.randomAlphabetic(9))
                        .withColValue(BIDSTABLE_DIRECT.IS_SUSPENDED.getName(), false)
                        .withColValue(BIDSTABLE_DIRECT.IS_DELETED.getName(), booleanToLong(false))
                        .withColValue(BIDSTABLE_DIRECT.IS_ARCHIVED.getName(), booleanToLong(isArchived))
                        .withColValue(BIDSTABLE_DIRECT.BID_TYPE.getName(),
                                GdiShowConditionType.KEYWORD.name().toLowerCase())
        );

        return builder.build();
    }
}
