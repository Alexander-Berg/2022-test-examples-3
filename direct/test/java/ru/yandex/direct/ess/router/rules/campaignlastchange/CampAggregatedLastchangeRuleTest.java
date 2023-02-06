package ru.yandex.direct.ess.router.rules.campaignlastchange;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.ess.common.utils.TablesEnum;
import ru.yandex.direct.ess.logicobjects.campaignlastchange.CampAggregatedLastchangeObject;
import ru.yandex.direct.ess.router.configuration.TestConfiguration;
import ru.yandex.direct.ess.router.models.TEssEvent;
import ru.yandex.direct.ess.router.testutils.BannerImagesTableChange;
import ru.yandex.direct.ess.router.testutils.BannersTableChange;
import ru.yandex.direct.ess.router.testutils.PhrasesTableChange;
import ru.yandex.grut.objects.proto.client.Schema.EObjectType;
import ru.yandex.grut.objects.proto.client.Schema.TBannerCandidateMeta;
import ru.yandex.grut.watchlog.Watch.TEvent;
import ru.yandex.inside.yt.kosher.common.YtTimestamp;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.binlog.model.Operation.DELETE;
import static ru.yandex.direct.binlog.model.Operation.INSERT;
import static ru.yandex.direct.binlog.model.Operation.UPDATE;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_IMAGES;
import static ru.yandex.direct.dbschema.ppc.Tables.PHRASES;
import static ru.yandex.direct.ess.router.testutils.BannerImagesTableChange.createBannerImagesEvent;
import static ru.yandex.direct.ess.router.testutils.BannersTableChange.createBannersEvent;
import static ru.yandex.direct.ess.router.testutils.PhrasesTableChange.createPhrasesEvent;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
class CampAggregatedLastchangeRuleTest {

    @Autowired
    private CampAggregatedLastchangeRule rule;

    @Test
    void mapWatchlogEventTest() {
        LocalDateTime utcNow = LocalDateTime.now().minusHours(3).withNano(0);
        var ytTimestamp = YtTimestamp.fromInstant(utcNow.toInstant(ZoneOffset.UTC)).getValue();
        var bannerCandidateMeta = TBannerCandidateMeta.newBuilder()
                .setId(1L)
                .setDirectId(2L)
                .setAdGroupId(3L)
                .setCampaignId(4L)
                .build();

        TEssEvent watchlogEvent = TEssEvent.newBuilder()
                .setEvent(TEvent.newBuilder()
                        .setObjectMeta(bannerCandidateMeta.toByteString())
                        .setTimestamp(ytTimestamp)
                        .build())
                .setDirectCampaignId(5L)
                .setObjectType(EObjectType.OT_BANNER_CANDIDATE)
                .build();

        CampAggregatedLastchangeObject expected = new CampAggregatedLastchangeObject(TablesEnum.BANNER_CANDIDATES, 2L, 5L, utcNow.plusHours(3));

        List<CampAggregatedLastchangeObject> resultObjects = rule.mapWatchlogEvent(watchlogEvent);
        assertThat(resultObjects).hasSize(1);
        assertThat(resultObjects).containsExactly(expected);
    }

    @Test
    void mapBinlogEventTest_InsertIntoBanners() {
        List<BannersTableChange> bannersTableChanges = new ArrayList<>();
        BannersTableChange bannersTableChange1 = new BannersTableChange().withBid(1L).withCid(2L).withPid(3L);
        bannersTableChange1.addInsertedColumn(BANNERS.LAST_CHANGE, LocalDateTime.of(2018, 1, 2, 12, 12, 12));
        bannersTableChanges.add(bannersTableChange1);

        BannersTableChange bannersTableChange2 = new BannersTableChange().withBid(2L).withCid(3L).withPid(4L);
        bannersTableChange2.addInsertedColumn(BANNERS.LAST_CHANGE, LocalDateTime.of(2018, 1, 2, 12, 12, 13));
        bannersTableChanges.add(bannersTableChange2);

        BinlogEvent binlogEvent = createBannersEvent(bannersTableChanges, INSERT);
        List<CampAggregatedLastchangeObject> resultObjects = rule.mapBinlogEvent(binlogEvent);
        CampAggregatedLastchangeObject[] expected =
                bannersTableChanges.stream()
                        .map(tableChange -> new CampAggregatedLastchangeObject(TablesEnum.BANNERS, tableChange.bid,
                                tableChange.cid,
                                (LocalDateTime) tableChange.getColumnValues(BANNERS.LAST_CHANGE).after))
                        .toArray(CampAggregatedLastchangeObject[]::new);
        assertThat(resultObjects).hasSize(2);
        assertThat(resultObjects).containsExactly(expected);
    }

    @Test
    void mapBinlogEventTest_DeleteFromBanners() {
        List<BannersTableChange> bannersTableChanges = new ArrayList<>();
        LocalDateTime localDateTime = LocalDateTime.of(2018, 1, 2, 12, 12, 12);
        BannersTableChange bannersTableChange1 = new BannersTableChange().withBid(1L).withCid(2L).withPid(3L);
        bannersTableChange1.addDeletedColumn(BANNERS.LAST_CHANGE, localDateTime);
        bannersTableChanges.add(bannersTableChange1);

        BannersTableChange bannersTableChange2 = new BannersTableChange().withBid(2L).withCid(3L).withPid(4L);
        bannersTableChange2.addDeletedColumn(BANNERS.LAST_CHANGE, localDateTime.plusSeconds(1));
        bannersTableChanges.add(bannersTableChange2);


        BinlogEvent binlogEvent = createBannersEvent(bannersTableChanges, DELETE);

        LocalDateTime binlogUtcTimestamp = LocalDateTime.of(2019, 5, 1, 0, 0);
        LocalDateTime binlogMskTime = LocalDateTime.of(2019, 5, 1, 3, 0);
        binlogEvent.withUtcTimestamp(binlogUtcTimestamp);

        List<CampAggregatedLastchangeObject> resultObjects = rule.mapBinlogEvent(binlogEvent);

        CampAggregatedLastchangeObject[] expected =
                bannersTableChanges.stream()
                        .map(tableChange -> new CampAggregatedLastchangeObject(TablesEnum.BANNERS, tableChange.bid,
                                tableChange.cid, binlogMskTime))
                        .toArray(CampAggregatedLastchangeObject[]::new);
        assertThat(resultObjects).hasSize(2);
        assertThat(resultObjects).containsExactly(expected);
    }

    @Test
    void mapBinlogEventTest_UpdateBanners() {
        List<BannersTableChange> bannersTableChanges = new ArrayList<>();
        LocalDateTime successLocalDateTime = LocalDateTime.of(2018, 1, 2, 12, 12, 14);

        /* Поля lastChange нет в списке изменившихся полей */
        BannersTableChange bannersTableChangeWithoutLastChange =
                new BannersTableChange().withBid(1L).withCid(2L).withPid(3L);
        bannersTableChanges.add(bannersTableChangeWithoutLastChange);

        /* Поле lastChange поменялось, только это изменение должно учесться в результате */
        BannersTableChange bannersTableChangeWithLastChange =
                new BannersTableChange().withBid(2L).withCid(3L).withPid(4L);
        bannersTableChangeWithLastChange.addChangedColumn(BANNERS.LAST_CHANGE, successLocalDateTime,
                successLocalDateTime.plusSeconds(1));
        bannersTableChanges.add(bannersTableChangeWithLastChange);

        /* Поле lastChange есть в списке изменившихся полей, но его значение не изменилось */
        BannersTableChange bannersTableChangeLastChangeEquals =
                new BannersTableChange().withBid(2L).withCid(4L).withPid(4L);
        LocalDateTime localDateTime = LocalDateTime.of(2018, 1, 2, 12, 12, 12);
        bannersTableChangeLastChangeEquals.addChangedColumn(BANNERS.LAST_CHANGE, localDateTime, localDateTime);

        BinlogEvent binlogEvent = createBannersEvent(bannersTableChanges, UPDATE);
        List<CampAggregatedLastchangeObject> resultObjects = rule.mapBinlogEvent(binlogEvent);
        CampAggregatedLastchangeObject[] expected = new CampAggregatedLastchangeObject[]{
                new CampAggregatedLastchangeObject(TablesEnum.BANNERS, 2L, 3L,
                        successLocalDateTime.plusSeconds(1))
        };

        assertThat(resultObjects).hasSize(1);
        assertThat(resultObjects).containsExactly(expected);
    }

    @Test
    void mapBinlogEventTest_InsertIntoBannerImages() {
        List<BannerImagesTableChange> bannerImagesTableChanges = new ArrayList<>();
        LocalDateTime localDateTime = LocalDateTime.of(2018, 1, 2, 12, 12, 12);
        BannerImagesTableChange bannerImagesTableChange1 = new BannerImagesTableChange().withImageId(1L);
        bannerImagesTableChange1.addInsertedColumn(BANNER_IMAGES.BID, 2L);
        bannerImagesTableChange1.addInsertedColumn(BANNER_IMAGES.DATE_ADDED, localDateTime);
        bannerImagesTableChanges.add(bannerImagesTableChange1);

        BannerImagesTableChange bannerImagesTableChange2 = new BannerImagesTableChange().withImageId(2L);
        bannerImagesTableChange2.addInsertedColumn(BANNER_IMAGES.BID, 3L);
        bannerImagesTableChange2.addInsertedColumn(BANNER_IMAGES.DATE_ADDED, localDateTime.plusSeconds(1));
        bannerImagesTableChanges.add(bannerImagesTableChange2);

        BinlogEvent binlogEvent = createBannerImagesEvent(bannerImagesTableChanges, INSERT);

        List<CampAggregatedLastchangeObject> resultObjects = rule.mapBinlogEvent(binlogEvent);
        CampAggregatedLastchangeObject[] expected =
                bannerImagesTableChanges.stream()
                        .map(tableChange -> new CampAggregatedLastchangeObject(TablesEnum.BANNER_IMAGES,
                                tableChange.imageId, (Long) tableChange.getColumnValues(BANNER_IMAGES.BID).after,
                                (LocalDateTime) tableChange.getColumnValues(BANNER_IMAGES.DATE_ADDED).after))
                        .toArray(CampAggregatedLastchangeObject[]::new);
        assertThat(resultObjects).hasSize(2);
        assertThat(resultObjects).containsExactly(expected);
    }

    @Test
    void mapBinlogEventTest_DeleteFromBannerImages() {
        List<BannerImagesTableChange> bannerImagesTableChanges = new ArrayList<>();
        LocalDateTime localDateTime = LocalDateTime.of(2018, 1, 2, 12, 12, 12);
        BannerImagesTableChange bannerImagesTableChange1 = new BannerImagesTableChange().withImageId(1L);
        bannerImagesTableChange1.addDeletedColumn(BANNER_IMAGES.BID, 2L);
        bannerImagesTableChange1.addDeletedColumn(BANNER_IMAGES.DATE_ADDED, localDateTime);
        bannerImagesTableChanges.add(bannerImagesTableChange1);

        BannerImagesTableChange bannerImagesTableChange2 = new BannerImagesTableChange().withImageId(2L);
        bannerImagesTableChange2.addDeletedColumn(BANNER_IMAGES.BID, 3L);
        bannerImagesTableChange2.addDeletedColumn(BANNER_IMAGES.DATE_ADDED, localDateTime.plusSeconds(1));
        bannerImagesTableChanges.add(bannerImagesTableChange2);

        BinlogEvent binlogEvent = createBannerImagesEvent(bannerImagesTableChanges, DELETE);

        LocalDateTime binlogUtcTimestamp = LocalDateTime.of(2019, 5, 1, 0, 0);
        LocalDateTime binlogMskTime = LocalDateTime.of(2019, 5, 1, 3, 0);
        binlogEvent.withUtcTimestamp(binlogUtcTimestamp);

        List<CampAggregatedLastchangeObject> resultObjects = rule.mapBinlogEvent(binlogEvent);

        CampAggregatedLastchangeObject[] expected =
                bannerImagesTableChanges.stream()
                        .map(tableChange -> new CampAggregatedLastchangeObject(TablesEnum.BANNER_IMAGES,
                                tableChange.imageId, (Long) tableChange.getColumnValues(BANNER_IMAGES.BID).before,
                                binlogMskTime))
                        .toArray(CampAggregatedLastchangeObject[]::new);
        assertThat(resultObjects).hasSize(2);
        assertThat(resultObjects).containsExactly(expected);
    }

    @Test
    void mapBinlogEventTest_UpdateBannerImages() {
        List<BannerImagesTableChange> bannerImagesTableChanges = new ArrayList<>();
        LocalDateTime successLocalDateTime = LocalDateTime.of(2018, 1, 2, 12, 12, 14);

        /* Поле dateAdded изменяется, только это изменение должно учесться */
        BannerImagesTableChange bannerImagesTableChangeDateAdded = new BannerImagesTableChange().withImageId(1L);
        bannerImagesTableChangeDateAdded.addChangedColumn(BANNER_IMAGES.DATE_ADDED, successLocalDateTime,
                successLocalDateTime.plusSeconds(1));
        bannerImagesTableChanges.add(bannerImagesTableChangeDateAdded);

        /* Поля dateAdded нет в списке полей */
        BannerImagesTableChange bannerImagesTableChangeWithoutDateAdded = new BannerImagesTableChange().withImageId(2L);
        bannerImagesTableChanges.add(bannerImagesTableChangeWithoutDateAdded);

        /* Поле dateAdded есть в списке полей, но изначения до изменений и после одинаковое */
        BannerImagesTableChange bannerImagesTableDateAddedEquals = new BannerImagesTableChange().withImageId(1L);
        LocalDateTime localDateTime = LocalDateTime.of(2018, 1, 2, 12, 12, 12);
        bannerImagesTableDateAddedEquals.addChangedColumn(BANNER_IMAGES.DATE_ADDED, localDateTime, localDateTime);
        bannerImagesTableChanges.add(bannerImagesTableDateAddedEquals);

        BinlogEvent binlogEvent = createBannerImagesEvent(bannerImagesTableChanges, UPDATE);

        List<CampAggregatedLastchangeObject> resultObjects = rule.mapBinlogEvent(binlogEvent);

        CampAggregatedLastchangeObject[] expected = new CampAggregatedLastchangeObject[]{
                new CampAggregatedLastchangeObject(TablesEnum.BANNER_IMAGES, 1L, null,
                        successLocalDateTime.plusSeconds(1))
        };
        assertThat(resultObjects).hasSize(1);
        assertThat(resultObjects).containsExactly(expected);
    }

    @Test
    void mapBinlogEventTest_InsertIntoPhrases() {
        List<PhrasesTableChange> phrasesTableChanges = new ArrayList<>();
        LocalDateTime localDateTime = LocalDateTime.of(2018, 1, 2, 12, 12, 12);

        PhrasesTableChange phrasesTableChange1 = new PhrasesTableChange().withPid(1L).withCid(2L);
        phrasesTableChange1.addInsertedColumn(PHRASES.LAST_CHANGE, localDateTime);
        phrasesTableChanges.add(phrasesTableChange1);

        PhrasesTableChange phrasesTableChange2 = new PhrasesTableChange().withPid(2L).withCid(3L);
        phrasesTableChange2.addInsertedColumn(PHRASES.LAST_CHANGE, localDateTime);
        phrasesTableChanges.add(phrasesTableChange2);

        BinlogEvent binlogEvent = createPhrasesEvent(phrasesTableChanges, INSERT);

        List<CampAggregatedLastchangeObject> resultObjects = rule.mapBinlogEvent(binlogEvent);
        CampAggregatedLastchangeObject[] expected =
                phrasesTableChanges.stream()
                        .map(tableChange -> new CampAggregatedLastchangeObject(TablesEnum.PHRASES, tableChange.pid,
                                tableChange.cid,
                                (LocalDateTime) tableChange.getColumnValues(PHRASES.LAST_CHANGE).after))
                        .toArray(CampAggregatedLastchangeObject[]::new);
        assertThat(resultObjects).hasSize(2);
        assertThat(resultObjects).containsExactly(expected);
    }

    @Test
    void mapBinlogEventTest_DeleteFromPhrases() {
        List<PhrasesTableChange> phrasesTableChanges = new ArrayList<>();
        LocalDateTime localDateTime = LocalDateTime.of(2018, 1, 2, 12, 12, 12);

        PhrasesTableChange phrasesTableChange1 = new PhrasesTableChange().withPid(1L).withCid(2L);
        phrasesTableChange1.addDeletedColumn(PHRASES.LAST_CHANGE, localDateTime);
        phrasesTableChanges.add(phrasesTableChange1);

        PhrasesTableChange phrasesTableChange2 = new PhrasesTableChange().withPid(2L).withCid(3L);
        phrasesTableChange2.addDeletedColumn(PHRASES.LAST_CHANGE, localDateTime);
        phrasesTableChanges.add(phrasesTableChange2);

        BinlogEvent binlogEvent = createPhrasesEvent(phrasesTableChanges, DELETE);
        LocalDateTime binlogUtcTimestamp = LocalDateTime.of(2019, 5, 1, 0, 0);
        LocalDateTime binlogMskTime = LocalDateTime.of(2019, 5, 1, 3, 0);
        binlogEvent.withUtcTimestamp(binlogUtcTimestamp);

        List<CampAggregatedLastchangeObject> resultObjects = rule.mapBinlogEvent(binlogEvent);
        CampAggregatedLastchangeObject[] expected =
                phrasesTableChanges.stream()
                        .map(tableChange -> new CampAggregatedLastchangeObject(TablesEnum.PHRASES, tableChange.pid,
                                tableChange.cid, binlogMskTime))
                        .toArray(CampAggregatedLastchangeObject[]::new);

        assertThat(resultObjects).hasSize(2);
        assertThat(resultObjects).containsExactly(expected);
    }

    @Test
    void mapBinlogEventTest_UpdatePhrases() {
        List<PhrasesTableChange> phrasesTableChanges = new ArrayList<>();
        LocalDateTime localDateTime = LocalDateTime.of(2018, 1, 2, 12, 12, 12);
        LocalDateTime successLocalDateTime = LocalDateTime.of(2018, 1, 2, 12, 12, 14);

        /* Поля lastChange нет в списке изменившихся полей */
        PhrasesTableChange phrasesTableChangeWithoutLastChange = new PhrasesTableChange().withPid(1L).withCid(2L);
        phrasesTableChanges.add(phrasesTableChangeWithoutLastChange);

        /* Поле lastChange поменялось, только это изменение должно учесться в результате */
        PhrasesTableChange phrasesTableChangeWithLastChange = new PhrasesTableChange().withPid(2L).withCid(3L);
        phrasesTableChangeWithLastChange.addChangedColumn(PHRASES.LAST_CHANGE, successLocalDateTime,
                successLocalDateTime.plusSeconds(1));
        phrasesTableChanges.add(phrasesTableChangeWithLastChange);

        /* Поле lastChange есть в списке изменившихся полей, но его значение не изменилось */
        PhrasesTableChange phrasesTableChangeLastChangeEquals = new PhrasesTableChange().withPid(3L).withCid(4L);
        phrasesTableChangeLastChangeEquals.addChangedColumn(PHRASES.LAST_CHANGE, localDateTime, localDateTime);
        phrasesTableChanges.add(phrasesTableChangeLastChangeEquals);

        BinlogEvent binlogEvent = createPhrasesEvent(phrasesTableChanges, UPDATE);

        List<CampAggregatedLastchangeObject> resultObjects = rule.mapBinlogEvent(binlogEvent);
        CampAggregatedLastchangeObject[] expected = new CampAggregatedLastchangeObject[]{
                new CampAggregatedLastchangeObject(TablesEnum.PHRASES, 2L, 3L, successLocalDateTime.plusSeconds(1))
        };

        assertThat(resultObjects).hasSize(1);
        assertThat(resultObjects).containsExactly(expected);
    }

}
