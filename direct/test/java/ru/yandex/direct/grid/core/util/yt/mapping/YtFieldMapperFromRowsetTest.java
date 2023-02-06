package ru.yandex.direct.grid.core.util.yt.mapping;

import java.math.BigDecimal;
import java.util.List;

import org.jooq.types.ULong;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowCondition;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowConditionAutobudgetPriority;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowConditionStatusModerate;
import ru.yandex.direct.grid.core.entity.showcondition.repository.GridShowConditionMapping;
import ru.yandex.direct.grid.schema.yt.tables.BidstableDirect;
import ru.yandex.direct.grid.schema.yt.tables.records.BidstableDirectRecord;
import ru.yandex.direct.jooqmapper.read.JooqReaderWithSupplier;
import ru.yandex.direct.jooqmapper.read.JooqReaderWithSupplierBuilder;
import ru.yandex.direct.ytwrapper.dynamic.dsl.YtMappingUtils;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.common.jooqmapperex.read.ReaderBuildersEx.fromLongFieldToBoolean;
import static ru.yandex.direct.common.jooqmapperex.read.ReaderBuildersEx.fromStringFieldToEnum;
import static ru.yandex.direct.grid.schema.yt.Tables.BIDSTABLE_DIRECT;
import static ru.yandex.direct.jooqmapper.read.ReaderBuilders.fromField;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

public class YtFieldMapperFromRowsetTest {
    private static final BidstableDirect SHOW_CONDITIONS = BIDSTABLE_DIRECT.as("K");

    private YtFieldMapper<GdiShowCondition, BidstableDirectRecord> showConditionsMapper;

    @Before
    public void before() {
        JooqReaderWithSupplier<GdiShowCondition> internalMapper =
                JooqReaderWithSupplierBuilder.builder(GdiShowCondition::new)
                        .readProperty(GdiShowCondition.ID, fromField(SHOW_CONDITIONS.ID))
                        .readProperty(GdiShowCondition.BS_ID, fromField(SHOW_CONDITIONS.PHRASE_ID))
                        .readProperty(GdiShowCondition.GROUP_ID, fromField(SHOW_CONDITIONS.PID))
                        .readProperty(GdiShowCondition.CAMPAIGN_ID, fromField(SHOW_CONDITIONS.CID))
                        .readProperty(GdiShowCondition.PRICE, fromField(SHOW_CONDITIONS.PRICE)
                                .by(YtMappingUtils::fromMicros))
                        .readProperty(GdiShowCondition.PRICE_CONTEXT, fromField(SHOW_CONDITIONS.PRICE_CONTEXT)
                                .by(YtMappingUtils::fromMicros))
                        .readProperty(GdiShowCondition.SHOWS_FORECAST, fromField(SHOW_CONDITIONS.SHOWS_FORECAST))
                        .readProperty(GdiShowCondition.AUTOBUDGET_PRIORITY,
                                fromField(SHOW_CONDITIONS.AUTOBUDGET_PRIORITY)
                                        .by(GridShowConditionMapping::autobudgetPriorityFromNum))
                        .readProperty(GdiShowCondition.STATUS_MODERATE,
                                fromStringFieldToEnum(SHOW_CONDITIONS.STATUS_MODERATE,
                                        GdiShowConditionStatusModerate.class))
                        .readProperty(GdiShowCondition.SUSPENDED, fromLongFieldToBoolean(SHOW_CONDITIONS.IS_SUSPENDED))
                        .build();

        showConditionsMapper = new YtFieldMapper<>(internalMapper, SHOW_CONDITIONS);
    }

    @Test
    public void testMapper() {
        UnversionedRowset rowset = rowsetBuilder()
                .add(rowBuilder()
                        .withColValue(SHOW_CONDITIONS.ID.getName(), 111L)
                        .withColValue(SHOW_CONDITIONS.CID.getName(), 123L)
                        .withColValue(SHOW_CONDITIONS.PID.getName(), 31L)
                        .withColValue(SHOW_CONDITIONS.PRICE.getName(), 3210000L)
                        .withColValue(SHOW_CONDITIONS.PRICE_CONTEXT.getName(), 3310000L)
                        .withColValue(SHOW_CONDITIONS.PHRASE_ID.getName(), 123213L)
                        .withColValue(SHOW_CONDITIONS.SHOWS_FORECAST.getName(), 13L)
                        .withColValue(SHOW_CONDITIONS.AUTOBUDGET_PRIORITY.getName(), 1L)
                        .withColValue(SHOW_CONDITIONS.STATUS_MODERATE.getName(), "New")
                        .withColValue(SHOW_CONDITIONS.IS_SUSPENDED.getName(), 0L)
                )
                .build();

        List<GdiShowCondition> gdiShowConditions = showConditionsMapper.fromRowset(rowset);

        GdiShowCondition expected = new GdiShowCondition()
                .withId(111L)
                .withBsId(ULong.valueOf(123213L))
                .withGroupId(31L)
                .withCampaignId(123L)
                .withPrice(BigDecimal.valueOf(3.21))
                .withPriceContext(BigDecimal.valueOf(3.31))
                .withShowsForecast(13L)
                .withAutobudgetPriority(GdiShowConditionAutobudgetPriority.LOW)
                .withStatusModerate(GdiShowConditionStatusModerate.NEW)
                .withSuspended(false);


        assertThat(gdiShowConditions.get(0))
                .isEqualTo(expected);
    }
}
