package ru.yandex.direct.ess.router.rules.aggregatedstatuses;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.aggregatedstatuses.AggregatedStatusObjectType;
import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.ess.logicobjects.aggregatedstatuses.AggregatedStatusEventObject;
import ru.yandex.direct.ess.router.configuration.TestConfiguration;
import ru.yandex.direct.ess.router.testutils.BannersTableChange;
import ru.yandex.direct.ess.router.testutils.ModerateBannerPagesTableChange;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.binlog.model.Operation.DELETE;
import static ru.yandex.direct.binlog.model.Operation.INSERT;
import static ru.yandex.direct.binlog.model.Operation.UPDATE;
import static ru.yandex.direct.dbschema.ppc.tables.Banners.BANNERS;
import static ru.yandex.direct.dbschema.ppc.tables.ModerateBannerPages.MODERATE_BANNER_PAGES;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
class AggregatedStatusesRuleBannersTest {
    private static final Long BID = 123L;
    private static final Long MODERATE_BANNER_PAGE_ID = 1L;

    @Autowired
    private AggregatedStatusesRule rule;

    @Test
    void mapBinlogEventInsert() {
        var tableChange = getModerateBannerPagesTableChange();
        BinlogEvent binlogEvent = ModerateBannerPagesTableChange.createEvent(singletonList(tableChange), INSERT);

        binlogProduceEventsCheck(binlogEvent);
    }

    @Test
    void mapBinlogEventDelete() {
        var tableChange = getModerateBannerPagesTableChange();
        BinlogEvent binlogEvent = ModerateBannerPagesTableChange.createEvent(singletonList(tableChange), DELETE);

        binlogProduceEventsCheck(binlogEvent);
    }

    @Test
    void mapBinlogEventUpdateStatusModerate() {
        var tableChange = getModerateBannerPagesTableChange();
        tableChange.addChangedColumn(MODERATE_BANNER_PAGES.STATUS_MODERATE, "No", "Yes");
        BinlogEvent binlogEvent = ModerateBannerPagesTableChange.createEvent(singletonList(tableChange), UPDATE);

        binlogProduceEventsCheck(binlogEvent);
    }

    @Test
    void mapBinlogEventUpdateStatusModerateOperator() {
        var tableChange = getModerateBannerPagesTableChange();
        tableChange.addChangedColumn(MODERATE_BANNER_PAGES.STATUS_MODERATE_OPERATOR, "No", "Yes");
        BinlogEvent binlogEvent = ModerateBannerPagesTableChange.createEvent(singletonList(tableChange), UPDATE);

        binlogProduceEventsCheck(binlogEvent);
    }

    @Test
    void mapBinlogEventUpdateIsRemoved() {
        var tableChange = getModerateBannerPagesTableChange();
        tableChange.addChangedColumn(MODERATE_BANNER_PAGES.IS_REMOVED, 0L, 1L);
        BinlogEvent binlogEvent = ModerateBannerPagesTableChange.createEvent(singletonList(tableChange), UPDATE);

        binlogProduceEventsCheck(binlogEvent);
    }

    @Test
    void mapBinlogEventUpdateVersionIgnored() {
        var tableChange = getModerateBannerPagesTableChange();
        tableChange.addChangedColumn(MODERATE_BANNER_PAGES.VERSION, 10L, 11L);
        BinlogEvent binlogEvent = ModerateBannerPagesTableChange.createEvent(singletonList(tableChange), UPDATE);

        List<AggregatedStatusEventObject> resultObjects = rule.mapBinlogEvent(binlogEvent);

        assertThat(resultObjects).isEmpty();
    }

    @Test
    void mapBinlogEventUpdateBannerFlags() {
        var tableChange = getBannersTableChange();
        tableChange.addChangedColumn(BANNERS.FLAGS, null, BannerFlags.ASOCIAL);
        BinlogEvent binlogEvent = BannersTableChange.createBannersEvent(singletonList(tableChange), UPDATE);

        binlogProduceEventsCheckBanners(binlogEvent);
    }

    @Test
    void mapBinlogEventUpdateBannerFlags2() {
        var tableChange = getBannersTableChange();
        tableChange.addChangedColumn(BANNERS.FLAGS, BannerFlags.ASOCIAL, null);
        BinlogEvent binlogEvent = BannersTableChange.createBannersEvent(singletonList(tableChange), UPDATE);

        binlogProduceEventsCheckBanners(binlogEvent);
    }

    private ModerateBannerPagesTableChange getModerateBannerPagesTableChange() {
        return new ModerateBannerPagesTableChange()
                .withModerateBannerPageId(MODERATE_BANNER_PAGE_ID)
                .withBid(BID);
    }

    private BannersTableChange getBannersTableChange() {
        return new BannersTableChange()
                .withBid(BID);
    }

    private void binlogProduceEventsCheck(BinlogEvent binlogEvent) {
        List<AggregatedStatusEventObject> resultObjects = rule.mapBinlogEvent(binlogEvent);

        assertThat(resultObjects).containsExactly(new AggregatedStatusEventObject(
                AggregatedStatusObjectType.AD, BID, null, null, false));
    }

    private void binlogProduceEventsCheckBanners(BinlogEvent binlogEvent) {
        List<AggregatedStatusEventObject> resultObjects = rule.mapBinlogEvent(binlogEvent);

        assertThat(resultObjects).containsExactly(new AggregatedStatusEventObject(
                AggregatedStatusObjectType.AD, BID, 0L, 0L, false));
    }
}
