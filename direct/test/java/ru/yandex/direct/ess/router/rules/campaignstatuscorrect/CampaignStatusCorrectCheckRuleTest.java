package ru.yandex.direct.ess.router.rules.campaignstatuscorrect;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.dbschema.ppc.Tables;
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;
import ru.yandex.direct.ess.logicobjects.campaignstatuscorrect.CampaignStatusCorrectCheckObject;
import ru.yandex.direct.ess.router.configuration.TestConfiguration;
import ru.yandex.direct.ess.router.testutils.BannersTableChange;
import ru.yandex.direct.ess.router.testutils.PhrasesTableChange;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.binlog.model.Operation.DELETE;
import static ru.yandex.direct.binlog.model.Operation.INSERT;
import static ru.yandex.direct.binlog.model.Operation.UPDATE;
import static ru.yandex.direct.ess.router.testutils.BannersTableChange.createBannersEvent;
import static ru.yandex.direct.ess.router.testutils.PhrasesTableChange.createPhrasesEvent;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
class CampaignStatusCorrectCheckRuleTest {

    @Autowired
    private CampaignStatusCorrectCheckRule rule;

    @Test
    void mapBinlogEventTest_InsertIntoBanners() {
        List<BannersTableChange> bannersTableChanges = new ArrayList<>();
        BannersTableChange bannersTableChange1 = new BannersTableChange().withBid(1L).withCid(2L).withPid(3L)
                .withBannerType(BannersBannerType.cpm_banner);
        bannersTableChanges.add(bannersTableChange1);

        BannersTableChange bannersTableChange2 = new BannersTableChange().withBid(2L).withCid(3L).withPid(4L)
                .withBannerType(BannersBannerType.cpm_banner);
        bannersTableChanges.add(bannersTableChange2);

        BinlogEvent binlogEvent = createBannersEvent(bannersTableChanges, INSERT);
        List<CampaignStatusCorrectCheckObject> resultObjects = rule.mapBinlogEvent(binlogEvent);
        CampaignStatusCorrectCheckObject[] expected =
                bannersTableChanges.stream()
                        .map(tableChange -> new CampaignStatusCorrectCheckObject(tableChange.cid))
                        .toArray(CampaignStatusCorrectCheckObject[]::new);
        assertThat(resultObjects).hasSize(2);
        assertThat(resultObjects).containsExactly(expected);
    }

    @Test
    void mapBinlogEventTest_DeleteFromBanners() {
        List<BannersTableChange> bannersTableChanges = new ArrayList<>();
        BannersTableChange bannersTableChange1 = new BannersTableChange().withBid(1L).withCid(2L).withPid(3L);
        bannersTableChanges.add(bannersTableChange1);

        BannersTableChange bannersTableChange2 = new BannersTableChange().withBid(2L).withCid(3L).withPid(4L);
        bannersTableChanges.add(bannersTableChange2);


        BinlogEvent binlogEvent = createBannersEvent(bannersTableChanges, DELETE);

        List<CampaignStatusCorrectCheckObject> resultObjects = rule.mapBinlogEvent(binlogEvent);

        CampaignStatusCorrectCheckObject[] expected =
                bannersTableChanges.stream()
                        .map(tableChange -> new CampaignStatusCorrectCheckObject(tableChange.cid))
                        .toArray(CampaignStatusCorrectCheckObject[]::new);
        assertThat(resultObjects).hasSize(2);
        assertThat(resultObjects).containsExactly(expected);
    }

    @Test
    void mapBinlogEventTest_UpdateBanners() {
        List<BannersTableChange> bannersTableChanges = new ArrayList<>();

        BannersTableChange bannersTableChange =
                new BannersTableChange().withBid(1L).withCid(2L).withPid(3L).withBannerType(BannersBannerType.cpm_banner);
        bannersTableChange.addChangedColumn(Tables.BANNERS.STATUS_MODERATE, "New", "Ready");
        bannersTableChanges.add(bannersTableChange);

        BannersTableChange bannersTableChange1 =
                new BannersTableChange().withBid(4L).withCid(5L).withPid(6L).withBannerType(BannersBannerType.cpm_banner);
        bannersTableChange1.addChangedColumn(Tables.BANNERS.STATUS_POST_MODERATE, "New", "Ready");
        bannersTableChanges.add(bannersTableChange1);

        BannersTableChange bannersTableChange2 =
                new BannersTableChange().withBid(7L).withCid(8L).withPid(9L).withBannerType(BannersBannerType.cpm_banner);
        bannersTableChange2.addChangedColumn(Tables.BANNERS.STATUS_SHOW, "Yes", "No");
        bannersTableChanges.add(bannersTableChange2);

        BannersTableChange bannersTableChange3 =
                new BannersTableChange().withBid(10L).withCid(11L).withPid(12L).withBannerType(BannersBannerType.cpm_banner);
        bannersTableChange3.addChangedColumn(Tables.BANNERS.STATUS_ARCH, "Yes", "No");
        bannersTableChanges.add(bannersTableChange3);

        BannersTableChange bannersTableChange4 =
                new BannersTableChange().withBid(13L).withCid(14L).withPid(15L).withBannerType(BannersBannerType.cpm_banner);
        bannersTableChange4.addChangedColumn(Tables.BANNERS.STATUS_ACTIVE, "Yes", "No");
        bannersTableChanges.add(bannersTableChange4);

        BannersTableChange bannersTableChange5 =
                new BannersTableChange().withBid(16L).withCid(17L).withPid(18L).withBannerType(BannersBannerType.cpm_audio);
        bannersTableChange5.addChangedColumn(Tables.BANNERS.STATUS_MODERATE, "New", "Ready");
        bannersTableChanges.add(bannersTableChange5);


        BinlogEvent binlogEvent = createBannersEvent(bannersTableChanges, UPDATE);
        List<CampaignStatusCorrectCheckObject> resultObjects = rule.mapBinlogEvent(binlogEvent);
        CampaignStatusCorrectCheckObject[] expected = new CampaignStatusCorrectCheckObject[]{
                new CampaignStatusCorrectCheckObject(bannersTableChange.cid),
                new CampaignStatusCorrectCheckObject(bannersTableChange1.cid),
                new CampaignStatusCorrectCheckObject(bannersTableChange2.cid),
                new CampaignStatusCorrectCheckObject(bannersTableChange3.cid),
                new CampaignStatusCorrectCheckObject(bannersTableChange5.cid),
        };

        assertThat(resultObjects).hasSize(5);
        assertThat(resultObjects).containsExactly(expected);
    }

    @Test
    void mapBinlogEventTest_InsertIntoPhrases() {
        List<PhrasesTableChange> phrasesTableChanges = new ArrayList<>();

        PhrasesTableChange phrasesTableChange1 = new PhrasesTableChange().withPid(1L).withCid(2L);
        phrasesTableChanges.add(phrasesTableChange1);

        PhrasesTableChange phrasesTableChange2 = new PhrasesTableChange().withPid(2L).withCid(3L);
        phrasesTableChanges.add(phrasesTableChange2);

        BinlogEvent binlogEvent = createPhrasesEvent(phrasesTableChanges, INSERT);

        List<CampaignStatusCorrectCheckObject> resultObjects = rule.mapBinlogEvent(binlogEvent);
        CampaignStatusCorrectCheckObject[] expected =
                phrasesTableChanges.stream()
                        .map(tableChange -> new CampaignStatusCorrectCheckObject(tableChange.cid))
                        .toArray(CampaignStatusCorrectCheckObject[]::new);
        assertThat(resultObjects).hasSize(2);
        assertThat(resultObjects).containsExactly(expected);
    }

    @Test
    void mapBinlogEventTest_DeleteFromPhrases() {
        List<PhrasesTableChange> phrasesTableChanges = new ArrayList<>();

        PhrasesTableChange phrasesTableChange1 = new PhrasesTableChange().withPid(1L).withCid(2L);
        phrasesTableChanges.add(phrasesTableChange1);

        PhrasesTableChange phrasesTableChange2 = new PhrasesTableChange().withPid(2L).withCid(3L);
        phrasesTableChanges.add(phrasesTableChange2);

        BinlogEvent binlogEvent = createPhrasesEvent(phrasesTableChanges, DELETE);

        List<CampaignStatusCorrectCheckObject> resultObjects = rule.mapBinlogEvent(binlogEvent);
        CampaignStatusCorrectCheckObject[] expected =
                phrasesTableChanges.stream()
                        .map(tableChange -> new CampaignStatusCorrectCheckObject(tableChange.cid))
                        .toArray(CampaignStatusCorrectCheckObject[]::new);

        assertThat(resultObjects).hasSize(2);
        assertThat(resultObjects).containsExactly(expected);
    }

    @Test
    void mapBinlogEventTest_UpdatePhrases() {
        List<PhrasesTableChange> phrasesTableChanges = new ArrayList<>();

        PhrasesTableChange phrasesTableChange = new PhrasesTableChange().withPid(1L).withCid(2L);
        phrasesTableChange.addChangedColumn(Tables.PHRASES.STATUS_MODERATE, "New", "Ready");
        phrasesTableChanges.add(phrasesTableChange);

        PhrasesTableChange phrasesTableChange2 = new PhrasesTableChange().withPid(3L).withCid(4L);
        phrasesTableChange2.addChangedColumn(Tables.PHRASES.STATUS_POST_MODERATE, "New", "Ready");
        phrasesTableChanges.add(phrasesTableChange2);

        PhrasesTableChange phrasesTableChange3 = new PhrasesTableChange().withPid(5L).withCid(6L);
        phrasesTableChange3.addChangedColumn(Tables.PHRASES.STATUS_BS_SYNCED, "No", "Yes");
        phrasesTableChanges.add(phrasesTableChange3);

        BinlogEvent binlogEvent = createPhrasesEvent(phrasesTableChanges, UPDATE);

        List<CampaignStatusCorrectCheckObject> resultObjects = rule.mapBinlogEvent(binlogEvent);
        CampaignStatusCorrectCheckObject[] expected = new CampaignStatusCorrectCheckObject[]{
                new CampaignStatusCorrectCheckObject(phrasesTableChange.cid),
                new CampaignStatusCorrectCheckObject(phrasesTableChange2.cid)
        };

        assertThat(resultObjects).hasSize(2);
        assertThat(resultObjects).containsExactly(expected);
    }
}
