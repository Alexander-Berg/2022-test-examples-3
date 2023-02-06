package ru.yandex.direct.ess.router.rules.bsexport.resources;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.binlog.model.Operation;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject;
import ru.yandex.direct.ess.router.configuration.TestConfiguration;
import ru.yandex.direct.ess.router.testutils.BannerNamesChange;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppc.tables.BannerNames.BANNER_NAMES;
import static ru.yandex.direct.ess.router.testutils.BannerNamesChange.createBannerNamesEvent;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
class BannerNamesTest {

    @Autowired
    private BsExportBannerResourcesRule rule;

    @Test
    void insertNotSuitableModerationStatusTest() {
        BannerNamesChange bannerNamesChangeSending = new BannerNamesChange().withBid(1L);
        bannerNamesChangeSending.addInsertedColumn(BANNER_NAMES.STATUS_MODERATE, "Sending");
        BannerNamesChange bannerNamesChangeSent = new BannerNamesChange().withBid(2L);
        bannerNamesChangeSent.addInsertedColumn(BANNER_NAMES.STATUS_MODERATE, "Sent");

        BannerNamesChange bannerNamesChangeReady = new BannerNamesChange().withBid(3L);
        bannerNamesChangeReady.addInsertedColumn(BANNER_NAMES.STATUS_MODERATE, "Ready");

        BannerNamesChange bannerNamesChangeNew = new BannerNamesChange().withBid(4L);
        bannerNamesChangeNew.addInsertedColumn(BANNER_NAMES.STATUS_MODERATE, "New");

        var changes = List.of(
                bannerNamesChangeSending,
                bannerNamesChangeSent,
                bannerNamesChangeReady,
                bannerNamesChangeNew);
        var binlogEvent = createBannerNamesEvent(changes, Operation.INSERT);
        var objects = rule.mapBinlogEvent(binlogEvent);
        assertThat(objects).isEmpty();
    }

    @Test
    void insertSuitableModerationTest() {
        BannerNamesChange bannerNamesChangeYes = new BannerNamesChange().withBid(1L);
        bannerNamesChangeYes.addInsertedColumn(BANNER_NAMES.STATUS_MODERATE, "Yes");

        BannerNamesChange bannerNamesChangeNo = new BannerNamesChange().withBid(2L);
        bannerNamesChangeNo.addInsertedColumn(BANNER_NAMES.STATUS_MODERATE, "No");

        var binlogEvent = createBannerNamesEvent(List.of(bannerNamesChangeYes, bannerNamesChangeNo),
                Operation.INSERT);
        var objects = rule.mapBinlogEvent(binlogEvent);

        var expectedObjects = List.of(
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_NAME)
                        .build(),
                new BsExportBannerResourcesObject.Builder()
                        .setBid(2L)
                        .setResourceType(BannerResourceType.BANNER_NAME)
                        .build()
        );
        assertThat(objects).hasSize(2);
        assertThat(objects)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedObjects.toArray(BsExportBannerResourcesObject[]::new));
    }

    @Test
    void updateNotSuitableModerationStatusTest() {
        BannerNamesChange bannerNamesChangeSending = new BannerNamesChange().withBid(1L);
        bannerNamesChangeSending.addChangedColumn(BANNER_NAMES.STATUS_MODERATE, "Ready", "Sending");
        BannerNamesChange bannerNamesChangeSent = new BannerNamesChange().withBid(2L);
        bannerNamesChangeSent.addChangedColumn(BANNER_NAMES.STATUS_MODERATE, "Sending", "Sent");

        BannerNamesChange bannerNamesChangeReady = new BannerNamesChange().withBid(3L);
        bannerNamesChangeReady.addChangedColumn(BANNER_NAMES.STATUS_MODERATE, "New", "Ready");

        BannerNamesChange bannerNamesChangeNew = new BannerNamesChange().withBid(4L);
        bannerNamesChangeNew.addChangedColumn(BANNER_NAMES.STATUS_MODERATE, "Yes", "New");

        var changes = List.of(
                bannerNamesChangeSending,
                bannerNamesChangeSent,
                bannerNamesChangeReady,
                bannerNamesChangeNew);
        var binlogEvent = createBannerNamesEvent(changes, Operation.INSERT);
        var objects = rule.mapBinlogEvent(binlogEvent);
        assertThat(objects).isEmpty();
    }

    @Test
    void updateSuitableModerationTest() {
        BannerNamesChange bannerNamesChangeYes = new BannerNamesChange().withBid(1L);
        bannerNamesChangeYes.addChangedColumn(BANNER_NAMES.STATUS_MODERATE, "Sent", "Yes");

        BannerNamesChange bannerNamesChangeNo = new BannerNamesChange().withBid(2L);
        bannerNamesChangeNo.addChangedColumn(BANNER_NAMES.STATUS_MODERATE, "Yes", "No");

        var binlogEvent = createBannerNamesEvent(List.of(bannerNamesChangeYes, bannerNamesChangeNo),
                Operation.INSERT);
        var objects = rule.mapBinlogEvent(binlogEvent);

        var expectedObjects = List.of(
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_NAME)
                        .build(),
                new BsExportBannerResourcesObject.Builder()
                        .setBid(2L)
                        .setResourceType(BannerResourceType.BANNER_NAME)
                        .build()
        );
        assertThat(objects).hasSize(2);
        assertThat(objects)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedObjects.toArray(BsExportBannerResourcesObject[]::new));
    }

    @Test
    void deleteTest() {
        BannerNamesChange bannerNamesChange = new BannerNamesChange().withBid(1L);

        var binlogEvent = createBannerNamesEvent(List.of(bannerNamesChange), Operation.DELETE);
        var objects = rule.mapBinlogEvent(binlogEvent);

        var expectedObjects = List.of(
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_NAME)
                        .setDeleted(true)
                        .build()
        );
        assertThat(objects).hasSize(1);
        assertThat(objects)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedObjects.toArray(BsExportBannerResourcesObject[]::new));
    }

    @Test
    void updateNameTest() {
        BannerNamesChange bannerNamesChangeName = new BannerNamesChange().withBid(1L);
        bannerNamesChangeName.addChangedColumn(BANNER_NAMES.NAME, "name1", "name2");

        var binlogEvent = createBannerNamesEvent(List.of(bannerNamesChangeName), Operation.UPDATE);
        var objects = rule.mapBinlogEvent(binlogEvent);

        var expectedObjects = List.of(
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_NAME)
                        .build()
        );
        assertThat(objects).hasSize(1);
        assertThat(objects)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedObjects.toArray(BsExportBannerResourcesObject[]::new));
    }
}
