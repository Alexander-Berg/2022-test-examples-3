package ru.yandex.direct.ess.router.rules.mediascopeintegration;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.ess.logicobjects.mediascopeintegration.MediascopePositionChangeObject;
import ru.yandex.direct.ess.router.configuration.TestConfiguration;
import ru.yandex.direct.ess.router.testutils.BannerMeasurersTableChange;
import ru.yandex.direct.ess.router.testutils.BannersPerformanceTableChange;
import ru.yandex.direct.ess.router.testutils.CampaignsTableChange;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.binlog.model.Operation.INSERT;
import static ru.yandex.direct.binlog.model.Operation.UPDATE;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS_PERFORMANCE;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_MEASURERS;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.ess.router.testutils.BannerMeasurersTableChange.createBannersMeasurersEvent;
import static ru.yandex.direct.ess.router.testutils.BannersPerformanceTableChange.createBannersPerformanceEvent;
import static ru.yandex.direct.ess.router.testutils.CampaignsTableChange.createCampaignEvent;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
class MediascopePositionChangeRuleTest {

    private static final long CID = 7L;
    private static final long BID = 6L;

    @Autowired
    private MediascopePositionChangeRule rule;

    @Test
    void mapBinlogEvent_CampaignNameChanged_CidInChangeObject() {
        CampaignsTableChange campaignsTableChange = new CampaignsTableChange().withCid(CID);
        campaignsTableChange.addChangedColumn(CAMPAIGNS.NAME, "before", "after");
        BinlogEvent binlogEvent = createCampaignEvent(singletonList(campaignsTableChange), UPDATE);

        List<MediascopePositionChangeObject> resultObjects = rule.mapBinlogEvent(binlogEvent);

        assertThat(resultObjects).containsExactly(new MediascopePositionChangeObject(CID, null));
    }

    @Test
    void mapBinlogEvent_CampaignStartDateChanged_CidInChangeObject() {
        var campaignsTableChange = new CampaignsTableChange().withCid(CID);
        campaignsTableChange.addChangedColumn(CAMPAIGNS.START_TIME, "2019-11-11", "2019-12-12");
        var binlogEvent = createCampaignEvent(singletonList(campaignsTableChange), UPDATE);

        List<MediascopePositionChangeObject> resultObjects = rule.mapBinlogEvent(binlogEvent);

        assertThat(resultObjects).containsExactly(new MediascopePositionChangeObject(CID, null));
    }

    @Test
    void mapBinlogEvent_CampaignFinishDateChanged_CidInChangeObject() {
        var campaignsTableChange = new CampaignsTableChange().withCid(CID);
        campaignsTableChange.addChangedColumn(CAMPAIGNS.FINISH_TIME, null, "2019-12-12");
        var binlogEvent = createCampaignEvent(singletonList(campaignsTableChange), UPDATE);

        List<MediascopePositionChangeObject> resultObjects = rule.mapBinlogEvent(binlogEvent);

        assertThat(resultObjects).containsExactly(new MediascopePositionChangeObject(CID, null));
    }

    @Test
    void mapBinlogEvent_BannerCreativeIdChanged_BidInChangeObject() {
        var bannersPerformanceTableChange = new BannersPerformanceTableChange().withBid(BID);
        bannersPerformanceTableChange.addChangedColumn(BANNERS_PERFORMANCE.CREATIVE_ID, 5, 6);
        var binlogEvent = createBannersPerformanceEvent(singletonList(bannersPerformanceTableChange), UPDATE);

        List<MediascopePositionChangeObject> resultObjects = rule.mapBinlogEvent(binlogEvent);

        assertThat(resultObjects).containsExactly(new MediascopePositionChangeObject(null, BID));
    }

    @Test
    void mapBinlogEvent_BannerMeasurersInsertWithIntegration_BidInChangeObject() {
        var bannerMeasurersTableChange = new BannerMeasurersTableChange().withBid(BID);
        bannerMeasurersTableChange.addChangedColumn(BANNER_MEASURERS.HAS_INTEGRATION, null, 1L);
        var binlogEvent = createBannersMeasurersEvent(singletonList(bannerMeasurersTableChange), INSERT);

        List<MediascopePositionChangeObject> resultObjects = rule.mapBinlogEvent(binlogEvent);

        assertThat(resultObjects).containsExactly(new MediascopePositionChangeObject(null, BID));
    }

    @Test
    void mapBinlogEvent_BannerMeasurersInsertWithoutIntegration_EmptyChangeObject() {
        var bannerMeasurersTableChange = new BannerMeasurersTableChange().withBid(BID);
        bannerMeasurersTableChange.addChangedColumn(BANNER_MEASURERS.HAS_INTEGRATION, null, 0L);
        var binlogEvent = createBannersMeasurersEvent(singletonList(bannerMeasurersTableChange), INSERT);

        List<MediascopePositionChangeObject> resultObjects = rule.mapBinlogEvent(binlogEvent);

        assertThat(resultObjects).isEmpty();
    }

    @Test
    void mapBinlogEvent_BannerMeasurersUpdateWithIntegration_BidInChangeObject() {
        var bannerMeasurersTableChange = new BannerMeasurersTableChange().withBid(BID);
        bannerMeasurersTableChange.addChangedColumn(BANNER_MEASURERS.HAS_INTEGRATION, 0L, 1L);
        var binlogEvent = createBannersMeasurersEvent(singletonList(bannerMeasurersTableChange), UPDATE);

        List<MediascopePositionChangeObject> resultObjects = rule.mapBinlogEvent(binlogEvent);

        assertThat(resultObjects).containsExactly(new MediascopePositionChangeObject(null, BID));
    }

    @Test
    void mapBinlogEvent_BannerMeasurersUpdateWithoutIntegration_EmptyChangeObject() {
        var bannerMeasurersTableChange = new BannerMeasurersTableChange().withBid(BID);
        bannerMeasurersTableChange.addChangedColumn(BANNER_MEASURERS.HAS_INTEGRATION, 1L, 0L);
        var binlogEvent = createBannersMeasurersEvent(singletonList(bannerMeasurersTableChange), UPDATE);

        List<MediascopePositionChangeObject> resultObjects = rule.mapBinlogEvent(binlogEvent);

        assertThat(resultObjects).isEmpty();
    }
}
