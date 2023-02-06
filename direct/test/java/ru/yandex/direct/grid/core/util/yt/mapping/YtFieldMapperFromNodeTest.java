package ru.yandex.direct.grid.core.util.yt.mapping;

import java.math.BigDecimal;

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
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.common.jooqmapperex.read.ReaderBuildersEx.fromLongFieldToBoolean;
import static ru.yandex.direct.common.jooqmapperex.read.ReaderBuildersEx.fromStringFieldToEnum;
import static ru.yandex.direct.grid.schema.yt.Tables.BIDSTABLE_DIRECT;
import static ru.yandex.direct.jooqmapper.read.ReaderBuilders.fromField;

public class YtFieldMapperFromNodeTest {
    private static final BidstableDirect SHOW_CONDITIONS = BIDSTABLE_DIRECT.as("K");

    private YtFieldMapper<GdiShowCondition, BidstableDirectRecord> showConditionsReader;

    @Before
    public void before() {
        JooqReaderWithSupplier<GdiShowCondition> internalReader =
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

        showConditionsReader = new YtFieldMapper<>(internalReader, SHOW_CONDITIONS);
    }

    @Test
    public void testMapper() {
        YTreeMapNode node = YTree.builder()
                .beginMap()
                .key("id").value(111L)
                .key("cid").value(123L)
                .key("pid").value(31L)
                .key("price").value(3210000L)
                .key("price_context").value(3310000L)
                .key("PhraseID").value(123213L)
                .key("showsForecast").value(13L)
                .key("autobudgetPriority").value(1L)
                .key("statusModerate").value("New")
                .key("is_suspended").value(0L)
                .buildMap();

        GdiShowCondition showCondition = showConditionsReader.fromNode(node);
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

        assertThat(showCondition).isEqualTo(expected);
    }
}
